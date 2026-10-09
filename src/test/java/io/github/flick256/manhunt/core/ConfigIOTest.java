package io.github.flick256.manhunt.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigIOTest {

    @TempDir
    Path dir;

    private void assertDefaults(ManhuntConfig cfg) {
        Settings d = new Settings();
        assertEquals(d.poolHearts, cfg.settings.poolHearts);
        assertEquals(d.hungerMultiplier, cfg.settings.hungerMultiplier);
        assertEquals(d.headStartSeconds, cfg.settings.headStartSeconds);
        assertEquals(d.releaseMode, cfg.settings.releaseMode);
        assertEquals(d.staggerSeconds, cfg.settings.staggerSeconds);
        assertEquals(d.mathRespawn, cfg.settings.mathRespawn);
        assertEquals(d.mathQuestions, cfg.settings.mathQuestions);
        assertEquals(d.mathDifficulty, cfg.settings.mathDifficulty);
        assertEquals(d.shareInventory, cfg.settings.shareInventory);
        assertEquals(d.shareEnderChest, cfg.settings.shareEnderChest);
        assertEquals(d.shareHunger, cfg.settings.shareHunger);
        assertEquals(d.teamSelfSelect, cfg.settings.teamSelfSelect);
        assertEquals(d.clearInventoryOnStart, cfg.settings.clearInventoryOnStart);
        assertEquals(d.giveCompass, cfg.settings.giveCompass);
        assertTrue(cfg.owners.isEmpty());
        assertEquals(GameKind.CLASSIC, cfg.kind);
    }

    private Path write(String name, String content) throws Exception {
        Path p = dir.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p;
    }

    @Test
    void missingFileGivesDefaults() {
        assertDefaults(ConfigIO.load(dir.resolve("nope.json")));
    }

    @Test
    void nullPathGivesDefaults() {
        assertDefaults(ConfigIO.load(null));
    }

    @Test
    void directoryPathGivesDefaults() throws Exception {
        Path sub = Files.createDirectories(dir.resolve("adir"));
        assertDefaults(ConfigIO.load(sub));
    }

    @Test
    void corruptFilesGiveDefaults() throws Exception {
        String[] corrupt = {
                "{ this is not json",
                "",
                "   ",
                "[1,2,3]",
                "\"just a string\"",
                "42",
                "null",
                "{\"settings\": [1,2]}",
                "{\"settings\": {\"poolHearts\": {\"x\": 1}}, \"owners\": 5, \"kind\": 12}",
        };
        int i = 0;
        for (String c : corrupt) {
            Path p = write("corrupt" + (i++) + ".json", c);
            ManhuntConfig cfg = ConfigIO.load(p);
            assertTrue(cfg.settings.poolHearts >= 1);
            if (c.startsWith("{\"settings\": {\"poolHearts\"")) {
                assertEquals(20, cfg.settings.poolHearts);
                assertEquals(GameKind.CLASSIC, cfg.kind);
                assertTrue(cfg.owners.isEmpty());
            } else {
                assertDefaults(cfg);
            }
        }
    }

    @Test
    void invalidUtf8GivesDefaults() throws Exception {
        Path p = dir.resolve("bin.json");
        Files.write(p, new byte[]{(byte) 0xff, (byte) 0xfe, (byte) 0x00, (byte) 0x81});
        assertDefaults(ConfigIO.load(p));
    }

    @Test
    void saveThenLoadRoundTrip() {
        ManhuntConfig cfg = new ManhuntConfig();
        cfg.kind = GameKind.TEAMS;
        cfg.owners = new java.util.ArrayList<>(List.of("Steve", "Alex"));
        cfg.settings.poolHearts = 33;
        cfg.settings.hungerMultiplier = 3.5;
        cfg.settings.headStartSeconds = 120;
        cfg.settings.releaseMode = ReleaseMode.STAGGERED;
        cfg.settings.staggerSeconds = 45;
        cfg.settings.mathRespawn = true;
        cfg.settings.mathQuestions = 4;
        cfg.settings.mathDifficulty = 2;
        cfg.settings.shareInventory = false;
        cfg.settings.shareEnderChest = false;
        cfg.settings.shareHunger = false;
        cfg.settings.teamSelfSelect = false;
        cfg.settings.clearInventoryOnStart = false;
        cfg.settings.giveCompass = false;

        Path file = dir.resolve("config").resolve("manhunt.json");
        ConfigIO.save(file, cfg);
        ManhuntConfig back = ConfigIO.load(file);

        assertEquals(GameKind.TEAMS, back.kind);
        assertEquals(List.of("Steve", "Alex"), back.owners);
        assertEquals(33, back.settings.poolHearts);
        assertEquals(3.5, back.settings.hungerMultiplier);
        assertEquals(120, back.settings.headStartSeconds);
        assertEquals(ReleaseMode.STAGGERED, back.settings.releaseMode);
        assertEquals(45, back.settings.staggerSeconds);
        assertTrue(back.settings.mathRespawn);
        assertEquals(4, back.settings.mathQuestions);
        assertEquals(2, back.settings.mathDifficulty);
        assertFalse(back.settings.shareInventory);
        assertFalse(back.settings.shareEnderChest);
        assertFalse(back.settings.shareHunger);
        assertFalse(back.settings.teamSelfSelect);
        assertFalse(back.settings.clearInventoryOnStart);
        assertFalse(back.settings.giveCompass);
    }

    @Test
    void savedFileIsPrettyWithEnumNames() throws Exception {
        ManhuntConfig cfg = new ManhuntConfig();
        cfg.settings.releaseMode = ReleaseMode.STAGGERED;
        cfg.kind = GameKind.TEAMS;
        Path file = dir.resolve("pretty.json");
        ConfigIO.save(file, cfg);
        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("\n  "), "expected pretty printing");
        assertTrue(text.contains("\"releaseMode\": \"STAGGERED\""), text);
        assertTrue(text.contains("\"kind\": \"TEAMS\""), text);
        assertFalse(Files.exists(dir.resolve("pretty.json.tmp")), "temp file must be moved away");
    }

    @Test
    void saveClampsOutOfRangeValues() {
        ManhuntConfig cfg = new ManhuntConfig();
        cfg.settings.poolHearts = 0;
        cfg.settings.mathQuestions = 99;
        Path file = dir.resolve("clamped.json");
        ConfigIO.save(file, cfg);
        ManhuntConfig back = ConfigIO.load(file);
        assertEquals(1, back.settings.poolHearts);
        assertEquals(10, back.settings.mathQuestions);
    }

    @Test
    void saveOverwritesExistingFile() throws Exception {
        Path file = write("over.json", "{\"kind\":\"TEAMS\"}");
        ManhuntConfig cfg = new ManhuntConfig();
        cfg.kind = GameKind.CLASSIC;
        ConfigIO.save(file, cfg);
        assertEquals(GameKind.CLASSIC, ConfigIO.load(file).kind);
    }

    @Test
    void partialFileFillsMissingFieldsWithDefaults() throws Exception {
        Path p = write("partial.json", "{\"settings\": {\"poolHearts\": 30}}");
        ManhuntConfig cfg = ConfigIO.load(p);
        assertEquals(30, cfg.settings.poolHearts);
        assertEquals(new Settings().headStartSeconds, cfg.settings.headStartSeconds);
        assertEquals(GameKind.CLASSIC, cfg.kind);
        assertTrue(cfg.owners.isEmpty());
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception {
        Path p = write("unknown.json", "{\"future\": true, \"settings\": {\"poolHearts\": 7, \"newThing\": [1]}}");
        ManhuntConfig cfg = ConfigIO.load(p);
        assertEquals(7, cfg.settings.poolHearts);
    }

    @Test
    void outOfRangeValuesAreClampedOnLoad() throws Exception {
        Path p = write("range.json", "{\"settings\": {\"poolHearts\": 9999, \"hungerMultiplier\": 0.1, "
                + "\"headStartSeconds\": -5, \"staggerSeconds\": 0, \"mathQuestions\": 50, \"mathDifficulty\": 7}}");
        ManhuntConfig cfg = ConfigIO.load(p);
        assertEquals(200, cfg.settings.poolHearts);
        assertEquals(1.0, cfg.settings.hungerMultiplier);
        assertEquals(0, cfg.settings.headStartSeconds);
        assertEquals(1, cfg.settings.staggerSeconds);
        assertEquals(10, cfg.settings.mathQuestions);
        assertEquals(3, cfg.settings.mathDifficulty);
    }

    @Test
    void wrongTypesFallBackToDefaultsPerField() throws Exception {
        Path p = write("types.json", "{\"settings\": {\"poolHearts\": \"abc\", \"giveCompass\": \"maybe\", "
                + "\"headStartSeconds\": {\"a\":1}, \"shareHunger\": false, \"releaseMode\": 3}}");
        ManhuntConfig cfg = ConfigIO.load(p);
        Settings d = new Settings();
        assertEquals(d.poolHearts, cfg.settings.poolHearts);
        assertEquals(d.giveCompass, cfg.settings.giveCompass);
        assertEquals(d.headStartSeconds, cfg.settings.headStartSeconds);
        assertEquals(d.releaseMode, cfg.settings.releaseMode);
        assertFalse(cfg.settings.shareHunger, "valid field still read");
    }

    @Test
    void numericStringsAndBooleanStringsAreAccepted() throws Exception {
        Path p = write("strings.json", "{\"settings\": {\"poolHearts\": \"30\", \"mathRespawn\": \"true\", "
                + "\"hungerMultiplier\": 4.5}}");
        ManhuntConfig cfg = ConfigIO.load(p);
        assertEquals(30, cfg.settings.poolHearts);
        assertTrue(cfg.settings.mathRespawn);
        assertEquals(4.5, cfg.settings.hungerMultiplier);
    }

    @Test
    void enumNamesAreCaseInsensitiveAndUnknownFallsBack() throws Exception {
        Path p = write("enums.json", "{\"kind\": \"teams\", \"settings\": {\"releaseMode\": \"staggered\"}}");
        ManhuntConfig cfg = ConfigIO.load(p);
        assertEquals(GameKind.TEAMS, cfg.kind);
        assertEquals(ReleaseMode.STAGGERED, cfg.settings.releaseMode);

        Path q = write("enums2.json", "{\"kind\": \"NOPE\", \"settings\": {\"releaseMode\": \"NOPE\"}}");
        ManhuntConfig bad = ConfigIO.load(q);
        assertEquals(GameKind.CLASSIC, bad.kind);
        assertEquals(ReleaseMode.ALL_AT_ONCE, bad.settings.releaseMode);
    }

    @Test
    void ownersKeepStringEntriesOnly() throws Exception {
        Path p = write("owners.json", "{\"owners\": [\"Steve\", 42, null, \"  \", \"Alex\", \"Steve\", \" Bob \"]}");
        ManhuntConfig cfg = ConfigIO.load(p);
        assertEquals(List.of("Steve", "Alex", "Bob"), cfg.owners);
    }

    @Test
    void saveCreatesMissingParentDirectories() {
        Path file = dir.resolve("a").resolve("b").resolve("c").resolve("manhunt.json");
        ConfigIO.save(file, new ManhuntConfig());
        assertTrue(Files.exists(file));
    }

    @Test
    void saveToUnwritableLocationThrowsUnchecked() throws Exception {
        Path blocker = write("blocker", "file, not a directory");
        Path file = blocker.resolve("manhunt.json");
        assertThrows(UncheckedIOException.class, () -> ConfigIO.save(file, new ManhuntConfig()));
    }
}
