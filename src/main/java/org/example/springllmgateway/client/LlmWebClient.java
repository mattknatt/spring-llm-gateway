package org.example.springllmgateway.client;

import lombok.RequiredArgsConstructor;
import org.example.springllmgateway.exception.LlmUnavailableException;
import org.example.springllmgateway.config.LlmProperties;
import org.example.springllmgateway.model.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmWebClient {

    private final WebClient webClient;
    private final LlmProperties llmProperties;

    private static final int MAX_RETRIES = 3;

    public Mono<String> sendMessages(List<Message> messages) {
        LlmRequest request = new LlmRequest(llmProperties.model(), messages);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new LlmUnavailableException(
                                        body.isEmpty() ? "LLM unavailable" : "LLM unavailable: " + body)))
                .bodyToMono(LlmResponse.class)
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(1))
                        .filter(ex -> ex instanceof LlmUnavailableException))
                .map(response -> {
                    if (response.choices() == null || response.choices().isEmpty()) {
                        throw new IllegalStateException("LLM returned no choices");
                    }
                    return response.choices().get(0).message().content();
                });
    }
}




