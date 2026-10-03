package com.example.calculator.service;

import com.example.calculator.dto.CalculationResponse;
import com.example.calculator.dto.PageResponse;
import com.example.calculator.entity.Calculation;
import com.example.calculator.exception.ExpressionException;
import com.example.calculator.exception.ResourceNotFoundException;
import com.example.calculator.repository.CalculationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 计算服务测试：使用真实 H2（JPA 自动配置）验证保存、查询、删除、清空与失败记录。
 */
@DataJpaTest
@Import(CalculationService.class)
class CalculationServiceTest {

    @Autowired
    private CalculationService service;

    @Autowired
    private CalculationRepository repository;

    @Test
    @DisplayName("成功计算并保存历史记录")
    void calculateAndSave() {
        CalculationResponse resp = service.calculate("2 + 3");
        assertNotNull(resp.getId());
        assertEquals("2 + 3", resp.getExpression());
        assertEquals(5.0, resp.getResult());
        assertTrue(resp.isSuccess());
        assertNull(resp.getErrorMessage());

        assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("除零时保存失败记录并抛出异常")
    void divideByZeroSavesFailureRecord() {
        ExpressionException ex = assertThrows(ExpressionException.class,
                () -> service.calculate("1 / 0"));
        assertTrue(ex.getMessage().contains("除数不能为零"));

        assertEquals(1, repository.count());
        Calculation saved = repository.findAll().get(0);
        assertFalse(saved.isSuccess());
        assertNull(saved.getResult());
        assertTrue(saved.getErrorMessage().contains("除数不能为零"));
    }

    @Test
    @DisplayName("空表达式抛出异常且不保存记录")
    void emptyExpressionNotSaved() {
        assertThrows(ExpressionException.class, () -> service.calculate(""));
        assertEquals(0, repository.count());
    }

    @Test
    @DisplayName("历史记录按时间倒序分页查询")
    void historyOrderedByTimeDesc() {
        service.calculate("1 + 1");
        service.calculate("2 + 2");
        service.calculate("3 + 3");

        PageResponse<CalculationResponse> page = service.getHistory(0, 10);
        assertEquals(3, page.getTotalElements());
        List<CalculationResponse> content = page.getContent();
        assertEquals("3 + 3", content.get(0).getExpression());
        assertEquals("2 + 2", content.get(1).getExpression());
        assertEquals("1 + 1", content.get(2).getExpression());
    }

    @Test
    @DisplayName("按表达式关键字搜索历史记录（扩展功能）")
    void searchHistoryByKeyword() {
        service.calculate("1 + 1");
        service.calculate("2 + 2");
        service.calculate("10 + 1");

        // 命中两条：表达式含 "+ 1"
        PageResponse<CalculationResponse> hit = service.getHistory(0, 10, "+ 1");
        assertEquals(2, hit.getTotalElements());
        for (CalculationResponse c : hit.getContent()) {
            assertTrue(c.getExpression().contains("+ 1"));
        }

        // 无命中
        assertEquals(0, service.getHistory(0, 10, "999").getTotalElements());

        // 空关键字 / null 等同于不筛选
        assertEquals(3, service.getHistory(0, 10, "   ").getTotalElements());
        assertEquals(3, service.getHistory(0, 10, null).getTotalElements());
        assertEquals(3, service.getHistory(0, 10).getTotalElements());
    }

    @Test
    @DisplayName("分页参数边界处理")
    void pageBounds() {
        for (int i = 1; i <= 5; i++) {
            service.calculate(i + " + " + 1);
        }
        PageResponse<CalculationResponse> page = service.getHistory(0, 100);
        assertEquals(5, page.getTotalElements());
        assertEquals(0, page.getPage());
        assertEquals(100, page.getSize());

        // 超出范围的页不报错
        PageResponse<CalculationResponse> beyond = service.getHistory(99, 10);
        assertTrue(beyond.getContent().isEmpty());
    }

    @Test
    @DisplayName("删除单条历史记录")
    void deleteById() {
        CalculationResponse resp = service.calculate("1 + 1");
        service.deleteById(resp.getId());
        assertEquals(0, repository.count());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteById(resp.getId()));
    }

    @Test
    @DisplayName("清空全部历史记录")
    void clearAll() {
        service.calculate("1 + 1");
        service.calculate("2 + 2");
        long deleted = service.clearAll();
        assertEquals(2, deleted);
        assertEquals(0, repository.count());
    }
}
