package com.utown.utownbackend.config;

import com.utown.utownbackend.exception.ResourceConflictException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests the GlobalExceptionHandler by using a stub controller that throws each exception type.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("EntityNotFoundException should return 404 with ProblemDetail")
    void entityNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/entity-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Test entity not found"));
    }

    @Test
    @DisplayName("ResourceConflictException should return 409 with ProblemDetail")
    void resourceConflict_shouldReturn409() throws Exception {
        mockMvc.perform(get("/test/resource-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").value("Test resource conflict"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException should return 400 with field errors")
    void validationError_shouldReturn400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("Unexpected exception should return 500 with sanitized message")
    void unexpectedException_shouldReturn500() throws Exception {
        mockMvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException should return 400 when JSON is malformed")
    void malformedJson_shouldReturn400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TypeMismatchException should return 400 when path parameter cannot be converted")
    void typeMismatch_shouldReturn400() throws Exception {
        mockMvc.perform(get("/test/items/abc"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Stub controller used only for testing exception handler mappings.
     */
    @RestController
    @RequestMapping("/test")
    public static class StubController {

        @GetMapping("/entity-not-found")
        public void entityNotFound() {
            throw new EntityNotFoundException("Test entity not found");
        }

        @GetMapping("/resource-conflict")
        public void resourceConflict() {
            throw new ResourceConflictException("Test resource conflict");
        }

        @PostMapping("/validation")
        public void validation(@Valid @RequestBody StubRequest request) {
            // Validation will trigger before reaching here
        }

        @GetMapping("/unexpected-error")
        public void unexpectedError() {
            throw new RuntimeException("Something broke");
        }

        @GetMapping("/items/{id}")
        public void getItemById(@PathVariable Long id) {
            // Path variable type conversion triggers before reaching here
        }
    }

    record StubRequest(@NotBlank String name) {}
}
