package io.github.flick256.manhunt.core.quiz;

import static org.junit.jupiter.api.Assertions.*;

import io.github.flick256.manhunt.core.Settings;
import java.util.Random;
import org.junit.jupiter.api.Test;

class QuestionTest {
    @Test
    void multipleChoiceAcceptsLetterOrText() {
        Question q = new Question(Topic.BIOLOGY, "Q?", java.util.List.of("Nucleus", "Mitochondrion", "Ribosome", "Golgi"), "Mitochondrion");
        assertTrue(q.check("b"));
        assertTrue(q.check("B"));
        assertTrue(q.check(" b) "));
        assertTrue(q.check("mitochondrion"));
        assertTrue(q.check("= Mitochondrion"));
        assertFalse(q.check("A"));
        assertFalse(q.check("nucleus"));
        assertFalse(q.check("E"));
        assertFalse(q.check(""));
        assertFalse(q.check(null));
    }

    @Test
    void frenchAnswersIgnoreAccents() {
        Question q = new Question(Topic.FRENCH, "Q?", java.util.List.of("été", "hiver", "printemps", "automne"), "été");
        assertTrue(q.check("ete"));
        assertTrue(q.check("été"));
        assertTrue(q.check("A"));
    }

    @Test
    void numericAcceptsIntegersDecimalsAndFractions() {
        Question q = Question.numeric(Topic.METHODS, "Q", "0.5");
        assertTrue(q.check("0.5"));
        assertTrue(q.check("1/2"));
        assertTrue(q.check(".5"));
        assertFalse(q.check("2"));
        assertFalse(q.check("abc"));
        Question n = Question.numeric(Topic.MATH, "Q", "-3");
        assertTrue(n.check("-3"));
        assertTrue(n.check(" =-3 "));
        assertFalse(n.check("3"));
    }

    @Test
    void mathTopicUsesTheMathQuiz() {
        Random r = new Random(1);
        for (int i = 0; i < 200; i++) {
            Question q = QuizBank.next(Topic.MATH, 1 + i % 3, r, null);
            assertFalse(q.isMultipleChoice());
            assertTrue(q.prompt().endsWith("= ?"), q.prompt());
            assertNotNull(Question.parseNumber(q.correct()));
        }
    }

    @Test
    void topicParsingAndSettingsTopics() {
        assertEquals(Topic.BIOLOGY, Topic.parse("Bio"));
        assertEquals(Topic.METHODS, Topic.parse("Maths Methods"));
        assertEquals(Topic.FRENCH, Topic.parse("FRANÇAIS"));
        assertNull(Topic.parse("cooking"));
        Settings s = new Settings();
        assertEquals(java.util.List.of(Topic.MATH), s.enabledTopics());
        s.setTopic(Topic.PHYSICS, true);
        s.setTopic(Topic.BIOLOGY, true);
        assertEquals(java.util.List.of(Topic.MATH, Topic.BIOLOGY, Topic.PHYSICS), s.enabledTopics());
        s.setTopic(Topic.MATH, false);
        s.setTopic(Topic.BIOLOGY, false);
        s.setTopic(Topic.PHYSICS, false); // last one stays on
        assertEquals(java.util.List.of(Topic.PHYSICS), s.enabledTopics());
        s.quizTopics = new java.util.ArrayList<>(java.util.List.of("junk"));
        s.clamp();
        assertEquals(java.util.List.of(Topic.MATH), s.enabledTopics());
    }
}
