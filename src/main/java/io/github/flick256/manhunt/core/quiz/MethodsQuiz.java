package io.github.flick256.manhunt.core.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generator of easy VCE Mathematical Methods (Units 3 and 4) questions.
 *
 * <p>Each template draws its numbers from the {@link Random}, then computes the single exact answer in code.
 * The output is therefore deterministic for a seeded Random, and every answer is checkable by hand.
 */
public final class MethodsQuiz {
    private MethodsQuiz() {}

    /** Returned only if a template fails unexpectedly; the test suite checks that it is never produced. */
    static final String FALLBACK_PROMPT = "What is the derivative of x^2 at x = 3?";

    private static final int TEMPLATES = 28;
    private static final int[] BASES = {2, 3, 5};
    private static final int[] LOG_BASES = {2, 3, 5, 10};
    private static final int[] DIE_SIDES = {4, 6, 8, 10, 12};
    private static final int[] COMPLEMENT_DENOMINATORS = {4, 5, 8, 10};
    /** Probabilities num/den whose denominator divides a chosen n, so that n * p is a whole number. */
    private static final int[][] PROBABILITIES = {{1, 2}, {1, 4}, {3, 4}, {1, 5}, {2, 5}, {3, 5}, {1, 10}, {3, 10}};

    /** The next question for a seeded or unseeded Random. Never throws. */
    public static Question next(Random rnd) {
        if (rnd == null) {
            rnd = new Random();
        }
        try {
            switch (rnd.nextInt(TEMPLATES)) {
                case 0: return gradientOfLine(rnd);
                case 1: return gradientThroughPoints(rnd);
                case 2: return yIntercept(rnd);
                case 3: return solveLinear(rnd);
                case 4: return sumOfRoots(rnd);
                case 5: return turningPointX(rnd);
                case 6: return quadraticAtPoint(rnd);
                case 7: return domainStart(rnd);
                case 8: return indexMultiply(rnd);
                case 9: return indexDivide(rnd);
                case 10: return powerOfPower(rnd);
                case 11: return logOfPower(rnd);
                case 12: return logOfSquareBase(rnd);
                case 13: return lnExp(rnd);
                case 14: return derivativePower(rnd);
                case 15: return derivativeConstant(rnd);
                case 16: return gradientQuadraticAtPoint(rnd);
                case 17: return integralConstant(rnd);
                case 18: return integralLinear(rnd);
                case 19: return stationaryPoint(rnd);
                case 20: return dieGreaterThan(rnd);
                case 21: return independentEvents(rnd);
                case 22: return uniformExpected(rnd);
                case 23: return binomialMean(rnd);
                case 24: return coinSequences(rnd);
                case 25: return complementProbability(rnd);
                case 26: return normalRule(rnd);
                case 27: return indexLawMcq(rnd);
                default: return fallback();
            }
        } catch (RuntimeException e) {
            return fallback();
        }
    }

    // ---- functions and graphs -------------------------------------------------------------

    private static Question gradientOfLine(Random rnd) {
        int m = nonZero(rnd, 9);
        int c = nonZero(rnd, 9);
        return numeric("What is the gradient of the line y = " + head(m, "x") + tail(c, "") + "?", m);
    }

    private static Question gradientThroughPoints(Random rnd) {
        int m = nonZero(rnd, 6);
        int b = between(rnd, -9, 9);
        int x1 = between(rnd, -5, 5);
        int x2 = x1;
        while (x2 == x1) {
            x2 = between(rnd, -5, 5);
        }
        String p1 = "(" + x1 + ", " + (m * x1 + b) + ")";
        String p2 = "(" + x2 + ", " + (m * x2 + b) + ")";
        return numeric("What is the gradient of the line through " + p1 + " and " + p2 + "?", m);
    }

    private static Question yIntercept(Random rnd) {
        int m = nonZero(rnd, 9);
        int c = nonZero(rnd, 9);
        return numeric("What is the y-intercept of the line y = " + head(m, "x") + tail(c, "") + "?", c);
    }

    private static Question solveLinear(Random rnd) {
        int a = between(rnd, 2, 9);
        int x = between(rnd, -9, 9);
        int b = nonZero(rnd, 12);
        return numeric("Solve " + head(a, "x") + tail(b, "") + " = " + (a * x + b) + " for x.", x);
    }

    private static Question sumOfRoots(Random rnd) {
        int r1 = nonZero(rnd, 9);
        int r2 = r1;
        while (r2 == r1) {
            r2 = nonZero(rnd, 9);
        }
        return numeric("What is the sum of the roots of " + factor(r1) + factor(r2) + " = 0?", r1 + r2);
    }

    private static Question turningPointX(Random rnd) {
        int h = nonZero(rnd, 9);
        int k = nonZero(rnd, 9);
        return numeric("What is the x-coordinate of the turning point of y = (x" + tail(-h, "") + ")^2"
                + tail(k, "") + "?", h);
    }

    private static Question quadraticAtPoint(Random rnd) {
        int a = nonZero(rnd, 4);
        int b = between(rnd, -6, 6);
        int c = between(rnd, -9, 9);
        int k = between(rnd, -3, 3);
        int value = a * k * k + b * k + c;
        return numeric("If f(x) = " + head(a, "x^2") + tail(b, "x") + tail(c, "") + ", what is f(" + k + ")?", value);
    }

    private static Question domainStart(Random rnd) {
        int k = nonZero(rnd, 9);
        // y = sqrt(x + k) needs x + k >= 0, so the smallest x in the domain is -k.
        return numeric("What is the smallest value of x in the domain of y = sqrt(x" + tail(k, "") + ")?", -k);
    }

    // ---- algebra ---------------------------------------------------------------------------

    private static Question indexMultiply(Random rnd) {
        int base = BASES[rnd.nextInt(BASES.length)];
        int a = between(rnd, 1, 9);
        int b = between(rnd, 1, 9);
        return numeric("If " + base + "^" + a + " x " + base + "^" + b + " = " + base + "^n, what is n?", a + b);
    }

    private static Question indexDivide(Random rnd) {
        int a = between(rnd, 2, 9);
        int b = between(rnd, 1, a - 1);
        return numeric("If 10^" + a + " / 10^" + b + " = 10^n, what is n?", a - b);
    }

    private static Question powerOfPower(Random rnd) {
        int base = BASES[rnd.nextInt(BASES.length)];
        int a = between(rnd, 2, 5);
        int b = between(rnd, 2, 5);
        return numeric("If (" + base + "^" + a + ")^" + b + " = " + base + "^n, what is n?", a * b);
    }

    private static Question logOfPower(Random rnd) {
        int base = LOG_BASES[rnd.nextInt(LOG_BASES.length)];
        int k = between(rnd, 1, 9);
        return numeric("Evaluate log_" + base + "(" + base + "^" + k + ").", k);
    }

    private static Question logOfSquareBase(Random rnd) {
        int base = LOG_BASES[rnd.nextInt(LOG_BASES.length)];
        int k = between(rnd, 1, 6);
        // log_b((b^2)^k) = log_b(b^(2k)) = 2k
        return numeric("Evaluate log_" + base + "(" + (base * base) + "^" + k + ").", 2 * k);
    }

    private static Question lnExp(Random rnd) {
        int k = between(rnd, 1, 9);
        return numeric("What is ln(e^" + k + ") + e^0?", k + 1);
    }

    // ---- calculus --------------------------------------------------------------------------

    private static Question derivativePower(Random rnd) {
        int a = nonZero(rnd, 6);
        int n = between(rnd, 2, 4);
        int k = nonZero(rnd, 3);
        // d/dx (a x^n) = a n x^(n-1)
        return numeric("What is the gradient of y = " + head(a, "x^" + n) + " at x = " + k + "?",
                a * n * ipow(k, n - 1));
    }

    private static Question derivativeConstant(Random rnd) {
        int k = nonZero(rnd, 9);
        return numeric("What is the gradient of the horizontal line y = " + k + "?", 0);
    }

    private static Question gradientQuadraticAtPoint(Random rnd) {
        int b = between(rnd, -6, 6);
        int k = between(rnd, -4, 4);
        // d/dx (x^2 + bx) = 2x + b
        return numeric("What is the gradient of y = x^2" + tail(b, "x") + " at x = " + k + "?", 2 * k + b);
    }

    private static Question integralConstant(Random rnd) {
        int c = nonZero(rnd, 9);
        int k = between(rnd, 1, 9);
        return numeric("Evaluate the integral of " + c + " dx from 0 to " + k + ".", c * k);
    }

    private static Question integralLinear(Random rnd) {
        int m = between(rnd, 1, 5);
        int k = between(rnd, 1, 6);
        // integral of 2m x from 0 to k is m k^2
        return numeric("Evaluate the integral of " + (2 * m) + "x dx from 0 to " + k + ".", m * k * k);
    }

    private static Question stationaryPoint(Random rnd) {
        int p = nonZero(rnd, 9);
        // y = x^2 - 2px has dy/dx = 2x - 2p = 0 at x = p
        return numeric("What is the x-coordinate of the stationary point of y = x^2" + tail(-2 * p, "x") + "?", p);
    }

    // ---- probability and statistics -------------------------------------------------------

    private static Question dieGreaterThan(Random rnd) {
        int sides = DIE_SIDES[rnd.nextInt(DIE_SIDES.length)];
        int k = between(rnd, 1, sides - 1);
        return numeric("A fair " + sides + "-sided die is rolled. What is P(number greater than " + k + ")?",
                fraction(sides - k, sides));
    }

    private static Question independentEvents(Random rnd) {
        int d1 = between(rnd, 2, 5);
        int n1 = between(rnd, 1, d1 - 1);
        int d2 = between(rnd, 2, 5);
        int n2 = between(rnd, 1, d2 - 1);
        return numeric("A and B are independent, with P(A) = " + n1 + "/" + d1 + " and P(B) = " + n2 + "/" + d2
                + ". What is P(A and B)?", fraction(n1 * n2, d1 * d2));
    }

    private static Question uniformExpected(Random rnd) {
        int n = 2 * between(rnd, 1, 5) + 1;
        // A fair spinner on 1..n has expected value (n + 1) / 2, a whole number for odd n.
        return numeric("A fair spinner is equally likely to show each whole number from 1 to " + n
                + ". Expected value?", (n + 1) / 2);
    }

    private static Question binomialMean(Random rnd) {
        int[] p = PROBABILITIES[rnd.nextInt(PROBABILITIES.length)];
        int k = between(rnd, 2, 9);
        int n = p[1] * k;
        // E(X) = np = (p[0] / p[1]) * n = p[0] * k
        return numeric("A binomial X has n = " + n + " and p = " + p[0] + "/" + p[1] + ". What is E(X)?", p[0] * k);
    }

    private static Question coinSequences(Random rnd) {
        int n = between(rnd, 1, 10);
        return numeric("How many different sequences of heads and tails are possible from " + n
                + " tosses of a coin?", 1 << n);
    }

    private static Question complementProbability(Random rnd) {
        int den = COMPLEMENT_DENOMINATORS[rnd.nextInt(COMPLEMENT_DENOMINATORS.length)];
        int num = between(rnd, 1, den - 1);
        return numeric("If P(A) = " + num + "/" + den + ", what is P(A')?", fraction(den - num, den));
    }

    private static Question normalRule(Random rnd) {
        int k = between(rnd, 1, 3);
        String[] within = {"68%", "95%", "99.7%"};
        String correct = within[k - 1];
        String[] wrong = new String[3];
        int w = 0;
        for (String s : within) {
            if (!s.equals(correct)) {
                wrong[w++] = s;
            }
        }
        wrong[2] = "50%";
        return multipleChoice(rnd,
                "About what % of a normal distribution lies within " + k + " standard deviation"
                        + (k == 1 ? "" : "s") + " of the mean?",
                correct, wrong);
    }

    private static Question indexLawMcq(Random rnd) {
        return multipleChoice(rnd, "Which expression equals a^m x a^n?", "a^(m+n)",
                "a^(mn)", "a^(m-n)", "a^(m/n)");
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static Question numeric(String prompt, long answer) {
        return Question.numeric(Topic.METHODS, prompt, String.valueOf(answer));
    }

    private static Question numeric(String prompt, String answer) {
        return Question.numeric(Topic.METHODS, prompt, answer);
    }

    /** A multiple choice question with the correct answer placed at a random position. */
    private static Question multipleChoice(Random rnd, String prompt, String correct, String... wrong) {
        List<String> choices = new ArrayList<>(Question.mcq(Topic.METHODS, prompt, correct, wrong).choices());
        Collections.shuffle(choices, rnd);
        return new Question(Topic.METHODS, prompt, choices, correct);
    }

    private static Question fallback() {
        return Question.numeric(Topic.METHODS, FALLBACK_PROMPT, "6");
    }

    /** Integer in lo..hi inclusive (hi must be at least lo). */
    private static int between(Random rnd, int lo, int hi) {
        return lo + rnd.nextInt(hi - lo + 1);
    }

    /** Non-zero integer with absolute value 1..max. */
    private static int nonZero(Random rnd, int max) {
        int v = 1 + rnd.nextInt(max);
        return rnd.nextBoolean() ? v : -v;
    }

    /** A leading term: 3x, -x, x^2, -4 (the coefficient is dropped when it is 1 and a variable follows). */
    private static String head(int c, String v) {
        String body = (Math.abs(c) == 1 && !v.isEmpty() ? "" : String.valueOf(Math.abs(c))) + v;
        return c < 0 ? "-" + body : body;
    }

    /** A following term with its sign: " + 3x", " - x", " - 5". Empty when c is 0. */
    private static String tail(int c, String v) {
        if (c == 0) {
            return "";
        }
        String body = (Math.abs(c) == 1 && !v.isEmpty() ? "" : String.valueOf(Math.abs(c))) + v;
        return (c < 0 ? " - " : " + ") + body;
    }

    /** The bracket for a root r of the equation: (x - r). */
    private static String factor(int r) {
        return r >= 0 ? "(x - " + r + ")" : "(x + " + (-r) + ")";
    }

    /** An exact fraction in lowest terms; num and den must be positive. */
    private static String fraction(int num, int den) {
        int g = gcd(num, den);
        int n = num / g;
        int d = den / g;
        return d == 1 ? String.valueOf(n) : n + "/" + d;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static int ipow(int base, int exp) {
        int result = 1;
        for (int i = 0; i < exp; i++) {
            result *= base;
        }
        return result;
    }
}
