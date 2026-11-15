package com.jklu.campusflow.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AgentChatRequest(
        @NotBlank @Size(max = 2000) String message,
        UUID conversationId
) {}
