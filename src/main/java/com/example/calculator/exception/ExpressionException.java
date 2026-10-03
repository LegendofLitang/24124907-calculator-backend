package com.example.calculator.exception;

/**
 * 表达式解析/计算错误，映射为 HTTP 400。
 * 覆盖：空表达式、非法字符、括号不匹配、连续运算符、除零、结果溢出、数字格式错误等。
 */
public class ExpressionException extends RuntimeException {

    public ExpressionException(String message) {
        super(message);
    }
}
