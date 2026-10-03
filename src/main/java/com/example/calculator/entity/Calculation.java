package com.example.calculator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 计算历史记录实体。
 *
 * <p>无论计算成功还是失败（除零、溢出等），都会保存一条记录，
 * 通过 {@code success} 与 {@code errorMessage} 区分结果。
 */
@Entity
@Table(name = "calculations")
public class Calculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户输入的原始表达式。 */
    @Column(nullable = false, length = 500)
    private String expression;

    /** 计算结果；计算失败时为 null。 */
    private Double result;

    /** 计算是否成功。 */
    @Column(nullable = false)
    private boolean success;

    /** 失败时的错误信息；成功时为 null。 */
    @Column(length = 500)
    private String errorMessage;

    /** 计算时间。 */
    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    protected Calculation() {
        // JPA 需要无参构造
    }

    public Calculation(String expression, Double result, boolean success,
                       String errorMessage, LocalDateTime calculatedAt) {
        this.expression = expression;
        this.result = result;
        this.success = success;
        this.errorMessage = errorMessage;
        this.calculatedAt = calculatedAt;
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

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
