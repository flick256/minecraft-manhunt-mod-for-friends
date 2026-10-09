package io.github.flick256.manhunt.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsTest {

    @Test
    void defaultsMatchContract() {
        Settings s = new Settings();
        assertEquals(20, s.poolHearts);
        assertEquals(2.0, s.hungerMultiplier);
        assertEquals(60, s.headStartSeconds);
        assertEquals(ReleaseMode.ALL_AT_ONCE, s.releaseMode);
        assertEquals(60, s.staggerSeconds);
        assertEquals(false, s.mathRespawn);
        assertEquals(1, s.mathQuestions);
        assertEquals(1, s.mathDifficulty);
        assertEquals(true, s.shareInventory);
        assertEquals(true, s.shareEnderChest);
        assertEquals(true, s.shareHunger);
        assertEquals(true, s.teamSelfSelect);
        assertEquals(true, s.clearInventoryOnStart);
        assertEquals(true, s.giveCompass);
    }

    @Test
    void poolHealthIsTwoPerHeart() {
        Settings s = new Settings();
        assertEquals(40f, s.poolHealth());
        s.poolHearts = 1;
        assertEquals(2f, s.poolHealth());
    }

    @Test
    void clampLowerBounds() {
        Settings s = new Settings();
        s.poolHearts = 0;
        s.hungerMultiplier = 0.5;
        s.headStartSeconds = -1;
        s.staggerSeconds = 0;
        s.mathQuestions = 0;
        s.mathDifficulty = 0;
        s.clamp();
        assertEquals(1, s.poolHearts);
        assertEquals(1.0, s.hungerMultiplier);
        assertEquals(0, s.headStartSeconds);
        assertEquals(1, s.staggerSeconds);
        assertEquals(1, s.mathQuestions);
        assertEquals(1, s.mathDifficulty);
    }

    @Test
    void clampUpperBounds() {
        Settings s = new Settings();
        s.poolHearts = 999;
        s.hungerMultiplier = 50.0;
        s.headStartSeconds = 100000;
        s.staggerSeconds = 9999;
        s.mathQuestions = 11;
        s.mathDifficulty = 4;
        s.clamp();
        assertEquals(200, s.poolHearts);
        assertEquals(8.0, s.hungerMultiplier);
        assertEquals(3600, s.headStartSeconds);
        assertEquals(600, s.staggerSeconds);
        assertEquals(10, s.mathQuestions);
        assertEquals(3, s.mathDifficulty);
    }

    @Test
    void clampKeepsValuesInRange() {
        Settings s = new Settings();
        s.poolHearts = 200;
        s.hungerMultiplier = 8.0;
        s.headStartSeconds = 3600;
        s.staggerSeconds = 600;
        s.mathQuestions = 10;
        s.mathDifficulty = 3;
        s.clamp();
        assertEquals(200, s.poolHearts);
        assertEquals(8.0, s.hungerMultiplier);
        assertEquals(3600, s.headStartSeconds);
        assertEquals(600, s.staggerSeconds);
        assertEquals(10, s.mathQuestions);
        assertEquals(3, s.mathDifficulty);
    }

    @Test
    void nullEnumBecomesDefault() {
        Settings s = new Settings();
        s.releaseMode = null;
        s.clamp();
        assertEquals(ReleaseMode.ALL_AT_ONCE, s.releaseMode);
    }

    @Test
    void nanMultiplierBecomesDefault() {
        Settings s = new Settings();
        s.hungerMultiplier = Double.NaN;
        s.clamp();
        assertEquals(2.0, s.hungerMultiplier);
    }

    @Test
    void staggeredModeIsKept() {
        Settings s = new Settings();
        s.releaseMode = ReleaseMode.STAGGERED;
        s.clamp();
        assertEquals(ReleaseMode.STAGGERED, s.releaseMode);
    }
}
