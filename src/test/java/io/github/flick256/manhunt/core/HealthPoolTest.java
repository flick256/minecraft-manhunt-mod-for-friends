package io.github.flick256.manhunt.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthPoolTest {
    private static final float DELTA = 1e-4f;

    @Test
    void noChangeKeepsValue() {
        HealthPool p = new HealthPool(40, 40);
        assertEquals(40f, p.merge(new float[]{40f}), DELTA);
        assertEquals(40f, p.current(), DELTA);
    }

    @Test
    void singleMemberDamage() {
        HealthPool p = new HealthPool(40, 40);
        assertEquals(30f, p.merge(new float[]{30f}), DELTA);
        assertEquals(30f, p.current(), DELTA);
    }

    @Test
    void damageFromSeveralMembersInSameTickCountsLargestOnly() {
        HealthPool p = new HealthPool(40, 40);
        // member damages: 10 and 5 from one hit that touched both -> the pool loses the larger one
        assertEquals(30f, p.merge(new float[]{30f, 35f, 40f}), DELTA);
    }

    @Test
    void smallHealFromNonPrimaryMemberIsIgnoredButBigHealCounts() {
        HealthPool p = new HealthPool(20, 40);
        // member 1 regenerates 1 hp on its own timer: ignored; member 0 (primary) regen counts
        assertEquals(20f, p.merge(new float[]{20f, 21f}, new boolean[]{true, false}), DELTA);
        assertEquals(21f, p.merge(new float[]{21f, 20f}, new boolean[]{true, false}), DELTA);
        // a golden apple style heal of 4 hp on a non-primary member counts
        assertEquals(25f, p.merge(new float[]{21f, 25f}, new boolean[]{true, false}), DELTA);
    }

    @Test
    void healingTakesLargestSingleGainNotSum() {
        HealthPool p = new HealthPool(20, 40);
        // gains 10 and 5 -> counts once as max = 10
        assertEquals(30f, p.merge(new float[]{30f, 25f}), DELTA);
    }

    @Test
    void mixedDamageAndHealAreCombined() {
        HealthPool p = new HealthPool(30, 40);
        // damage 5 (from 25), gain 5 (from 35) -> 30 - 5 + 5
        assertEquals(30f, p.merge(new float[]{25f, 35f}), DELTA);
    }

    @Test
    void mergeIsClampedToMax() {
        HealthPool p = new HealthPool(38, 40);
        assertEquals(40f, p.merge(new float[]{40f, 40f}), DELTA);
        assertEquals(40f, p.merge(new float[]{45f}), DELTA);
    }

    @Test
    void mergeIsClampedToZero() {
        HealthPool p = new HealthPool(5, 40);
        assertEquals(0f, p.merge(new float[]{0f, 0f, 0f}), DELTA);
        assertTrue(p.isDead());
    }

    @Test
    void mergeUsesStoredValueAsBaseline() {
        HealthPool p = new HealthPool(40, 40);
        p.merge(new float[]{30f});
        // baseline is now 30, members still at 30: no change
        assertEquals(30f, p.merge(new float[]{30f}), DELTA);
        // one more hit of 4 from baseline 30
        assertEquals(26f, p.merge(new float[]{26f}), DELTA);
    }

    @Test
    void tinyDifferencesAreIgnored() {
        HealthPool p = new HealthPool(40, 40);
        assertEquals(40f, p.merge(new float[]{40f - 0.00005f}), DELTA);
        assertEquals(40f, p.merge(new float[]{40f + 0.00005f}), DELTA);
    }

    @Test
    void nullOrEmptyMemberArrayKeepsCurrent() {
        HealthPool p = new HealthPool(12, 40);
        assertEquals(12f, p.merge(null), DELTA);
        assertEquals(12f, p.merge(new float[0]), DELTA);
        assertEquals(12f, p.current(), DELTA);
    }

    @Test
    void nonLethalDamageNeverGoesBelowOne() {
        HealthPool p = new HealthPool(10, 40);
        p.damage(20f, false);
        assertEquals(1f, p.current(), DELTA);
        assertFalse(p.isDead());
    }

    @Test
    void nonLethalDamageOnLowPoolDoesNotIncreaseHealth() {
        HealthPool p = new HealthPool(1f, 40);
        p.damage(5f, false);
        assertEquals(1f, p.current(), DELTA);
        HealthPool low = new HealthPool(0.5f, 40);
        low.damage(3f, false);
        assertEquals(0.5f, low.current(), DELTA);
    }

    @Test
    void lethalDamageCanKill() {
        HealthPool p = new HealthPool(10, 40);
        p.damage(20f, true);
        assertEquals(0f, p.current(), DELTA);
        assertTrue(p.isDead());
    }

    @Test
    void deadThreshold() {
        assertTrue(new HealthPool(0f, 40).isDead());
        assertTrue(new HealthPool(0.00005f, 40).isDead());
        assertFalse(new HealthPool(0.01f, 40).isDead());
    }

    @Test
    void healIsCappedAtMax() {
        HealthPool p = new HealthPool(35, 40);
        p.heal(10f);
        assertEquals(40f, p.current(), DELTA);
        p.heal(-3f);
        assertEquals(40f, p.current(), DELTA);
    }

    @Test
    void setClampsToRange() {
        HealthPool p = new HealthPool(10, 40);
        p.set(100f);
        assertEquals(40f, p.current(), DELTA);
        p.set(-5f);
        assertEquals(0f, p.current(), DELTA);
    }

    @Test
    void setMaxClampsCurrent() {
        HealthPool p = new HealthPool(40, 40);
        p.setMax(30f);
        assertEquals(30f, p.max(), DELTA);
        assertEquals(30f, p.current(), DELTA);
        p.setMax(60f);
        assertEquals(60f, p.max(), DELTA);
        assertEquals(30f, p.current(), DELTA);
    }

    @Test
    void constructorClampsCurrentIntoRange() {
        assertEquals(40f, new HealthPool(55f, 40f).current(), DELTA);
        assertEquals(0f, new HealthPool(-3f, 40f).current(), DELTA);
    }
}
