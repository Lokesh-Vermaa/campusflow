package com.jklu.campusflow.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.common.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class GlobalRateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 180;
    private static final int MAX_LOGIN_PER_MINUTE = 5;
    private static final int MAX_MESSAGES_PER_MINUTE = 10;
    private static final int MAX_UPLOADS_PER_MINUTE = 5;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, Deque<Instant>> requestLogByIp = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> loginLogByIp = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> messagesLogByIp = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> uploadsLogByIp = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);

        // Path-specific limit: Login (5/min)
        if (path.endsWith("/auth/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            if (isRateLimited(loginLogByIp, ip, MAX_LOGIN_PER_MINUTE)) {
                sendRateLimitError(response, "RATE_LIMIT_EXCEEDED", "Too many login attempts. Please wait 60 seconds.");
                return;
            }
        }

        // Path-specific limit: Messages (10/min)
        if ((path.contains("/agent/chat") || path.contains("/messages")) && "POST".equalsIgnoreCase(request.getMethod())) {
            if (isRateLimited(messagesLogByIp, ip, MAX_MESSAGES_PER_MINUTE)) {
                sendRateLimitError(response, "RATE_LIMIT_EXCEEDED", "Too many messages sent. Please wait 60 seconds.");
                return;
            }
        }

        // Path-specific limit: Uploads (5/min)
        if (path.contains("/upload") && "POST".equalsIgnoreCase(request.getMethod())) {
            if (isRateLimited(uploadsLogByIp, ip, MAX_UPLOADS_PER_MINUTE)) {
                sendRateLimitError(response, "RATE_LIMIT_EXCEEDED", "Upload rate limit reached. Please wait 60 seconds.");
                return;
            }
        }

        // Global limit: 180/min
        if (isRateLimited(requestLogByIp, ip, MAX_REQUESTS_PER_MINUTE)) {
            sendRateLimitError(response, "RATE_LIMIT_EXCEEDED", "Global request rate limit exceeded. Please wait 60 seconds.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse error = new ErrorResponse(
                code,
                message,
                RequestIdFilter.getCurrentRequestId()
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private boolean isRateLimited(Map<String, Deque<Instant>> store, String ip, int maxRequests) {
        Deque<Instant> timestamps = store.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            Instant threshold = Instant.now().minusSeconds(WINDOW_SECONDS);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                return true;
            }
            timestamps.addLast(Instant.now());
            return false;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void resetAll() {
        requestLogByIp.clear();
        loginLogByIp.clear();
        messagesLogByIp.clear();
        uploadsLogByIp.clear();
    }
}
