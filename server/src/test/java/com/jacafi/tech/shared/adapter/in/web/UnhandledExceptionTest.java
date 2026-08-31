package com.jacafi.tech.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

@DisplayName("an unhandled exception")
class UnhandledExceptionTest {

    @RestController
    static class ExplodingController {

        @GetMapping("/boom")
        String boom() {

            String value = null;
            return value.trim();
        }

        @GetMapping("/business-failure")
        String businessFailure() {
            throw new SensitiveBusinessException();
        }

        @GetMapping("/optimistic-lock-failure")
        String optimisticLockFailure() {
            throw new JpaOptimisticLockingFailureException(new OptimisticLockException("submitted-secret"));
        }
    }

    static class SensitiveBusinessException extends BusinessException {

        SensitiveBusinessException() {
            super(ErrorCode.DATA_CONFLICT, "submitted-secret", null);
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ExplodingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("answers 500 with a status, a code, a generic sentence and a trace id")
    void answersTheMinimum() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("GEN-001"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("Erro interno. Informe o identificador de rastreio."))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("carries no stack trace, no class name and no exception name")
    void carriesNothingElse() throws Exception {
        String body = mockMvc.perform(get("/boom")).andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("NullPointerException")
                .doesNotContain("java.lang")
                .doesNotContain("com.jacafi")
                .doesNotContain("ExplodingController")
                .doesNotContain("at ")
                .doesNotContain(".java:")
                .doesNotContain("Cannot invoke");
    }

    @Test
    @DisplayName("answers exactly the agreed properties, and no others")
    void answersNoExtraProperties() throws Exception {

        mockMvc.perform(get("/boom"))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("answers a business exception from the stable catalogue without its message")
    void answersBusinessExceptionsWithoutTheirMessage() throws Exception {
        String body = mockMvc.perform(get("/business-failure"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("GEN-005"))
                .andExpect(jsonPath("$.detail").value("A operação conflita com dados já registrados."))
                .andExpect(jsonPath("$.traceId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("submitted-secret");
    }

    @Test
    @DisplayName("answers a JPA optimistic locking failure as a safe conflict")
    void answersOptimisticLockingFailuresAsSafeConflicts() throws Exception {
        String body = mockMvc.perform(get("/optimistic-lock-failure"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("GEN-005"))
                .andExpect(jsonPath("$.detail").value("A operação conflita com dados já registrados."))
                .andExpect(jsonPath("$.traceId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("submitted-secret");
    }
}
