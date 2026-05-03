package org.example.springllmgateway.client;

import org.example.springllmgateway.model.Message;

import java.util.List;

public record LlmRequest(String model, List<Message> messages) {}

