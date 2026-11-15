package com.jklu.campusflow.auth;

import com.jklu.campusflow.auth.dto.*;
import com.jklu.campusflow.common.error.ErrorResponse;
import com.jklu.campusflow.common.security.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String clientIp = resolveClientIp(request);
        UserProfileResponse user = authService.login(loginRequest, clientIp);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.email(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.role()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        request.getSession(true);
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok(new LoginResponse(user));
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
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        ResponseCookie clearSessionCookie = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearSessionCookie.toString());

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
