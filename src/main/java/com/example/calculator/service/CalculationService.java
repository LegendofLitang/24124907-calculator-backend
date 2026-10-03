package com.example.calculator.service;

import com.example.calculator.dto.CalculationResponse;
import com.example.calculator.dto.PageResponse;
import com.example.calculator.entity.Calculation;
import com.example.calculator.exception.ExpressionException;
import com.example.calculator.exception.ResourceNotFoundException;
import com.example.calculator.repository.CalculationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计算与历史记录服务。
 */
@Service
public class CalculationService {

    private final CalculationRepository repository;

    public CalculationService(CalculationRepository repository) {
        this.repository = repository;
    }

    /**
     * 计算表达式并保存历史记录。
     *
     * <p>表达式语法非法（空、非法字符、括号不匹配、连续运算符、数字格式错误）时抛出
     * {@link ExpressionException}，不保存任何记录；
     * 除零、溢出等"语义错误"同样抛出异常，但会保存一条失败记录（success=false），
     * 便于用户在历史中看到出错的表达式与原因。
     *
     * @param expression 用户输入表达式
     * @return 计算记录响应
     */
    @Transactional
    public CalculationResponse calculate(String expression) {
        if (expression == null) {
            throw new ExpressionException("表达式不能为空");
        }
        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            throw new ExpressionException("表达式不能为空");
        }

        double result;
        try {
            result = ExpressionEvaluator.evaluate(trimmed);
        } catch (ExpressionException e) {
            // 语义错误（除零、溢出）也保存失败记录；语法错误（非法字符等）也可保存，
            // 统一按失败记录处理，便于用户回顾。
            save(trimmed, null, false, e.getMessage());
            throw e;
        }
        return CalculationResponse.from(save(trimmed, result, true, null));
    }

    private Calculation save(String expression, Double result, boolean success, String errorMessage) {
        Calculation c = new Calculation(expression, result, success, errorMessage, LocalDateTime.now());
        return repository.save(c);
    }

    /**
     * 分页查询历史记录，按计算时间倒序。
     */
    @Transactional(readOnly = true)
    public PageResponse<CalculationResponse> getHistory(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(Math.min(size, 100), 1);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "calculatedAt", "id"));
        Page<Calculation> result = repository.findAllByOrderByCalculatedAtDesc(pageable);
        Page<CalculationResponse> dtoPage = result.map(CalculationResponse::from);
        return PageResponse.from(dtoPage);
    }

    /**
     * 删除单条历史记录；不存在时抛出 404。
     */
    @Transactional
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("历史记录不存在: id=" + id);
        }
        repository.deleteById(id);
    }

    /**
     * 清空全部历史记录，返回删除条数。
     */
    @Transactional
    public long clearAll() {
        long count = repository.count();
        repository.deleteAll();
        return count;
    }
}
