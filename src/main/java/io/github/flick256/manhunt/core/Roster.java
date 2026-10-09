package io.github.flick256.manhunt.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Who is a hunter and which runner team everybody belongs to. UUID based, no Minecraft types. */
public final class Roster {
    public static final String CLASSIC_TEAM = "runners";
    public static final int MAX_ID_LENGTH = 16;

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]+");
    /** Palette for autoBalance: {id, displayName, color}. */
    private static final String[][] PALETTE = {
            {"red", "Red", "red"},
            {"blue", "Blue", "blue"},
            {"green", "Green", "green"},
            {"yellow", "Yellow", "yellow"},
            {"aqua", "Aqua", "aqua"},
            {"pink", "Pink", "light_purple"},
            {"gold", "Gold", "gold"},
            {"white", "White", "white"},
    };

    private final Map<UUID, String> names = new HashMap<>();
    private final Set<UUID> hunters = new LinkedHashSet<>();
    private final Map<String, TeamInfo> teams = new LinkedHashMap<>();

    public Roster() {}

    public Map<UUID, String> names() {
        return Collections.unmodifiableMap(names);
    }

    /** Remembers a player's name for display. Ignores null or blank names. */
    public void rememberName(UUID id, String name) {
        if (id == null || name == null || name.isBlank()) {
            return;
        }
        names.put(id, name);
    }

    /** Remembered name, or the UUID string when the name is unknown. */
    public String nameOf(UUID id) {
        if (id == null) {
            return "?";
        }
        String name = names.get(id);
        return name != null ? name : id.toString();
    }

    public Role roleOf(UUID id) {
        if (isHunter(id)) {
            return Role.HUNTER;
        }
        if (teamOf(id) != null) {
            return Role.RUNNER;
        }
        return Role.NONE;
    }

    /** Id of the team the player is on, or null. */
    public String teamOf(UUID id) {
        if (id == null) {
            return null;
        }
        for (TeamInfo team : teams.values()) {
            if (team.members.contains(id)) {
                return team.id;
            }
        }
        return null;
    }

    public boolean isHunter(UUID id) {
        return id != null && hunters.contains(id);
    }

    public boolean isRunner(UUID id) {
        return teamOf(id) != null;
    }

    public Set<UUID> hunters() {
        return Collections.unmodifiableSet(hunters);
    }

    /** Teams in insertion order. */
    public Collection<TeamInfo> teams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    /** Team with the given id, or null. */
    public TeamInfo team(String id) {
        return id == null ? null : teams.get(id);
    }

    public Set<UUID> allRunners() {
        Set<UUID> all = new LinkedHashSet<>();
        for (TeamInfo team : teams.values()) {
            all.addAll(team.members);
        }
        return Collections.unmodifiableSet(all);
    }

    /**
     * Creates a team. Id must be 1..16 characters of [a-z0-9_-] and unique. Blank display name falls back to the id,
     * null or blank colour falls back to "white".
     *
     * @return null on success, otherwise an error message
     */
    public String createTeam(String id, String displayName, String color) {
        if (id == null || id.isBlank()) {
            return "Team id must not be blank.";
        }
        if (id.length() > MAX_ID_LENGTH) {
            return "Team id must be at most " + MAX_ID_LENGTH + " characters.";
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            return "Team id may only contain a-z, 0-9, '_' and '-'.";
        }
        if (teams.containsKey(id)) {
            return "A team with id '" + id + "' already exists.";
        }
        String name = displayName == null || displayName.isBlank() ? id : displayName;
        String col = color == null || color.isBlank() ? "white" : color;
        teams.put(id, new TeamInfo(id, name, col));
        return null;
    }

    /** Removes a team; its members become NONE. Returns null or an error. */
    public String removeTeam(String id) {
        if (teams.remove(id) == null) {
            return "No such team: " + id;
        }
        return null;
    }

    /** Makes the player a hunter, removing them from any team. Returns null or an error. */
    public String assignHunter(UUID id) {
        if (id == null) {
            return "No player given.";
        }
        for (TeamInfo team : teams.values()) {
            team.members.remove(id);
        }
        hunters.add(id);
        return null;
    }

    /** Puts the player on the team, removing them from hunters and any other team. Returns null or an error. */
    public String assignRunner(UUID id, String teamId) {
        if (id == null) {
            return "No player given.";
        }
        TeamInfo target = team(teamId);
        if (target == null) {
            return "No such team: " + teamId;
        }
        hunters.remove(id);
        for (TeamInfo team : teams.values()) {
            if (team != target) {
                team.members.remove(id);
            }
        }
        target.members.add(id);
        return null;
    }

    /** Removes the player from hunters and from any team. */
    public void unassign(UUID id) {
        if (id == null) {
            return;
        }
        hunters.remove(id);
        for (TeamInfo team : teams.values()) {
            team.members.remove(id);
        }
    }

    /** Removes all hunters and all teams. Remembered names are kept. */
    public void clearAll() {
        hunters.clear();
        teams.clear();
    }

    /** Creates the classic runner team ("runners") if it does not exist. */
    public void ensureClassicTeam() {
        if (!teams.containsKey(CLASSIC_TEAM)) {
            createTeam(CLASSIC_TEAM, "Runners", "green");
        }
    }

    /**
     * TEAMS helper: shuffles the players and deals them round-robin into {@code teamCount} palette teams.
     * Replaces all existing teams. Players in the list stop being hunters.
     *
     * @return null on success, otherwise an error message
     */
    public String autoBalance(List<UUID> players, int teamCount, Random rnd) {
        if (teamCount < 2 || teamCount > PALETTE.length) {
            return "Team count must be between 2 and " + PALETTE.length + ".";
        }
        if (players == null) {
            return "No players given.";
        }
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID id : players) {
            if (id != null) {
                unique.add(id);
            }
        }
        if (unique.size() < teamCount) {
            return "Need at least " + teamCount + " players, have " + unique.size() + ".";
        }
        List<UUID> shuffled = new ArrayList<>(unique);
        Collections.shuffle(shuffled, rnd == null ? new Random() : rnd);

        teams.clear();
        for (int i = 0; i < teamCount; i++) {
            String[] p = PALETTE[i];
            teams.put(p[0], new TeamInfo(p[0], p[1], p[2]));
        }
        for (int i = 0; i < shuffled.size(); i++) {
            String[] p = PALETTE[i % teamCount];
            teams.get(p[0]).members.add(shuffled.get(i));
        }
        hunters.removeAll(unique);
        return null;
    }

    /** Human readable lines for status output. */
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        if (hunters.isEmpty()) {
            lines.add("Hunters: none");
        } else {
            lines.add("Hunters (" + hunters.size() + "): " + joinNames(hunters));
        }
        if (teams.isEmpty()) {
            lines.add("Teams: none");
        }
        for (TeamInfo team : teams.values()) {
            String members = team.members.isEmpty() ? "no members" : joinNames(team.members);
            lines.add("Team " + team.displayName + " [" + team.id + ", " + team.color + "] ("
                    + team.members.size() + "): " + members);
        }
        return lines;
    }

    private String joinNames(Collection<UUID> ids) {
        List<String> out = new ArrayList<>();
        for (UUID id : ids) {
            out.add(nameOf(id));
        }
        return String.join(", ", out);
    }
}
