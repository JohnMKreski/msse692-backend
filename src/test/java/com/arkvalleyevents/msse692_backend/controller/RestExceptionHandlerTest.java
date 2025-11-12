package com.arkvalleyevents.msse692_backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.endsWith;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import com.arkvalleyevents.msse692_backend.config.AppUserUpsertFilter;


/**
 * Focused tests for RestExceptionHandler structured error responses.
 */
@WebMvcTest(controllers = TestErrorController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AppUserUpsertFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class RestExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // Security is excluded for this slice test; no additional beans required

    @Test
    @DisplayName("ConstraintViolationException returns structured 400 with details and requestId")
    void constraintViolation() throws Exception {
        mockMvc.perform(get("/test/violation")
                .header("X-Request-ID", "test-rid-1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("CONSTRAINT_VIOLATION"))
            .andExpect(jsonPath("$.details[0].field").value(endsWith("value")))
            .andExpect(jsonPath("$.details[0].message").value("must be greater than 0"))
            .andExpect(jsonPath("$.requestId").value("test-rid-1"));
    }

    @Test
    @DisplayName("NoSuchElementException returns 404 NOT_FOUND payload")
    void notFound() throws Exception {
        mockMvc.perform(get("/test/not-found")
                .header("X-Request-ID", "test-rid-2")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Item not found"))
            .andExpect(jsonPath("$.path").value("/test/not-found"))
            .andExpect(jsonPath("$.requestId").value("test-rid-2"));
    }

    @Test
    @DisplayName("Generic exception falls back to INTERNAL_ERROR")
    void generic() throws Exception {
        mockMvc.perform(get("/test/generic")
                .header("X-Request-ID", "test-rid-3")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.requestId").value("test-rid-3"));
    }
}
