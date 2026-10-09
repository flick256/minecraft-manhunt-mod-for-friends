package io.github.flick256.manhunt.menu;

import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.core.Side;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

/**
 * Solo testing: a virtual teammate pool, fast timers, previews of the freeze, stagger and quiz, and
 * the three victory ceremonies. Every button reports its result in chat.
 */
final class TestLabMenu extends GuiMenu {

	/** Three hearts, in hit points. */
	private static final float THREE_HEARTS = 6f;

	TestLabMenu(int containerId, Inventory inventory) {
		super(containerId, inventory);
		refresh();
	}

	@Override
	protected void build(ManhuntGame g) {
		boolean active = g.test().isActive();
		boolean fast = g.test().fastTimers();

		show(4, Icons.make(Items.REDSTONE_TORCH, "Test Lab", ChatFormatting.LIGHT_PURPLE,
				"Solo test active: " + (active ? "yes" : "no"),
				"Game phase: " + g.phase(),
				"Time scale: x" + g.timeScale(),
				"Virtual teammates act on the shared pool."));

		set(10, Icons.make(Items.GRASS_BLOCK, "Start solo test", ChatFormatting.GREEN,
				"You alone are the runner, with a virtual teammate.",
				"Phase goes straight to RUNNING, no hunters needed."),
				(p, game, c) -> {
					String err = game.test().startSolo(p);
					if (err != null) {
						Msg.send(p, Msg.bad(err));
					} else {
						Msg.send(p, Msg.good("Solo test started."));
					}
					refresh();
				});

		set(12, Icons.make(Items.RED_CONCRETE, "End solo test", ChatFormatting.RED,
				"Stops the game and clears test flags."),
				(p, game, c) -> {
					game.test().endSolo();
					Msg.send(p, Msg.info("Solo test ended."));
					refresh();
				});

		set(14, Icons.toggle(Items.CLOCK, "Fast timers (x0.1)", fast,
				"Shrinks head start, timers and ceremony pacing.",
				"Click to flip."),
				(p, game, c) -> {
					game.test().setFastTimers(!game.test().fastTimers());
					refresh();
				});

		set(19, Icons.make(Items.IRON_SWORD, "Virtual damage: 3 hearts", ChatFormatting.RED,
				"A virtual teammate takes 3 hearts (never lethal)."),
				(p, game, c) -> {
					game.test().virtualDamage(p, THREE_HEARTS);
					Msg.send(p, Msg.info("Virtual teammate took 3 hearts of damage."));
				});

		set(21, Icons.make(Items.GOLDEN_APPLE, "Virtual heal: 3 hearts", ChatFormatting.GOLD,
				"A virtual teammate heals 3 hearts."),
				(p, game, c) -> {
					game.test().virtualHeal(p, THREE_HEARTS); // VERIFY: contract lists virtualHeal without a signature.
					Msg.send(p, Msg.info("Virtual teammate healed 3 hearts."));
				});

		set(23, Icons.make(Items.COOKED_BEEF, "Virtual hunger: drain 6", ChatFormatting.YELLOW,
				"The shared pool loses 6 food points."),
				(p, game, c) -> {
					game.test().virtualHunger(6);
					Msg.send(p, Msg.info("Shared hunger drained by 6."));
				});

		set(25, Icons.make(Items.DIAMOND_SWORD, "Virtual gear drop", ChatFormatting.AQUA,
				"Puts a diamond kit and golden carrots in the shared inventory."),
				(p, game, c) -> {
					game.test().virtualGear(p);
					Msg.send(p, Msg.info("Virtual gear dropped into the shared inventory."));
				});

		set(28, Icons.make(Items.CHEST, "Shared inventory", ChatFormatting.WHITE,
				"Prints the runner team's shared inventory to chat."),
				(p, game, c) -> {
					String summary = game.sync().inventorySummary(Roster.CLASSIC_TEAM);
					Msg.send(p, Msg.info("Shared inventory: " + summary));
				});

		set(30, Icons.make(Items.BLUE_ICE, "Preview freeze: 10s", ChatFormatting.AQUA,
				"Freezes you for 10 seconds with a countdown."),
				(p, game, c) -> {
					game.test().previewFreeze(p, 10);
					Msg.send(p, Msg.info("Freezing you for 10 seconds."));
				});

		set(32, Icons.make(Items.REPEATER, "Preview staggered release", ChatFormatting.AQUA,
				"Chat shows when each of 4 fake hunters would be released."),
				(p, game, c) -> game.test().previewStagger(p));

		set(34, Icons.make(Items.PAPER, "Math quiz now", ChatFormatting.LIGHT_PURPLE,
				"Starts a respawn quiz for you, even if the setting is off."),
				(p, game, c) -> game.test().quizNow(p));

		set(37, Icons.make(Items.TOTEM_OF_UNDYING, "Ceremony: runners win", ChatFormatting.GOLD,
				"Golden fanfare and particles for the runners."),
				(p, game, c) -> game.test().ceremony(p, Side.RUNNERS));

		set(39, Icons.make(Items.WITHER_SKELETON_SKULL, "Ceremony: hunters win", ChatFormatting.DARK_RED,
				"Lightning and wither sounds for the hunters."),
				(p, game, c) -> game.test().ceremony(p, Side.HUNTERS));

		set(41, Icons.make(Items.NETHER_STAR, "Ceremony: team wins", ChatFormatting.LIGHT_PURPLE,
				"Team victory ceremony for you."),
				(p, game, c) -> game.test().ceremony(p, Side.TEAM));

		set(49, Icons.make(Items.ARROW, "Back", ChatFormatting.YELLOW, "Return to the main panel."),
				p -> Menus.openMain(p));
	}
}
