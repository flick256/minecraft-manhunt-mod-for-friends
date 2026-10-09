package io.github.flick256.manhunt.core;

import java.util.Random;

/**
 * Generates and checks respawn math questions.
 *
 * <ul>
 *   <li>1: + and - with operands 1..20, answers never negative.</li>
 *   <li>2: + and - with operands 1..100 (answers never negative), and single products of 1..12 x 1..12.</li>
 *   <li>3: 2-digit x 1-digit products, exact 2-digit divisions by 2..9, 3-digit + and - (subtraction may go negative).</li>
 * </ul>
 */
public final class MathQuiz {
    private MathQuiz() {}

    public static MathQuestion next(int difficulty, Random rnd) {
        int d = Math.max(1, Math.min(3, difficulty));
        switch (d) {
            case 1:
                return easy(rnd);
            case 2:
                return medium(rnd);
            default:
                return hard(rnd);
        }
    }

    /** True if {@code input} is the answer. Trims whitespace, accepts a leading '=', false on non-numeric input. */
    public static boolean check(MathQuestion q, String input) {
        if (q == null || input == null) {
            return false;
        }
        String s = input.trim();
        while (s.startsWith("=")) {
            s = s.substring(1).trim();
        }
        if (s.isEmpty()) {
            return false;
        }
        try {
            return Integer.parseInt(s) == q.answer();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static MathQuestion easy(Random r) {
        int a = 1 + r.nextInt(20);
        int b = 1 + r.nextInt(20);
        if (r.nextBoolean()) {
            return binary(a, '+', b, a + b);
        }
        if (a < b) {
            int t = a;
            a = b;
            b = t;
        }
        return binary(a, '-', b, a - b);
    }

    private static MathQuestion medium(Random r) {
        switch (r.nextInt(3)) {
            case 0: {
                int a = 1 + r.nextInt(100);
                int b = 1 + r.nextInt(100);
                return binary(a, '+', b, a + b);
            }
            case 1: {
                int a = 1 + r.nextInt(100);
                int b = 1 + r.nextInt(100);
                if (a < b) {
                    int t = a;
                    a = b;
                    b = t;
                }
                return binary(a, '-', b, a - b);
            }
            default: {
                int a = 1 + r.nextInt(12);
                int b = 1 + r.nextInt(12);
                return binary(a, '*', b, a * b);
            }
        }
    }

    private static MathQuestion hard(Random r) {
        switch (r.nextInt(4)) {
            case 0: {
                int a = 10 + r.nextInt(90);
                int b = 2 + r.nextInt(8);
                return binary(a, '*', b, a * b);
            }
            case 1: {
                int b = 2 + r.nextInt(8);
                int qMin = (10 + b - 1) / b;
                int qMax = 99 / b;
                int q = qMin + r.nextInt(qMax - qMin + 1);
                return binary(b * q, '/', b, q);
            }
            case 2: {
                int a = 100 + r.nextInt(900);
                int b = 100 + r.nextInt(900);
                return binary(a, '+', b, a + b);
            }
            default: {
                int a = 100 + r.nextInt(900);
                int b = 100 + r.nextInt(900);
                return binary(a, '-', b, a - b);
            }
        }
    }

    private static MathQuestion binary(int a, char op, int b, int answer) {
        return new MathQuestion(a + " " + op + " " + b + " = ?", answer);
    }
}
