package com.jklu.campusflow.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String requestId,
        Map<String, String> details
) {
    public ErrorResponse(String code, String message, String requestId) {
        this(code, message, requestId, null);
    }
}
