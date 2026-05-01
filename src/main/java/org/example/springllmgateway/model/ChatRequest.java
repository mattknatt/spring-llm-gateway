package org.example.springllmgateway.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String personality,
        @NotBlank String message,
        String sessionId) {}
