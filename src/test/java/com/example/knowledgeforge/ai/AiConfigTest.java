package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigTest {

    @Test
    void blank_key_selects_stub_and_key_selects_spring_ai() {
        assertThat(new AiConfig().noteGenerationPort(AppConfig.fromProperties(new Properties())))
                .isInstanceOf(NoOpNoteGenerationService.class);

        Properties p = new Properties();
        p.setProperty("openai.api-key", "sk-test");
        p.setProperty("openai.model", "gpt-x");
        NoteGenerationPort port = new AiConfig().noteGenerationPort(AppConfig.fromProperties(p));
        assertThat(port).isInstanceOf(SpringAiNoteGenerationService.class);
        assertThat(port.modelName()).isEqualTo("gpt-x");
    }
}
