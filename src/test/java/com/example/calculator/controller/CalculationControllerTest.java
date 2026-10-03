package com.example.calculator.controller;

import com.example.calculator.repository.CalculationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 集成测试：覆盖主要成功与失败场景。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalculationRepository repository;

    @BeforeEach
    void cleanDatabase() {
        // 每个测试方法独立数据，避免 @SpringBootTest 共享内存库导致记录累积
        repository.deleteAll();
    }

    // ---------- 成功场景 ----------
    @Test
    @DisplayName("POST 成功计算并保存")
    void calculateSuccess() throws Exception {
        mockMvc.perform(post("/api/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\": \"10 / (2 + 3)\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.expression").value("10 / (2 + 3)"))
                .andExpect(jsonPath("$.result").value(2.0))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    @DisplayName("GET 分页历史倒序")
    void historyPage() throws Exception {
        mockMvc.perform(post("/api/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expression\": \"1 + 1\"}"));
        mockMvc.perform(post("/api/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expression\": \"2 + 2\"}"));

        mockMvc.perform(get("/api/calculations").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].expression").value("2 + 2"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("DELETE 单条记录")
    void deleteOne() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\": \"3 * 3\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        long id = mapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete("/api/calculations/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE 清空历史")
    void clearAll() throws Exception {
        mockMvc.perform(delete("/api/calculations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").isNumber());
    }

    // ---------- 失败场景 ----------
    @Test
    @DisplayName("空表达式返回 400 结构化错误")
    void emptyExpression() throws Exception {
        mockMvc.perform(post("/api/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/calculations"));
    }

    @Test
    @DisplayName("除零返回 400")
    void divideByZero() throws Exception {
        mockMvc.perform(post("/api/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\": \"1 / 0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("除数不能为零")));
    }

    @Test
    @DisplayName("非法字符返回 400")
    void illegalChars() throws Exception {
        mockMvc.perform(post("/api/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\": \"1 + abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("不支持的字符")));
    }

    @Test
    @DisplayName("括号不匹配返回 400")
    void unmatchedParen() throws Exception {
        mockMvc.perform(post("/api/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\": \"(1 + 2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("括号不匹配")));
    }

    @Test
    @DisplayName("缺少 expression 字段返回 400")
    void missingField() throws Exception {
        mockMvc.perform(post("/api/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("请求体不是 JSON 返回 400")
    void notJson() throws Exception {
        mockMvc.perform(post("/api/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("删除不存在的记录返回 404")
    void deleteMissing() throws Exception {
        mockMvc.perform(delete("/api/calculations/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("历史记录不存在: id=999999"));
    }
}
