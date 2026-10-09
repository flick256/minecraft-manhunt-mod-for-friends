package io.github.flick256.manhunt.game;

import io.github.flick256.manhunt.core.Settings;
import io.github.flick256.manhunt.core.quiz.Question;
import io.github.flick256.manhunt.core.quiz.QuizBank;
import io.github.flick256.manhunt.core.quiz.Topic;
import io.github.flick256.manhunt.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Quiz a hunter must pass before they can move again after dying. The dying player picks one of the subjects the
 * owner enabled (math, VCE-style methods, biology, chemistry, physics, history, french), then answers
 * {@code mathQuestions} questions. While in the quiz the player is frozen in place, blinded and invulnerable
 * (NOT in spectator mode: spectators can fly around and scout the runners, which would make dying an advantage).
 * State is keyed by UUID so a player who disconnects keeps their progress.
 */
public final class RespawnQuiz {
    /** How often (ticks) the current question is re-shown on the action bar. */
    private static final int REMIND_PERIOD = 60;
    /** The blindness effect is re-applied this often (ticks) and lasts a bit longer than that. */
    private static final int BLIND_REFRESH = 40;
    private static final int BLIND_DURATION = 100;

    private static final class State {
        Topic topic;      // null until the player has picked a subject
        Question current; // null until a subject is picked
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

    /** Locks the player and starts the quiz (asks for a subject first when several are enabled). Also used by the Test Lab. */
    public void start(ServerPlayer p) {
        Settings s = game.settings();
        State st = new State();
        st.needed = Math.max(1, s.mathQuestions);
        st.solved = 0;
        List<Topic> topics = s.enabledTopics();
        if (topics.size() == 1) {
            st.topic = topics.get(0);
        }
        states.put(p.getUUID(), st);
        lock(p);
        if (st.topic == null) {
            askForTopic(p, topics);
        } else {
            ask(p, st);
        }
    }

    /**
     * Chat line from a player. Returns true when the text was consumed (a subject choice or a graded answer);
     * false when the player is not in the quiz or the text is just ordinary chat, which is then broadcast normally.
     */
    public boolean handleAnswer(ServerPlayer p, String text) {
        return handle(p, text, false);
    }

    /**
     * {@code /manhunt answer <text>} or {@code /manhunt topic <subject>}. Returns false only when the player is not in
     * the quiz; text that is not an answer gets a hint instead of being ignored.
     */
    public boolean handleCommand(ServerPlayer p, String text) {
        return handle(p, text, true);
    }

    private boolean handle(ServerPlayer p, String text, boolean fromCommand) {
        State st = states.get(p.getUUID());
        if (st == null) {
            return false;
        }
        String in = text == null ? "" : text.trim();
        List<Topic> topics = game.settings().enabledTopics();

        // Grade an answer first, so a subject word can never hide a real answer.
        if (st.current != null && st.current.isAnswerAttempt(in)) {
            if (st.current.check(in)) {
                st.solved++;
                if (st.solved >= st.needed) {
                    release(p);
                    return true;
                }
                Msg.sound(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                Msg.send(p, Msg.good("Correct! " + (st.needed - st.solved) + " more to go."));
            } else {
                Msg.send(p, Msg.bad("Wrong! New question."));
                Msg.sound(p, SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
            }
            ask(p, st);
            return true;
        }

        // Pick or switch subject: by name ("biology", "bio") or by its number in the list.
        Topic picked = parseTopic(in, topics);
        if (picked != null) {
            if (!topics.contains(picked)) {
                Msg.send(p, Msg.bad(picked.display() + " is not enabled. Choose: " + names(topics)));
                return true;
            }
            st.topic = picked;
            Msg.send(p, Msg.info("Subject: " + picked.display()));
            ask(p, st);
            return true;
        }

        if (fromCommand) {
            remind(p, st);
            return true;
        }
        return false; // ordinary chat
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

    private void askForTopic(ServerPlayer p, List<Topic> topics) {
        Msg.title(p,
                Component.literal("Pick a subject").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("to answer and respawn").withStyle(ChatFormatting.YELLOW),
                10, 70, 20);
        sendTopicList(p, topics);
    }

    private void sendTopicList(ServerPlayer p, List<Topic> topics) {
        Msg.send(p, Msg.info("Pick a subject to answer so you can respawn (type its name or number in chat):"));
        for (int i = 0; i < topics.size(); i++) {
            Msg.send(p, Component.literal("  " + (i + 1) + ") " + topics.get(i).display() + " - " + topics.get(i).blurb())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private void ask(ServerPlayer p, State st) {
        String avoid = st.current == null ? null : st.current.prompt();
        st.current = QuizBank.next(st.topic, game.settings().mathDifficulty, rnd, avoid);
        Msg.title(p,
                Component.literal("Solve to respawn " + progress(st)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(st.topic.display()).withStyle(ChatFormatting.YELLOW),
                10, 70, 20);
        remind(p, st);
    }

    /** Sends the chat prompt and action bar for the current question (or the subject list) without generating a new one. */
    private void remind(ServerPlayer p, State st) {
        if (st.current == null) {
            sendTopicList(p, game.settings().enabledTopics());
            Msg.actionbar(p, actionbar(st));
            return;
        }
        Msg.send(p, Msg.info("[" + st.topic.display() + "] Question " + progress(st) + ": " + st.current.prompt()));
        List<String> choices = st.current.choices();
        for (int i = 0; i < choices.size(); i++) {
            Msg.send(p, Component.literal("   " + (char) ('A' + i) + ") " + choices.get(i)).withStyle(ChatFormatting.YELLOW));
        }
        String how = st.current.isMultipleChoice()
                ? "Type A, B, C or D in chat (or /manhunt answer <letter>)."
                : "Type the number in chat (or /manhunt answer <n>).";
        if (game.settings().enabledTopics().size() > 1) {
            how += " Type a subject name to switch subject.";
        }
        Msg.send(p, Component.literal(how).withStyle(ChatFormatting.GRAY));
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
        Msg.sound(p, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
        Msg.send(p, Msg.good("Respawned!"));
    }

    private Component actionbar(State st) {
        if (st.current == null) {
            return Component.literal("Pick a subject: " + names(game.settings().enabledTopics()))
                    .withStyle(ChatFormatting.GOLD);
        }
        String q = st.current.prompt();
        if (q.length() > 70) {
            q = q.substring(0, 67) + "...";
        }
        return Component.literal(st.topic.display() + " " + progress(st) + ": " + q).withStyle(ChatFormatting.GOLD);
    }

    private static String progress(State st) {
        return "(" + (st.solved + 1) + "/" + st.needed + ")";
    }

    private static String names(List<Topic> topics) {
        StringBuilder sb = new StringBuilder();
        for (Topic t : topics) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(t.id());
        }
        return sb.toString();
    }

    /** A subject by name/alias, or by its 1-based number in the enabled list. */
    private static Topic parseTopic(String in, List<Topic> enabled) {
        Topic byName = Topic.parse(in);
        if (byName != null) {
            return byName;
        }
        try {
            int n = Integer.parseInt(in.trim());
            if (n >= 1 && n <= enabled.size()) {
                return enabled.get(n - 1);
            }
        } catch (NumberFormatException ignored) {
            // not a number
        }
        return null;
    }
}
