package com.arister.common.dto;

import com.arister.common.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private ErrorDetails error;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .error(null)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String errorMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(errorMessage)
                .data(null)
                .error(ErrorDetails.builder()
                        .errorCode(errorCode.getCode())
                        .errorMessage(errorMessage)
                        .build())
                .build();
    }
}
