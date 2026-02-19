package com.arister.common.exception;

import com.arister.common.enums.CommonErrorCode;
import com.arister.common.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = CommonErrorCode.BAD_REQUEST;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
