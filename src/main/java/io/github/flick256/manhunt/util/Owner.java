package io.github.flick256.manhunt.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;

import io.github.flick256.manhunt.game.ManhuntGame;

/**
 * Who may run owner-only manhunt commands and open the menus.
 *
 * Rules, in order:
 *  1. Command source with no entity (console / RCON) => owner.
 *  2. The integrated-server host (first player to join a singleplayer / LAN-opened world) => owner.
 *  3. Player name (case-insensitive) or UUID listed in config owners => owner.
 *  4. Fabric permission node manhunt:admin: TRUE => owner, FALSE => not owner, DEFAULT => continue.
 *  5. Only if the owners list is EMPTY and the node is DEFAULT: vanilla PermissionLevel.OWNERS.
 */
public final class Owner {
	static final Identifier ADMIN_PERMISSION = Identifier.fromNamespaceAndPath("manhunt", "admin");

	private static ManhuntGame game;
	private static UUID hostId;

	private Owner() {
	}

	/** Call on server start (and with null on server stop). Resets the remembered integrated-server host. */
	public static void bind(ManhuntGame g) {
		game = g;
		hostId = null;
	}

	/** Call for every player join. Remembers the singleplayer / integrated-server host (first joiner only). */
	public static void noteJoin(ServerPlayer p) {
		if (hostId != null || p == null) {
			return;
		}
		MinecraftServer server = p.level().getServer();
		if (server == null) {
			return;
		}
		// Verified: MinecraftServer.isSingleplayerOwner(NameAndId) is used by Fabric's registry sync for the singleplayer host.
		if (server.isSingleplayerOwner(new NameAndId(p.getUUID(), p.getGameProfile().name()))) {
			hostId = p.getUUID();
		}
	}

	/** UUID of the remembered integrated-server host, or null. */
	public static UUID hostId() {
		return hostId;
	}

	/** Copy of the configured owner entries (names or UUID strings); empty when none. */
	public static List<String> owners() {
		ManhuntGame g = game;
		if (g == null || g.config() == null || g.config().owners == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(g.config().owners);
	}

	public static boolean isOwner(CommandSourceStack src) {
		if (src == null) {
			return false;
		}
		// Rule 1. Console / RCON have no entity. (Command blocks also have none: see report gap.)
		if (src.getEntity() == null) {
			return true;
		}
		if (src.getEntity() instanceof ServerPlayer player) {
			return isOwner(player);
		}
		// Non-player entity: only the permission node / fallback can grant it.
		return grantedByPermission(src, owners().isEmpty());
	}

	public static boolean isOwner(ServerPlayer p) {
		if (p == null) {
			return false;
		}
		// Rule 2.
		if (p.getUUID().equals(hostId)) {
			return true;
		}
		// Rule 3.
		List<String> owners = owners();
		if (listed(owners, p.getUUID(), p.getGameProfile().name())) {
			return true;
		}
		// Rules 4 and 5.
		return grantedByPermission(p, owners.isEmpty());
	}

	private static boolean grantedByPermission(PermissionContextOwner who, boolean noListedOwners) {
		TriState node = who.checkPermission(ADMIN_PERMISSION);
		if (node == TriState.TRUE) {
			return true;
		}
		if (node == TriState.FALSE) {
			return false;
		}
		return noListedOwners && who.checkPermission(ADMIN_PERMISSION, PermissionLevel.OWNERS);
	}

	private static boolean listed(List<String> owners, UUID id, String name) {
		for (String entry : owners) {
			if (entry == null) {
				continue;
			}
			String s = entry.trim();
			if (s.isEmpty()) {
				continue;
			}
			if (s.equalsIgnoreCase(name)) {
				return true;
			}
			UUID parsed = parseUuid(s);
			if (parsed != null && parsed.equals(id)) {
				return true;
			}
		}
		return false;
	}

	/** Accepts UUIDs with or without dashes. Returns null if not a UUID. */
	public static UUID parseUuid(String text) {
		if (text == null) {
			return null;
		}
		String s = text.trim();
		try {
			if (s.length() == 32) {
				s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20);
			}
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
