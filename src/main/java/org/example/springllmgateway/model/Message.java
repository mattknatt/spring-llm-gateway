package org.example.springllmgateway.model;


public record Message(
        String role,
        String content
) {
}
