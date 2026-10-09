package io.github.flick256.manhunt.menu;

import io.github.flick256.manhunt.core.GameKind;
import io.github.flick256.manhunt.core.Phase;
import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.core.TeamInfo;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Team management for TEAMS mode: one wool block per team. Left click picks its members,
 * right click deletes it. Create makes "Team N"; auto-assign balances all online players.
 */
final class TeamsMenu extends GuiMenu {

	private static final int MAX_TEAMS = 8;
	private static final int MIN_AUTO_TEAMS = 2;
	/** Chat color names for new teams, in order. */
	private static final String[] PALETTE = {"red", "blue", "green", "yellow", "aqua", "light_purple", "gold", "white"};

	/** Team count used by the auto-assign button (2..8), cycled by the counter. */
	private int autoCount = MIN_AUTO_TEAMS;

	TeamsMenu(int containerId, Inventory inventory) {
		super(containerId, inventory);
		refresh();
	}

	@Override
	protected void build(ManhuntGame g) {
		Roster roster = g.roster();
		boolean classic = g.kind() == GameKind.CLASSIC;

		show(4, Icons.make(Items.WHITE_BANNER, "Teams", ChatFormatting.WHITE,
				"Mode: " + g.kind(),
				classic
						? "CLASSIC: the single runner team is shown here."
						: "Left click a team: pick its members.",
				"Right click a team: delete it."));

		int slot = 9;
		for (TeamInfo team : roster.teams()) {
			if (slot > 17) {
				break;
			}
			final String teamId = team.id;
			List<String> lines = new ArrayList<>();
			lines.add(team.members.size() + " member(s)");
			for (UUID member : team.members) {
				if (lines.size() >= 9) {
					lines.add("...");
					break;
				}
				String name = roster.nameOf(member);
				lines.add(name != null ? name : member.toString().substring(0, 8));
			}
			lines.add("Left click: pick members.");
			lines.add("Right click: delete team.");
			set(slot, Icons.make(Icons.woolFor(team.color), team.displayName, Msg.fmt(team.color),
					lines.toArray(new String[0])),
					(p, game, c) -> teamClick(p, game, teamId, c));
			slot++;
		}
		if (roster.teams().isEmpty()) {
			show(13, Icons.make(Items.BARRIER, "No teams yet", ChatFormatting.GRAY,
					"Use Create team below."));
		}

		set(19, Icons.make(Items.LIME_DYE, "Create team", ChatFormatting.GREEN,
				"Adds the next free Team N with an unused color.",
				"At most " + MAX_TEAMS + " teams."),
				(p, game, c) -> createTeam(p, game));

		set(21, Icons.make(Items.REPEATER, "Team count: " + autoCount, ChatFormatting.AQUA,
				"Used by auto-assign.",
				"Left click: +1, right click: -1 (" + MIN_AUTO_TEAMS + " to " + MAX_TEAMS + ")."),
				(p, game, c) -> {
					autoCount = c.right() ? previousCount() : nextCount();
					refresh();
				});

		set(23, Icons.make(Items.HOPPER, "Auto-assign", ChatFormatting.YELLOW,
				"Balances every online player into " + autoCount + " teams.",
				"Replaces all current teams."),
				(p, game, c) -> autoAssign(p, game));

		set(49, Icons.make(Items.ARROW, "Back", ChatFormatting.YELLOW, "Return to the main panel."),
				p -> Menus.openMain(p));
	}

	private void teamClick(ServerPlayer p, ManhuntGame g, String teamId, Click c) {
		if (c.right()) {
			if (g.phase() != Phase.IDLE) {
				Msg.send(p, Msg.bad("Stop the game before deleting teams."));
				return;
			}
			String err = g.removeTeam(teamId);
			if (err != null) {
				Msg.send(p, Msg.bad(err));
			} else {
				Msg.send(p, Msg.good("Deleted team " + teamId + "."));
			}
			refresh();
		} else {
			Menus.openPicker(p, PickerMode.RUNNER_TO_TEAM, teamId);
		}
	}

	private void createTeam(ServerPlayer p, ManhuntGame g) {
		if (g.phase() != Phase.IDLE) {
			Msg.send(p, Msg.bad("Stop the game before creating teams."));
			return;
		}
		Roster roster = g.roster();
		if (roster.teams().size() >= MAX_TEAMS) {
			Msg.send(p, Msg.bad("At most " + MAX_TEAMS + " teams."));
			return;
		}
		int n = 1;
		while (roster.team("team" + n) != null) {
			n++;
		}
		String err = g.createTeam("team" + n, "Team " + n, nextColor(roster));
		if (err != null) {
			Msg.send(p, Msg.bad(err));
		} else {
			Msg.send(p, Msg.good("Created Team " + n + "."));
		}
		refresh();
	}

	private void autoAssign(ServerPlayer p, ManhuntGame g) {
		if (g.phase() != Phase.IDLE) {
			Msg.send(p, Msg.bad("Stop the game before re-assigning teams."));
			return;
		}
		String err = g.autoAssign(autoCount);
		if (err != null) {
			Msg.send(p, Msg.bad(err));
		} else {
			Msg.send(p, Msg.good("Assigned online players into " + autoCount + " teams."));
		}
		refresh();
	}

	/** First palette color no existing team uses; falls back to cycling. */
	private static String nextColor(Roster roster) {
		for (String candidate : PALETTE) {
			boolean used = false;
			for (TeamInfo team : roster.teams()) {
				used |= candidate.equals(team.color);
			}
			if (!used) {
				return candidate;
			}
		}
		return PALETTE[roster.teams().size() % PALETTE.length];
	}

	private int nextCount() {
		return autoCount >= MAX_TEAMS ? MIN_AUTO_TEAMS : autoCount + 1;
	}

	private int previousCount() {
		return autoCount <= MIN_AUTO_TEAMS ? MAX_TEAMS : autoCount - 1;
	}
}
