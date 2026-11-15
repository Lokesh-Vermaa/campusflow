package com.jklu.campusflow.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        String id,
        String email,
        String name,
        String role,
        String department,
        String avatarUrl
) {}
