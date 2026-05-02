package org.example.springllmgateway.client;

import java.util.List;

public record LlmResponse(List<CompletionChoice> choices) {}

