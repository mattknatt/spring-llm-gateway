package org.example.springllmgateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.springllmgateway.model.ChatRequest;
import org.example.springllmgateway.model.ChatResponse;
import org.example.springllmgateway.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public Mono<ResponseEntity<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request).map(ResponseEntity::ok);
    }

}
