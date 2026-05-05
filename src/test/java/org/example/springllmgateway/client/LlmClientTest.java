package org.example.springllmgateway.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.example.springllmgateway.exception.LlmClientException;
import org.example.springllmgateway.exception.LlmUnavailableException;
import org.example.springllmgateway.model.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LlmClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void llmProperties(DynamicPropertyRegistry registry) {
        registry.add("llm.api.url", wireMock::baseUrl);
        registry.add("llm.api.key", () -> "test-key");
        registry.add("llm.api.model", () -> "test-model");
    }

    @Autowired
    private LlmClient llmClient;

    private static final String RESPONSE_BODY = """
            {"choices":[{"message":{"role":"assistant","content":"hello"}}]}
            """;

    @Test
    void sendMessages_returnsContent_andSendsAuthHeaderAndModel() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(RESPONSE_BODY)));

        String result = llmClient.sendMessages(List.of(new Message("user", "hi")));

        assertThat(result).isEqualTo("hello");
        wireMock.verify(postRequestedFor(urlEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("test-model"))));
    }

    @Test
    void sendMessages_throws_whenResponseHasNoChoices() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[]}")));

        assertThatThrownBy(() -> llmClient.sendMessages(List.of(new Message("user", "hi"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no choices");
    }

    @Test
    void sendMessages_throwsLlmClientException_whenServerReturns400() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> llmClient.sendMessages(List.of(new Message("user", "hi"))))
                .isInstanceOf(LlmClientException.class)
                .hasMessageContaining("400");
    }

    @Test
    void sendMessages_throwsLlmClientException_whenServerReturns401() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> llmClient.sendMessages(List.of(new Message("user", "hi"))))
                .isInstanceOf(LlmClientException.class)
                .hasMessageContaining("401");
    }

    @Test
    void sendMessages_retries_whenServerReturns429_thenSucceeds() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("retry-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("recovered"));
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("retry-429")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(RESPONSE_BODY)));

        String result = llmClient.sendMessages(List.of(new Message("user", "hi")));

        assertThat(result).isEqualTo("hello");
        assertThat(wireMock.findAll(postRequestedFor(urlEqualTo("/chat/completions"))))
                .hasSize(2);
    }

    @Test
    void sendMessages_retries_whenServerReturns500_thenSucceeds() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("retry-500")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("recovered"));
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("retry-500")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(RESPONSE_BODY)));

        String result = llmClient.sendMessages(List.of(new Message("user", "hi")));

        assertThat(result).isEqualTo("hello");
        assertThat(wireMock.findAll(postRequestedFor(urlEqualTo("/chat/completions"))))
                .hasSize(2);
    }

    @Test
    void sendMessages_throwsLlmUnavailableException_whenServerReturns429_andRetriesExhausted() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> llmClient.sendMessages(List.of(new Message("user", "hi"))))
                .isInstanceOf(LlmUnavailableException.class);
        assertThat(wireMock.findAll(postRequestedFor(urlEqualTo("/chat/completions"))))
                .hasSize(4);
    }

    @Test
    void sendMessages_retries_whenServerReturns503_thenSucceeds() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("retry-503")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("recovered"));
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("retry-503")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(RESPONSE_BODY)));

        String result = llmClient.sendMessages(List.of(new Message("user", "hi")));

        assertThat(result).isEqualTo("hello");
        assertThat(wireMock.findAll(postRequestedFor(urlEqualTo("/chat/completions"))))
                .hasSize(2);
    }
}
