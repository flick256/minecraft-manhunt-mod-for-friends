package io.github.flick256.manhunt.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ScheduleTest {

    @Test
    void allAtOnceUsesHeadStartForEveryone() {
        assertArrayEquals(new int[]{60, 60, 60},
                Schedule.releaseSeconds(3, 60, ReleaseMode.ALL_AT_ONCE, 30));
    }

    @Test
    void staggeredAddsGapPerHunter() {
        assertArrayEquals(new int[]{60, 120, 180},
                Schedule.releaseSeconds(3, 60, ReleaseMode.STAGGERED, 60));
    }

    @Test
    void staggeredWithZeroHeadStart() {
        assertArrayEquals(new int[]{0, 5, 10, 15},
                Schedule.releaseSeconds(4, 0, ReleaseMode.STAGGERED, 5));
    }

    @Test
    void noHuntersGivesEmptyArray() {
        assertArrayEquals(new int[0], Schedule.releaseSeconds(0, 60, ReleaseMode.STAGGERED, 60));
        assertArrayEquals(new int[0], Schedule.releaseSeconds(-2, 60, ReleaseMode.ALL_AT_ONCE, 60));
    }

    @Test
    void singleHunterIsHeadStart() {
        assertArrayEquals(new int[]{90}, Schedule.releaseSeconds(1, 90, ReleaseMode.STAGGERED, 10));
    }
}
