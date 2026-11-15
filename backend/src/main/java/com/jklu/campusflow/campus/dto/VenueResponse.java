package com.jklu.campusflow.campus.dto;

import java.util.List;
import java.util.UUID;

public record VenueResponse(
        UUID id,
        String name,
        String type,
        int capacity,
        String location,
        boolean isEligible,
        String eligibilityReason,
        List<String> features,
        boolean requiresApproval
) {}