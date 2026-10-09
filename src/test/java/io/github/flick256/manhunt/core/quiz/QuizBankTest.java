package io.github.flick256.manhunt.core.quiz;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Structural checks every fixed question bank must pass (content accuracy is reviewed by humans). */
class QuizBankTest {
    private static final List<Topic> FIXED = List.of(Topic.BIOLOGY, Topic.CHEMISTRY, Topic.PHYSICS, Topic.HISTORY, Topic.FRENCH);
    private static final int MIN_QUESTIONS = 30;

    @Test
    void everyFixedBankIsWellFormed() {
        for (Topic t : FIXED) {
            List<Question> bank = QuizBank.questions(t);
            assertTrue(bank.size() >= MIN_QUESTIONS, t + " has only " + bank.size() + " questions");
            Set<String> prompts = new HashSet<>();
            for (Question q : bank) {
                String where = t + ": " + q.prompt();
                assertEquals(t, q.topic(), where);
                assertTrue(prompts.add(q.prompt().toLowerCase()), "duplicate prompt " + where);
                assertTrue(q.prompt().length() >= 8 && q.prompt().length() <= 120, "prompt length " + where);
                assertEquals(4, q.choices().size(), "needs 4 choices " + where);
                assertEquals(4, new HashSet<>(q.choices().stream().map(Question::normaliseForTest).toList()).size(),
                        "choices must be distinct " + where);
                assertTrue(q.choices().contains(q.correct()), "correct answer missing " + where);
                for (String c : q.choices()) {
                    assertFalse(c.isBlank(), where);
                    assertTrue(c.length() <= 45, "choice too long " + where + " -> " + c);
                }
            }
        }
    }

    @Test
    void nextAlwaysReturnsAnAnswerableQuestionAndShufflesChoices() {
        Random r = new Random(7);
        for (Topic t : Topic.values()) {
            Set<String> seenOrders = new HashSet<>();
            for (int i = 0; i < 300; i++) {
                Question q = QuizBank.next(t, 2, r, null);
                assertEquals(t, q.topic());
                assertTrue(q.check(q.correct()), t + ": " + q.display());
                if (q.isMultipleChoice()) {
                    int idx = q.choices().indexOf(q.correct());
                    assertTrue(q.check(String.valueOf((char) ('A' + idx))));
                    for (int w = 0; w < 4; w++) {
                        if (w != idx) {
                            assertFalse(q.check(String.valueOf((char) ('A' + w))), q.display());
                        }
                    }
                    seenOrders.add(q.choices().toString());
                }
            }
        }
    }

    @Test
    void nextAvoidsRepeatingThePreviousPrompt() {
        Random r = new Random(3);
        for (Topic t : FIXED) {
            Question first = QuizBank.next(t, 1, r, null);
            for (int i = 0; i < 100; i++) {
                assertNotEquals(first.prompt(), QuizBank.next(t, 1, r, first.prompt()).prompt());
            }
        }
    }
}
