package org.example.springllmgateway.memory;

import org.example.springllmgateway.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ConversationMemoryTest {

    private ConversationMemory memory;

    @BeforeEach
    void setUp() {
        memory = new ConversationMemory();
    }

    @Test
    void getHistory_returnsEmpty_whenSessionIdIsNull() {
        assertThat(memory.getHistory(null)).isEmpty();
    }

    @Test
    void getHistory_returnsEmpty_forUnknownSession() {
        assertThat(memory.getHistory("nope")).isEmpty();
    }

    @Test
    void append_thenGetHistory_returnsAppendedMessages_inOrder() {
        memory.append("s1", new Message("user", "hi"));
        memory.append("s1", new Message("assistant", "hello"));

        assertThat(memory.getHistory("s1")).containsExactly(
                new Message("user", "hi"),
                new Message("assistant", "hello"));
    }

    @Test
    void append_keepsOnlyLast20Messages() {
        IntStream.range(0, 25).forEach(i ->
                memory.append("s1", new Message("user", "msg-" + i)));

        List<Message> history = memory.getHistory("s1");

        assertThat(history).hasSize(20);
        assertThat(history.get(0).content()).isEqualTo("msg-5");
        assertThat(history.get(19).content()).isEqualTo("msg-24");
    }

    @Test
    void getHistory_returnsDefensiveCopy() {
        memory.append("s1", new Message("user", "hi"));

        List<Message> history = memory.getHistory("s1");
        history.clear();

        assertThat(memory.getHistory("s1")).hasSize(1);
    }

    @Test
    void append_ignoresNullSessionId() {
        assertThatCode(() -> memory.append(null, new Message("user", "hi")))
                .doesNotThrowAnyException();
    }
}
