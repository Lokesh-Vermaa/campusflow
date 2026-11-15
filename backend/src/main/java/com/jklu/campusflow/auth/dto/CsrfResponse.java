package com.jklu.campusflow.auth.dto;

public record CsrfResponse(
        String token,
        String headerName
) {}
