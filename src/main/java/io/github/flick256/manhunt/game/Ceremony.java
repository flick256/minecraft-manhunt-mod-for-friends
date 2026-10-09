package io.github.flick256.manhunt.game;

import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.core.Side;
import io.github.flick256.manhunt.core.Winner;
import io.github.flick256.manhunt.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes; // VERIFY: ParticleTypes not in Fabric reference (vanilla name)
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Victory / defeat show, {@link #TOTAL_TICKS} ticks long. Titles, a fanfare built from note block sounds,
 * particle rings around the winners, and a chat banner every 60 ticks.
 *
 * <p>When the show ends it calls {@link ManhuntGame#finishCeremony()}. A show started through
 * {@link #playPreview} (Test Lab) never touches the game phase.
 */
public final class Ceremony {
    public static final int TOTAL_TICKS = 320;

    private static final int PARTICLE_PERIOD = 5;
    private static final int BANNER_PERIOD = 60;
    private static final int THUNDER_PERIOD = 40;
    private static final int BURST_PERIOD = 20;

    /** Rising arpeggio for runner victories, one note every 4 ticks (t = 0..24). */
    private static final float[] ARPEGGIO = {0.5f, 0.63f, 0.75f, 1.0f, 1.26f, 1.5f, 2.0f};
    /** Descending minor sting for hunter victories (A G F E D), one note every 6 ticks (t = 0..24). */
    private static final float[] MINOR_STING = {1.0f, 0.89f, 0.75f, 0.67f, 0.56f};

    private final ManhuntGame game;
    private final List<ServerPlayer> winners = new ArrayList<>();
    private final List<ServerPlayer> losers = new ArrayList<>();
    private boolean playing;
    private boolean previewOnly;
    private int time;
    private Side side = Side.RUNNERS;
    private String label = "";

    public Ceremony(ManhuntGame game) {
        this.game = game;
    }

    /** Plays the full show; at the end the game is reset via {@link ManhuntGame#finishCeremony()}. */
    public void play(Winner w, List<ServerPlayer> winners, List<ServerPlayer> losers, String winnerLabel) {
        begin(w, winners, losers, winnerLabel, false);
    }

    /**
     * Test Lab variant: plays the same show for {@code owner} as the winner (no losers) and does NOT call
     * {@link ManhuntGame#finishCeremony()} at the end, so a running or idle game is left alone.
     */
    public void playPreview(Side previewSide, ServerPlayer owner) {
        String name = game.roster().nameOf(owner.getUUID());
        String who = name == null ? "Test" : name;
        String teamId = previewSide == Side.TEAM ? "test"
                : previewSide == Side.RUNNERS ? Roster.CLASSIC_TEAM
                : null;
        begin(new Winner(previewSide, teamId), List.of(owner), List.of(), who, true);
    }

    public boolean isPlaying() {
        return playing;
    }

    public int totalTicks() {
        return TOTAL_TICKS;
    }

    /** Advances the show by one tick. Ends by calling finishCeremony (unless preview). */
    public void tick() {
        if (!playing) {
            return;
        }
        time++;
        if (time >= TOTAL_TICKS) {
            finish();
            return;
        }
        step(time);
    }

    /** Stops the show immediately without calling finishCeremony. Restores winners' invulnerability. */
    public void cancel() {
        for (ServerPlayer p : winners) {
            p.setInvulnerable(false);
        }
        winners.clear();
        losers.clear();
        playing = false;
        previewOnly = false;
        time = 0;
    }

    // ---------------------------------------------------------------- internals

    private void begin(Winner w, List<ServerPlayer> winnerList, List<ServerPlayer> loserList,
                       String winnerLabel, boolean preview) {
        if (playing) {
            cancel();
        }
        side = w.side();
        label = winnerLabel == null ? "" : winnerLabel;
        previewOnly = preview;
        winners.clear();
        losers.clear();
        for (ServerPlayer p : winnerList) {
            if (p != null) {
                winners.add(p);
            }
        }
        for (ServerPlayer p : loserList) {
            if (p != null) {
                losers.add(p);
            }
        }
        for (ServerPlayer p : winners) {
            p.setInvulnerable(true);
        }
        playing = true;
        time = 0;
        step(0);
    }

    private void finish() {
        boolean preview = previewOnly;
        cancel();
        if (!preview) {
            game.finishCeremony();
        }
    }

    private void step(int t) {
        boolean runnersWin = side != Side.HUNTERS;
        if (t == 0) {
            titles(runnersWin);
        }
        if (t % PARTICLE_PERIOD == 0) {
            visuals(t, runnersWin);
        }
        if (t > 0 && t % BANNER_PERIOD == 0) {
            banner();
        }
        if (!runnersWin && t > 0 && t % THUNDER_PERIOD == 0) {
            thunder();
        }
        if (runnersWin) {
            victoryMusic(t);
        } else {
            hunterMusic(t);
        }
    }

    private void titles(boolean runnersWin) {
        for (ServerPlayer p : online(winners)) {
            if (runnersWin) {
                String sub = side == Side.TEAM
                        ? label + " wins the manhunt!"
                        : label + " beat the Ender Dragon!";
                Msg.title(p, styled("VICTORY!", ChatFormatting.GOLD, ChatFormatting.BOLD),
                        styled(sub, ChatFormatting.YELLOW), 10, 80, 20);
            } else {
                Msg.title(p, styled("HUNTERS WIN!", ChatFormatting.RED, ChatFormatting.BOLD),
                        styled("The runners have fallen", ChatFormatting.GRAY), 10, 80, 20);
            }
        }
        for (ServerPlayer p : online(losers)) {
            Msg.title(p, styled("DEFEAT", ChatFormatting.RED, ChatFormatting.BOLD),
                    styled(runnersWin ? "Better luck next time" : "The hunters caught you", ChatFormatting.GRAY),
                    10, 80, 20);
        }
    }

    private void visuals(int t, boolean runnersWin) {
        double progress = t / (double) TOTAL_TICKS;
        double radius = 0.5 + 2.5 * progress;      // ring expands over the show
        double phase = t * 0.2;                    // ring rotates
        double rise = (t % 60) / 60.0 * 2.5;       // inner ring rises and restarts every 3 seconds

        for (ServerPlayer p : online(winners)) {
            ServerLevel level = p.level();
            double x = p.getX();
            double y = p.getY();
            double z = p.getZ();
            if (runnersWin) {
                ring(level, x, y + 1.0, z, radius, 16, phase, ParticleTypes.END_ROD);
                ring(level, x, y + rise, z, 1.2, 10, -phase, ParticleTypes.HAPPY_VILLAGER);
                if (t % BURST_PERIOD == 0) {
                    burst(level, x, y + 1.0, z, ParticleTypes.FIREWORK, 30, 0.4, 0.15);
                    burst(level, x, y + 1.0, z, ParticleTypes.TOTEM_OF_UNDYING, 20, 0.3, 0.2);
                }
            } else {
                ring(level, x, y + 0.1, z, radius, 14, phase, ParticleTypes.SOUL_FIRE_FLAME);
                ring(level, x, y + rise, z, 0.8, 8, -phase, ParticleTypes.CRIT);
                if (t % BURST_PERIOD == 0) {
                    burst(level, x, y + 1.0, z, ParticleTypes.SMOKE, 30, 0.4, 0.02);
                }
            }
        }
        if (!runnersWin) {
            for (ServerPlayer p : online(losers)) {
                ring(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), 1.0, 12, phase, ParticleTypes.SMOKE);
            }
        }
    }

    /** Fanfare for runner / team victories. */
    private void victoryMusic(int t) {
        if (t == 0) {
            playAll(Ceremony.soundOf(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), 0.8f, 1.0f); // VERIFY: SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
        }
        if (t % 4 == 0 && t <= 24) {
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_BELL), 1.0f, ARPEGGIO[t / 4]); // VERIFY: SoundEvents.NOTE_BLOCK_BELL
        }
        // Held chord: strikes every 8 ticks from t=32 to t=96, bass only on the first strike.
        if (t >= 32 && t <= 96 && (t - 32) % 8 == 0) {
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_CHIME), 0.9f, 1.0f);  // VERIFY: SoundEvents.NOTE_BLOCK_CHIME
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_CHIME), 0.9f, 1.26f);
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_CHIME), 0.9f, 1.5f);
            if (t == 32) {
                playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_BASS), 1.0f, 0.5f); // VERIFY: SoundEvents.NOTE_BLOCK_BASS
            }
        }
        if (t == 160) {
            playAll(Ceremony.soundOf(SoundEvents.PLAYER_LEVELUP), 1.0f, 1.0f);    // VERIFY: SoundEvents.PLAYER_LEVELUP
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_CHIME), 1.0f, 2.0f);  // VERIFY: SoundEvents.NOTE_BLOCK_CHIME
        }
    }

    /** Sting for hunter victories. */
    private void hunterMusic(int t) {
        if (t == 0) {
            playAll(Ceremony.soundOf(SoundEvents.WITHER_SPAWN), 0.3f, 1.0f); // VERIFY: SoundEvents.WITHER_SPAWN
        }
        if (t % 6 == 0 && t / 6 < MINOR_STING.length) {
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_BASS), 1.0f, MINOR_STING[t / 6]); // VERIFY: SoundEvents.NOTE_BLOCK_BASS
        }
        // Held minor chord (root, minor third, fifth) struck at t = 40, 64, 88.
        if (t == 40 || t == 64 || t == 88) {
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_BASS), 1.0f, 0.5f);
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_BASS), 1.0f, 0.595f);
            playAll(Ceremony.soundOf(SoundEvents.NOTE_BLOCK_BASS), 1.0f, 0.75f);
        }
    }

    /** Hunter-victory stand-in for a visual-only lightning bolt: thunder sound plus particles at each player. */
    private void thunder() {
        for (ServerPlayer p : everyone()) {
            Msg.sound(p, Ceremony.soundOf(SoundEvents.LIGHTNING_BOLT_THUNDER), 0.6f, 1.0f); // VERIFY: SoundEvents.LIGHTNING_BOLT_THUNDER
            ServerLevel level = p.level();
            burst(level, p.getX(), p.getY() + 1.0, p.getZ(), ParticleTypes.CRIT, 30, 0.5, 0.1);
            burst(level, p.getX(), p.getY() + 1.0, p.getZ(), ParticleTypes.SMOKE, 20, 0.4, 0.05);
        }
    }

    private void banner() {
        String text = "=".repeat(20) + " " + label + " " + "=".repeat(20);
        Component line = Component.literal(text)
                .withStyle(side == Side.HUNTERS ? ChatFormatting.RED : ChatFormatting.GOLD);
        for (ServerPlayer p : everyone()) {
            Msg.send(p, line);
        }
    }

    private void playAll(SoundEvent sound, float volume, float pitch) {
        for (ServerPlayer p : everyone()) {
            Msg.sound(p, sound, volume, pitch);
        }
    }

    private List<ServerPlayer> everyone() {
        List<ServerPlayer> all = online(winners);
        all.addAll(online(losers));
        return all;
    }

    /** Only players who are still the online instance of the same object (disconnects drop out). */
    private List<ServerPlayer> online(List<ServerPlayer> list) {
        List<ServerPlayer> out = new ArrayList<>(list.size());
        for (ServerPlayer p : list) {
            if (game.player(p.getUUID()) == p) {
                out.add(p);
            }
        }
        return out;
    }

    private static Component styled(String text, ChatFormatting... formats) {
        return Component.literal(text).withStyle(formats);
    }

    // VERIFY: ServerLevel#sendParticles(ParticleOptions, double x, double y, double z, int count,
    //         double dx, double dy, double dz, double speed) - not in Fabric reference; vanilla signature.
    private static void ring(ServerLevel level, double cx, double cy, double cz, double radius,
                             int points, double phase, ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double a = phase + Math.PI * 2.0 * i / points;
            level.sendParticles(particle, cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    // VERIFY: same ServerLevel#sendParticles signature as ring().
    private static void burst(ServerLevel level, double x, double y, double z, ParticleOptions particle,
                              int count, double spread, double speed) {
        level.sendParticles(particle, x, y, z, count, spread, spread, spread, speed);
    }

    /**
     * Accepts a sound constant whether {@code SoundEvents.X} is a plain {@link SoundEvent} or a
     * {@code Holder<SoundEvent>} (the Fabric reference uses both shapes). Package-private, used by RespawnQuiz.
     */
    static SoundEvent soundOf(SoundEvent sound) {
        return sound;
    }

    static SoundEvent soundOf(Holder<SoundEvent> sound) {
        return sound.value();
    }
}
