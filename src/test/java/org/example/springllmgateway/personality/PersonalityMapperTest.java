package org.example.springllmgateway.personality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalityMapperTest {

    private final PersonalityMapper mapper = new PersonalityMapper();

    @ParameterizedTest
    @ValueSource(strings = {"helper", "pirate", "coder"})
    void getPrompt_returnsNonBlankPrompt_forKnownPersonality(String personality) {
        assertThat(mapper.getPrompt(personality)).isNotBlank();
    }

    @Test
    void getPrompt_throws_forUnknownPersonality() {
        assertThatThrownBy(() -> mapper.getPrompt("ninja"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ninja");
    }
}
