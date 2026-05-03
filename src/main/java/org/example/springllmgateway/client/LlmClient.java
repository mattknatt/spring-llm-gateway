package org.example.springllmgateway.client;

import lombok.RequiredArgsConstructor;
import org.example.springllmgateway.config.LlmProperties;
import org.example.springllmgateway.exception.LlmUnavailableException;
import org.example.springllmgateway.model.Message;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmClient {

    private final RestClient restClient;
    private final LlmProperties llmProperties;

    @Retryable(
            includes = LlmUnavailableException.class,
            maxRetries = 3,
            delay = 1000,
            multiplier = 2,
            jitter = 200,
            maxDelay = 5000
    )
    public String sendMessages(List<Message> messages) {
        LlmRequest request = new LlmRequest(llmProperties.model(), messages);
        LlmResponse response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503,
                        (req, res) -> { throw new LlmUnavailableException("LLM unavailable"); })
                .body(LlmResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("LLM returned no choices");
        }
        return response.choices().get(0).message().content();
    }
}
