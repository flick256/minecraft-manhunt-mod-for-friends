package io.github.flick256.manhunt.core.quiz;

import io.github.flick256.manhunt.core.MathQuestion;
import io.github.flick256.manhunt.core.MathQuiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Picks the next respawn question for a topic. */
public final class QuizBank {
    private QuizBank() {}

    /** All fixed (multiple choice) questions of a topic; empty for the generated topics MATH and METHODS. */
    public static List<Question> questions(Topic topic) {
        switch (topic) {
            case BIOLOGY:
                return BiologyBank.questions();
            case CHEMISTRY:
                return ChemistryBank.questions();
            case PHYSICS:
                return PhysicsBank.questions();
            case HISTORY:
                return HistoryBank.questions();
            case FRENCH:
                return FrenchBank.questions();
            default:
                return List.of();
        }
    }

    /**
     * Next question for {@code topic}. {@code mathDifficulty} (1..3) only affects the MATH topic.
     * {@code avoidPrompt} (may be null) is the previous prompt, so the same question is not asked twice in a row.
     */
    public static Question next(Topic topic, int mathDifficulty, Random rnd, String avoidPrompt) {
        for (int attempt = 0; attempt < 8; attempt++) {
            Question q = pick(topic, mathDifficulty, rnd);
            if (avoidPrompt == null || !avoidPrompt.equals(q.prompt())) {
                return q;
            }
        }
        return pick(topic, mathDifficulty, rnd);
    }

    private static Question pick(Topic topic, int mathDifficulty, Random rnd) {
        switch (topic) {
            case MATH: {
                MathQuestion m = MathQuiz.next(mathDifficulty, rnd);
                String text = m.text().trim();
                if (text.endsWith("?")) {
                    text = text.substring(0, text.length() - 1).trim();
                }
                if (text.endsWith("=")) {
                    text = text.substring(0, text.length() - 1).trim();
                }
                return Question.numeric(Topic.MATH, text + " = ?", String.valueOf(m.answer()));
            }
            case METHODS:
                return MethodsQuiz.next(rnd);
            default: {
                List<Question> bank = questions(topic);
                if (bank.isEmpty()) {
                    return pick(Topic.MATH, mathDifficulty, rnd);
                }
                Question q = bank.get(rnd.nextInt(bank.size()));
                List<String> shuffled = new ArrayList<>(q.choices());
                Collections.shuffle(shuffled, rnd);
                return new Question(q.topic(), q.prompt(), shuffled, q.correct());
            }
        }
    }
}
