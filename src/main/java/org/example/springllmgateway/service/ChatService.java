package org.example.springllmgateway.service;

import lombok.RequiredArgsConstructor;
import org.example.springllmgateway.client.LlmClient;
import org.example.springllmgateway.memory.ConversationMemory;
import org.example.springllmgateway.model.ChatRequest;
import org.example.springllmgateway.model.ChatResponse;
import org.example.springllmgateway.model.Message;
import org.example.springllmgateway.personality.PersonalityMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final PersonalityMapper personalityMapper;
    private final ConversationMemory conversationMemory;
    private final LlmClient llmClient;

    public ChatResponse chat(ChatRequest request) {
        String systemPrompt = personalityMapper.getPrompt(request.personality());
        List<Message> history = conversationMemory.getHistory(request.sessionId());

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.addAll(history);
        messages.add(new Message("user", request.message()));

        String reply = llmClient.sendMessages(messages);
        conversationMemory.append(request.sessionId(), new Message("user", request.message()));
        conversationMemory.append(request.sessionId(), new Message("assistant", reply));
        return new ChatResponse(reply, request.sessionId());
    }
}
