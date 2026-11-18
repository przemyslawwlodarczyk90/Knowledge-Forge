package com.example.knowledgeforge.ai;

import com.example.knowledgeforge.config.AppConfig;
import com.example.knowledgeforge.quiz.NoOpQuizGenerationService;
import com.example.knowledgeforge.quiz.QuizGenerationPort;
import com.example.knowledgeforge.quiz.SpringAiQuizGenerationService;
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

    @Test
    void quiz_port_follows_the_same_key_switch() {
        assertThat(new AiConfig().quizGenerationPort(AppConfig.fromProperties(new Properties())))
                .isInstanceOf(NoOpQuizGenerationService.class);

        Properties p = new Properties();
        p.setProperty("openai.api-key", "sk-test");
        p.setProperty("openai.model", "gpt-q");
        QuizGenerationPort port = new AiConfig().quizGenerationPort(AppConfig.fromProperties(p));
        assertThat(port).isInstanceOf(SpringAiQuizGenerationService.class);
        assertThat(port.modelName()).isEqualTo("gpt-q");
    }
}
