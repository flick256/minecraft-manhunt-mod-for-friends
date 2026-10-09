package io.github.flick256.manhunt;

import io.github.flick256.manhunt.cmd.ManhuntCommands;
import io.github.flick256.manhunt.core.ConfigIO;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.util.Owner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Entry point. Wires Fabric events to the single {@link ManhuntGame} instance (one per server).
 * Every handler is a no-op while {@link #game} is null (server not started or already stopping).
 */
public class ManhuntMod implements ModInitializer {
	public static final String MOD_ID = "manhunt";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final String CONFIG_FILE = "manhunt.json";

	private static ManhuntGame game;

	/** The running game, or null before SERVER_STARTED / after SERVER_STOPPING. Menus must null-check. */
	public static ManhuntGame game() {
		return game;
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				ManhuntCommands.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
			game = new ManhuntGame(server, ConfigIO.load(path), path);
			Owner.bind(game);
			LOGGER.info("Manhunt ready (config: {})", path);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (game != null) {
				game.stop(false);
				game.save();
				game = null;
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (game != null) {
				game.tick();
			}
		});

		ServerPlayerEvents.JOIN.register(player -> {
			// Integrated-server host detection: the first joiner becomes the owner (see Owner).
			Owner.noteJoin(player);
			if (game != null) {
				game.onPlayerJoin(player);
			}
		});

		ServerPlayerEvents.LEAVE.register(player -> {
			if (game != null) {
				game.onPlayerLeave(player);
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (game != null) {
				game.onAfterRespawn(oldPlayer, newPlayer);
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (game != null) {
				game.onAfterDeath(entity, source);
			}
		});

		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, boundChatType) ->
				game == null || game.allowChat(message, sender));

		// Frozen hunters (head start / freeze) cannot interact with the world.
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				isFrozen(player) ? InteractionResult.FAIL : InteractionResult.PASS);

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
				isFrozen(player) ? InteractionResult.FAIL : InteractionResult.PASS);

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
				isFrozen(player) ? InteractionResult.FAIL : InteractionResult.PASS);

		UseItemCallback.EVENT.register((player, level, hand) ->
				isFrozen(player) ? InteractionResult.FAIL : InteractionResult.PASS);

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !isFrozen(player));

		LOGGER.info("Manhunt loaded");
	}

	private static boolean isFrozen(Player player) {
		ManhuntGame g = game;
		return g != null && g.freeze().isFrozen(player.getUUID());
	}
}
