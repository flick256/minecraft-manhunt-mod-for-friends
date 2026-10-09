package io.github.flick256.manhunt.game;

import io.github.flick256.manhunt.core.MathQuestion;
import io.github.flick256.manhunt.core.MathQuiz;
import io.github.flick256.manhunt.core.Settings;
import io.github.flick256.manhunt.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Math quiz a hunter must solve before they can move again after dying. While in the quiz the player is frozen in
 * place, blinded and invulnerable (NOT in spectator mode: spectators can fly around and scout the runners, which
 * would make dying an advantage). State is keyed by UUID so a player who disconnects keeps their progress.
 */
public final class RespawnQuiz {
    /** How often (ticks) the current question is re-shown on the action bar. */
    private static final int REMIND_PERIOD = 60;
    /** The blindness effect is re-applied this often (ticks) and lasts a bit longer than that. */
    private static final int BLIND_REFRESH = 40;
    private static final int BLIND_DURATION = 100;

    private static final class State {
        MathQuestion current;
        int solved;
        int needed;
    }

    private final ManhuntGame game;
    private final Random rnd = new Random();
    private final Map<UUID, State> states = new HashMap<>();
    private int tickCounter;

    public RespawnQuiz(ManhuntGame game) {
        this.game = game;
    }

    /** Called by ManhuntGame after a hunter respawns. Does nothing unless the mathRespawn setting is on. */
    public void onHunterRespawned(ServerPlayer p) {
        if (!game.settings().mathRespawn) {
            return;
        }
        start(p);
    }

    public boolean isInQuiz(UUID id) {
        return states.containsKey(id);
    }

    /** Freezes + blinds the player and asks the first question. Also used by the Test Lab. */
    public void start(ServerPlayer p) {
        Settings s = game.settings();
        State st = new State();
        st.needed = Math.max(1, s.mathQuestions);
        st.solved = 0;
        states.put(p.getUUID(), st);
        lock(p);
        ask(p, st);
    }

    /**
     * Consumes a chat line (or /manhunt answer text) from a player in the quiz.
     * Returns false if the player is not in the quiz, true if the text was consumed (right or wrong).
     */
    public boolean handleAnswer(ServerPlayer p, String text) {
        State st = states.get(p.getUUID());
        if (st == null) {
            return false;
        }
        if (MathQuiz.check(st.current, text)) {
            st.solved++;
            if (st.solved >= st.needed) {
                release(p);
                return true;
            }
            Msg.sound(p, Ceremony.soundOf(SoundEvents.EXPERIENCE_ORB_PICKUP), 1.0f, 1.2f); // VERIFY: SoundEvents.EXPERIENCE_ORB_PICKUP
            Msg.send(p, Msg.good("Correct! " + (st.needed - st.solved) + " more to go."));
            ask(p, st);
        } else {
            Msg.send(p, Msg.bad("Wrong! New question."));
            Msg.sound(p, Ceremony.soundOf(SoundEvents.VILLAGER_NO), 1.0f, 1.0f); // VERIFY: SoundEvents.VILLAGER_NO
            ask(p, st);
        }
        return true;
    }

    /**
     * Re-shows the question on the action bar every {@link #REMIND_PERIOD} ticks, keeps the blindness on and
     * re-freezes quiz players who are online but no longer locked (e.g. after reconnecting).
     */
    public void tick() {
        tickCounter++;
        if (states.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, State> e : states.entrySet()) {
            ServerPlayer p = game.player(e.getKey());
            if (p == null) {
                continue; // offline: keep the state until they come back
            }
            if (!game.freeze().isFrozen(e.getKey())) {
                lock(p);
                remind(p, e.getValue());
            } else if (tickCounter % BLIND_REFRESH == 0) {
                blind(p);
            }
            if (tickCounter % REMIND_PERIOD == 0) {
                Msg.actionbar(p, actionbar(e.getValue()));
            }
        }
    }

    /** Releases everybody still in the quiz (online players are unfrozen and can see again) and forgets all state. */
    public void clear() {
        for (UUID id : states.keySet()) {
            ServerPlayer p = game.player(id);
            if (p != null) {
                unlock(p);
            }
        }
        states.clear();
    }

    // ---------------------------------------------------------------- internals

    /** Frozen at the respawn point, blind, invulnerable (the freeze handles the last two and blocks interaction). */
    private void lock(ServerPlayer p) {
        game.freeze().freeze(p);
        blind(p);
    }

    private void unlock(ServerPlayer p) {
        game.freeze().unfreeze(p);
        p.removeEffect(MobEffects.BLINDNESS);
    }

    private static void blind(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_DURATION, 0, false, false, false));
    }

    private void ask(ServerPlayer p, State st) {
        st.current = MathQuiz.next(game.settings().mathDifficulty, rnd);
        remind(p, st);
        String progress = progress(st);
        Msg.title(p,
                Component.literal("Solve to respawn " + progress).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(expression(st.current) + " = ?").withStyle(ChatFormatting.YELLOW),
                10, 70, 20);
    }

    /** Sends the chat prompt and action bar for the current question without generating a new one. */
    private void remind(ServerPlayer p, State st) {
        Msg.send(p, Msg.info("Solve to respawn " + progress(st) + ": " + expression(st.current)
                + " = ?  Type the number in chat or /manhunt answer <n>"));
        Msg.actionbar(p, actionbar(st));
    }

    private void release(ServerPlayer p) {
        states.remove(p.getUUID());
        unlock(p);
        // Died during the head start: still wait for the normal release time.
        if (game.secondsUntilRelease(p.getUUID()) > 0) {
            game.freeze().freeze(p);
        }
        Msg.title(p,
                Component.literal("Respawned!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal("Back in the hunt").withStyle(ChatFormatting.GRAY),
                10, 60, 20);
        Msg.sound(p, Ceremony.soundOf(SoundEvents.PLAYER_LEVELUP), 1.0f, 1.0f); // VERIFY: SoundEvents.PLAYER_LEVELUP
        Msg.send(p, Msg.good("Respawned!"));
    }

    private Component actionbar(State st) {
        return Component.literal("Respawn quiz " + progress(st) + ": " + expression(st.current) + " = ?")
                .withStyle(ChatFormatting.GOLD);
    }

    private static String progress(State st) {
        return "(" + (st.solved + 1) + "/" + st.needed + ")";
    }

    /** Tolerates MathQuestion.text() with or without a trailing "= ?". */
    private static String expression(MathQuestion q) {
        String t = q.text().trim();
        if (t.endsWith("?")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        if (t.endsWith("=")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return t;
    }
}
