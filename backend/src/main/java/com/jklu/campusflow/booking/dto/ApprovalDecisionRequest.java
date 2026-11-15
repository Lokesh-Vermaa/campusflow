package com.jklu.campusflow.booking.dto;

public record ApprovalDecisionRequest(
        String note,
        String reason
) {}
