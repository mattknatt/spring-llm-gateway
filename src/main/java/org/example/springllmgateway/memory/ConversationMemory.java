package org.example.springllmgateway.memory;

import lombok.RequiredArgsConstructor;
import org.example.springllmgateway.model.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ConversationMemory {

    private final Map<String, List<Message>> store = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY= 20;

    public List<Message> getHistory(String sessionId) {
        if (sessionId == null) return List.of();
        return store.getOrDefault(sessionId, List.of());
    }

    public void append(String sessionId, Message message) {
        if (sessionId == null) return;
        store.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);
        trimHistory(sessionId);
    }

    private void trimHistory(String sessionId) {
        List<Message> history = store.get(sessionId);
        if (history != null && history.size() > MAX_HISTORY) {
            store.put(sessionId, new ArrayList<>(history.subList(history.size() - MAX_HISTORY, history.size())));
        }
    }
}
