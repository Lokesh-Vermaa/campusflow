package com.jklu.campusflow.auth;

import com.jklu.campusflow.auth.dto.*;
import com.jklu.campusflow.common.error.ErrorResponse;
import com.jklu.campusflow.common.security.JwtAuthenticationFilter;
import com.jklu.campusflow.common.security.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String clientIp = resolveClientIp(request);
        AuthService.LoginResult result = authService.login(loginRequest, clientIp);

        boolean isSecure = request.isSecure();
        ResponseCookie authCookie = ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, result.token())
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();

        ResponseCookie sessionCookie = ResponseCookie.from(JwtAuthenticationFilter.ALT_COOKIE_NAME, result.token())
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        return ResponseEntity.ok(new LoginResponse(result.profile(), result.token()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            ErrorResponse error = new ErrorResponse(
                    "UNAUTHORIZED",
                    "Session expired or unauthenticated",
                    RequestIdFilter.getCurrentRequestId()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        String email = authentication.getName();
        UserProfileResponse profile = authService.getProfileByEmail(email);
        return ResponseEntity.ok(new UserResponse(profile));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletResponse response) {
        ResponseCookie clearAuthCookie = ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        ResponseCookie clearSessionCookie = ResponseCookie.from(JwtAuthenticationFilter.ALT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearAuthCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearSessionCookie.toString());

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute("_csrf");
        }

        String tokenValue = csrfToken != null ? csrfToken.getToken() : "csrf-token-initialized";
        String headerName = csrfToken != null ? csrfToken.getHeaderName() : "X-CSRF-TOKEN";

        return ResponseEntity.ok(new CsrfResponse(tokenValue, headerName));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
