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
}
