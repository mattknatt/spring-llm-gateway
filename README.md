# Spring LLM Gateway

A Spring Boot middleware service that acts as a bridge between clients and a Large Language Model (LLM). It manages personalities (system prompts), per-session conversation history, resilient retries, and clean error responses — all without exposing the upstream API directly to clients.

## Table of Contents

- [Architecture](#architecture)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Reference](#api-reference)
- [Personalities](#personalities)
- [Conversation Memory](#conversation-memory)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Project Structure](#project-structure)

---

## Architecture

```
Client
  │
  ▼
POST /api/v1/chat
  │
  ▼
ChatController
  │
  ▼
ChatService ──► PersonalityMapper   (system prompt selection)
  │       ──► ConversationMemory   (per-session history)
  │
  ▼
LlmClient  ──► RestClient ──► OpenRouter / LM Studio
  │             (with retry & backoff)
  ▼
ChatResponse (reply + sessionId)
```

Each request is enriched with a system prompt determined by `personality`, prepended with the session's conversation history, and forwarded to the upstream LLM via Spring `RestClient`. The response and the new turn are stored back into the session.

---

## Features

- **Personality system** — map a short key (`helper`, `pirate`, `coder`) to a system prompt.
- **Conversation memory** — per-session history stored in-memory with Caffeine (max 20 messages, 30-minute idle expiry, up to 1 000 concurrent sessions).
- **Resilient HTTP client** — automatic retry with exponential backoff on `429 Too Many Requests` and `5xx` errors.
- **Structured error responses** — a `@RestControllerAdvice` handler returns consistent JSON for all error cases.
- **Request validation** — Jakarta Bean Validation rejects malformed requests before they reach the service layer.
- **OpenAPI / Swagger UI** — interactive documentation served at `/swagger-ui.html`.
- **No hardcoded secrets** — all credentials are injected from environment variables.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 25 |
| Maven | 3.9+ |
| A running LLM endpoint | OpenRouter or LM Studio |

---

## Configuration

All sensitive values are read from environment variables. Set the following before starting the application:

| Variable | Description | Example |
|---|---|---|
| `LLM_API_URL` | Base URL of the LLM API | `https://openrouter.ai/api/v1` |
| `LLM_API_KEY` | Bearer token / API key | `sk-or-...` |
| `LLM_API_MODEL` | Model identifier to use | `openai/gpt-4o-mini` |

These map to `application.properties`:

```properties
llm.api.url=${LLM_API_URL}
llm.api.key=${LLM_API_KEY}
llm.api.model=${LLM_API_MODEL}
```

You can provide them via a `.env` file (loaded by your shell or IDE), system environment variables, or a `-D` flag at startup.

---

## Running the Application

```bash
# Export credentials
export LLM_API_URL=https://openrouter.ai/api/v1
export LLM_API_KEY=your-api-key
export LLM_API_MODEL=openai/gpt-4o-mini

# Build and run
./mvnw spring-boot:run
```

The service starts on port `8080` by default.

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

---

## API Reference

### POST /api/v1/chat

Send a message to the LLM through a chosen personality.

**Request body**

```json
{
  "personality": "coder",
  "message": "How do I write a for-loop in Java?",
  "sessionId": "user-123-abc"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `personality` | String | Yes | One of `helper`, `pirate`, `coder` |
| `message` | String | Yes | The user's message (must not be blank) |
| `sessionId` | String | No | Reuse a session to maintain conversation history. If omitted, a new UUID is generated. |

**Success response — 200 OK**

```json
{
  "response": "A for-loop in Java looks like: for (int i = 0; i < n; i++) { ... }",
  "sessionId": "user-123-abc"
}
```

**Validation error — 400 Bad Request**

```json
{
  "timestamp": "2025-05-07T10:00:00Z",
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "personality": "personality must be one of: helper, pirate, coder"
  }
}
```

**LLM unavailable — 503 Service Unavailable**

```json
{
  "timestamp": "2025-05-07T10:00:00Z",
  "status": 503,
  "message": "LLM unavailable"
}
```

---

## Personalities

| Key | Behaviour |
|---|---|
| `helper` | Helpful and supportive assistant; answers clearly and concisely. |
| `pirate` | Responds entirely in pirate slang. |
| `coder` | Expert software engineer; gives precise and technical answers. |

The personality key is mapped to a system prompt injected as the first message in every LLM request.

---

## Conversation Memory

When a `sessionId` is provided the service maintains a rolling history of up to **20 messages** for that session. On every request:

1. Previous messages are prepended to the LLM call (after the system prompt).
2. The new user message and the assistant's reply are appended to the session.

Sessions expire after **30 minutes of inactivity** and the cache holds at most **1 000 concurrent sessions**.

Omitting `sessionId` (or sending a blank string) starts a stateless, one-shot conversation and a fresh UUID is returned in the response for optional reuse.

---

## Error Handling

The `GlobalExceptionHandler` (`@RestControllerAdvice`) maps all application exceptions to structured JSON responses:

| Exception | HTTP status | Trigger |
|---|---|---|
| `LlmClientException` | 4xx (mirrored) | The upstream API rejected the request (e.g. 401, 403, 400) |
| `LlmUnavailableException` | 503 | 429 or 5xx from upstream, exhausted after retries |
| `MethodArgumentNotValidException` | 400 | Jakarta validation failure on the request body |

**Retry policy** — `LlmClient` retries on `LlmUnavailableException` up to **3 times** with exponential backoff:

| Parameter | Value |
|---|---|
| Max retries | 3 |
| Initial delay | 1 000 ms |
| Multiplier | 2× |
| Jitter | ±200 ms |
| Max delay | 5 000 ms |

---

## Testing

```bash
./mvnw test
```

The test suite covers:

| Test class | Scope | Approach |
|---|---|---|
| `LlmClientTest` | HTTP client | WireMock stubs — verifies retry, backoff, and error-code handling |
| `ChatControllerTest` | REST layer | `@WebMvcTest` with mocked service — validates request/response contracts |
| `ChatServiceTest` | Service logic | Mockito — verifies message ordering, sessionId generation, memory writes |
| `ConversationMemoryTest` | Memory | Unit — max-history limit, defensive copy, null-session guard |
| `PersonalityMapperTest` | Personality | Parameterised unit test for all valid and invalid keys |

---

## Project Structure

```
src/
  main/java/org/example/springllmgateway/
    client/           LlmClient, request/response records
    config/           RestClientConfig, LlmProperties
    controller/       ChatController
    exception/        GlobalExceptionHandler, LlmClientException, LlmUnavailableException
    memory/           ConversationMemory (Caffeine-backed)
    model/            ChatRequest, ChatResponse, Message
    personality/      PersonalityMapper
    service/          ChatService
  test/java/org/example/springllmgateway/
    client/           LlmClientTest (WireMock)
    controller/       ChatControllerTest
    memory/           ConversationMemoryTest
    personality/      PersonalityMapperTest
    service/          ChatServiceTest
```
