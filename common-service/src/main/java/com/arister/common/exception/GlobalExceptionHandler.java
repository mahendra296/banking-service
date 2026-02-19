package com.arister.common.exception;

import com.arister.common.dto.ApiResponse;
import com.arister.common.dto.ErrorDetails;
import com.arister.common.enums.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.error("ResourceNotFoundException: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode(ex.getErrorCode().getCode())
                .errorMessage(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .error(errorDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({BusinessException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.error("BusinessException: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode(ex.getErrorCode().getCode())
                .errorMessage(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .error(errorDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode(CommonErrorCode.VALIDATION_ERROR.getCode())
                .errorMessage("Validation failed for one or more fields")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .error(errorDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("IllegalArgumentException: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode(CommonErrorCode.BAD_REQUEST.getCode())
                .errorMessage(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .error(errorDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .errorCode(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .errorMessage("An unexpected error occurred. Please try again later.")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message("Internal server error")
                .data(null)
                .error(errorDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
