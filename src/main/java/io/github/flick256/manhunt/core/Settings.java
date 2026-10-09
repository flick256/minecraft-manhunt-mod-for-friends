package io.github.flick256.manhunt.core;

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
    }

    /** Shared pool max health in game hp (two per heart). */
    public float poolHealth() {
        return poolHearts * 2f;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
