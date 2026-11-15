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
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, Deque<Instant>> requestLogByIp = new ConcurrentHashMap<>();
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
        if (isRateLimited(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse error = new ErrorResponse(
                    "GLOBAL_RATE_LIMIT_EXCEEDED",
                    "Global request rate limit exceeded. Please wait 60 seconds.",
                    RequestIdFilter.getCurrentRequestId()
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        Deque<Instant> timestamps = requestLogByIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            Instant threshold = Instant.now().minusSeconds(WINDOW_SECONDS);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
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
}
