package io.github.flick256.manhunt.core;

import io.github.flick256.manhunt.core.quiz.Topic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Tunable game settings. Public mutable fields so Gson and menus can read and write them. Call {@link #clamp()} after changes. */
public final class Settings {
    /** Whole hearts shared by a runner team, 1..200. */
    public int poolHearts = 20;
    /** Drain divisor for shared hunger, 1.0..8.0. */
    public double hungerMultiplier = 2.0;
    /** Seconds hunters stay frozen at the start, 0..3600. */
    public int headStartSeconds = 60;
    public ReleaseMode releaseMode = ReleaseMode.ALL_AT_ONCE;
    /** Gap in seconds between hunters when STAGGERED, 1..600. */
    public int staggerSeconds = 60;
    public boolean mathRespawn = false;
    /** Correct answers needed to respawn, 1..10. */
    public int mathQuestions = 1;
    /** Math difficulty, 1..3. */
    public int mathDifficulty = 1;
    /** Subjects a hunter may pick for the respawn quiz (Topic ids, at least one). */
    public List<String> quizTopics = new ArrayList<>(List.of(Topic.MATH.id()));
    public boolean shareInventory = true;
    public boolean shareEnderChest = true;
    public boolean shareHunger = true;
    /** Non-owners may join a team themselves while IDLE. */
    public boolean teamSelfSelect = true;
    public boolean clearInventoryOnStart = true;
    public boolean giveCompass = true;

    /** Clamps every numeric field into its range and replaces a null enum with its default. */
    public void clamp() {
        poolHearts = clampInt(poolHearts, 1, 200);
        if (Double.isNaN(hungerMultiplier)) {
            hungerMultiplier = 2.0;
        }
        hungerMultiplier = Math.max(1.0, Math.min(8.0, hungerMultiplier));
        headStartSeconds = clampInt(headStartSeconds, 0, 3600);
        if (releaseMode == null) {
            releaseMode = ReleaseMode.ALL_AT_ONCE;
        }
        staggerSeconds = clampInt(staggerSeconds, 1, 600);
        mathQuestions = clampInt(mathQuestions, 1, 10);
        mathDifficulty = clampInt(mathDifficulty, 1, 3);
        Set<String> ids = new LinkedHashSet<>();
        if (quizTopics != null) {
            for (String raw : quizTopics) {
                Topic t = Topic.parse(raw);
                if (t != null) {
                    ids.add(t.id());
                }
            }
        }
        if (ids.isEmpty()) {
            ids.add(Topic.MATH.id());
        }
        quizTopics = new ArrayList<>(ids);
    }

    /** The enabled respawn quiz subjects, in {@link Topic} order. Never empty. */
    public List<Topic> enabledTopics() {
        List<Topic> out = new ArrayList<>();
        for (Topic t : Topic.values()) {
            if (quizTopics != null && quizTopics.contains(t.id())) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            out.add(Topic.MATH);
        }
        return out;
    }

    public boolean isTopicEnabled(Topic t) {
        return enabledTopics().contains(t);
    }

    /** Turns a subject on or off. The last enabled subject cannot be switched off. Returns the new state. */
    public boolean setTopic(Topic t, boolean on) {
        Set<String> ids = new LinkedHashSet<>(quizTopics == null ? List.of() : quizTopics);
        if (on) {
            ids.add(t.id());
        } else if (ids.size() > 1 || !ids.contains(t.id())) {
            ids.remove(t.id());
        }
        quizTopics = new ArrayList<>(ids);
        clamp();
        return isTopicEnabled(t);
    }

    /** Shared pool max health in game hp (two per heart). */
    public float poolHealth() {
        return poolHearts * 2f;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
