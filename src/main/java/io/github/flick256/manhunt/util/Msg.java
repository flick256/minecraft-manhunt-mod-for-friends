package io.github.flick256.manhunt.util;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket; // VERIFY: class + Component constructor (not in local reference)
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket; // VERIFY: class + Component constructor (not in local reference)
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket; // VERIFY: class + Component constructor (not in local reference)
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket; // VERIFY: class + (int fadeIn, int stay, int fadeOut) constructor (not in local reference)
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Chat, title, actionbar and sound helpers. All calls are server-side and per player unless noted. */
public final class Msg {
	private Msg() {
	}

	/** Gold "[Manhunt] " prefix. */
	public static MutableComponent prefix() {
		return Component.literal("[Manhunt] ").withStyle(ChatFormatting.GOLD);
	}

	/** Prefixed, gray body. */
	public static MutableComponent info(String text) {
		return prefix().append(colored(text, ChatFormatting.GRAY));
	}

	/** Prefixed, green body. */
	public static MutableComponent good(String text) {
		return prefix().append(colored(text, ChatFormatting.GREEN));
	}

	/** Prefixed, red body. */
	public static MutableComponent bad(String text) {
		return prefix().append(colored(text, ChatFormatting.RED));
	}

	/** Chat message to one player. */
	public static void send(ServerPlayer player, Component message) {
		player.sendSystemMessage(message);
	}

	/** Chat message to everybody on the server. */
	public static void broadcast(MinecraftServer server, Component message) {
		server.getPlayerList().broadcastSystemMessage(message, false);
	}

	/** Title + subtitle with timings in ticks, sent to one player only. */
	public static void title(ServerPlayer player, Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
	}

	/** Actionbar line to one player only. */
	public static void actionbar(ServerPlayer player, Component message) {
		player.connection.send(new ClientboundSetActionBarTextPacket(message));
	}

	/** Plays a sound at the player's position, audible only to that player. */
	public static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		player.playNotifySound(sound, SoundSource.MASTER, volume, pitch); // VERIFY: playNotifySound(SoundEvent, SoundSource, float, float) on ServerPlayer (not in local reference)
	}

	public static MutableComponent colored(String text, ChatFormatting fmt) {
		return Component.literal(text).withStyle(fmt);
	}

	/** Maps a team color name ("red", "dark_aqua", "pink", ...) to a chat format; unknown => WHITE. */
	public static ChatFormatting fmt(String colorName) {
		if (colorName == null) {
			return ChatFormatting.WHITE;
		}
		return switch (colorName.trim().toLowerCase(Locale.ROOT)) {
			case "black" -> ChatFormatting.BLACK;
			case "dark_blue" -> ChatFormatting.DARK_BLUE;
			case "dark_green" -> ChatFormatting.DARK_GREEN;
			case "dark_aqua" -> ChatFormatting.DARK_AQUA;
			case "dark_red" -> ChatFormatting.DARK_RED;
			case "dark_purple" -> ChatFormatting.DARK_PURPLE;
			case "gold" -> ChatFormatting.GOLD;
			case "gray", "grey" -> ChatFormatting.GRAY;
			case "dark_gray", "dark_grey" -> ChatFormatting.DARK_GRAY;
			case "blue" -> ChatFormatting.BLUE;
			case "green" -> ChatFormatting.GREEN;
			case "aqua" -> ChatFormatting.AQUA;
			case "red" -> ChatFormatting.RED;
			case "light_purple", "pink", "purple" -> ChatFormatting.LIGHT_PURPLE;
			case "yellow" -> ChatFormatting.YELLOW;
			default -> ChatFormatting.WHITE;
		};
	}
}
