package io.github.flick256.manhunt.core;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MathQuizTest {
    private static final Pattern TEXT = Pattern.compile("^(\\d+) ([+\\-*/]) (\\d+) = \\?$");

    /** Parses the question text and recomputes the answer independently of the generator. */
    private static int recompute(String text, char[] opOut) {
        Matcher m = TEXT.matcher(text);
        assertTrue(m.matches(), "unexpected text: " + text);
        int a = Integer.parseInt(m.group(1));
        int b = Integer.parseInt(m.group(3));
        char op = m.group(2).charAt(0);
        opOut[0] = op;
        return switch (op) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> {
                assertEquals(0, a % b, "division not exact: " + text);
                yield a / b;
            }
            default -> throw new AssertionError("bad op " + op);
        };
    }

    @Test
    void difficultyOneRanges() {
        Random rnd = new Random(1);
        for (int i = 0; i < 1000; i++) {
            MathQuestion q = MathQuiz.next(1, rnd);
            char[] op = new char[1];
            int answer = recompute(q.text(), op);
            assertEquals(answer, q.answer(), q.text());
            assertTrue(op[0] == '+' || op[0] == '-', q.text());
            Matcher m = TEXT.matcher(q.text());
            m.matches();
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(3));
            assertTrue(a >= 1 && a <= 20 && b >= 1 && b <= 20, q.text());
            assertTrue(q.answer() >= 0, q.text());
        }
    }

    @Test
    void difficultyTwoRanges() {
        Random rnd = new Random(2);
        int products = 0;
        for (int i = 0; i < 1000; i++) {
            MathQuestion q = MathQuiz.next(2, rnd);
            char[] op = new char[1];
            int answer = recompute(q.text(), op);
            assertEquals(answer, q.answer(), q.text());
            Matcher m = TEXT.matcher(q.text());
            m.matches();
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(3));
            assertTrue(a >= 1 && b >= 1, q.text());
            if (op[0] == '*') {
                products++;
                assertTrue(a <= 12 && b <= 12, q.text());
            } else {
                assertTrue(a <= 100 && b <= 100, q.text());
                assertTrue(q.answer() >= 0, q.text());
                assertTrue(op[0] == '+' || op[0] == '-', q.text());
            }
        }
        assertTrue(products > 0, "expected some multiplications");
    }

    @Test
    void difficultyThreeRanges() {
        Random rnd = new Random(3);
        int mult = 0, div = 0, add = 0, sub = 0, negative = 0;
        for (int i = 0; i < 1000; i++) {
            MathQuestion q = MathQuiz.next(3, rnd);
            char[] op = new char[1];
            int answer = recompute(q.text(), op);
            assertEquals(answer, q.answer(), q.text());
            Matcher m = TEXT.matcher(q.text());
            m.matches();
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(3));
            switch (op[0]) {
                case '*' -> {
                    mult++;
                    assertTrue(a >= 10 && a <= 99, q.text());
                    assertTrue(b >= 2 && b <= 9, q.text());
                }
                case '/' -> {
                    div++;
                    assertTrue(b >= 2 && b <= 9, q.text());
                    assertTrue(a >= 10 && a <= 99, q.text());
                    assertEquals(0, a % b, q.text());
                    assertTrue(q.answer() >= 2 && q.answer() <= 49, q.text());
                }
                case '+' -> {
                    add++;
                    assertTrue(a >= 100 && a <= 999 && b >= 100 && b <= 999, q.text());
                }
                case '-' -> {
                    sub++;
                    assertTrue(a >= 100 && a <= 999 && b >= 100 && b <= 999, q.text());
                    if (q.answer() < 0) {
                        negative++;
                    }
                }
                default -> throw new AssertionError(q.text());
            }
        }
        assertTrue(mult > 0 && div > 0 && add > 0 && sub > 0, "all operation kinds should appear");
        assertTrue(negative > 0, "difficulty 3 subtraction is allowed to go negative");
    }

    @Test
    void outOfRangeDifficultyIsClamped() {
        Random rnd = new Random(4);
        for (int i = 0; i < 100; i++) {
            MathQuestion low = MathQuiz.next(-5, rnd);
            assertTrue(low.answer() >= 0, low.text());
            MathQuestion high = MathQuiz.next(99, rnd);
            char[] op = new char[1];
            assertEquals(high.answer(), recompute(high.text(), op));
        }
    }

    @Test
    void sameSeedGivesSameQuestions() {
        for (int d = 1; d <= 3; d++) {
            Random a = new Random(77);
            Random b = new Random(77);
            for (int i = 0; i < 50; i++) {
                assertEquals(MathQuiz.next(d, a), MathQuiz.next(d, b));
            }
        }
    }

    @Test
    void checkAcceptsCorrectAnswersWithTolerance() {
        MathQuestion q = new MathQuestion("17 + 5 = ?", 22);
        assertTrue(MathQuiz.check(q, "22"));
        assertTrue(MathQuiz.check(q, " 22 "));
        assertTrue(MathQuiz.check(q, "= 22"));
        assertTrue(MathQuiz.check(q, "=22"));
        assertTrue(MathQuiz.check(q, "  =  22  "));
        assertTrue(MathQuiz.check(q, "==22"));
        assertTrue(MathQuiz.check(q, "+22"));
    }

    @Test
    void checkRejectsWrongAndNonNumericAnswers() {
        MathQuestion q = new MathQuestion("17 + 5 = ?", 22);
        assertFalse(MathQuiz.check(q, "23"));
        assertFalse(MathQuiz.check(q, "2 2"));
        assertFalse(MathQuiz.check(q, "22.0"));
        assertFalse(MathQuiz.check(q, "twenty two"));
        assertFalse(MathQuiz.check(q, ""));
        assertFalse(MathQuiz.check(q, "   "));
        assertFalse(MathQuiz.check(q, "="));
        assertFalse(MathQuiz.check(q, null));
        assertFalse(MathQuiz.check(null, "22"));
    }

    @Test
    void checkHandlesNegativeAnswers() {
        MathQuestion q = new MathQuestion("100 - 250 = ?", -150);
        assertTrue(MathQuiz.check(q, "-150"));
        assertTrue(MathQuiz.check(q, "= -150"));
        assertFalse(MathQuiz.check(q, "150"));
    }

    @Test
    void generatedAnswersPassCheck() {
        Random rnd = new Random(5);
        for (int d = 1; d <= 3; d++) {
            for (int i = 0; i < 200; i++) {
                MathQuestion q = MathQuiz.next(d, rnd);
                assertTrue(MathQuiz.check(q, Integer.toString(q.answer())), q.text());
                assertFalse(MathQuiz.check(q, Integer.toString(q.answer() + 1)), q.text());
            }
        }
    }
}
