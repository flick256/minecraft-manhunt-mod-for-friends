package io.github.flick256.manhunt.menu;

import io.github.flick256.manhunt.core.GameKind;
import io.github.flick256.manhunt.core.Phase;
import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.core.Settings;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.util.Msg;
import io.github.flick256.manhunt.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Main panel: mode, roles, start/stop, and links to the sub menus. */
final class MainMenu extends GuiMenu {

	MainMenu(int containerId, Inventory inventory) {
		super(containerId, inventory);
		refresh();
	}

	@Override
	protected void build(ManhuntGame g) {
		Settings s = g.settings();
		Phase phase = g.phase();
		GameKind kind = g.kind();
		Roster roster = g.roster();
		boolean idle = phase == Phase.IDLE;
		boolean classic = kind == GameKind.CLASSIC;

		show(4, Icons.make(Items.COMPASS, "Manhunt", ChatFormatting.GOLD,
				"Phase: " + phase,
				"Mode: " + kind,
				"Head start: " + TextUtil.secondsToClock(s.headStartSeconds),
				"Pool: " + s.poolHearts + " hearts",
				"Release: " + s.releaseMode));

		// Mode switch: only while idle. Otherwise a gray no-op that explains why.
		if (idle) {
			GameKind next = classic ? GameKind.TEAMS : GameKind.CLASSIC;
			set(10, Icons.make(Items.ENDER_EYE, "Mode: " + kind, ChatFormatting.AQUA,
					"Click to switch to " + next + ".",
					"CLASSIC: one runner team, hunters vs runners.",
					"TEAMS: several runner teams, last team standing wins."),
					(p, game, c) -> {
						String err = game.setKind(next);
						if (err != null) {
							Msg.send(p, Msg.bad(err));
						} else {
							Msg.send(p, Msg.good("Mode set to " + next + "."));
						}
						refresh();
					});
		} else {
			set(10, Icons.make(Blocks.WOOL.gray().asItem(), "Mode: " + kind + " (locked)", ChatFormatting.GRAY,
					"Stop the game to change the mode."),
					(p, game, c) -> Msg.send(p, Msg.bad("Stop the game before changing the mode.")));
		}

		int hunters = roster.hunters().size();
		set(12, Icons.make(Items.IRON_SWORD, "Hunters", ChatFormatting.RED,
				hunters + " assigned", "Click to pick hunters."),
				p -> Menus.openPicker(p, PickerMode.HUNTER, null));

		int runners = roster.allRunners().size();
		if (classic) {
			set(14, Icons.make(Items.DIAMOND_PICKAXE, "Runners", ChatFormatting.GREEN,
					runners + " assigned", "Click to pick runners."),
					p -> Menus.openPicker(p, PickerMode.RUNNER_TO_TEAM, Roster.CLASSIC_TEAM));
		} else {
			set(14, Icons.make(Items.DIAMOND_PICKAXE, "Runners", ChatFormatting.GREEN,
					runners + " assigned", "In TEAMS mode runners belong to teams.", "Click to open Teams."),
					p -> Menus.openTeams(p));
		}

		set(16, Icons.make(Blocks.WOOL.white().asItem(), "Teams", ChatFormatting.WHITE,
				roster.teams().size() + " teams", "Click to manage teams."),
				p -> Menus.openTeams(p));

		set(20, Icons.make(Items.COMPARATOR, "Settings", ChatFormatting.YELLOW,
				"Pool, hunger, head start, math quiz, sharing.", "Click to open."),
				p -> Menus.openSettings(p));

		set(22, Icons.make(Blocks.WOOL.lime().asItem(), "START", ChatFormatting.GREEN,
				"Head start: " + TextUtil.secondsToClock(s.headStartSeconds),
				hunters + " hunter(s), " + runners + " runner(s)",
				"Click to start the hunt."),
				(p, game, c) -> {
					String err = game.start(p);
					if (err == null) {
						Msg.send(p, Msg.good("Manhunt started."));
						p.closeContainer();
					} else {
						Msg.send(p, Msg.bad(err));
						refresh();
					}
				});

		set(24, Icons.make(Blocks.WOOL.red().asItem(), "STOP", ChatFormatting.RED,
				"Ends the game from any phase.", "Restores everybody and resets."),
				(p, game, c) -> {
					game.stop(true);
					Msg.send(p, Msg.info("Manhunt stopped."));
					refresh();
				});

		set(31, Icons.make(Items.REDSTONE, "Test Lab", ChatFormatting.LIGHT_PURPLE,
				"Test every feature alone.", "Click to open."),
				p -> Menus.openTestLab(p));

		List<String> status = roster.describe();
		String[] lines = status.isEmpty()
				? new String[] {"Nobody assigned yet."}
				: status.stream().limit(12).toArray(String[]::new);
		show(40, Icons.make(Items.BOOK, "Status", ChatFormatting.GOLD, lines));

		set(49, Icons.make(Items.BARRIER, "Close", ChatFormatting.RED, "Close this panel."),
				p -> p.closeContainer());
	}
}
