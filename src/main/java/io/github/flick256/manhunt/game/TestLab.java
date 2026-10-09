package io.github.flick256.manhunt.game;

import io.github.flick256.manhunt.core.GameKind;
import io.github.flick256.manhunt.core.Phase;
import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.core.Schedule;
import io.github.flick256.manhunt.core.Settings;
import io.github.flick256.manhunt.core.Side;
import io.github.flick256.manhunt.util.Msg;
import io.github.flick256.manhunt.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items; // VERIFY: DIAMOND_HELMET/CHESTPLATE/LEGGINGS/BOOTS, GOLDEN_CARROT, SHIELD

import java.util.Locale;
import java.util.UUID;

/**
 * Runtime-only helpers for testing every feature alone. Nothing here is persisted.
 *
 * <p>"Virtual teammate" operations act directly on the shared team pool of {@link Roster#CLASSIC_TEAM}
 * through {@link TeamSync}, so the owner sees the effect on their own health, hunger and inventory.
 */
public final class TestLab {
    public static final String VIRTUAL_NAME = "Steve_Test";

    private static final int FREEZE_MAX_SECONDS = 600;

    private final ManhuntGame game;
    private boolean active;
    private boolean fastTimers;
    private UUID soloOwner;

    // previewFreeze countdown (ticks, ignores timeScale)
    private UUID freezeOwner;
    private int freezeTicks;

    public TestLab(ManhuntGame game) {
        this.game = game;
    }

    public boolean isActive() {
        return active;
    }

    /** 0.1 while fast timers are on (durations scaled to 10%), otherwise 1.0. */
    public double timeScale() {
        return fastTimers ? 0.1 : 1.0;
    }

    public boolean fastTimers() {
        return fastTimers;
    }

    public void setFastTimers(boolean on) {
        fastTimers = on;
    }

    /**
     * Starts a CLASSIC game in which the owner is the only real runner, with the virtual teammate
     * {@value #VIRTUAL_NAME} sharing the team pool. Returns null on success, otherwise an error message.
     */
    public String startSolo(ServerPlayer owner) {
        if (game.phase() != Phase.IDLE) {
            return "Stop the current game first";
        }
        String err = game.setKind(GameKind.CLASSIC);
        if (err != null) {
            return err;
        }
        game.roster().ensureClassicTeam();
        err = game.roster().assignRunner(owner.getUUID(), Roster.CLASSIC_TEAM);
        if (err != null) {
            return err;
        }
        active = true;
        soloOwner = owner.getUUID();
        err = game.start(owner); // ManhuntGame.start accepts test mode: no hunters needed
        if (err != null) {
            active = false;
            soloOwner = null;
            return err;
        }
        Msg.send(owner, Msg.good("Virtual teammate " + VIRTUAL_NAME + " joined your team."));
        Msg.send(owner, Msg.info("The team pool is shared with " + VIRTUAL_NAME + "; the test buttons act as them."));
        Msg.send(owner, Msg.info("Damage / Heal / Hunger: change the shared pool as if " + VIRTUAL_NAME + " did it."));
        Msg.send(owner, Msg.info("Gear: adds a diamond kit, 16 golden carrots and a shield to the shared inventory."));
        Msg.send(owner, Msg.info("Freeze: freezes you with a countdown. Stagger: prints the hunter release schedule."));
        Msg.send(owner, Msg.info("Quiz: starts a respawn math quiz. Ceremony: plays a victory or hunters-win show."));
        Msg.send(owner, Msg.info("Fast timers: scales timers to 10% of their length. End test: stops the game."));
        return null;
    }

    /** Stops the test game, releases any freeze, clears the flags and removes the owner from the solo team. */
    public void endSolo() {
        UUID owner = soloOwner;
        boolean wasActive = active;
        active = false;
        fastTimers = false;
        soloOwner = null;
        releaseFreeze();
        game.stop(false);
        if (wasActive && owner != null) {
            game.unassign(owner);
        }
    }

    public void virtualDamage(ServerPlayer owner, float hp) {
        String err = poolError();
        if (err != null) {
            Msg.send(owner, Msg.bad(err));
            return;
        }
        game.sync().injectDamage(Roster.CLASSIC_TEAM, hp);
        poolReport(owner, VIRTUAL_NAME + " hit the team for " + fmtHp(hp) + " hp.");
    }

    public void virtualHeal(ServerPlayer owner, float hp) {
        String err = poolError();
        if (err != null) {
            Msg.send(owner, Msg.bad(err));
            return;
        }
        game.sync().injectHeal(Roster.CLASSIC_TEAM, hp);
        poolReport(owner, VIRTUAL_NAME + " healed the team for " + fmtHp(hp) + " hp.");
    }

    public void virtualHunger(ServerPlayer owner, int food) {
        String err = poolError();
        if (err != null) {
            Msg.send(owner, Msg.bad(err));
            return;
        }
        game.sync().injectHunger(Roster.CLASSIC_TEAM, food);
        Msg.send(owner, Msg.info(VIRTUAL_NAME + " drained " + food + " food. Shared food now "
                + game.sync().poolFood(Roster.CLASSIC_TEAM) + "."));
    }

    /** Adds a diamond kit, 16 golden carrots and a shield to the shared inventory. */
    public void virtualGear(ServerPlayer owner) {
        String err = poolError();
        if (err != null) {
            Msg.send(owner, Msg.bad(err));
            return;
        }
        String team = Roster.CLASSIC_TEAM;
        game.sync().injectItem(team, new ItemStack(Items.DIAMOND_SWORD));
        game.sync().injectItem(team, new ItemStack(Items.DIAMOND_PICKAXE));
        game.sync().injectItem(team, new ItemStack(Items.DIAMOND_HELMET));     // VERIFY: Items.DIAMOND_HELMET
        game.sync().injectItem(team, new ItemStack(Items.DIAMOND_CHESTPLATE)); // VERIFY: Items.DIAMOND_CHESTPLATE
        game.sync().injectItem(team, new ItemStack(Items.DIAMOND_LEGGINGS));   // VERIFY: Items.DIAMOND_LEGGINGS
        game.sync().injectItem(team, new ItemStack(Items.DIAMOND_BOOTS));      // VERIFY: Items.DIAMOND_BOOTS
        game.sync().injectItem(team, new ItemStack(Items.GOLDEN_CARROT, 16));  // VERIFY: Items.GOLDEN_CARROT, ItemStack(Item,int)
        game.sync().injectItem(team, new ItemStack(Items.SHIELD));             // VERIFY: Items.SHIELD
        Msg.send(owner, Msg.good("Virtual gear added to the shared inventory."));
        Msg.send(owner, Msg.info("Inventory: " + game.sync().inventorySummary(team)));
    }

    /** Freezes the owner for {@code seconds} with an action bar countdown. Timers scaling does not apply. */
    public void previewFreeze(ServerPlayer owner, int seconds) {
        int s = Math.max(1, Math.min(seconds, FREEZE_MAX_SECONDS));
        releaseFreeze();
        freezeOwner = owner.getUUID();
        freezeTicks = s * 20;
        game.freeze().freeze(owner);
        Msg.send(owner, Msg.info("Frozen for " + TextUtil.secondsToClock(s) + "."));
    }

    /** Prints the release schedule for 4 fake hunters using the current settings. */
    public void previewStagger(ServerPlayer owner) {
        Settings s = game.settings();
        int[] times = Schedule.releaseSeconds(4, s.headStartSeconds, s.releaseMode, s.staggerSeconds);
        Msg.send(owner, Msg.info("Preview with the current settings (4 fake hunters):"));
        for (int i = 0; i < times.length; i++) {
            String line = "Hunter " + (i + 1) + " released at " + TextUtil.secondsToClock(times[i]);
            if (fastTimers) {
                long real = Math.round(times[i] * timeScale());
                line += " (real life: " + TextUtil.secondsToClock((int) real) + ")";
            }
            Msg.send(owner, Msg.info(line));
        }
    }

    /** Runs the respawn quiz for the owner regardless of the mathRespawn setting. */
    public void quizNow(ServerPlayer owner) {
        game.quiz().start(owner);
    }

    /**
     * Plays the ceremony for the owner as the only winner. Allowed when no game is running, or during the
     * Test Lab solo game. Does not end or reset anything.
     */
    public void ceremony(ServerPlayer owner, Side side) {
        if (game.phase() != Phase.IDLE && !active) {
            Msg.send(owner, Msg.bad("Stop the current game first"));
            return;
        }
        if (game.ceremony().isPlaying()) {
            Msg.send(owner, Msg.bad("A ceremony is already playing"));
            return;
        }
        game.ceremony().playPreview(side, owner);
    }

    /** Drives the previewFreeze countdown. Called every server tick by ManhuntGame. */
    public void tick() {
        if (freezeOwner == null) {
            return;
        }
        ServerPlayer p = game.player(freezeOwner);
        if (p == null) {
            return; // offline: pause the countdown until they are back (FreezeManager.clear covers stop)
        }
        if (freezeTicks > 0) {
            freezeTicks--;
            if (freezeTicks % 20 == 0 && freezeTicks > 0) {
                Msg.actionbar(p, Component.literal("Frozen: " + TextUtil.secondsToClock((freezeTicks + 19) / 20))
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        if (freezeTicks <= 0) {
            game.freeze().unfreeze(p);
            freezeOwner = null;
            Msg.actionbar(p, Component.literal("Unfrozen").withStyle(ChatFormatting.GREEN));
            Msg.send(p, Msg.good("Unfrozen."));
        }
    }

    // ---------------------------------------------------------------- internals

    /** Null when the solo test is running and the shared pool exists, otherwise an error for the owner. */
    private String poolError() {
        if (!active) {
            return "Start the solo test first";
        }
        if (!game.sync().hasTeam(Roster.CLASSIC_TEAM)) {
            return "No shared pool yet (the game is not running)";
        }
        return null;
    }

    private void poolReport(ServerPlayer owner, String what) {
        String team = Roster.CLASSIC_TEAM;
        Msg.send(owner, Msg.info(what + " Shared health now " + fmtHp(game.sync().poolHealth(team))
                + "/" + fmtHp(game.sync().poolMax(team)) + "."));
    }

    private void releaseFreeze() {
        if (freezeOwner == null) {
            return;
        }
        ServerPlayer p = game.player(freezeOwner);
        if (p != null) {
            game.freeze().unfreeze(p);
        }
        freezeOwner = null;
        freezeTicks = 0;
    }

    private static String fmtHp(float v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
