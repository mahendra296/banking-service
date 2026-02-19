package com.arister.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    RESOURCE_NOT_FOUND("ERR_001", "Resource not found"),
    RESOURCE_ALREADY_EXISTS("ERR_002", "Resource already exists"),
    VALIDATION_ERROR("ERR_003", "Validation error"),
    BAD_REQUEST("ERR_004", "Bad request"),
    INTERNAL_SERVER_ERROR("ERR_005", "Internal server error"),
    DUPLICATE_RESOURCE("ERR_006", "Duplicate resource"),
    UNAUTHORIZED("ERR_007", "Unauthorized"),
    FORBIDDEN("ERR_008", "Forbidden");

    private final String code;
    private final String message;
}
