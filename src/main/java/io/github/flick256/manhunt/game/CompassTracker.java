package io.github.flick256.manhunt.game;

import io.github.flick256.manhunt.core.GameKind;
import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker; // VERIFY: record LodestoneTracker(Optional<GlobalPos>, boolean) in net.minecraft.world.item.component
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Hunters (CLASSIC) or every team member (TEAMS) receive a "Runner Tracker" compass that points at
 * the nearest enemy. In CLASSIC the enemies are the runners; in TEAMS they are online players of
 * other teams.
 *
 * <p>Targeting: the nearest live enemy in the tracker's dimension wins. If none is in that
 * dimension, the last position recorded for an enemy in the tracker's dimension is used (so a
 * runner who went to the Nether still "leaves a trail" in the Overworld). The compass always
 * points inside the tracker's own dimension, which is the only case vanilla lodestone compasses
 * honour.
 *
 * <p>{@link #tick()} is called every server tick by {@link ManhuntGame}. It records positions every
 * tick and performs the compass / actionbar update every {@value #INTERVAL_TICKS} ticks, using its
 * own counter.
 */
public final class CompassTracker {
	public static final String TRACKER_NAME = "Runner Tracker";
	static final String TRACKER_LORE = "Points to the nearest runner";
	private static final int INTERVAL_TICKS = 20;

	private final ManhuntGame game;
	/** enemy UUID -> (dimension -> last block position seen there). Never pruned during a game. */
	private final Map<UUID, Map<ResourceKey<Level>, BlockPos>> lastKnown = new HashMap<>();
	private int counter;

	public CompassTracker(ManhuntGame game) {
		this.game = game;
	}

	/** Called every server tick. Does nothing (and forgets positions) while no game is active. */
	public void tick() {
		if (!game.isGameActive()) {
			if (!lastKnown.isEmpty()) {
				lastKnown.clear();
			}
			counter = 0;
			return;
		}

		List<ServerPlayer> runners = onlineRunners();
		for (ServerPlayer runner : runners) {
			if (!runner.isSpectator()) {
				remember(runner);
			}
		}

		if (++counter < INTERVAL_TICKS) {
			return;
		}
		counter = 0;

		List<ServerPlayer> trackers = game.kind() == GameKind.CLASSIC ? game.hunters() : runners;
		for (ServerPlayer tracker : trackers) {
			if (!tracker.isSpectator()) {
				update(tracker, runners);
			}
		}
	}

	/** Gives the tracker compass unless the player already carries one (in any inventory slot, offhand included). */
	public void giveTo(ServerPlayer player) {
		Inventory inv = player.getInventory();
		if (hasTracker(inv)) {
			return;
		}
		ItemStack stack = new ItemStack(Items.COMPASS);
		stack.set(DataComponents.CUSTOM_NAME,
				Component.literal(TRACKER_NAME).withStyle(s -> s.withColor(0xFFAA00).withItalic(false)));
		stack.set(DataComponents.LORE,
				new ItemLore(List.of(Component.literal(TRACKER_LORE).withStyle(s -> s.withColor(0xAAAAAA).withItalic(false)))));
		inv.placeItemBackInInventory(stack); // VERIFY: adds to a free slot; behaviour when the inventory is full (drop vs. no-op)
	}

	/** Forgets all remembered positions (call on game start/stop). */
	public void clearLast() {
		lastKnown.clear();
		counter = 0;
	}

	// ----- internals -----

	/** Online runners: every online member of every runner team (CLASSIC: the "runners" team). */
	private List<ServerPlayer> onlineRunners() {
		List<ServerPlayer> out = new ArrayList<>();
		for (UUID id : game.roster().allRunners()) {
			ServerPlayer p = game.player(id);
			if (p != null) {
				out.add(p);
			}
		}
		return out;
	}

	private void remember(ServerPlayer runner) {
		lastKnown.computeIfAbsent(runner.getUUID(), id -> new HashMap<>())
				.put(runner.level().dimension(), runner.blockPosition());
	}

	/** Is {@code target} an enemy of {@code tracker}? CLASSIC: any runner. TEAMS: any runner of another team. */
	private boolean isEnemy(UUID tracker, UUID target) {
		Roster roster = game.roster();
		if (game.kind() == GameKind.CLASSIC) {
			return roster.isRunner(target);
		}
		String theirs = roster.teamOf(target);
		return theirs != null && !theirs.equals(roster.teamOf(tracker));
	}

	private void update(ServerPlayer tracker, List<ServerPlayer> runners) {
		ResourceKey<Level> dim = tracker.level().dimension();
		Target target = findTarget(tracker, dim, runners);

		if (target != null) {
			LodestoneTracker value = new LodestoneTracker(Optional.of(GlobalPos.of(dim, target.pos())), false); // VERIFY: GlobalPos.of(ResourceKey<Level>, BlockPos) (used in reference); LodestoneTracker ctor
			Inventory inv = tracker.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) { // main inventory, armor and offhand
				ItemStack stack = inv.getItem(i);
				if (isTracker(stack)) {
					stack.set(DataComponents.LODESTONE_TRACKER, value); // VERIFY: DataComponents.LODESTONE_TRACKER
				}
			}
		}

		boolean holding = isTracker(tracker.getMainHandItem()) || isTracker(tracker.getOffhandItem());
		if (holding) {
			Msg.actionbar(tracker, actionbarLine(tracker, target));
		}
	}

	private Component actionbarLine(ServerPlayer tracker, Target target) {
		if (target == null) {
			return Component.literal("Tracking: no runner located").withStyle(ChatFormatting.GRAY);
		}
		int blocks = (int) Math.round(Math.sqrt(distSq(tracker.blockPosition(), target.pos())));
		Component line = Component.literal("Tracking ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(target.name()).withStyle(ChatFormatting.GOLD))
				.append(Component.literal(" - " + blocks + " blocks").withStyle(ChatFormatting.GRAY));
		if (!target.live()) {
			line = line.copy().append(Component.literal(" (last seen)").withStyle(ChatFormatting.DARK_GRAY));
		}
		return line;
	}

	/** Nearest live enemy in {@code dim}; otherwise the nearest last-known enemy position in {@code dim}; else null. */
	private Target findTarget(ServerPlayer tracker, ResourceKey<Level> dim, List<ServerPlayer> runners) {
		UUID trackerId = tracker.getUUID();
		BlockPos from = tracker.blockPosition();

		ServerPlayer nearest = null;
		double best = Double.MAX_VALUE;
		for (ServerPlayer runner : runners) {
			if (runner == tracker || runner.isSpectator() || !isEnemy(trackerId, runner.getUUID())) {
				continue;
			}
			if (!runner.level().dimension().equals(dim)) {
				continue;
			}
			double d = distSq(from, runner.blockPosition());
			if (d < best) {
				best = d;
				nearest = runner;
			}
		}
		if (nearest != null) {
			return new Target(nearest.getUUID(), nearest.getGameProfile().name(), nearest.blockPosition(), true);
		}

		UUID bestId = null;
		BlockPos bestPos = null;
		double bestD = Double.MAX_VALUE;
		for (Map.Entry<UUID, Map<ResourceKey<Level>, BlockPos>> entry : lastKnown.entrySet()) {
			if (!isEnemy(trackerId, entry.getKey())) {
				continue;
			}
			BlockPos pos = entry.getValue().get(dim);
			if (pos == null) {
				continue;
			}
			double d = distSq(from, pos);
			if (d < bestD) {
				bestD = d;
				bestId = entry.getKey();
				bestPos = pos;
			}
		}
		if (bestId == null) {
			return null;
		}
		String name = game.roster().nameOf(bestId);
		return new Target(bestId, name != null ? name : "runner", bestPos, false);
	}

	private static boolean hasTracker(Inventory inv) {
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (isTracker(inv.getItem(i))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isTracker(ItemStack stack) {
		return !stack.isEmpty() && stack.is(Items.COMPASS)
				&& isTrackerName(stack.get(DataComponents.CUSTOM_NAME));
	}

	private static boolean isTrackerName(Component name) {
		return name != null && TRACKER_NAME.equals(name.getString());
	}

	private static double distSq(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dy = a.getY() - b.getY();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	private record Target(UUID id, String name, BlockPos pos, boolean live) {
	}
}
