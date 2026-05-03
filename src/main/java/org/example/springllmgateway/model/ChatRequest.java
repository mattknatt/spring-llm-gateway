package org.example.springllmgateway.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChatRequest(
        @NotBlank @Pattern(regexp = "helper|pirate|coder", message = "personality must be one of: helper, pirate, coder") String personality,
        @NotBlank String message,
        String sessionId) {}
