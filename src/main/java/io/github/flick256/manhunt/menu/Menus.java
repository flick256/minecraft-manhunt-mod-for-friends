package io.github.flick256.manhunt.menu;

import io.github.flick256.manhunt.ManhuntMod;
import io.github.flick256.manhunt.util.Msg;
import io.github.flick256.manhunt.util.Owner;
import net.minecraft.server.level.ServerPlayer;

/** Entry points for the owner control panel. Each opener checks owner and game first. */
public final class Menus {

	private Menus() {
	}

	public static void openMain(ServerPlayer p) {
		show(p, "Manhunt", MainMenu::new);
	}

	public static void openSettings(ServerPlayer p) {
		show(p, "Manhunt: Settings", SettingsMenu::new);
	}

	public static void openTeams(ServerPlayer p) {
		show(p, "Manhunt: Teams", TeamsMenu::new);
	}

	/** Player picker. {@code teamId} is used by RUNNER_TO_TEAM outside CLASSIC; may be null for HUNTER. */
	public static void openPicker(ServerPlayer p, PickerMode mode, String teamId) {
		show(p, mode == PickerMode.HUNTER ? "Manhunt: Hunters" : "Manhunt: Runners",
				(containerId, inventory) -> new PlayerPickerMenu(containerId, inventory, mode, teamId));
	}

	public static void openTestLab(ServerPlayer p) {
		show(p, "Manhunt: Test Lab", TestLabMenu::new);
	}

	private static void show(ServerPlayer p, String title, GuiMenu.Factory factory) {
		if (!Owner.isOwner(p)) {
			Msg.send(p, Msg.bad("Only the server owner can open the Manhunt panel."));
			return;
		}
		if (ManhuntMod.game() == null) {
			Msg.send(p, Msg.bad("Manhunt is not ready yet."));
			return;
		}
		GuiMenu.open(p, title, factory);
	}
}
