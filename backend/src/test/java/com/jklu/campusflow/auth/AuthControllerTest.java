package com.jklu.campusflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.auth.dto.LoginRequest;
import com.jklu.campusflow.common.security.JwtAuthenticationFilter;
import com.jklu.campusflow.common.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/auth/login with valid credentials returns 200, user profile, and sets HttpOnly cookies")
    void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest("student@demo.jklu.test", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("student@demo.jklu.test"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.name").value("Aarav Sharma"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(cookie().exists(JwtAuthenticationFilter.AUTH_COOKIE_NAME))
                .andExpect(cookie().httpOnly(JwtAuthenticationFilter.AUTH_COOKIE_NAME, true))
                .andExpect(cookie().exists(JwtAuthenticationFilter.ALT_COOKIE_NAME));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid password returns 401 BAD_CREDENTIALS")
    void testLoginInvalidPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("student@demo.jklu.test", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/auth/me returns 200 with profile when authenticated via cookie")
    void testGetMeAuthenticatedViaCookie() throws Exception {
        String token = jwtService.generateToken(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "student@demo.jklu.test",
                "STUDENT"
        );

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie(JwtAuthenticationFilter.AUTH_COOKIE_NAME, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("student@demo.jklu.test"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
    }

    @Test
    @DisplayName("GET /api/auth/me returns 200 with profile when authenticated via Bearer token")
    void testGetMeAuthenticatedViaBearer() throws Exception {
        String token = jwtService.generateToken(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "staff@demo.jklu.test",
                "STAFF"
        );

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("staff@demo.jklu.test"))
                .andExpect(jsonPath("$.user.role").value("STAFF"));
    }

    @Test
    @DisplayName("GET /api/auth/me returns 401 when unauthenticated")
    void testGetMeUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/logout clears auth cookies and returns 200")
    void testLogout() throws Exception {
        String token = jwtService.generateToken(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "student@demo.jklu.test",
                "STUDENT"
        );

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie(JwtAuthenticationFilter.AUTH_COOKIE_NAME, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().maxAge(JwtAuthenticationFilter.AUTH_COOKIE_NAME, 0));
    }

    @Test
    @DisplayName("GET /api/auth/csrf returns token and header name")
    void testGetCsrfToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").isNotEmpty());
    }
}
