package com.jklu.campusflow.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        UserProfileResponse user,
        String token
) {
    public LoginResponse(UserProfileResponse user) {
        this(user, null);
    }
}
