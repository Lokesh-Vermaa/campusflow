package com.jklu.campusflow.booking.model;

import com.jklu.campusflow.auth.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Column(nullable = false, length = 50)
    private String action; // APPROVED, REJECTED, REQUEST_CHANGES

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Approval() {}

    public Approval(UUID bookingId, User approver, String action, String remarks) {
        this.bookingId = bookingId;
        this.approver = approver;
        this.action = action;
        this.remarks = remarks;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public User getApprover() { return approver; }
    public void setApprover(User approver) { this.approver = approver; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
