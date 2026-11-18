package com.example.knowledgeforge.quiz;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Zamienia surowe pytania modelu na pytania gotowe do puli. Odrzuca wszystko, co nie spełnia
 * kontraktu (4 różne, niepuste odpowiedzi, poprawny indeks, treść i wyjaśnienie), duplikaty
 * (względem puli i w obrębie partii) oraz pytania z odpowiedziami typu "wszystkie powyższe".
 * Na koniec tasuje kolejność odpowiedzi — modele mają skłonność do umieszczania poprawnej
 * odpowiedzi na tej samej pozycji, a w quizie nie może istnieć wzorzec do zgadywania.
 */
public final class QuizDraftValidator {

    static final int MAX_QUESTION_CHARS = 1200;
    static final int MAX_CODE_CHARS = 4000;
    static final int MAX_OPTION_CHARS = 600;
    static final int MAX_EXPLANATION_CHARS = 1500;

    private static final Pattern OPTION_PREFIX = Pattern.compile("^\\s*(?:[A-Da-d]|[1-4])\\s*[).:\\-]\\s+");
    private static final Pattern BANNED_OPTION = Pattern.compile(
            "^(wszystkie|żadne|zadne)\\s+(z\\s+)?(powyższ|powyzsz|wymienion)|^all of the above|^none of the above",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final Random random;

    public QuizDraftValidator(Random random) {
        this.random = random;
    }

    /**
     * @param existingKeys znormalizowane treści pytań już w puli ({@link #key}) — uzupełniane o
     *                     przyjęte pytania, żeby kolejne porcje też się z nimi nie dublowały
     */
    public List<QuizQuestion> accept(QuizBatch batch, QuizDifficulty expected, UUID topicId,
                                     Set<String> existingKeys, Instant now) {
        List<QuizQuestion> out = new ArrayList<>();
        if (batch == null || batch.questions() == null) return out;
        for (QuizBatch.Item item : batch.questions()) {
            QuizQuestion q = toQuestion(item, expected, topicId, now);
            if (q == null) continue;
            if (!existingKeys.add(key(q.question(), q.code()))) continue;
            out.add(q);
        }
        return out;
    }

    QuizQuestion toQuestion(QuizBatch.Item item, QuizDifficulty expected, UUID topicId, Instant now) {
        if (item == null) return null;
        String question = text(item.question(), MAX_QUESTION_CHARS);
        String explanation = text(item.explanation(), MAX_EXPLANATION_CHARS);
        if (question == null || explanation == null) return null;
        if (item.options() == null || item.options().size() != 4) return null;
        Integer correct = item.correctIndex();
        if (correct == null || correct < 0 || correct > 3) return null;

        List<String> options = new ArrayList<>(4);
        Set<String> seen = new HashSet<>();
        for (String raw : item.options()) {
            String option = text(raw == null ? null : OPTION_PREFIX.matcher(raw).replaceFirst(""), MAX_OPTION_CHARS);
            if (option == null || BANNED_OPTION.matcher(option).find()) return null;
            if (!seen.add(normalize(option))) return null;
            options.add(option);
        }

        String correctText = options.get(correct);
        shuffle(options);
        return new QuizQuestion(UUID.randomUUID(), topicId, expected, question, code(item.code()),
                List.copyOf(options), options.indexOf(correctText), explanation, now);
    }

    private void shuffle(List<String> options) {
        for (int i = options.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String tmp = options.get(i);
            options.set(i, options.get(j));
            options.set(j, tmp);
        }
    }

    /** Klucz deduplikacji — treść bez wielkości liter, akcentów i interpunkcji (+ kod, jeśli jest). */
    public static String key(String question, String code) {
        return normalize(question) + "|" + normalize(code);
    }

    public static Set<String> keys(Collection<QuizQuestion> questions) {
        Set<String> keys = new HashSet<>();
        questions.forEach(q -> keys.add(key(q.question(), q.code())));
        return keys;
    }

    static String normalize(String s) {
        if (s == null) return "";
        String folded = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return NON_ALNUM.matcher(folded.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    }

    private static String text(String s, int max) {
        if (s == null) return null;
        String t = s.strip();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    /** Kod zachowuje wcięcia — przycinamy tylko puste linie na brzegach i końcowe spacje. */
    private static String code(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.replaceAll("^(\\s*\\n)+", "").stripTrailing();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[\\w+#.-]*\\s*\\n?", "").replaceFirst("\\n?```\\s*$", "").stripTrailing();
        }
        if (t.isBlank()) return null;
        return t.length() > MAX_CODE_CHARS ? t.substring(0, MAX_CODE_CHARS) : t;
    }
}
