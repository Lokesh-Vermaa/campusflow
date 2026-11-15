package com.jklu.campusflow.common.idempotency;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(nullable = false, length = 255)
    private String key;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "request_hash", nullable = false, length = 255)
    private String requestHash;

    @Column(name = "response_code", nullable = false)
    private int responseCode;

    @Column(name = "response_body", nullable = false, columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    public IdempotencyKey() {}

    public IdempotencyKey(String key, UUID userId, String requestHash, int responseCode, String responseBody, OffsetDateTime expiresAt) {
        this.key = key;
        this.userId = userId;
        this.requestHash = requestHash;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.createdAt = OffsetDateTime.now();
        this.expiresAt = expiresAt;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public int getResponseCode() { return responseCode; }
    public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
