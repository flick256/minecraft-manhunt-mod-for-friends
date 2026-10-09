package io.github.flick256.manhunt.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HungerPoolTest {
    private static final float DELTA = 1e-4f;

    @Test
    void constructorWritesValues() {
        HungerPool h = new HungerPool(20, 5f);
        assertEquals(20, h.food());
        assertEquals(5f, h.saturation(), DELTA);
    }

    @Test
    void constructorClampsFoodAndSaturation() {
        HungerPool h = new HungerPool(99, 50f);
        assertEquals(20, h.food());
        assertEquals(20f, h.saturation(), DELTA);
        HungerPool low = new HungerPool(-4, -2f);
        assertEquals(0, low.food());
        assertEquals(0f, low.saturation(), DELTA);
    }

    @Test
    void multiplierOneShowsEveryFoodPoint() {
        HungerPool h = new HungerPool(20, 5f);
        h.merge(new int[]{19}, new float[]{5f}, 1.0);
        assertEquals(19, h.food());
    }

    @Test
    void multiplierTwoHidesFirstLostPoint() {
        HungerPool h = new HungerPool(20, 5f);
        h.merge(new int[]{19}, new float[]{5f}, 2.0);
        assertEquals(20, h.food());
        h.merge(new int[]{19}, new float[]{5f}, 2.0);
        assertEquals(19, h.food());
    }

    @Test
    void multiplierTwoWithThreeLostPoints() {
        HungerPool h = new HungerPool(20, 5f);
        h.merge(new int[]{17}, new float[]{5f}, 2.0);
        // exact 18.5 -> written 19
        assertEquals(19, h.food());
    }

    @Test
    void drainUsesLargestMemberDrop() {
        HungerPool h = new HungerPool(20, 5f);
        // drops: 0, 2, 1 -> max 2 -> exact 20 - 1 = 19
        h.merge(new int[]{20, 18, 19}, new float[]{5f, 5f, 5f}, 2.0);
        assertEquals(19, h.food());
    }

    @Test
    void gainUsesLargestMemberGainAndIsNotScaled() {
        HungerPool h = new HungerPool(10, 0f);
        // gains 4 and 2 -> max 4 added unscaled
        h.merge(new int[]{14, 12}, new float[]{0f, 0f}, 2.0);
        assertEquals(14, h.food());
    }

    @Test
    void unchangedMembersDoNotChangeState() {
        HungerPool h = new HungerPool(17, 3.5f);
        for (int i = 0; i < 5; i++) {
            h.merge(new int[]{17, 17}, new float[]{3.5f, 3.5f}, 2.0);
        }
        assertEquals(17, h.food());
        assertEquals(3.5f, h.saturation(), DELTA);
    }

    @Test
    void saturationDrainIsScaledByMultiplier() {
        HungerPool h = new HungerPool(20, 5f);
        h.merge(new int[]{20}, new float[]{3f}, 2.0);
        // satDrop 2 / 2 = 1 -> exact sat 4
        assertEquals(4f, h.saturation(), DELTA);
        h.merge(new int[]{20}, new float[]{3f}, 1.0);
        // satDrop 1 (from written 4 to 3) / 1 = 1 -> 3
        assertEquals(3f, h.saturation(), DELTA);
    }

    @Test
    void saturationGainIsAddedAndCappedByFood() {
        HungerPool h = new HungerPool(20, 5f);
        h.merge(new int[]{20}, new float[]{6f}, 2.0);
        assertEquals(6f, h.saturation(), DELTA);
        HungerPool small = new HungerPool(4, 4f);
        small.merge(new int[]{4}, new float[]{20f}, 1.0);
        assertEquals(4f, small.saturation(), DELTA);
    }

    @Test
    void saturationNeverExceedsWrittenFood() {
        HungerPool h = new HungerPool(4, 4f);
        h.merge(new int[]{2}, new float[]{4f}, 1.0);
        assertEquals(2, h.food());
        assertEquals(2f, h.saturation(), DELTA);
    }

    @Test
    void foodReachesZeroAndStays() {
        HungerPool h = new HungerPool(1, 0f);
        h.merge(new int[]{0}, new float[]{0f}, 1.0);
        assertEquals(0, h.food());
        h.merge(new int[]{0}, new float[]{0f}, 1.0);
        assertEquals(0, h.food());
    }

    @Test
    void mismatchedArrayLengthsThrow() {
        HungerPool h = new HungerPool(20, 5f);
        assertThrows(IllegalArgumentException.class, () -> h.merge(new int[]{20, 20}, new float[]{5f}, 1.0));
    }

    @Test
    void nullOrEmptyMembersAreIgnored() {
        HungerPool h = new HungerPool(12, 2f);
        h.merge(null, null, 2.0);
        h.merge(new int[0], new float[0], 2.0);
        assertEquals(12, h.food());
        assertEquals(2f, h.saturation(), DELTA);
    }

    @Test
    void nonPositiveMultiplierActsAsOne() {
        HungerPool h = new HungerPool(20, 5f);
        h.merge(new int[]{19}, new float[]{5f}, 0.0);
        assertEquals(19, h.food());
    }

    @Test
    void drainIsUnscaled() {
        HungerPool h = new HungerPool(20, 5f);
        h.drain(3);
        assertEquals(17, h.food());
        assertEquals(5f, h.saturation(), DELTA);
        h.drain(10);
        assertEquals(7, h.food());
        assertEquals(5f, h.saturation(), DELTA);
        h.drain(20);
        assertEquals(0, h.food());
        assertEquals(0f, h.saturation(), DELTA);
    }

    @Test
    void drainCapsSaturationAtRemainingFood() {
        HungerPool h = new HungerPool(10, 8f);
        h.drain(6);
        assertEquals(4, h.food());
        assertEquals(4f, h.saturation(), DELTA);
    }

    @Test
    void drainNegativeIsIgnored() {
        HungerPool h = new HungerPool(10, 2f);
        h.drain(-5);
        assertEquals(10, h.food());
    }

    @Test
    void setReplacesValues() {
        HungerPool h = new HungerPool(20, 5f);
        h.set(25, 3f);
        assertEquals(20, h.food());
        h.set(-1, 0f);
        assertEquals(0, h.food());
        h.set(7, 30f);
        assertEquals(7, h.food());
        assertEquals(7f, h.saturation(), DELTA);
    }

    /**
     * Simulates a real player with mult 2: each step the player loses one real food point, the pool is merged, then
     * the written value is copied back to the player. The bar must last exactly twice as many real drops.
     */
    @Test
    void simulatedDrainLastsTwiceAsLong() {
        checkLastsSteps(2.0, 40);
    }

    @Test
    void simulatedDrainWithMultiplierOneLastsTwentySteps() {
        checkLastsSteps(1.0, 20);
    }

    private static void checkLastsSteps(double mult, int expectedSteps) {
        HungerPool h = new HungerPool(20, 0f);
        int actual = 20;
        int steps = 0;
        while (h.food() > 0 && steps < 1000) {
            actual = Math.max(0, actual - 1);
            h.merge(new int[]{actual}, new float[]{0f}, mult);
            actual = h.food();
            steps++;
        }
        assertEquals(expectedSteps, steps);
        assertEquals(0, h.food());
    }
}
