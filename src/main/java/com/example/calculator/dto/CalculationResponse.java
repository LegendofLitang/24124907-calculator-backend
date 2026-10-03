package com.example.calculator.dto;

import com.example.calculator.entity.Calculation;

import java.time.LocalDateTime;

/**
 * 计算记录响应 DTO（单条记录）。
 */
public class CalculationResponse {

    private Long id;
    private String expression;
    private Double result;
    private LocalDateTime calculatedAt;
    private boolean success;
    private String errorMessage;

    public CalculationResponse() {
    }

    public CalculationResponse(Long id, String expression, Double result,
                               LocalDateTime calculatedAt, boolean success,
                               String errorMessage) {
        this.id = id;
        this.expression = expression;
        this.result = result;
        this.calculatedAt = calculatedAt;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static CalculationResponse from(Calculation c) {
        return new CalculationResponse(
                c.getId(),
                c.getExpression(),
                c.getResult(),
                c.getCalculatedAt(),
                c.isSuccess(),
                c.getErrorMessage());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Double getResult() {
        return result;
    }

    public void setResult(Double result) {
        this.result = result;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
