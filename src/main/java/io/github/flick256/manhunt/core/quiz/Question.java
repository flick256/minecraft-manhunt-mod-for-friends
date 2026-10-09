package io.github.flick256.manhunt.core.quiz;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One respawn question. Multiple choice questions have {@code choices} (the correct one is {@code correct});
 * numeric questions have no choices and {@code correct} is the number as text.
 */
public record Question(Topic topic, String prompt, List<String> choices, String correct) {

    public Question {
        choices = List.copyOf(choices);
    }

    /** Multiple choice question. Give the correct answer first, then exactly three wrong ones; they are shuffled when asked. */
    public static Question mcq(Topic topic, String prompt, String correct, String... wrong) {
        List<String> all = new ArrayList<>();
        all.add(correct);
        all.addAll(List.of(wrong));
        return new Question(topic, prompt, all, correct);
    }

    /** Numeric question with an exact answer. */
    public static Question numeric(Topic topic, String prompt, String answer) {
        return new Question(topic, prompt, List.of(), answer);
    }

    public boolean isMultipleChoice() {
        return !choices.isEmpty();
    }

    /** The question as shown to the player, with lettered options when it is multiple choice. */
    public String display() {
        if (!isMultipleChoice()) {
            return prompt;
        }
        StringBuilder sb = new StringBuilder(prompt);
        for (int i = 0; i < choices.size(); i++) {
            sb.append("  ").append((char) ('A' + i)).append(") ").append(choices.get(i));
        }
        return sb.toString();
    }

    /** True if {@code input} is the right answer: a letter or the text for multiple choice, a number for numeric. */
    public boolean check(String input) {
        if (input == null) {
            return false;
        }
        String s = input.trim();
        while (s.startsWith("=")) {
            s = s.substring(1).trim();
        }
        if (s.isEmpty()) {
            return false;
        }
        return isMultipleChoice() ? checkChoice(s) : checkNumber(s);
    }

    /**
     * True if {@code input} looks like an attempt to answer (a letter A-D or one of the choice texts for multiple
     * choice, a number for numeric). Other chat is not an answer and should not cost the player a question.
     */
    public boolean isAnswerAttempt(String input) {
        if (input == null) {
            return false;
        }
        String s = input.trim();
        while (s.startsWith("=")) {
            s = s.substring(1).trim();
        }
        if (s.isEmpty()) {
            return false;
        }
        if (!isMultipleChoice()) {
            return parseNumber(s) != null;
        }
        String t = s;
        if (t.length() == 2 && (t.charAt(1) == ')' || t.charAt(1) == '.')) {
            t = t.substring(0, 1);
        }
        if (t.length() == 1) {
            int idx = Character.toUpperCase(t.charAt(0)) - 'A';
            if (idx >= 0 && idx < choices.size()) {
                return true;
            }
        }
        String n = normalise(s);
        for (String c : choices) {
            if (normalise(c).equals(n)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkChoice(String s) {
        if (s.length() == 1) {
            int idx = Character.toUpperCase(s.charAt(0)) - 'A';
            if (idx >= 0 && idx < choices.size()) {
                return normalise(choices.get(idx)).equals(normalise(correct));
            }
        }
        // "b)" / "B." style
        if (s.length() == 2 && (s.charAt(1) == ')' || s.charAt(1) == '.')) {
            return checkChoice(s.substring(0, 1));
        }
        return normalise(s).equals(normalise(correct));
    }

    private boolean checkNumber(String s) {
        Double given = parseNumber(s);
        Double want = parseNumber(correct);
        return given != null && want != null && Math.abs(given - want) < 1e-6;
    }

    /** Accepts integers, decimals and simple fractions such as 3/4. */
    static Double parseNumber(String text) {
        String t = text.trim().replace(',', '.');
        try {
            int slash = t.indexOf('/');
            if (slash > 0) {
                double a = Double.parseDouble(t.substring(0, slash).trim());
                double b = Double.parseDouble(t.substring(slash + 1).trim());
                return b == 0 ? null : a / b;
            }
            double d = Double.parseDouble(t);
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Test helper: the normalised form used to compare answers. */
    public static String normaliseForTest(String text) {
        return normalise(text);
    }

    /** Lower case, accents removed, punctuation and extra spaces dropped. */
    static String normalise(String text) {
        String n = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.replaceAll("[^a-z0-9+\\-*/=<>^.% ]", " ").replaceAll("\\s+", " ").trim();
    }
}
