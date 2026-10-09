package io.github.flick256.manhunt.core;

/** Computes when each hunter is released, in seconds since the game started. */
public final class Schedule {
    private Schedule() {}

    /**
     * ALL_AT_ONCE: every entry is {@code headStart}.
     * STAGGERED: entry i is {@code headStart + i * stagger}.
     */
    public static int[] releaseSeconds(int hunterCount, int headStart, ReleaseMode mode, int stagger) {
        if (hunterCount <= 0) {
            return new int[0];
        }
        boolean staggered = mode == ReleaseMode.STAGGERED;
        int[] out = new int[hunterCount];
        for (int i = 0; i < hunterCount; i++) {
            out[i] = staggered ? headStart + i * stagger : headStart;
        }
        return out;
    }
}
