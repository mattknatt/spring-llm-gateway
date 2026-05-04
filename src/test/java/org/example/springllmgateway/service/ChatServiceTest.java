package org.example.springllmgateway.service;

import org.example.springllmgateway.client.LlmClient;
import org.example.springllmgateway.memory.ConversationMemory;
import org.example.springllmgateway.model.ChatRequest;
import org.example.springllmgateway.model.ChatResponse;
import org.example.springllmgateway.model.Message;
import org.example.springllmgateway.personality.PersonalityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private PersonalityMapper personalityMapper;

    @Mock
    private ConversationMemory conversationMemory;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private ChatService chatService;

    @Test
    void chat_buildsMessagesInOrder_andReturnsResponse() {
        ChatRequest request = new ChatRequest("helper", "what's up?", "s1");
        when(personalityMapper.getPrompt("helper")).thenReturn("system-prompt");
        when(conversationMemory.getHistory("s1")).thenReturn(List.of(
                new Message("user", "earlier"),
                new Message("assistant", "earlier-reply")));
        when(llmClient.sendMessages(any())).thenReturn("hi there");

        ChatResponse response = chatService.chat(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(llmClient).sendMessages(captor.capture());

        assertThat(captor.getValue()).containsExactly(
                new Message("system", "system-prompt"),
                new Message("user", "earlier"),
                new Message("assistant", "earlier-reply"),
                new Message("user", "what's up?"));
        assertThat(response).isEqualTo(new ChatResponse("hi there", "s1"));
    }

    @Test
    void chat_appendsUserAndAssistantMessages_afterLlmCall() {
        ChatRequest request = new ChatRequest("helper", "ping", "s1");
        when(personalityMapper.getPrompt("helper")).thenReturn("p");
        when(conversationMemory.getHistory("s1")).thenReturn(List.of());
        when(llmClient.sendMessages(any())).thenReturn("pong");

        chatService.chat(request);

        var inOrder = inOrder(llmClient, conversationMemory);
        inOrder.verify(llmClient).sendMessages(any());
        inOrder.verify(conversationMemory).append(eq("s1"), eq(new Message("user", "ping")));
        inOrder.verify(conversationMemory).append(eq("s1"), eq(new Message("assistant", "pong")));
    }
}
