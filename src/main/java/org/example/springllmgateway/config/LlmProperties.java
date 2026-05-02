package org.example.springllmgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm.api")
public record LlmProperties(
        String url,
        String key,
        String model
) {
}
