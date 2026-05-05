package org.example.springllmgateway.exception;

public class LlmClientException extends RuntimeException {
    private final int statusCode;

    public LlmClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
