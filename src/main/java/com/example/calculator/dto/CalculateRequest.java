package com.example.calculator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 计算请求 DTO。
 */
public class CalculateRequest {

    @NotBlank(message = "表达式不能为空")
    @Size(max = 500, message = "表达式过长")
    private String expression;

    public CalculateRequest() {
    }

    public CalculateRequest(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}