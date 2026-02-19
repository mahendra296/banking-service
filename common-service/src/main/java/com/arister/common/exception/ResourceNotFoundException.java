package com.arister.common.exception;

import com.arister.common.enums.CommonErrorCode;
import com.arister.common.enums.ErrorCode;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = CommonErrorCode.RESOURCE_NOT_FOUND;
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.errorCode = CommonErrorCode.RESOURCE_NOT_FOUND;
    }
}
