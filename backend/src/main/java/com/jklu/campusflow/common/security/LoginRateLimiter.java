package com.jklu.campusflow.common.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 15 * 60; // 15 minutes

    private final Map<String, Deque<Instant>> failedAttemptsByIp = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Deque<Instant> attempts = failedAttemptsByIp.get(ip);
        if (attempts == null) {
            return false;
        }

        synchronized (attempts) {
            pruneExpiredAttempts(attempts);
            return attempts.size() >= MAX_FAILED_ATTEMPTS;
        }
    }

    public long getRetryAfterSeconds(String ip) {
        Deque<Instant> attempts = failedAttemptsByIp.get(ip);
        if (attempts == null) {
            return 0;
        }

        synchronized (attempts) {
            pruneExpiredAttempts(attempts);
            if (attempts.isEmpty()) {
                return 0;
            }
            Instant oldest = attempts.peekFirst();
            if (oldest == null) {
                return 0;
            }
            long elapsed = Instant.now().getEpochSecond() - oldest.getEpochSecond();
            return Math.max(0, WINDOW_SECONDS - elapsed);
        }
    }

    public void recordFailure(String ip) {
        failedAttemptsByIp.compute(ip, (k, existing) -> {
            Deque<Instant> attempts = (existing != null) ? existing : new ArrayDeque<>();
            synchronized (attempts) {
                pruneExpiredAttempts(attempts);
                attempts.addLast(Instant.now());
            }
            return attempts;
        });
    }

    public void recordSuccess(String ip) {
        failedAttemptsByIp.remove(ip);
    }

    public void resetAll() {
        failedAttemptsByIp.clear();
    }

    private void pruneExpiredAttempts(Deque<Instant> attempts) {
        Instant threshold = Instant.now().minusSeconds(WINDOW_SECONDS);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) {
            attempts.pollFirst();
        }
    }
}
