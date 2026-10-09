package io.github.flick256.manhunt.core.quiz;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MethodsQuizTest {
    private static final int ITERATIONS = 5000;
    private static final int MAX_PROMPT = 110;

    /** An answer re-derived in the test from the digits in a prompt. */
    private record Expected(String kind, double value) {}

    // Prompt shapes the test can re-derive independently of MethodsQuiz.
    private static final Pattern GRADIENT_LINE =
            Pattern.compile("What is the gradient of the line y = (-?\\d*)x((?: [+-] \\d+)?)\\?");
    private static final Pattern Y_INTERCEPT =
            Pattern.compile("What is the y-intercept of the line y = (-?\\d*)x((?: [+-] \\d+)?)\\?");
    private static final Pattern THROUGH_POINTS =
            Pattern.compile("What is the gradient of the line through \\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\)\\?");
    private static final Pattern SOLVE =
            Pattern.compile("Solve (-?\\d*)x((?: [+-] \\d+)?) = (-?\\d+) for x\\.");
    private static final Pattern SUM_OF_ROOTS =
            Pattern.compile("What is the sum of the roots of \\(x ([+-]) (\\d+)\\)\\(x ([+-]) (\\d+)\\) = 0\\?");
    private static final Pattern QUADRATIC_VALUE =
            Pattern.compile("If f\\(x\\) = (-?\\d*)x\\^2((?: [+-] \\d*x)?)((?: [+-] \\d+)?), what is f\\((-?\\d+)\\)\\?");
    private static final Pattern INDEX_MULTIPLY =
            Pattern.compile("If (\\d+)\\^(\\d+) x (\\d+)\\^(\\d+) = (\\d+)\\^n, what is n\\?");
    private static final Pattern INTEGRAL_CONSTANT =
            Pattern.compile("Evaluate the integral of (-?\\d+) dx from 0 to (\\d+)\\.");
    private static final Pattern STATIONARY =
            Pattern.compile("What is the x-coordinate of the stationary point of y = x\\^2((?: [+-] \\d+x)?)\\?");
    private static final Pattern DIE =
            Pattern.compile("A fair (\\d+)-sided die is rolled\\. What is P\\(number greater than (\\d+)\\)\\?");
    private static final Pattern INDEPENDENT =
            Pattern.compile("A and B are independent, with P\\(A\\) = (\\d+)/(\\d+) and P\\(B\\) = (\\d+)/(\\d+)\\. What is P\\(A and B\\)\\?");
    private static final Pattern COIN =
            Pattern.compile("How many different sequences of heads and tails are possible from (\\d+) tosses of a coin\\?");

    @Test
    void generatedQuestionsAreWellFormedAndCorrectlyAnswered() {
        Random shared = new Random(20260101L);
        Set<String> shapes = new HashSet<>();
        Set<String> recomputedKinds = new HashSet<>();
        for (int i = 0; i < ITERATIONS; i++) {
            check(MethodsQuiz.next(new Random(i)), shapes, recomputedKinds);
            check(MethodsQuiz.next(shared), shapes, recomputedKinds);
        }
        assertTrue(shapes.size() >= 10, () -> "only " + shapes.size() + " distinct templates: " + shapes);
        assertTrue(recomputedKinds.size() >= 3, () -> "only independently recomputed: " + recomputedKinds);
    }

    @Test
    void sameSeedGivesSameQuestions() {
        Random a = new Random(99);
        Random b = new Random(99);
        for (int i = 0; i < 1000; i++) {
            assertEquals(MethodsQuiz.next(a), MethodsQuiz.next(b));
        }
    }

    @Test
    void nullRandomDoesNotThrow() {
        Question q = MethodsQuiz.next(null);
        assertEquals(Topic.METHODS, q.topic());
        assertTrue(q.check(q.correct()));
    }

    private static void check(Question q, Set<String> shapes, Set<String> recomputedKinds) {
        assertEquals(Topic.METHODS, q.topic());
        String p = q.prompt();
        assertFalse(p.isBlank(), "empty prompt");
        assertTrue(p.length() <= MAX_PROMPT, () -> "prompt too long: " + p);
        assertNotEquals(MethodsQuiz.FALLBACK_PROMPT, p, "generator fell back after an exception");
        assertTrue(q.check(q.correct()), () -> "correct answer rejected: " + q.display());
        shapes.add(p.replaceAll("\\d", "#"));

        if (q.isMultipleChoice()) {
            assertEquals(4, q.choices().size(), p);
            assertEquals(4, new HashSet<>(q.choices().stream().map(Question::normaliseForTest).toList()).size(),
                    () -> "choices not distinct: " + q.display());
            assertTrue(q.choices().contains(q.correct()), p);
            int idx = q.choices().indexOf(q.correct());
            assertTrue(q.check(String.valueOf((char) ('A' + idx))), p);
        } else {
            assertTrue(q.choices().isEmpty(), p);
            Double got = Question.parseNumber(q.correct());
            assertNotNull(got, () -> "unparseable numeric answer: " + q.correct());
            Expected e = recompute(p);
            if (e != null) {
                recomputedKinds.add(e.kind());
                assertEquals(e.value(), got.doubleValue(), 1e-9, () -> p + " answered " + q.correct());
            }
        }
    }

    /** Re-derives the answer from the digits in a prompt, or returns null for a shape it does not know. */
    private static Expected recompute(String prompt) {
        Matcher m;
        if ((m = GRADIENT_LINE.matcher(prompt)).matches()) {
            return new Expected("gradient of a line", coefficient(m.group(1)));
        }
        if ((m = Y_INTERCEPT.matcher(prompt)).matches()) {
            return new Expected("y-intercept", signedTerm(m.group(2)));
        }
        if ((m = THROUGH_POINTS.matcher(prompt)).matches()) {
            int x1 = Integer.parseInt(m.group(1));
            int y1 = Integer.parseInt(m.group(2));
            int x2 = Integer.parseInt(m.group(3));
            int y2 = Integer.parseInt(m.group(4));
            return new Expected("gradient through two points", (double) (y2 - y1) / (x2 - x1));
        }
        if ((m = SOLVE.matcher(prompt)).matches()) {
            int a = coefficient(m.group(1));
            int b = signedTerm(m.group(2));
            int c = Integer.parseInt(m.group(3));
            return new Expected("solve linear equation", (double) (c - b) / a);
        }
        if ((m = SUM_OF_ROOTS.matcher(prompt)).matches()) {
            int r1 = "-".equals(m.group(1)) ? Integer.parseInt(m.group(2)) : -Integer.parseInt(m.group(2));
            int r2 = "-".equals(m.group(3)) ? Integer.parseInt(m.group(4)) : -Integer.parseInt(m.group(4));
            return new Expected("sum of roots", r1 + r2);
        }
        if ((m = QUADRATIC_VALUE.matcher(prompt)).matches()) {
            int a = coefficient(m.group(1));
            int b = signedTerm(m.group(2));
            int c = signedTerm(m.group(3));
            int k = Integer.parseInt(m.group(4));
            return new Expected("quadratic value f(k)", a * k * k + b * k + c);
        }
        if ((m = INDEX_MULTIPLY.matcher(prompt)).matches()) {
            assertEquals(m.group(1), m.group(3), prompt);
            assertEquals(m.group(1), m.group(5), prompt);
            return new Expected("index law product", Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(4)));
        }
        if ((m = INTEGRAL_CONSTANT.matcher(prompt)).matches()) {
            return new Expected("definite integral of a constant", Integer.parseInt(m.group(1)) * Integer.parseInt(m.group(2)));
        }
        if ((m = STATIONARY.matcher(prompt)).matches()) {
            // y = x^2 + bx has its stationary point at x = -b/2
            return new Expected("stationary point", -signedTerm(m.group(1)) / 2.0);
        }
        if ((m = DIE.matcher(prompt)).matches()) {
            double sides = Integer.parseInt(m.group(1));
            double k = Integer.parseInt(m.group(2));
            return new Expected("fair die probability", (sides - k) / sides);
        }
        if ((m = INDEPENDENT.matcher(prompt)).matches()) {
            double product = Integer.parseInt(m.group(1)) * Integer.parseInt(m.group(3));
            double denominator = Integer.parseInt(m.group(2)) * Integer.parseInt(m.group(4));
            return new Expected("independent events", product / denominator);
        }
        if ((m = COIN.matcher(prompt)).matches()) {
            return new Expected("coin sequences", Math.pow(2, Integer.parseInt(m.group(1))));
        }
        return null;
    }

    /** "" means 1, "-" means -1, otherwise the number. */
    private static int coefficient(String g) {
        if (g.isEmpty()) {
            return 1;
        }
        if (g.equals("-")) {
            return -1;
        }
        return Integer.parseInt(g);
    }

    /** A signed term such as " + 4" or " - 3x" (the x is ignored); null or blank gives 0. */
    private static int signedTerm(String g) {
        if (g == null || g.isBlank()) {
            return 0;
        }
        String s = g.trim();
        int sign = s.charAt(0) == '-' ? -1 : 1;
        String magnitude = s.substring(1).trim().replace("x", "");
        return sign * (magnitude.isEmpty() ? 1 : Integer.parseInt(magnitude));
    }
}
