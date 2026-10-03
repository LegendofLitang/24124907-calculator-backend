# 计算器系统 · 后端

学号：24124907 ｜ 作者：LegendofLitang

基于 **Java 17 + Spring Boot 3.3 + Spring Data JPA + H2** 的计算器后端，提供 REST API
与安全表达式解析（词法分析 + 递归下降，**不使用 eval / ScriptEngine**，杜绝注入）。

## 技术栈

| 层 | 技术 |
| ---- | ---- |
| 后端 | Java 17 · Spring Boot 3.3 · Spring Web（REST） |
| 持久化 | Spring Data JPA · H2 数据库（文件库） |
| 校验 | Spring Boot Validation（Jakarta Bean Validation） |
| 构建 | Maven |
| 测试 | JUnit 5 · Spring Boot Test · MockMvc（32 个用例） |

## 项目结构

```
src/main/java/com/example/calculator/
├── CalculatorApplication.java   启动类
├── controller/                  CalculationController      REST 接口层
├── service/                     CalculationService         业务逻辑
│                                ExpressionEvaluator       表达式解析器（核心）
├── repository/                  CalculationRepository      数据访问
├── entity/                      Calculation                历史记录实体
├── dto/                         CalculateRequest / CalculationResponse / PageResponse
├── exception/                   ExpressionException / ResourceNotFoundException
│                                GlobalExceptionHandler     全局异常处理
└── config/                      WebConfig                  CORS 配置
```

## 运行

```bash
mvn spring-boot:run
```

- 服务地址：http://localhost:8080
- H2 控制台：http://localhost:8080/h2-console
  （JDBC URL：`jdbc:h2:file:./data/calculatordb`，用户 `sa`，空密码）

首次启动会在项目根目录创建 `data/` 存放 H2 文件数据库，历史记录重启不丢失。

## 数据库初始化

本项目使用 **H2 文件型数据库**，无需手动安装数据库、无需执行建表 SQL。

| 项 | 说明 |
| ---- | ---- |
| 数据库类型 | H2（嵌入式文件库） |
| 数据文件位置 | 项目根目录 `./data/calculatordb.mv.db` |
| JDBC URL | `jdbc:h2:file:./data/calculatordb;AUTO_SERVER=TRUE` |
| 用户名 / 密码 | `sa` / 空 |
| 建表方式 | Hibernate 自动建表（`spring.jpa.hibernate.ddl-auto=update`） |

**初始化步骤：**

1. 确认已安装 **JDK 17**：`java -version`
2. 直接启动应用，Hibernate 会自动创建 `data/` 目录与 `calculations` 表
3. 用浏览器打开 H2 控制台验证：
   - 地址：http://localhost:8080/h2-console
   - JDBC URL 填 `jdbc:h2:file:./data/calculatordb`
   - 用户名 `sa`，密码留空，点击 **Connect**
   - 执行 `SELECT * FROM CALCULATIONS;` 可看到历史数据

**数据表结构（由实体 `Calculation.java` 自动生成）：**

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | BIGINT | 主键，自增 |
| `expression` | VARCHAR(500) | 计算表达式，非空 |
| `result` | DOUBLE | 计算结果，失败时为 NULL |
| `success` | BOOLEAN | 计算是否成功，非空 |
| `error_message` | VARCHAR(500) | 失败原因，成功时为 NULL |
| `calculated_at` | TIMESTAMP | 计算时间，非空 |

**重置数据库**：停止应用后删除项目根目录的 `data/` 文件夹，重新启动即可得到全新数据库。

**改用内存库**（每次启动数据清空，仅用于演示）：把 `src/main/resources/application.yml` 中的 url 改为
`jdbc:h2:mem:calculatordb` 即可。

## 配置说明

主要配置集中在 `src/main/resources/application.yml`：

| 配置项 | 默认值 | 说明 |
| ---- | ---- | ---- |
| `server.port` | `8080` | 服务端口，被占用时改这里 |
| `spring.datasource.url` | `jdbc:h2:file:./data/calculatordb` | 数据库连接串 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 自动建表策略 |
| `spring.h2.console.enabled` | `true` | 是否开启 H2 控制台 |
| `logging.level.com.example.calculator` | `debug` | 本项目包的日志级别 |

**CORS 配置**：位于 `src/main/java/com/example/calculator/config/WebConfig.java`，
已对 `/api/**` 放开跨域，因此前端可以独立部署在任意域名下直接调用本后端。

## 前后端对接方式

前端通过 **HTTP + JSON** 调用本后端的 REST API，两者完全独立、可分别部署。

```
浏览器（前端 index.html + app.js）
      │  fetch  POST / GET / DELETE  →  /api/calculations
      ▼
Spring Boot 后端（本仓库）  →  H2 数据库
```

- **请求格式**：`Content-Type: application/json`，body 形如 `{"expression": "1+2*3"}`
- **响应格式**：JSON，字段见下方「REST API」
- **核心约定**：**表达式由前端原样发送，计算结果完全在后端产生**，前端只负责展示
- **前端如何配置后端地址**：在前端仓库 `app.js` 顶部的 `API_BASE` 变量中设置
  - 由本后端提供页面时（同源 8080）留空字符串，走相对路径
  - 前端独立部署时填本后端的公网地址，例如 `https://your-backend.example.com`
- **错误处理**：后端所有错误统一返回 `{timestamp, status, error, message, path}`，
  前端读取 `message` 字段展示给用户

## REST API

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/api/calculations` | 计算表达式并保存历史，body：`{"expression":"10/(2+3)"}` |
| GET | `/api/calculations?page=0&size=10` | 分页查询历史（按时间倒序） |
| DELETE | `/api/calculations/{id}` | 删除单条记录 |
| DELETE | `/api/calculations` | 清空全部历史 |

**成功响应（201）**

```json
{
  "id": 1,
  "expression": "10 / (2 + 3)",
  "result": 2.0,
  "calculatedAt": "2026-09-28T12:00:00",
  "success": true,
  "errorMessage": null
}
```

**错误响应（400，统一结构）**

```json
{
  "timestamp": "2026-09-28T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "除数不能为零",
  "path": "/api/calculations"
}
```

## 表达式语法

- 运算符：`+` `-` `*` `/` `%`
- 括号：`(` `)`
- 数字：整数、小数（支持 `.5`、`5.`），支持负数与一元正负号（`-3`、`-(-5)`、`+3`）
- 优先级：括号 > 一元正负号 > `* / %` > `+ -`，同级从左到右
- 限制：表达式最长 500 字符，不接受科学计数法（`1e5`）

## 测试

```bash
mvn test
```

覆盖：四则运算、优先级、括号、小数、负数、取模、除零、非法字符、空表达式、
括号不匹配、连续运算符、数字格式错误、溢出、历史保存/分页/删除/清空，
以及 Controller 的成功与失败场景。

## 部署

评测期间后端部署地址：`<部署后填写，例如 http://1.2.3.4:8080>`

推荐部署方式：

**方式一：云服务器（推荐，文件系统天然持久）**

1. 购买阿里云 / 腾讯云轻量应用服务器（学生机即可），系统选 Ubuntu 22.04 或 Windows Server
2. 安装 **JDK 17**
3. 本地上传打包好的 `target/calculator-system-1.0.0.jar` 到服务器
4. 服务器上执行：`java -jar calculator-system-1.0.0.jar`
5. 在云控制台**安全组放行 8080 端口**
6. 浏览器访问 `http://<公网IP>:8080` 验证

> 因为 H2 是文件数据库，用云服务器部署时历史记录会保存在服务器磁盘上，**重启不丢**，
> 满足作业「重启后历史数据不丢失」的要求。

**方式二：容器平台（Railway / Render / Koyeb）**

- ⚠️ 容器平台默认文件系统是临时的，**必须挂载持久化卷（Volume）**指向 `./data`，
  否则每次重新部署 H2 数据都会清空
- 启动命令：`java -jar target/calculator-system-1.0.0.jar`
- 平台通常通过环境变量注入端口，必要时在 `application.yml` 中改为 `${PORT:8080}`

## 交付物

| 文件 | 说明 |
| ---- | ---- |
| `README.md` | 本文件：项目简介、技术栈、运行环境、安装启动、配置说明、数据库初始化、前后端对接 |
| `codestyle.md` | 代码规范文档（基于 Google Java Style Guide） |
| `pom.xml` | Maven 构建配置 |
| `src/` | 源码（主代码 + 测试） |

## 关联仓库与规范文档

| 项目 | 地址 |
| ---- | ---- |
| 前端仓库 | `24124907-calculator-frontend` |
| 后端仓库 | `24124907-calculator-backend`（本仓库） |
| 前端代码规范 | `24124907-calculator-frontend` 仓库中的 `codestyle.md` |
| 后端代码规范 | 本仓库中的 [codestyle.md](codestyle.md) |

## 代码规范

本项目遵循 **[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)**，
并结合 Alibaba Java 开发手册整理为项目落地约定，详见 [codestyle.md](codestyle.md)。
