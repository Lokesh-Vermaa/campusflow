package com.jklu.campusflow.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.auth.dto.LoginRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class RateLimitTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @AfterEach
    void tearDown() {
        loginRateLimiter.resetAll();
    }

    @Test
    @DisplayName("Verify rate limiting triggers 429 after 5 failed login attempts")
    void testRateLimitingOnLogin() throws Exception {
        String testIp = "192.168.1.100";
        LoginRequest badRequest = new LoginRequest("student@demo.jklu.test", "wrongpassword");

        // First 5 attempts should return 401 Unauthorized
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
        }

        // 6th attempt must be blocked with 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }
}
