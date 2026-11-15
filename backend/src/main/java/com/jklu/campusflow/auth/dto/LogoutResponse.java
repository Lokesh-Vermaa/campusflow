package com.jklu.campusflow.auth.dto;

public record LogoutResponse(
        boolean success,
        String message
) {}
