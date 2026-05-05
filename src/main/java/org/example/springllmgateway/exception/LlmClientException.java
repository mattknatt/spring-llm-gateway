package org.example.springllmgateway.exception;

public class LlmClientException extends RuntimeException {
    public LlmClientException(String message) {
        super(message);
    }
}
