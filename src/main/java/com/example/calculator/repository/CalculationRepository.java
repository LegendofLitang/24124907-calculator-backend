package com.example.calculator.repository;

import com.example.calculator.entity.Calculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 计算历史 Repository。
 */
public interface CalculationRepository extends JpaRepository<Calculation, Long> {

    /** 按计算时间倒序分页查询。 */
    Page<Calculation> findAllByOrderByCalculatedAtDesc(Pageable pageable);

    /**
     * 按表达式关键字模糊查询（SQL LIKE %keyword%），时间倒序分页。
     * 用于历史记录搜索扩展功能。
     */
    Page<Calculation> findByExpressionContainingOrderByCalculatedAtDesc(String keyword, Pageable pageable);
}
