package io.github.flick256.manhunt.game;

import io.github.flick256.manhunt.core.ConfigIO;
import io.github.flick256.manhunt.core.GameKind;
import io.github.flick256.manhunt.core.ManhuntConfig;
import io.github.flick256.manhunt.core.Phase;
import io.github.flick256.manhunt.core.Role;
import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.core.Schedule;
import io.github.flick256.manhunt.core.Settings;
import io.github.flick256.manhunt.core.Side;
import io.github.flick256.manhunt.core.TeamInfo;
import io.github.flick256.manhunt.core.Winner;
import io.github.flick256.manhunt.util.Msg;
import io.github.flick256.manhunt.util.Owner;
import io.github.flick256.manhunt.util.TextUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Game state machine: IDLE -> HEAD_START -> RUNNING -> CELEBRATION -> IDLE.
 * One instance per server, created at SERVER_STARTED.
 */
public final class ManhuntGame {

    private static final int TICKS_PER_SECOND = 20;
    /** Teams mode: everybody is frozen for this many (scaled) seconds before the GO! */
    private static final int TEAMS_COUNTDOWN_SECONDS = 10;
    private static final float FINAL_DAMAGE = 1000f;

    private final MinecraftServer server;
    private final ManhuntConfig config;
    private final Path configFile;
    private final Roster roster;
    private final TeamSync sync;
    private final FreezeManager freeze;
    private final CompassTracker compass;
    private final RespawnQuiz quiz;
    private final Ceremony ceremony;
    private final TestLab test;

    private Phase phase = Phase.IDLE;
    private GameKind kind;
    private long tickCounter;
    private long startTick;
    /** Hunters (Classic) or every participant (Teams) that are not released yet -> tick at which they are released. */
    private final Map<UUID, Integer> releaseAt = new HashMap<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private final Set<String> downTeams = new HashSet<>();
    /** Teams that take part in the running game (had an online member at start). */
    private final Set<String> startedTeams = new LinkedHashSet<>();
    private boolean firstReleaseAnnounced;
    private String lastWinnerLabel = "";

    public ManhuntGame(MinecraftServer server, ManhuntConfig config, Path configFile) {
        this.server = server;
        this.config = config;
        this.configFile = configFile;
        // VERIFY: ManhuntConfig.kind / settings / owners are public fields (contract shows no modifiers).
        this.kind = config.kind == null ? GameKind.CLASSIC : config.kind;
        this.roster = new Roster();
        this.sync = new TeamSync(this);
        this.freeze = new FreezeManager();
        this.compass = new CompassTracker(this);
        this.quiz = new RespawnQuiz(this);
        this.ceremony = new Ceremony(this);
        this.test = new TestLab(this);
        if (kind == GameKind.CLASSIC) roster.ensureClassicTeam();
    }

    // ------------------------------------------------------------------ accessors

    public MinecraftServer server() { return server; }
    public ManhuntConfig config() { return config; }
    public Settings settings() { return config.settings; }
    public Roster roster() { return roster; }
    public Phase phase() { return phase; }
    public GameKind kind() { return kind; }
    public TeamSync sync() { return sync; }
    public FreezeManager freeze() { return freeze; }
    public CompassTracker compass() { return compass; }
    public RespawnQuiz quiz() { return quiz; }
    public Ceremony ceremony() { return ceremony; }
    public TestLab test() { return test; }

    public void save() {
        config.settings.clamp();
        ConfigIO.save(configFile, config);
    }

    public double timeScale() {
        return test.timeScale();
    }

    public String setKind(GameKind k) {
        if (k == null) return "Unknown game mode.";
        if (phase != Phase.IDLE) return "Stop the running game before changing the mode.";
        if (k == GameKind.CLASSIC) {
            // Classic has exactly one runner team; other teams are dropped.
            for (TeamInfo t : new ArrayList<>(roster.teams())) {
                if (!Roster.CLASSIC_TEAM.equals(t.id)) roster.removeTeam(t.id); // VERIFY: TeamInfo.id public
            }
            roster.ensureClassicTeam();
        } else {
            for (UUID h : new ArrayList<>(roster.hunters())) roster.unassign(h);
            roster.removeTeam(Roster.CLASSIC_TEAM);
        }
        kind = k;
        config.kind = k;
        save();
        return null;
    }

    // ------------------------------------------------------------------ lifecycle

    public String start(ServerPlayer starter) {
        if (phase != Phase.IDLE) return "A game is already running.";
        boolean relaxed = test.isActive();
        if (kind == GameKind.CLASSIC) {
            if (onlineMembers(Roster.CLASSIC_TEAM).isEmpty()) return "Need at least one online runner.";
            if (!relaxed && onlineHunters().isEmpty()) return "Need at least one online hunter.";
        } else {
            int ready = 0;
            for (TeamInfo t : roster.teams()) {
                if (!onlineMembers(t.id).isEmpty()) ready++; // VERIFY: TeamInfo.id public
            }
            if (relaxed && ready < 1) return "Need a team with an online member.";
            if (!relaxed && ready < 2) return "Need at least 2 teams with an online member each.";
        }

        startedTeams.clear();
        downTeams.clear();
        eliminated.clear();
        releaseAt.clear();
        firstReleaseAnnounced = false;
        lastWinnerLabel = "";
        for (TeamInfo t : roster.teams()) {
            if (kind == GameKind.CLASSIC && !Roster.CLASSIC_TEAM.equals(t.id)) continue;
            if (!onlineMembers(t.id).isEmpty()) startedTeams.add(t.id);
        }

        phase = Phase.HEAD_START;
        startTick = tickCounter;
        double scale = timeScale();
        if (kind == GameKind.CLASSIC) {
            List<UUID> hunters = new ArrayList<>(roster.hunters());
            int[] secs = Schedule.releaseSeconds(hunters.size(), settings().headStartSeconds,
                    settings().releaseMode, settings().staggerSeconds);
            for (int i = 0; i < hunters.size(); i++) {
                int sec = i < secs.length ? secs[i] : settings().headStartSeconds;
                releaseAt.put(hunters.get(i), toTicks(sec, scale));
            }
        } else {
            int countdown = toTicks(TEAMS_COUNTDOWN_SECONDS, scale);
            for (UUID id : startedMemberIds()) releaseAt.put(id, countdown);
        }

        for (String teamId : startedTeams) sync.beginTeam(teamId, onlineMembers(teamId));

        for (ServerPlayer p : onlinePlayers(participantIds())) {
            p.setGameMode(GameType.SURVIVAL); // VERIFY: ServerPlayer#setGameMode(GameType)
        }
        for (ServerPlayer h : onlineHunters()) {
            if (settings().clearInventoryOnStart) h.getInventory().clearContent(); // VERIFY: Inventory#clearContent()
            h.setHealth((float) h.getAttributeValue(Attributes.MAX_HEALTH));
            h.getFoodData().setFoodLevel(20); // VERIFY: FoodData#setFoodLevel(int)
            h.getFoodData().setSaturation(20f); // VERIFY: FoodData#setSaturation(float)
            if (settings().giveCompass) compass.giveTo(h);
        }
        if (kind == GameKind.TEAMS && settings().giveCompass) {
            for (ServerPlayer p : onlinePlayers(startedMemberIds())) compass.giveTo(p);
        }
        for (Map.Entry<UUID, Integer> e : releaseAt.entrySet()) {
            ServerPlayer p = player(e.getKey());
            if (p != null && e.getValue() > 0) freeze.freeze(p);
        }

        if (releaseAt.isEmpty()) phase = Phase.RUNNING; // test lab / no hunters: go straight away
        announceStart();
        return null;
    }

    public void stop(boolean announce) {
        if (phase == Phase.IDLE) return;
        phase = Phase.IDLE; // first, so that anything fired by the cleanup below sees an idle game
        freeze.clear(server);
        sync.endAll();
        quiz.clear();
        if (ceremony.isPlaying()) ceremony.cancel();
        compass.clearLast();
        for (ServerPlayer p : onlinePlayers(participantIds())) {
            p.setGameMode(GameType.SURVIVAL);
        }
        releaseAt.clear();
        eliminated.clear();
        downTeams.clear();
        startedTeams.clear();
        firstReleaseAnnounced = false;
        if (announce) Msg.broadcast(server, Msg.bad("The manhunt was stopped."));
    }

    public void endGame(Winner w) {
        if (w == null || !isGameActive()) return;
        phase = Phase.CELEBRATION;
        releaseAt.clear();
        freeze.clear(server);
        quiz.clear();

        List<ServerPlayer> winners;
        List<ServerPlayer> losers = new ArrayList<>();
        String label;
        if (w.side() == Side.HUNTERS) {
            winners = onlineHunters();
            losers.addAll(onlinePlayers(startedMemberIds()));
            label = "Hunters";
        } else if (w.side() == Side.RUNNERS) {
            String team = w.teamId() != null ? w.teamId() : Roster.CLASSIC_TEAM;
            winners = onlineMembers(team);
            losers.addAll(onlineHunters());
            label = "Runners";
        } else {
            String team = w.teamId() == null ? "" : w.teamId();
            winners = onlineMembers(team);
            for (String other : startedTeams) {
                if (!other.equals(team)) losers.addAll(onlineMembers(other));
            }
            label = teamDisplay(team);
        }
        lastWinnerLabel = label;
        ceremony.play(w, winners, losers, label);
    }

    public void finishCeremony() {
        // Ceremony may also be played from the test lab while a game runs: only reset after a real game ends.
        if (phase != Phase.CELEBRATION) return;
        String label = lastWinnerLabel;
        stop(false);
        Msg.broadcast(server, Msg.good(label.isEmpty() ? "Game over!" : "Game over - " + label + " win!"));
    }

    public void tick() {
        if (phase == Phase.IDLE) return;
        tickCounter++;
        if (phase == Phase.CELEBRATION) {
            ceremony.tick();
            return;
        }
        processReleases();
        freeze.tick(server);
        sync.tick();
        if (!isGameActive()) return; // sync may have ended the game (team down)
        compass.tick();
        quiz.tick();
        test.tick();
    }

    public int secondsUntilRelease(UUID hunter) {
        Integer at = releaseAt.get(hunter);
        if (at == null) return 0;
        long remaining = at - elapsedTicks();
        if (remaining <= 0) return 0;
        return (int) ((remaining + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
    }

    // ------------------------------------------------------------------ roster operations

    public String assignHunter(UUID id) {
        if (id == null) return "Unknown player.";
        String err = requireIdle();
        if (err != null) return err;
        if (kind != GameKind.CLASSIC) return "Hunters only exist in Classic mode.";
        rememberOnline(id);
        return roster.assignHunter(id);
    }

    public String assignRunner(UUID id, String teamId) {
        if (id == null) return "Unknown player.";
        String err = requireIdle();
        if (err != null) return err;
        if (kind == GameKind.CLASSIC && !Roster.CLASSIC_TEAM.equals(teamId)) {
            return "In Classic mode the only runner team is 'runners'.";
        }
        rememberOnline(id);
        return roster.assignRunner(id, teamId);
    }

    public String unassign(UUID id) {
        if (id == null) return "Unknown player.";
        // Unassign is allowed in any phase, except for players who are part of a running game:
        // leaving mid-game would dodge the hunt.
        if (phase != Phase.IDLE && roster.roleOf(id) != Role.NONE) {
            return "You can't leave the roster during a game.";
        }
        roster.unassign(id);
        return null;
    }

    public String createTeam(String id, String displayName, String color) {
        String err = requireIdle();
        if (err != null) return err;
        if (kind != GameKind.TEAMS) return "Teams are only used in Teams mode.";
        return roster.createTeam(id, displayName, color);
    }

    public String removeTeam(String id) {
        String err = requireIdle();
        if (err != null) return err;
        if (kind != GameKind.TEAMS) return "Teams are only used in Teams mode.";
        return roster.removeTeam(id);
    }

    public String autoAssign(int teamCount) {
        String err = requireIdle();
        if (err != null) return err;
        if (kind != GameKind.TEAMS) return "Auto-balance is only used in Teams mode.";
        List<UUID> ids = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ids.add(p.getUUID());
            roster.rememberName(p.getUUID(), p.getGameProfile().name());
        }
        if (ids.isEmpty()) return "No players are online.";
        return roster.autoBalance(ids, teamCount, new Random());
    }

    public String joinTeamSelf(ServerPlayer p, String teamId) {
        if (p == null) return "Unknown player.";
        if (!settings().teamSelfSelect) return "Only owners can pick teams.";
        String err = requireIdle();
        if (err != null) return err;
        if (kind == GameKind.CLASSIC && !Roster.CLASSIC_TEAM.equals(teamId)) {
            return "In Classic mode the only runner team is 'runners'.";
        }
        if (roster.isHunter(p.getUUID())) return "Only an owner can change a hunter's role.";
        roster.rememberName(p.getUUID(), p.getGameProfile().name());
        return roster.assignRunner(p.getUUID(), teamId);
    }

    // ------------------------------------------------------------------ queries

    public ServerPlayer player(UUID id) {
        return id == null ? null : server.getPlayerList().getPlayer(id);
    }

    public List<ServerPlayer> hunters() {
        return onlineHunters();
    }

    public List<ServerPlayer> teamMembers(String teamId) {
        return onlineMembers(teamId);
    }

    public List<ServerPlayer> allRunners() {
        return onlinePlayers(roster.allRunners());
    }

    public boolean isParticipant(ServerPlayer p) {
        return p != null && isGameActive() && roster.roleOf(p.getUUID()) != Role.NONE;
    }

    public boolean isGameActive() {
        return phase == Phase.HEAD_START || phase == Phase.RUNNING;
    }

    public String teamDisplay(String teamId) {
        if (teamId == null) return "";
        TeamInfo t = roster.team(teamId);
        return t != null && t.displayName != null ? t.displayName : teamId; // VERIFY: TeamInfo.displayName public
    }

    // ------------------------------------------------------------------ event hooks

    public void onPlayerJoin(ServerPlayer p) {
        UUID id = p.getUUID();
        Owner.noteJoin(p);
        roster.rememberName(id, p.getGameProfile().name());
        // Players who were modified (max health) and left before the game ended get restored here.
        sync.restoreIfTracked(p);
        if (phase == Phase.IDLE) return;

        Role role = roster.roleOf(id);
        if (role == Role.NONE) return;
        if (eliminated.contains(id)) {
            p.setGameMode(GameType.SPECTATOR);
            return;
        }
        if (!isGameActive()) return;

        boolean pending = pending(id);
        if (role == Role.HUNTER) {
            if (pending) freeze.freeze(p);
            else if (freeze.isFrozen(id)) freeze.unfreeze(p);
            if (settings().giveCompass) compass.giveTo(p);
            return;
        }
        String team = roster.teamOf(id);
        if (team == null || !startedTeams.contains(team) || downTeams.contains(team)) {
            p.setGameMode(GameType.SPECTATOR);
            Msg.send(p, Msg.info("Your team is not part of this game."));
            return;
        }
        sync.addMember(team, p);
        if (kind == GameKind.TEAMS && settings().giveCompass) compass.giveTo(p);
        if (pending) freeze.freeze(p);
        else if (freeze.isFrozen(id)) freeze.unfreeze(p);
    }

    public void onPlayerLeave(ServerPlayer p) {
        // Frozen state is kept on purpose: FreezeManager re-freezes the player when they come back.
        sync.removeMember(p);
    }

    public void onAfterDeath(LivingEntity e, DamageSource src) {
        if (e == null || !isGameActive()) return;
        if (e.getType() == EntityType.ENDER_DRAGON) { // VERIFY: EntityType.ENDER_DRAGON (EnderDragon class is not in the local reference)
            onDragonDeath(src);
            return;
        }
        if (e instanceof ServerPlayer p && roster.isRunner(p.getUUID())) {
            String team = roster.teamOf(p.getUUID());
            if (team != null) onRunnerTeamDown(team);
        }
        // Hunter deaths: nothing here; the quiz is started on respawn.
    }

    public void onAfterRespawn(ServerPlayer oldP, ServerPlayer newP) {
        if (newP == null || phase == Phase.IDLE) return;
        UUID id = newP.getUUID();
        Role role = roster.roleOf(id);
        if (role == Role.NONE) return;
        if (eliminated.contains(id)) {
            newP.setGameMode(GameType.SPECTATOR);
            return;
        }
        if (!isGameActive()) return;

        if (role == Role.HUNTER) {
            if (oldP != null) freeze.unfreeze(oldP);
            if (settings().giveCompass) compass.giveTo(newP);
            if (pending(id)) freeze.freeze(newP);
            if (settings().mathRespawn) quiz.onHunterRespawned(newP);
            return;
        }
        String team = roster.teamOf(id);
        if (team != null && startedTeams.contains(team) && !downTeams.contains(team)) {
            sync.addMember(team, newP);
            if (kind == GameKind.TEAMS && settings().giveCompass) compass.giveTo(newP);
        }
        if (pending(id)) freeze.freeze(newP);
    }

    public boolean allowChat(PlayerChatMessage m, ServerPlayer sender) {
        if (m == null || sender == null) return true;
        if (quiz.isInQuiz(sender.getUUID())) {
            return !quiz.handleAnswer(sender, m.signedContent()); // consumed answers are not broadcast
        }
        return true;
    }

    public void onRunnerTeamDown(String teamId) {
        if (teamId == null || !isGameActive()) return;
        if (!startedTeams.contains(teamId) || !downTeams.add(teamId)) return; // once per team

        List<UUID> members = teamMemberIds(teamId);
        if (kind == GameKind.CLASSIC) {
            for (ServerPlayer p : onlinePlayers(members)) killPlayer(p);
            endGame(new Winner(Side.HUNTERS, null));
            return;
        }

        eliminated.addAll(members);
        for (ServerPlayer p : onlinePlayers(members)) killPlayer(p);
        Msg.broadcast(server, Msg.bad(teamDisplay(teamId) + " has been eliminated!"));

        List<String> stillIn = teamsStillIn();
        if (stillIn.size() == 1) {
            endGame(new Winner(Side.TEAM, stillIn.get(0)));
        } else if (stillIn.isEmpty()) {
            stop(true);
        }
    }

    // ------------------------------------------------------------------ internals

    private void onDragonDeath(DamageSource src) {
        if (kind == GameKind.CLASSIC) {
            endGame(new Winner(Side.RUNNERS, Roster.CLASSIC_TEAM));
            return;
        }
        String team = null;
        if (src != null && src.getEntity() instanceof ServerPlayer killer) {
            team = roster.teamOf(killer.getUUID());
        }
        if (team == null || !startedTeams.contains(team) || downTeams.contains(team)) {
            team = teamWithMostAlive();
        }
        if (team == null) return;
        endGame(new Winner(Side.TEAM, team));
    }

    private void processReleases() {
        long elapsed = elapsedTicks();
        List<UUID> due = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : releaseAt.entrySet()) {
            if (elapsed >= e.getValue()) due.add(e.getKey());
        }
        if (!due.isEmpty()) {
            for (UUID id : due) releaseAt.remove(id);
            releaseDue(due);
        }
        if (phase == Phase.HEAD_START && releaseAt.isEmpty()) phase = Phase.RUNNING;
        if (phase == Phase.HEAD_START && elapsed % TICKS_PER_SECOND == 0) showCountdown();
    }

    private void releaseDue(List<UUID> due) {
        for (UUID id : due) {
            ServerPlayer p = player(id);
            if (kind == GameKind.TEAMS) {
                if (p != null) {
                    freeze.unfreeze(p);
                    Msg.title(p, Component.literal("GO!"), Component.literal("Good luck"), 5, 40, 10);
                    Msg.sound(p, SoundEvents.NOTE_BLOCK_PLING, 1f, 1f); // VERIFY: SoundEvents.NOTE_BLOCK_PLING
                }
                continue;
            }
            if (p != null) {
                freeze.unfreeze(p);
                Msg.title(p, Component.literal("GO! Hunt them down!"), Component.literal(""), 5, 40, 10);
                Msg.sound(p, SoundEvents.NOTE_BLOCK_PLING, 1f, 1f); // VERIFY: SoundEvents.NOTE_BLOCK_PLING
            }
            Msg.broadcast(server, Msg.info(nameOf(id) + " has been released!"));
            if (!firstReleaseAnnounced) {
                firstReleaseAnnounced = true;
                for (ServerPlayer r : onlineMembers(Roster.CLASSIC_TEAM)) {
                    Msg.send(r, Msg.good("The first hunter is out. Keep running!"));
                }
            }
        }
        if (kind == GameKind.TEAMS && !due.isEmpty()) {
            Msg.broadcast(server, Msg.good("GO! The teams are released."));
        }
    }

    private void showCountdown() {
        String prefix = kind == GameKind.TEAMS ? "Starting in " : "Released in ";
        for (UUID id : releaseAt.keySet()) {
            ServerPlayer p = player(id);
            if (p != null && freeze.isFrozen(id)) {
                Msg.actionbar(p, Component.literal(prefix + TextUtil.secondsToClock(secondsUntilRelease(id))));
            }
        }
    }

    private void announceStart() {
        if (releaseAt.isEmpty()) {
            Msg.broadcast(server, Msg.good("Manhunt started (no hunters, test game)."));
            return;
        }
        if (kind == GameKind.CLASSIC) {
            int first = releaseAt.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            Msg.broadcast(server, Msg.good("Manhunt has begun! Runners get " + clock(first) + " before the hunters are released."));
            for (ServerPlayer r : onlineMembers(Roster.CLASSIC_TEAM)) {
                Msg.title(r, Component.literal("Run!"), Component.literal("You have " + clock(first) + " head start"), 10, 60, 10);
                Msg.send(r, Msg.info("You have " + clock(first) + " head start."));
                Msg.sound(r, SoundEvents.UI_BUTTON_CLICK, 1f, 1f); // VERIFY: SoundEvents.UI_BUTTON_CLICK
            }
            for (ServerPlayer h : onlineHunters()) {
                int ticks = releaseAt.getOrDefault(h.getUUID(), 0);
                String sub = ticks > 0 ? "Released in " + clock(ticks) : "Go!";
                Msg.title(h, Component.literal("Hunter"), Component.literal(sub), 10, 60, 10);
                Msg.send(h, Msg.info(ticks > 0 ? "You are frozen for " + clock(ticks) + "." : "You are free."));
                Msg.sound(h, SoundEvents.UI_BUTTON_CLICK, 1f, 1f); // VERIFY: SoundEvents.UI_BUTTON_CLICK
            }
        } else {
            int countdown = releaseAt.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            Msg.broadcast(server, Msg.good("Teams manhunt begins! Get ready, " + clock(countdown) + " to the GO."));
            for (ServerPlayer p : onlinePlayers(startedMemberIds())) {
                Msg.title(p, Component.literal("Get ready!"), Component.literal("Starting in " + clock(countdown)), 10, 60, 10);
                Msg.sound(p, SoundEvents.UI_BUTTON_CLICK, 1f, 1f); // VERIFY: SoundEvents.UI_BUTTON_CLICK
            }
        }
    }

    private void killPlayer(ServerPlayer p) {
        if (!p.isAlive()) return;
        p.setInvulnerable(false);
        ServerLevel level = (ServerLevel) p.level();
        // Verified in the 26.2 reference: hurtServer(level, level.damageSources().generic(), amount).
        p.hurtServer(level, level.damageSources().generic(), FINAL_DAMAGE);
    }

    private void rememberOnline(UUID id) {
        ServerPlayer p = player(id);
        if (p != null) roster.rememberName(id, p.getGameProfile().name());
    }

    private String requireIdle() {
        return phase == Phase.IDLE ? null : "Roles and teams can only change while no game is running.";
    }

    private boolean pending(UUID id) {
        return releaseAt.containsKey(id);
    }

    private long elapsedTicks() {
        return tickCounter - startTick;
    }

    private String nameOf(UUID id) {
        String name = roster.nameOf(id);
        return name != null ? name : "A hunter";
    }

    private static String clock(int ticks) {
        return TextUtil.secondsToClock((ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
    }

    private static int toTicks(int seconds, double scale) {
        return Math.max(0, (int) Math.round(seconds * (double) TICKS_PER_SECOND * scale));
    }

    private List<UUID> teamMemberIds(String teamId) {
        TeamInfo t = roster.team(teamId);
        return t == null ? List.of() : new ArrayList<>(t.members); // VERIFY: TeamInfo.members public
    }

    private List<UUID> startedMemberIds() {
        List<UUID> ids = new ArrayList<>();
        for (String teamId : startedTeams) ids.addAll(teamMemberIds(teamId));
        return ids;
    }

    /** Every roster player whose role matters for the current game (hunters + started team members). */
    private List<UUID> participantIds() {
        List<UUID> ids = new ArrayList<>(roster.hunters());
        ids.addAll(startedMemberIds());
        return ids;
    }

    private List<String> teamsStillIn() {
        List<String> out = new ArrayList<>();
        for (String teamId : startedTeams) {
            if (!downTeams.contains(teamId)) out.add(teamId);
        }
        return out;
    }

    private String teamWithMostAlive() {
        String best = null;
        int bestCount = 0;
        for (String teamId : startedTeams) {
            if (downTeams.contains(teamId)) continue;
            int alive = 0;
            for (UUID id : teamMemberIds(teamId)) {
                if (!eliminated.contains(id)) alive++;
            }
            if (alive > bestCount) {
                best = teamId;
                bestCount = alive;
            }
        }
        return best;
    }

    private List<ServerPlayer> onlineHunters() {
        return onlinePlayers(roster.hunters());
    }

    private List<ServerPlayer> onlineMembers(String teamId) {
        return onlinePlayers(teamMemberIds(teamId));
    }

    private List<ServerPlayer> onlinePlayers(Collection<UUID> ids) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : ids) {
            ServerPlayer p = player(id);
            if (p != null) out.add(p);
        }
        return out;
    }
}
