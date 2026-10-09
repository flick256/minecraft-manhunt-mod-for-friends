package io.github.flick256.manhunt.menu;

import io.github.flick256.manhunt.core.ReleaseMode;
import io.github.flick256.manhunt.core.Settings;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Every {@link Settings} field, editable by click. Left click increases, right click decreases,
 * shift multiplies the step for hearts and head start. Changes are clamped and saved at once.
 */
final class SettingsMenu extends GuiMenu {

	SettingsMenu(int containerId, Inventory inventory) {
		super(containerId, inventory);
		refresh();
	}

	/** Signed step for a click: normal amount, or the shifted amount when shift is held. */
	private static int step(Click c, int normal, int shifted) {
		int amount = c.shift() ? shifted : normal;
		return c.right() ? -amount : amount;
	}

	/** Clamp, persist, and (for pool/hunger) push the new values into the running game. */
	private static void commit(ManhuntGame game, boolean liveSync) {
		game.settings().clamp();
		game.save();
		if (liveSync) {
			game.sync().applySettingsLive();
		}
	}

	@Override
	protected void build(ManhuntGame g) {
		Settings s = g.settings();

		show(4, Icons.make(Items.BOOK, "Settings", ChatFormatting.GOLD,
				"Changes apply at once and are saved.",
				"Left click: increase, right click: decrease.",
				"Shift: x5 hearts, x60 seconds."));

		// Pool and hunger
		set(10, Icons.make(Items.APPLE, "Pool: " + s.poolHearts + " hearts", ChatFormatting.RED,
				"Shared life per runner team (" + s.poolHearts * 2 + " hp).",
				"Left/right: +1/-1, shift: +5/-5."),
				(p, game, c) -> {
					game.settings().poolHearts += step(c, 1, 5);
					commit(game, true);
					refresh();
				});

		set(11, Icons.make(Items.COOKED_BEEF, "Hunger multiplier: x" + String.format(Locale.ROOT, "%.1f", s.hungerMultiplier),
				ChatFormatting.GOLD,
				"Shared hunger drain is divided by this.",
				"Left/right: +0.5/-0.5."),
				(p, game, c) -> {
					game.settings().hungerMultiplier += c.right() ? -0.5 : 0.5;
					commit(game, true);
					refresh();
				});

		set(12, Icons.make(Items.CLOCK, "Head start: " + TextUtil.secondsToClock(s.headStartSeconds),
				ChatFormatting.AQUA,
				"Hunters stay frozen this long after start.",
				"Left/right: +10s/-10s, shift: +60s/-60s."),
				(p, game, c) -> {
					game.settings().headStartSeconds += step(c, 10, 60);
					commit(game, false);
					refresh();
				});

		set(13, Icons.make(Items.REPEATER, "Release: " + s.releaseMode, ChatFormatting.AQUA,
				"ALL_AT_ONCE: every hunter is released together.",
				"STAGGERED: one hunter every N seconds.",
				"Click to switch."),
				(p, game, c) -> {
					ReleaseMode current = game.settings().releaseMode;
					game.settings().releaseMode = current == ReleaseMode.STAGGERED
							? ReleaseMode.ALL_AT_ONCE
							: ReleaseMode.STAGGERED;
					commit(game, false);
					refresh();
				});

		set(14, Icons.make(Items.CLOCK, "Stagger: " + TextUtil.secondsToClock(s.staggerSeconds),
				ChatFormatting.AQUA,
				"Gap between hunter releases when STAGGERED.",
				"Left/right: +10s/-10s."),
				(p, game, c) -> {
					game.settings().staggerSeconds += step(c, 10, 10);
					commit(game, false);
					refresh();
				});

		// Math respawn
		set(19, Icons.toggle(Items.ENCHANTED_BOOK, "Math respawn", s.mathRespawn,
				"Hunters answer math questions to respawn.",
				"Click to flip."),
				(p, game, c) -> {
					game.settings().mathRespawn = !game.settings().mathRespawn;
					commit(game, false);
					refresh();
				});

		set(20, Icons.make(Items.PAPER, "Math questions: " + s.mathQuestions, ChatFormatting.LIGHT_PURPLE,
				"Correct answers needed to respawn.",
				"Left/right: +1/-1."),
				(p, game, c) -> {
					game.settings().mathQuestions += step(c, 1, 1);
					commit(game, false);
					refresh();
				});

		set(21, Icons.make(Items.EXPERIENCE_BOTTLE, "Math difficulty: " + s.mathDifficulty,
				ChatFormatting.LIGHT_PURPLE,
				"1 = easy, 2 = medium, 3 = hard.",
				"Click to cycle (right click cycles back)."),
				(p, game, c) -> {
					int d = game.settings().mathDifficulty;
					game.settings().mathDifficulty = c.right()
							? (d <= 1 ? 3 : d - 1)
							: (d >= 3 ? 1 : d + 1);
					commit(game, false);
					refresh();
				});

		// Sharing
		set(28, Icons.toggle(Items.CHEST, "Share inventory", s.shareInventory,
				"Runner team members share one inventory.",
				"Click to flip."),
				(p, game, c) -> {
					game.settings().shareInventory = !game.settings().shareInventory;
					commit(game, false);
					refresh();
				});

		set(29, Icons.toggle(Items.ENDER_CHEST, "Share ender chest", s.shareEnderChest,
				"Runner team members share one ender chest.",
				"Click to flip."),
				(p, game, c) -> {
					game.settings().shareEnderChest = !game.settings().shareEnderChest;
					commit(game, false);
					refresh();
				});

		set(30, Icons.toggle(Items.BREAD, "Share hunger", s.shareHunger,
				"Runner team members share food and saturation.",
				"Click to flip."),
				(p, game, c) -> {
					game.settings().shareHunger = !game.settings().shareHunger;
					commit(game, false);
					refresh();
				});

		// Team and start options
		set(31, Icons.toggle(Items.NAME_TAG, "Team self-select", s.teamSelfSelect,
				"Non-owners may run /manhunt join <team> while idle.",
				"Click to flip."),
				(p, game, c) -> {
					game.settings().teamSelfSelect = !game.settings().teamSelfSelect;
					commit(game, false);
					refresh();
				});

		set(32, Icons.toggle(Items.BUCKET, "Clear inventory on start", s.clearInventoryOnStart,
				"Empties runner inventories when the game starts.",
				"Click to flip."),
				(p, game, c) -> {
					game.settings().clearInventoryOnStart = !game.settings().clearInventoryOnStart;
					commit(game, false);
					refresh();
				});

		set(33, Icons.toggle(Items.COMPASS, "Give compass", s.giveCompass,
				"Hunters get a tracking compass at start.",
				"Click to flip."),
				(p, game, c) -> {
					game.settings().giveCompass = !game.settings().giveCompass;
					commit(game, false);
					refresh();
				});

		set(49, Icons.make(Items.ARROW, "Back", ChatFormatting.YELLOW, "Return to the main panel."),
				p -> Menus.openMain(p));
	}
}
