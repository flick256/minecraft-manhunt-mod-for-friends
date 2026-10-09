package io.github.flick256.manhunt.core.quiz;

import java.util.Random;

/** Generated easy VCE Mathematical Methods questions (placeholder, being written). */
public final class MethodsQuiz {
    private MethodsQuiz() {}

    public static Question next(Random rnd) {
        int a = 2 + rnd.nextInt(8);
        return Question.numeric(Topic.METHODS, "d/dx of " + a + "x^2 at x = 1 is ?", String.valueOf(2 * a));
    }
}
