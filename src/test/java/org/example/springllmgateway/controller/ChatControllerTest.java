package org.example.springllmgateway.controller;

import org.example.springllmgateway.exception.LlmUnavailableException;
import org.example.springllmgateway.model.ChatResponse;
import org.example.springllmgateway.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void post_returns400_whenPersonalityMissing() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hi","sessionId":"s1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.personality").exists());
    }

    @Test
    void post_returns400_whenPersonalityDoesNotMatchPattern() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personality":"ninja","message":"hi","sessionId":"s1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.personality").exists());
    }

    @Test
    void post_returns400_whenMessageIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personality":"helper","message":"","sessionId":"s1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.message").exists());
    }

    @Test
    void post_returns503_whenServiceThrowsLlmUnavailable() throws Exception {
        when(chatService.chat(any())).thenThrow(new LlmUnavailableException("upstream down"));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personality":"helper","message":"hi","sessionId":"s1"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("upstream down"));
    }

    @Test
    void post_returns200_andResponseBody_onSuccess() throws Exception {
        when(chatService.chat(any())).thenReturn(new ChatResponse("hello", "s1"));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personality":"helper","message":"hi","sessionId":"s1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("hello"))
                .andExpect(jsonPath("$.sessionId").value("s1"));
    }
}
