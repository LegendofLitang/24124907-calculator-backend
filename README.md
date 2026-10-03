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

## 关联仓库

前端仓库：`24124907-calculator-frontend`
