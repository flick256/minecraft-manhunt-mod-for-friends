package io.github.flick256.manhunt.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RosterTest {

    private static UUID u() {
        return UUID.randomUUID();
    }

    @Test
    void createTeamValidation() {
        Roster r = new Roster();
        assertNull(r.createTeam("red", "Red", "red"));
        assertNotNull(r.createTeam("red", "Again", "red"), "duplicate");
        assertNotNull(r.createTeam(null, "X", "red"), "null id");
        assertNotNull(r.createTeam("", "X", "red"), "empty id");
        assertNotNull(r.createTeam("   ", "X", "red"), "blank id");
        assertNotNull(r.createTeam("abcdefghijklmnopq", "X", "red"), "17 chars");
        assertNull(r.createTeam("abcdefghijklmnop", "X", "red"), "16 chars ok");
        assertNotNull(r.createTeam("Red", "X", "red"), "uppercase");
        assertNotNull(r.createTeam("a b", "X", "red"), "space");
        assertNotNull(r.createTeam("a.b", "X", "red"), "dot");
        assertNull(r.createTeam("a_b-9", "X", "red"));
    }

    @Test
    void createTeamFallbacks() {
        Roster r = new Roster();
        r.createTeam("alpha", null, null);
        TeamInfo t = r.team("alpha");
        assertEquals("alpha", t.displayName);
        assertEquals("white", t.color);
        assertTrue(t.members.isEmpty());
    }

    @Test
    void teamsKeepInsertionOrder() {
        Roster r = new Roster();
        r.createTeam("zeta", "Z", "red");
        r.createTeam("alpha", "A", "blue");
        r.createTeam("mid", "M", "gold");
        List<String> ids = r.teams().stream().map(t -> t.id).collect(Collectors.toList());
        assertEquals(List.of("zeta", "alpha", "mid"), ids);
    }

    @Test
    void assignRunnerRemovesFromHunterAndOtherTeam() {
        Roster r = new Roster();
        r.createTeam("a", "A", "red");
        r.createTeam("b", "B", "blue");
        UUID p = u();
        r.assignHunter(p);
        assertEquals(Role.HUNTER, r.roleOf(p));
        assertNull(r.assignRunner(p, "a"));
        assertFalse(r.isHunter(p));
        assertEquals("a", r.teamOf(p));
        assertEquals(Role.RUNNER, r.roleOf(p));
        assertNull(r.assignRunner(p, "b"));
        assertEquals("b", r.teamOf(p));
        assertFalse(r.team("a").members.contains(p));
        assertTrue(r.team("b").members.contains(p));
    }

    @Test
    void assignRunnerToUnknownTeamFails() {
        Roster r = new Roster();
        UUID p = u();
        r.assignHunter(p);
        assertNotNull(r.assignRunner(p, "nope"));
        assertTrue(r.isHunter(p), "failed assign must not change state");
    }

    @Test
    void assignHunterRemovesFromTeam() {
        Roster r = new Roster();
        r.createTeam("a", "A", "red");
        UUID p = u();
        r.assignRunner(p, "a");
        assertNull(r.assignHunter(p));
        assertNull(r.teamOf(p));
        assertTrue(r.isHunter(p));
        assertEquals(Role.HUNTER, r.roleOf(p));
    }

    @Test
    void unassignClearsEverything() {
        Roster r = new Roster();
        r.createTeam("a", "A", "red");
        UUID h = u();
        UUID p = u();
        r.assignHunter(h);
        r.assignRunner(p, "a");
        r.unassign(h);
        r.unassign(p);
        assertEquals(Role.NONE, r.roleOf(h));
        assertEquals(Role.NONE, r.roleOf(p));
        assertFalse(r.isRunner(p));
    }

    @Test
    void removeTeamMakesMembersNone() {
        Roster r = new Roster();
        r.createTeam("a", "A", "red");
        UUID p = u();
        r.assignRunner(p, "a");
        assertNull(r.removeTeam("a"));
        assertNull(r.team("a"));
        assertEquals(Role.NONE, r.roleOf(p));
        assertNotNull(r.removeTeam("a"));
    }

    @Test
    void queriesAndUnmodifiableViews() {
        Roster r = new Roster();
        r.createTeam("a", "A", "red");
        UUID h = u();
        UUID p = u();
        r.assignHunter(h);
        r.assignRunner(p, "a");
        assertEquals(Set.of(h), r.hunters());
        assertEquals(Set.of(p), r.allRunners());
        assertTrue(r.isRunner(p));
        assertFalse(r.isRunner(h));
        assertEquals(Role.NONE, r.roleOf(u()));
        assertNull(r.teamOf(null));
        assertThrows(UnsupportedOperationException.class, () -> r.hunters().add(u()));
        assertThrows(UnsupportedOperationException.class, () -> r.allRunners().add(u()));
        assertThrows(UnsupportedOperationException.class, () -> r.teams().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.names().put(u(), "x"));
    }

    @Test
    void namesRememberedAndFallback() {
        Roster r = new Roster();
        UUID p = u();
        assertEquals(p.toString(), r.nameOf(p));
        r.rememberName(p, "Steve");
        assertEquals("Steve", r.nameOf(p));
        r.rememberName(p, "Alex");
        assertEquals("Alex", r.nameOf(p));
        r.rememberName(null, "x");
        r.rememberName(u(), null);
        assertEquals(1, r.names().size());
    }

    @Test
    void ensureClassicTeamIsIdempotentAndKeepsMembers() {
        Roster r = new Roster();
        r.ensureClassicTeam();
        assertNotNull(r.team(Roster.CLASSIC_TEAM));
        UUID p = u();
        r.assignRunner(p, Roster.CLASSIC_TEAM);
        r.ensureClassicTeam();
        assertEquals(1, r.teams().size());
        assertEquals(Roster.CLASSIC_TEAM, r.teamOf(p));
    }

    @Test
    void clearAllRemovesHuntersAndTeamsButKeepsNames() {
        Roster r = new Roster();
        UUID p = u();
        r.rememberName(p, "Steve");
        r.ensureClassicTeam();
        r.assignRunner(p, Roster.CLASSIC_TEAM);
        r.assignHunter(u());
        r.clearAll();
        assertTrue(r.teams().isEmpty());
        assertTrue(r.hunters().isEmpty());
        assertEquals(Role.NONE, r.roleOf(p));
        assertEquals("Steve", r.nameOf(p));
    }

    @Test
    void autoBalanceErrors() {
        Roster r = new Roster();
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            players.add(u());
        }
        assertNotNull(r.autoBalance(players, 1, new Random(1)));
        assertNotNull(r.autoBalance(players, 9, new Random(1)));
        assertNotNull(r.autoBalance(players.subList(0, 2), 3, new Random(1)));
        assertNotNull(r.autoBalance(null, 2, new Random(1)));
        assertTrue(r.teams().isEmpty(), "errors must not change teams");
    }

    @Test
    void autoBalanceDistributesEvenlyWithPaletteNames() {
        for (int teamCount = 2; teamCount <= 8; teamCount++) {
            Roster r = new Roster();
            r.assignHunter(u());
            List<UUID> players = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                players.add(u());
            }
            assertNull(r.autoBalance(players, teamCount, new Random(42 + teamCount)));
            assertEquals(teamCount, r.teams().size());
            int min = Integer.MAX_VALUE;
            int max = 0;
            int total = 0;
            for (TeamInfo t : r.teams()) {
                min = Math.min(min, t.members.size());
                max = Math.max(max, t.members.size());
                total += t.members.size();
            }
            assertTrue(max - min <= 1, "uneven: " + min + ".." + max);
            assertEquals(13, total);
            assertEquals(Set.copyOf(players), r.allRunners());
        }
    }

    @Test
    void autoBalancePaletteOrderAndColors() {
        Roster r = new Roster();
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            players.add(u());
        }
        assertNull(r.autoBalance(players, 8, new Random(7)));
        String[][] expected = {
                {"red", "Red", "red"}, {"blue", "Blue", "blue"}, {"green", "Green", "green"},
                {"yellow", "Yellow", "yellow"}, {"aqua", "Aqua", "aqua"}, {"pink", "Pink", "light_purple"},
                {"gold", "Gold", "gold"}, {"white", "White", "white"}};
        List<TeamInfo> teams = new ArrayList<>(r.teams());
        for (int i = 0; i < 8; i++) {
            assertEquals(expected[i][0], teams.get(i).id);
            assertEquals(expected[i][1], teams.get(i).displayName);
            assertEquals(expected[i][2], teams.get(i).color);
        }
    }

    @Test
    void autoBalanceReplacesTeamsAndMakesHuntersRunners() {
        Roster r = new Roster();
        r.createTeam("old", "Old", "red");
        UUID outsider = u();
        UUID hunterInList = u();
        UUID oldMember = u();
        r.assignRunner(oldMember, "old");
        r.assignHunter(hunterInList);
        r.assignHunter(outsider);
        List<UUID> players = new ArrayList<>(List.of(hunterInList, u(), u(), u()));
        assertNull(r.autoBalance(players, 2, new Random(3)));
        assertNull(r.team("old"));
        assertEquals(Role.NONE, r.roleOf(oldMember));
        assertFalse(r.isHunter(hunterInList));
        assertTrue(r.isRunner(hunterInList));
        assertTrue(r.isHunter(outsider), "players not in the list keep their hunter role");
    }

    @Test
    void autoBalanceIsDeterministicWithSeed() {
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            players.add(u());
        }
        Roster a = new Roster();
        Roster b = new Roster();
        a.autoBalance(players, 3, new Random(99));
        b.autoBalance(players, 3, new Random(99));
        for (UUID p : players) {
            assertEquals(a.teamOf(p), b.teamOf(p));
        }
    }

    @Test
    void autoBalanceDedupesPlayers() {
        Roster r = new Roster();
        UUID p = u();
        UUID q = u();
        assertNotNull(r.autoBalance(List.of(p, p, p), 2, new Random(1)), "only one unique player");
        assertNull(r.autoBalance(List.of(p, p, q, q), 2, new Random(1)));
        assertEquals(2, r.allRunners().size());
    }

    @Test
    void describeReadable() {
        Roster r = new Roster();
        UUID h = u();
        UUID p = u();
        r.rememberName(h, "Hunter1");
        r.rememberName(p, "Runner1");
        r.createTeam("red", "Red", "red");
        r.createTeam("blue", "Blue", "blue");
        r.assignHunter(h);
        r.assignRunner(p, "red");
        List<String> lines = r.describe();
        assertEquals("Hunters (1): Hunter1", lines.get(0));
        assertEquals("Team Red [red, red] (1): Runner1", lines.get(1));
        assertEquals("Team Blue [blue, blue] (0): no members", lines.get(2));
    }

    @Test
    void describeEmpty() {
        List<String> lines = new Roster().describe();
        assertEquals(List.of("Hunters: none", "Teams: none"), lines);
    }
}
