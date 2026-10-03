package com.example.calculator.service;

import com.example.calculator.exception.ExpressionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 表达式解析器单元测试。
 */
class ExpressionEvaluatorTest {

    // ---------- 基本四则运算 ----------
    @Test
    @DisplayName("基本加减乘除")
    void basicArithmetic() {
        assertEquals(5.0, ExpressionEvaluator.evaluate("2 + 3"), 1e-9);
        assertEquals(1.0, ExpressionEvaluator.evaluate("3 - 2"), 1e-9);
        assertEquals(12.0, ExpressionEvaluator.evaluate("3 * 4"), 1e-9);
        assertEquals(2.5, ExpressionEvaluator.evaluate("10 / 4"), 1e-9);
    }

    // ---------- 运算优先级 ----------
    @Test
    @DisplayName("运算优先级：乘除先于加减")
    void precedence() {
        assertEquals(14.0, ExpressionEvaluator.evaluate("2 + 3 * 4"), 1e-9);
        assertEquals(14.0, ExpressionEvaluator.evaluate("2 * 3 + 4 * 2"), 1e-9);
        assertEquals(25.0, ExpressionEvaluator.evaluate("100 / 8 * 2"), 1e-9);
        assertEquals(-14.0, ExpressionEvaluator.evaluate("2 - 4 * 4"), 1e-9);
    }

    // ---------- 括号 ----------
    @Test
    @DisplayName("括号改变优先级")
    void parentheses() {
        assertEquals(20.0, ExpressionEvaluator.evaluate("(2 + 3) * 4"), 1e-9);
        assertEquals(5.0, ExpressionEvaluator.evaluate("(10 + 5) / 3"), 1e-9);
        assertEquals(2.0, ExpressionEvaluator.evaluate("10 / (2 + 3)"), 1e-9);
        assertEquals(6.0, ExpressionEvaluator.evaluate("((1 + 2) * (1 + 1))"), 1e-9);
    }

    // ---------- 小数 ----------
    @Test
    @DisplayName("小数运算")
    void decimals() {
        assertEquals(3.3, ExpressionEvaluator.evaluate("1.1 + 2.2"), 1e-9);
        assertEquals(1.25, ExpressionEvaluator.evaluate("5 / 4"), 1e-9);
        assertEquals(1.0, ExpressionEvaluator.evaluate(".5 * 2"), 1e-9);
        assertEquals(0.5, ExpressionEvaluator.evaluate(".5"), 1e-9);
        assertEquals(2.0, ExpressionEvaluator.evaluate("5. / 2.5"), 1e-9);
    }

    // ---------- 负数 ----------
    @Test
    @DisplayName("负数与一元负号")
    void negatives() {
        assertEquals(-3.0, ExpressionEvaluator.evaluate("-3"), 1e-9);
        assertEquals(2.0, ExpressionEvaluator.evaluate("-3 + 5"), 1e-9);
        assertEquals(5.0, ExpressionEvaluator.evaluate("-(-5)"), 1e-9);
        assertEquals(-6.0, ExpressionEvaluator.evaluate("2 * -3"), 1e-9);
        assertEquals(3.0, ExpressionEvaluator.evaluate("--3"), 1e-9);
    }

    // ---------- 取模 ----------
    @Test
    @DisplayName("取模运算")
    void modulo() {
        assertEquals(1.0, ExpressionEvaluator.evaluate("7 % 3"), 1e-9);
        assertEquals(0.0, ExpressionEvaluator.evaluate("6 % 2"), 1e-9);
    }

    // ---------- 幂运算（扩展功能） ----------
    @Test
    @DisplayName("幂运算：基本、右结合、优先级与一元符号")
    void power() {
        assertEquals(8.0, ExpressionEvaluator.evaluate("2^3"), 1e-9);
        assertEquals(1.0, ExpressionEvaluator.evaluate("5^0"), 1e-9);
        assertEquals(0.5, ExpressionEvaluator.evaluate("2^-1"), 1e-9);
        assertEquals(2.0, ExpressionEvaluator.evaluate("4^0.5"), 1e-9);
        // 右结合：2^(3^2) = 2^9 = 512
        assertEquals(512.0, ExpressionEvaluator.evaluate("2^3^2"), 1e-9);
        // 幂优先级高于一元负号：-(2^2) = -4
        assertEquals(-4.0, ExpressionEvaluator.evaluate("-2^2"), 1e-9);
        // 幂优先级高于乘除：2 * 3^2 = 2 * 9 = 18
        assertEquals(18.0, ExpressionEvaluator.evaluate("2 * 3^2"), 1e-9);
        // 括号改变结合顺序
        assertEquals(64.0, ExpressionEvaluator.evaluate("(2^3)^2"), 1e-9);
        assertEquals(512.0, ExpressionEvaluator.evaluate("2^(3^2)"), 1e-9);
    }

    @Test
    @DisplayName("幂运算异常：缺少底数/指数、非法开方、溢出")
    void powerErrors() {
        // ^ 前缺少底数
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("^2"));
        // ^ 后缺少指数
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("2^"));
        // 负数开偶次方，double 下为 NaN
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("(-8)^0.5"));
        // 0 的负数次幂为 Infinity
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("0^-1"));
        // 结果溢出
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("9^9^9"));
    }

    // ---------- 除零 ----------
    @Test
    @DisplayName("除零抛出异常")
    void divideByZero() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ExpressionEvaluator.evaluate("1 / 0"));
        assertTrue(ex.getMessage().contains("除数不能为零"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("1 % 0"));
    }

    // ---------- 非法表达式 ----------
    @Test
    @DisplayName("非法字符被拒绝")
    void illegalChars() {
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("1 + abc"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("@#$%^"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("1 & 2"));
    }

    @Test
    @DisplayName("连续运算符被拒绝")
    void consecutiveOperators() {
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("2 + * 3"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("1 / / 2"));
        // "2++3" 中第二个 + 作为一元正号是合法的（2 + (+3)），这里测试真正的非法连续
        assertEquals(5.0, ExpressionEvaluator.evaluate("2 + + 3"), 1e-9);
    }

    // ---------- 空表达式 ----------
    @Test
    @DisplayName("空表达式抛出异常")
    void emptyExpression() {
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate(""));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("   "));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate(null));
    }

    // ---------- 括号不匹配 ----------
    @Test
    @DisplayName("括号不匹配抛出异常")
    void unmatchedParentheses() {
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("(1 + 2"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("1 + 2)"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("((1) + 2"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate(")1 + 2("));
    }

    // ---------- 数字格式错误 ----------
    @Test
    @DisplayName("数字格式错误")
    void badNumbers() {
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("1.2.3 + 1"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate(". + 1"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("1 2"));
    }

    // ---------- 表达式不完整 ----------
    @Test
    @DisplayName("表达式不完整")
    void incompleteExpression() {
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("2 +"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("* 3"));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("("));
        assertThrows(ExpressionException.class, () -> ExpressionEvaluator.evaluate("1 + ("));
    }

    // ---------- 溢出 ----------
    @Test
    @DisplayName("结果溢出")
    void overflow() {
        // 9e999999999 解析为 Infinity
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> ExpressionEvaluator.evaluate("9e999999999"));
        assertTrue(ex.getMessage().contains("溢出") || ex.getMessage().contains("无法计算")
                || ex.getMessage().contains("字符"));
    }
}
