# 后端代码规范（Java）

> **规范来源**
> 本文档以 **[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)** 为主要依据，
> 并结合 **Alibaba Java 开发手册（嵩山版）** 中的工程实践条目整理而成。
> 本文所列规则为上述两套规范在本项目中的**落地约定**，与来源文档冲突时以来源文档为准。

---

## 1. 源文件基础

### 1.1 文件编码与换行

- 一律使用 **UTF-8** 编码，`pom.xml` 中通过 `project.build.sourceEncoding` 显式声明。
- 换行符统一为 **LF**（`\n`），由 `.gitattributes` 约束，避免跨平台差异。
- 文件末尾保留**一个**换行符。

### 1.2 文件结构顺序

每个源文件按以下顺序组织，各部分之间空**一个**空行：

1. 许可证 / 版权声明（如有）
2. `package` 语句
3. `import` 语句
4. 类级 Javadoc
5. `class` / `interface` 声明

```java
package com.example.calculator.service;

import com.example.calculator.exception.ExpressionException;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全表达式解析器。
 */
public final class ExpressionEvaluator {
    // ...
}
```

### 1.3 import 规范

- **禁止**使用通配符导入（`import java.util.*;`），必须逐个列出。
- **禁止**未使用的 import。
- 分组顺序，组间空一行：
  1. 本项目包（`com.example.calculator.*`）
  2. 第三方库（`org.springframework.*`、`jakarta.*`）
  3. JDK（`java.*`、`javax.*`）
- 每组内部按**字典序**排列。

---

## 2. 命名规范

| 类型 | 规则 | 示例 |
|---|---|---|
| 包名 | 全小写，点分隔，不使用下划线 | `com.example.calculator.service` |
| 类名 / 接口名 | **UpperCamelCase** 大驼峰 | `CalculationService`、`ExpressionEvaluator` |
| 方法名 | **lowerCamelCase** 小驼峰，动词开头 | `calculate`、`getHistory`、`deleteById` |
| 局部变量 | lowerCamelCase | `safePage`、`divisor`、`tokens` |
| 成员变量 | lowerCamelCase，**不加**前缀 | `repository`、`expression` |
| 常量 | **UPPER_SNAKE_CASE** 全大写加下划线 | `MAX_EXPRESSION_LENGTH` |
| 泛型类型参数 | 单个大写字母 | `T`、`E` |
| 测试方法 | lowerCamelCase，描述被测行为 | `divideByZeroSavesFailureRecord` |

**禁止**：

- 拼音命名（`jisuan`）、无意义缩写（`calcSvc`）、单字母变量（循环变量 `i`/`j` 除外）
- 成员变量加 `m_` / `_` 前缀

---

## 3. 代码格式

### 3.1 缩进与行宽

- 缩进使用 **4 个空格**，**禁止使用 Tab**。
- 单行不超过 **100** 个字符（本项目实际控制在 **120** 以内，与原规范略有放宽，原因是中文注释占位较多）。

### 3.2 大括号

- 左大括号**不换行**（K&R 风格），即使方法体只有一条语句也**不允许省略**大括号。
- `if` / `else` / `for` / `while` 后必须跟空格再接括号。

```java
// 正确
if (divisor == 0.0) {
    throw new ExpressionException("除数不能为零");
}

// 错误：省略大括号
if (divisor == 0.0) throw new ExpressionException("除数不能为零");
```

### 3.3 空行使用

- 方法之间空**一个**空行。
- 方法内部的逻辑段落之间可用空行分隔，但**不使用连续两个以上空行**。

### 3.4 空格

| 位置 | 规则 |
|---|---|
| 运算符两侧 | `left = left + parseTerm();` |
| 逗号之后 | `new Calculation(expression, result, success)` |
| 强制类型转换后 | `(String) value` |
| `if` / `for` / `while` 关键字与括号间 | `if (condition)` |

---

## 4. 分层与包结构

项目采用经典分层架构，**每个包职责单一**：

```
com.example.calculator/
├── CalculatorApplication.java   启动类，不写业务逻辑
├── controller/                  REST 接口层：只做参数接收与响应封装
├── service/                     业务逻辑层：事务边界、流程编排
├── repository/                  数据访问层：只定义查询方法
├── entity/                      数据库实体：与表结构一一对应
├── dto/                         数据传输对象：请求/响应模型
├── exception/                   自定义异常 + 全局异常处理
└── config/                      框架配置
```

**强制约定**：

- Controller **不写业务逻辑**，只调用 Service。
- Controller **禁止**直接注入 Repository。
- Entity **禁止**直接作为 API 返回值，必须转换为 DTO。
- Service 层方法如需事务，显式标注 `@Transactional`；只读方法标注 `@Transactional(readOnly = true)`。

```java
// 正确：Controller 只做转发
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public CalculationResponse calculate(@Valid @RequestBody CalculateRequest request) {
    return service.calculate(request.getExpression());
}
```

---

## 5. 注释规范

### 5.1 Javadoc

- **所有** `public` 类、`public` 方法必须写 Javadoc。
- Javadoc 首句为概要，以句号结尾。
- 使用 `@param`、`@return`、`@throws` 标注参数、返回值与异常。

```java
/**
 * 计算表达式并保存历史记录。
 *
 * <p>表达式语法非法时抛出 {@link ExpressionException}，不保存任何记录。
 *
 * @param expression 用户输入表达式
 * @return 计算记录响应
 * @throws ExpressionException 表达式为空、非法字符、括号不匹配、除零、溢出等
 */
@Transactional
public CalculationResponse calculate(String expression) {
```

### 5.2 行内注释

- 只解释**为什么**，不解释**是什么**。
- 使用 `//` 加一个空格。

```java
// 正确：解释设计原因
// 使用文件库而非内存库：历史记录是本系统的核心数据，需在重启后保留
url: jdbc:h2:file:./data/calculatordb

// 错误：复述代码
// 把 i 加 1
i++;
```

### 5.3 TODO 规范

临时代码必须标注，格式：`// TODO(姓名): 待办内容`。

---

## 6. 异常处理规范

- **禁止**捕获异常后不做任何处理（吞异常）。
- **禁止**使用 `e.printStackTrace()`，统一使用 SLF4J 日志。
- 业务异常继承 `RuntimeException`，由 `@RestControllerAdvice` 统一转换。
- 兜底 `@ExceptionHandler(Exception.class)` 必须记录 error 级日志，且**不向客户端暴露堆栈**。

```java
private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, WebRequest request) {
    log.error("未处理异常", ex);           // 服务端记录完整堆栈
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误", request);  // 客户端只见概要
}
```

---

## 7. 测试规范

- 测试类命名：`<被测类名>Test`，放在与主代码**相同包路径**下的 `src/test/java`。
- 测试方法必须加 `@DisplayName`，用中文描述**被测行为**而非方法名。
- 每个测试方法只验证**一个**行为。
- 方法名格式：`<场景><期望结果>`，例如 `divideByZeroSavesFailureRecord`。

```java
@Test
@DisplayName("除零时抛出异常")
void divideByZero() {
    ExpressionException ex = assertThrows(ExpressionException.class,
            () -> ExpressionEvaluator.evaluate("1 / 0"));
    assertTrue(ex.getMessage().contains("除数不能为零"));
}
```

---

## 8. 工具与自动化

| 工具 | 用途 |
|---|---|
| Maven | 构建与依赖管理 |
| JUnit 5 | 单元测试 / 集成测试 |
| Spring Boot Test + MockMvc | REST 接口测试 |

常用命令：

```bash
mvn test             # 运行全部测试
mvn clean package    # 打包
```
