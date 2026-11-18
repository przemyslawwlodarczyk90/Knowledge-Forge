package com.example.knowledgeforge.quiz;

import java.util.List;

/**
 * Jedno wywołanie modelu: {@code count} pytań o danym poziomie trudności, z własną "perspektywą"
 * ({@code angle}), żeby równoległe porcje nie generowały tych samych pytań.
 *
 * @param avoid początki pytań już obecnych w puli — model ma ich nie powtarzać
 */
public record QuizChunk(String topicTitle, String noteText, String userInstruction, QuizDifficulty difficulty,
                        int count, String angle, List<String> avoid, int index, int total) {
}
