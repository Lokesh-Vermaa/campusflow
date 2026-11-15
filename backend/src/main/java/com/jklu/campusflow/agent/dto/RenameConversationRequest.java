package com.jklu.campusflow.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameConversationRequest(@NotBlank @Size(max = 255) String title) {}
