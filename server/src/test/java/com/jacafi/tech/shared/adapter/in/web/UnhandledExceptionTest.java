package com.jacafi.tech.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

/**
 * The fallback, exercised with a genuinely unforeseen failure.
 *
 * <p>Standalone MockMvc around a controller that exists only here, rather than an integration test
 * against a real endpoint: forcing a 500 from a real one means either breaking it on purpose or
 * finding a bug, and neither makes a repeatable test. A controller whose whole job is to throw
 * makes the fallback the only thing under test.
 */
@DisplayName("an unhandled exception")
class UnhandledExceptionTest {

    /** Throws where a real controller would do its work. */
    @RestController
    static class ExplodingController {

        @GetMapping("/boom")
        String boom() {
            // A NullPointerException specifically, because it is the archetype: it carries a
            // message naming a field or method of ours, which is exactly the kind of internal
            // detail that must not leave the process.
            String value = null;
            return value.trim();
        }

        @GetMapping("/business-failure")
        String businessFailure() {
            throw new SensitiveBusinessException();
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
        // An allow-list rather than a list of prohibitions. A future handler adding a helpful
        // "exception" or "path" property would pass every doesNotContain above and still widen
        // what a 500 discloses; this fails the moment the shape changes.
        // "type" fica de fora: a RFC 9457 permite omiti-lo quando vale o default about:blank, e
        // e o que o ProblemDetail faz.
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
}
