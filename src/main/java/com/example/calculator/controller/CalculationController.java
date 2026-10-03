package com.example.calculator.controller;

import com.example.calculator.dto.CalculateRequest;
import com.example.calculator.dto.CalculationResponse;
import com.example.calculator.dto.PageResponse;
import com.example.calculator.service.CalculationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 计算器 REST API。
 */
@RestController
@RequestMapping("/api/calculations")
public class CalculationController {

    private final CalculationService service;

    public CalculationController(CalculationService service) {
        this.service = service;
    }

    /**
     * 计算表达式并保存历史。
     *
     * <pre>
     * POST /api/calculations
     * {"expression": "10 / (2 + 3)"}
     * </pre>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalculationResponse calculate(@Valid @RequestBody CalculateRequest request) {
        return service.calculate(request.getExpression());
    }

    /**
     * 分页查询历史记录（按计算时间倒序）。
     * 例：GET /api/calculations?page=0&size=10
     */
    @GetMapping
    public PageResponse<CalculationResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.getHistory(page, size);
    }

    /**
     * 删除单条历史记录。
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        service.deleteById(id);
    }

    /**
     * 清空全部历史记录。
     */
    @DeleteMapping
    public Map<String, Object> clearAll() {
        long deleted = service.clearAll();
        return Map.of("deleted", deleted);
    }
}
