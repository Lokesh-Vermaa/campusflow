package com.jklu.campusflow.common.idempotency;

import com.jklu.campusflow.common.error.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    public Optional<IdempotencyKey> findValidKey(String key, UUID userId, String requestPayload) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        Optional<IdempotencyKey> existing = repository.findByKeyAndUserId(key, userId);
        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();
            if (record.getExpiresAt().isBefore(OffsetDateTime.now())) {
                repository.delete(record);
                return Optional.empty();
            }

            String currentHash = hashPayload(requestPayload);
            if (!currentHash.equals(record.getRequestHash())) {
                throw new BusinessRuleException("Idempotency key re-used with a different request payload");
            }
            return Optional.of(record);
        }
        return Optional.empty();
    }

    public void saveKey(String key, UUID userId, String requestPayload, int responseCode, String responseBody) {
        if (key == null || key.isBlank()) {
            return;
        }

        String hash = hashPayload(requestPayload);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);
        IdempotencyKey record = new IdempotencyKey(key, userId, hash, responseCode, responseBody, expiresAt);
        repository.save(record);
    }

    private String hashPayload(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((payload != null ? payload : "").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return String.valueOf(payload != null ? payload.hashCode() : 0);
        }
    }
}
