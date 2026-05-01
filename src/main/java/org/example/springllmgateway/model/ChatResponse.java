package org.example.springllmgateway.model;

public record ChatResponse(
        String response,
        String sessionId
) {
}
