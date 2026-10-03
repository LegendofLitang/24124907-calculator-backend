package com.example.calculator.exception;

/**
 * 资源不存在，映射为 HTTP 404（例如查询/删除不存在的历史记录）。
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}