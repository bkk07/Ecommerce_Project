package com.ecommerce.productservice.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404")
    void shouldHandleResourceNotFoundExceptionWith404() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Product not found");

        // When
        ProblemDetail problem = handler.handleNotFound(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("Product not found");
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should handle ProductValidationException with 400")
    void shouldHandleProductValidationExceptionWith400() {
        // Given
        ProductValidationException ex = new ProductValidationException("Invalid product data");

        // When
        ProblemDetail problem = handler.handleProductValidation(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Invalid product data");
        assertThat(problem.getTitle()).isEqualTo("Product Validation Failed");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with 400")
    void shouldHandleMethodArgumentNotValidExceptionWith400() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("product", "name", "must not be blank");
        FieldError fieldError2 = new FieldError("product", "price", "must be positive");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ProblemDetail problem = handler.handleValidation(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).contains("name: must not be blank");
        assertThat(problem.getDetail()).contains("price: must be positive");
        assertThat(problem.getTitle()).isEqualTo("Validation Failed");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400")
    void shouldHandleIllegalArgumentExceptionWith400() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Each variant must have exactly one primary image");

        // When
        ProblemDetail problem = handler.handleIllegalArgument(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Each variant must have exactly one primary image");
        assertThat(problem.getTitle()).isEqualTo("Invalid Request");
    }

    @Test
    @DisplayName("Should handle IllegalStateException with 409")
    void shouldHandleIllegalStateExceptionWith409() {
        // Given
        IllegalStateException ex = new IllegalStateException("Cannot delete category with sub-categories");

        // When
        ProblemDetail problem = handler.handleIllegalState(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).isEqualTo("Cannot delete category with sub-categories");
        assertThat(problem.getTitle()).isEqualTo("Invalid State");
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 and NOT expose internal details")
    void shouldHandleGenericExceptionWith500AndNotExposeSensitiveInfo() {
        // Given
        Exception ex = new RuntimeException("Connection refused: mysql://localhost:3306 password=secret123");

        // When
        ProblemDetail problem = handler.handleGeneral(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        
        // CRITICAL: Should NOT expose the original exception message
        assertThat(problem.getDetail()).doesNotContain("mysql");
        assertThat(problem.getDetail()).doesNotContain("password");
        assertThat(problem.getDetail()).doesNotContain("secret123");
        assertThat(problem.getDetail()).doesNotContain("Connection refused");
        
        // Should contain error ID for tracking
        assertThat(problem.getDetail()).contains("error ID");
        assertThat(problem.getProperties()).containsKey("errorId");
    }

    @Test
    @DisplayName("Should include error ID in 500 response for tracking")
    void shouldIncludeErrorIdFor500Response() {
        // Given
        Exception ex = new NullPointerException("Something went wrong internally");

        // When
        ProblemDetail problem = handler.handleGeneral(ex);

        // Then
        assertThat(problem.getProperties().get("errorId")).isNotNull();
        assertThat(problem.getProperties().get("errorId").toString()).hasSize(8);
        assertThat(problem.getDetail()).contains(problem.getProperties().get("errorId").toString());
    }
}
