package io.github.flick256.manhunt.menu;

import io.github.flick256.manhunt.core.GameKind;
import io.github.flick256.manhunt.core.Role;
import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pages of online players as heads. Clicking toggles the player: hunter in HUNTER mode, or a
 * member of the picker's team in RUNNER_TO_TEAM mode (the classic runner team in CLASSIC).
 */
final class PlayerPickerMenu extends GuiMenu {

	private static final int PAGE_SIZE = 45;

	private final PickerMode mode;
	private final String teamId;
	private int page;

	PlayerPickerMenu(int containerId, Inventory inventory, PickerMode mode, String teamId) {
		super(containerId, inventory);
		this.mode = mode;
		this.teamId = teamId;
		refresh();
	}

	/** The team runners are toggled into; null when a runner pick has no team. */
	private String effectiveTeam(ManhuntGame g) {
		return g.kind() == GameKind.CLASSIC ? Roster.CLASSIC_TEAM : teamId;
	}

	@Override
	protected void build(ManhuntGame g) {
		Roster roster = g.roster();
		String team = effectiveTeam(g);

		List<ServerPlayer> online = new ArrayList<>();
		for (ServerPlayer player : g.server().getPlayerList().getPlayers()) {
			online.add(player);
		}

		int pages = Math.max(1, (online.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		page = Math.max(0, Math.min(page, pages - 1));
		String title = mode == PickerMode.HUNTER
				? "Pick hunters"
				: "Pick runners for " + (team == null ? "no team" : g.teamDisplay(team));

		if (online.isEmpty()) {
			show(22, Icons.make(Items.BARRIER, "No players online", ChatFormatting.RED,
					"Players must be online to be assigned."));
		}

		int from = page * PAGE_SIZE;
		for (int i = 0; i < PAGE_SIZE && from + i < online.size(); i++) {
			ServerPlayer target = online.get(from + i);
			UUID id = target.getUUID();
			String name = target.getName().getString();
			roster.rememberName(id, name);

			Role role = roster.roleOf(id);
			String currentTeam = roster.teamOf(id);
			boolean selected;
			List<Component> lore = new ArrayList<>();
			if (role == Role.HUNTER) {
				lore.add(Icons.text("Role: Hunter", ChatFormatting.RED));
			} else if (role == Role.RUNNER) {
				lore.add(Icons.text("Role: Runner in " + g.teamDisplay(currentTeam), ChatFormatting.GREEN));
			} else {
				lore.add(Icons.text("Role: none", ChatFormatting.GRAY));
			}
			if (mode == PickerMode.HUNTER) {
				selected = role == Role.HUNTER;
				lore.add(Icons.text(selected ? "Click: remove hunter." : "Click: make hunter.", ChatFormatting.YELLOW));
			} else {
				selected = team != null && team.equals(currentTeam);
				lore.add(Icons.text(selected ? "Click: remove from this team." : "Click: add to this team.",
						ChatFormatting.YELLOW));
			}

			ChatFormatting color = role == Role.HUNTER ? ChatFormatting.RED
					: role == Role.RUNNER ? ChatFormatting.GREEN : ChatFormatting.WHITE;
			ItemStack icon = Icons.head(name, color, lore);
			if (selected) {
				icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			}
			set(i, icon, (p, game, c) -> toggle(p, game, id, name));
		}

		if (page > 0) {
			set(45, Icons.make(Items.ARROW, "Previous page", ChatFormatting.YELLOW,
					"Page " + page + " of " + pages + "."),
					(p, game, c) -> {
						page--;
						refresh();
					});
		} else {
			show(45, Icons.filler());
		}

		show(47, Icons.make(Items.PAPER, title, ChatFormatting.WHITE,
				"Page " + (page + 1) + " of " + pages,
				online.size() + " player(s) online"));

		set(49, Icons.make(Items.BOOK, "Back", ChatFormatting.YELLOW, "Return to the previous panel."),
				(p, game, c) -> back(p, game));

		if (page < pages - 1) {
			set(53, Icons.make(Items.ARROW, "Next page", ChatFormatting.YELLOW,
					"Page " + (page + 2) + " of " + pages + "."),
					(p, game, c) -> {
						page++;
						refresh();
					});
		} else {
			show(53, Icons.filler());
		}
	}

	private void toggle(ServerPlayer clicker, ManhuntGame g, UUID id, String name) {
		Roster roster = g.roster();
		String err;
		if (mode == PickerMode.HUNTER) {
			err = roster.isHunter(id) ? g.unassign(id) : g.assignHunter(id);
		} else {
			String team = effectiveTeam(g);
			if (team == null) {
				Msg.send(clicker, Msg.bad("Pick a team first."));
				return;
			}
			err = team.equals(roster.teamOf(id)) ? g.unassign(id) : g.assignRunner(id, team);
		}
		if (err != null) {
			Msg.send(clicker, Msg.bad(err));
		} else {
			Msg.send(clicker, Msg.good(name + " updated."));
		}
		refresh();
	}

	private void back(ServerPlayer p, ManhuntGame g) {
		if (mode == PickerMode.RUNNER_TO_TEAM && g.kind() == GameKind.TEAMS) {
			Menus.openTeams(p);
		} else {
			Menus.openMain(p);
		}
	}
}
