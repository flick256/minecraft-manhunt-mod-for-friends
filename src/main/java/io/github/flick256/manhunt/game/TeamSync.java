package io.github.flick256.manhunt.game;

import io.github.flick256.manhunt.core.HealthPool;
import io.github.flick256.manhunt.core.HungerPool;
import io.github.flick256.manhunt.core.Settings;
import io.github.flick256.manhunt.core.SlotSync;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Shared state for runner teams: ONE health pool, ONE hunger pool, ONE inventory and ONE ender chest per team.
 *
 * <p>Every tick each begun team is diffed against what was last written to its online members:
 * health and hunger are merged by {@link HealthPool#merge} / {@link HungerPool#merge}, inventories by
 * {@link SlotSync#merge}, and the result is written back to every member that differs.
 *
 * <p>Server thread only; nothing here is thread-safe.
 */
public final class TeamSync {

	private static final float VANILLA_MAX_HEALTH = 20f;
	private static final int FULL_FOOD = 20;
	private static final float FULL_SATURATION = 5f;

	/** Slot equality used everywhere: same item, same components, same count. */
	private static final BiPredicate<ItemStack, ItemStack> SAME = (a, b) -> ItemStack.matches(a, b);

	private final ManhuntGame game;
	private final Map<String, Team> teams = new LinkedHashMap<>();
	/**
	 * Every player we ever modified, mapped to the team they were last part of.
	 * The value is null once a team has ended while the player was offline; such players are restored on join.
	 */
	private final Map<UUID, String> everTracked = new HashMap<>();

	public TeamSync(ManhuntGame game) {
		this.game = game;
	}

	// ------------------------------------------------------------------ lifecycle

	/** Starts (or restarts) a team: full pools, full food, canonical inventory from the first member or cleared. */
	public void beginTeam(String teamId, List<ServerPlayer> members) {
		if (teamId == null || members == null) {
			return;
		}
		Settings s = game.settings();
		float max = s.poolHealth();
		Team team = new Team(teamId, max);
		teams.put(teamId, team);

		List<ServerPlayer> online = new ArrayList<>(members.size());
		for (ServerPlayer p : members) {
			if (p == null) {
				continue;
			}
			online.add(p);
			team.members.add(p.getUUID());
			everTracked.put(p.getUUID(), teamId);
			applyMaxHealth(p, max);
			p.setHealth(max);
			writeFood(p, FULL_FOOD, FULL_SATURATION);
		}
		if (!online.isEmpty()) {
			initCanonical(team, online);
			if (s.shareInventory) {
				writeDiff(online, team.canonicalInv, false);
			}
			if (s.shareEnderChest) {
				writeDiff(online, team.canonicalEnder, true);
			}
		}
	}

	/** Late join or reconnect: copy the team state onto the player and set the attributes. */
	public void addMember(String teamId, ServerPlayer p) {
		Team team = teams.get(teamId);
		if (team == null || p == null) {
			return;
		}
		Settings s = game.settings();
		team.members.add(p.getUUID());
		everTracked.put(p.getUUID(), teamId);
		applyMaxHealth(p, team.health.max());
		if (!team.inventoryReady) {
			initCanonical(team, List.of(p));
		}
		if (s.shareInventory) {
			writeDiff(List.of(p), team.canonicalInv, false);
		}
		if (s.shareEnderChest) {
			writeDiff(List.of(p), team.canonicalEnder, true);
		}
		if (!p.isDeadOrDying()) {
			writeHealth(p, team.health.current());
			if (s.shareHunger) {
				writeFood(p, team.hunger.food(), team.hunger.saturation());
			}
		}
	}

	/** Disconnect: stop syncing p, but keep the team. Pools and canonical state are untouched. */
	public void removeMember(ServerPlayer p) {
		if (p == null) {
			return;
		}
		for (Team team : teams.values()) {
			team.members.remove(p.getUUID());
		}
	}

	/** Runs the sync for every begun team. Called by ManhuntGame.tick() during HEAD_START and RUNNING. */
	public void tick() {
		// Snapshot: onRunnerTeamDown may end the game and clear teams while we iterate.
		for (Team team : new ArrayList<>(teams.values())) {
			if (team.downReported || teams.get(team.id) != team) {
				continue;
			}
			tickTeam(team);
		}
	}

	/** Owner changed poolHearts mid game: resize pools and attributes. Hunger is unaffected. */
	public void applySettingsLive() {
		float max = game.settings().poolHealth();
		for (Team team : new ArrayList<>(teams.values())) {
			if (team.downReported) {
				continue;
			}
			List<ServerPlayer> online = online(team);
			if (anyDown(online)) {
				continue; // tick() reports it
			}
			if (!online.isEmpty()) {
				// Absorb what the players have now while the old max still applies.
				team.health.merge(healthsOf(online));
			}
			if (team.health.isDead()) {
				continue; // tick() reports it
			}
			team.health.setMax(max);
			for (ServerPlayer p : online) {
				applyMaxHealth(p, max);
				writeHealth(p, team.health.current());
			}
		}
	}

	/** Game over: restore every online tracked player, drop all pools. Offline players are restored on join. */
	public void endAll() {
		Iterator<Map.Entry<UUID, String>> it = everTracked.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, String> e = it.next();
			ServerPlayer p = game.player(e.getKey());
			if (p != null) {
				restoreVanilla(p);
				it.remove();
			} else {
				e.setValue(null);
			}
		}
		teams.clear();
	}

	/**
	 * Call from onPlayerJoin. If the player was in a team that is still running, re-adds them to it;
	 * otherwise undoes the health/food changes from an earlier game. Does nothing for untouched players.
	 */
	public void restoreIfTracked(ServerPlayer p) {
		if (p == null) {
			return;
		}
		UUID id = p.getUUID();
		if (!everTracked.containsKey(id)) {
			return;
		}
		String teamId = everTracked.get(id);
		Team tracked = teamId == null ? null : teams.get(teamId);
		if (tracked != null && !tracked.downReported) {
			addMember(teamId, p);
			return;
		}
		restoreVanilla(p);
		everTracked.remove(id);
	}

	// ------------------------------------------------------------------ queries

	public boolean hasTeam(String teamId) {
		return teams.containsKey(teamId);
	}

	public float poolHealth(String teamId) {
		Team team = teams.get(teamId);
		return team == null ? 0f : team.health.current();
	}

	public float poolMax(String teamId) {
		Team team = teams.get(teamId);
		return team == null ? 0f : team.health.max();
	}

	public int poolFood(String teamId) {
		Team team = teams.get(teamId);
		return team == null ? 0 : team.hunger.food();
	}

	// ------------------------------------------------------------------ test lab hooks

	/** Non lethal damage to the team pool (never below 1). Players are updated before returning. */
	public void injectDamage(String teamId, float hp) {
		Team team = absorbedTeam(teamId);
		if (team == null) {
			return;
		}
		team.health.damage(hp, false);
		pushToMembers(team);
	}

	public void injectHeal(String teamId, float hp) {
		Team team = absorbedTeam(teamId);
		if (team == null) {
			return;
		}
		team.health.heal(hp);
		pushToMembers(team);
	}

	/** Direct, unscaled hunger drain of the team pool. */
	public void injectHunger(String teamId, int foodPoints) {
		Team team = absorbedTeam(teamId);
		if (team == null) {
			return;
		}
		team.hunger.drain(foodPoints);
		pushToMembers(team);
	}

	/** Puts the stack into the shared main inventory: tops up matching stacks, then the first empty main slot. */
	public void injectItem(String teamId, ItemStack stack) {
		Team team = absorbedTeam(teamId);
		if (team == null || stack == null || stack.isEmpty() || !team.inventoryReady) {
			return;
		}
		insertIntoCanonical(team.canonicalInv, stack.copy());
		pushToMembers(team);
	}

	/** "3x diamond, 1x iron_pickaxe, ..." (top 10 by count), or "empty". */
	public String inventorySummary(String teamId) {
		Team team = teams.get(teamId);
		if (team == null) {
			return "empty";
		}
		Map<String, Integer> counts = new HashMap<>();
		for (ItemStack stack : team.canonicalInv) {
			if (stack == null || stack.isEmpty()) {
				continue;
			}
			var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
			String path = key == null ? "unknown" : key.getPath();
			counts.merge(path, stack.getCount(), Integer::sum);
		}
		if (counts.isEmpty()) {
			return "empty";
		}
		Comparator<Map.Entry<String, Integer>> order = Map.Entry.<String, Integer>comparingByValue()
				.reversed()
				.thenComparing(Map.Entry.<String, Integer>comparingByKey());
		return counts.entrySet().stream()
				.sorted(order)
				.limit(10)
				.map(e -> e.getValue() + "x " + e.getKey())
				.collect(Collectors.joining(", "));
	}

	// ------------------------------------------------------------------ per tick work

	private void tickTeam(Team team) {
		List<ServerPlayer> online = online(team);
		if (online.isEmpty()) {
			return;
		}
		Settings s = game.settings();

		// (1) health: any dead member or an empty pool ends the team's life.
		if (team.health.isDead() || anyDown(online)) {
			reportDown(team);
			return;
		}
		team.health.merge(healthsOf(online), regenOkOf(online));
		if (team.health.isDead()) {
			reportDown(team);
			return;
		}

		// (2) max health attribute, then (3) health written back to every member that differs.
		// Max goes first so that raising the pool max does not get clamped by the old attribute.
		for (ServerPlayer p : online) {
			applyMaxHealth(p, team.health.max());
		}
		for (ServerPlayer p : online) {
			writeHealth(p, team.health.current());
		}

		// (4) hunger, only when shared. Otherwise vanilla hunger is left alone.
		if (s.shareHunger) {
			syncHunger(team, online, s.hungerMultiplier);
		}

		// (5) inventory and ender chest.
		if (team.inventoryReady) {
			if (s.shareInventory) {
				syncContainer(team, online, false);
			}
			if (s.shareEnderChest) {
				syncContainer(team, online, true);
			}
		}
	}

	private void syncHunger(Team team, List<ServerPlayer> online, double multiplier) {
		int n = online.size();
		int[] food = new int[n];
		float[] sat = new float[n];
		for (int i = 0; i < n; i++) {
			var fd = online.get(i).getFoodData();
			food[i] = fd.getFoodLevel();
			sat[i] = fd.getSaturationLevel(); // VERIFY: FoodData#getSaturationLevel() (not in the Fabric reference)
		}
		team.hunger.merge(food, sat, multiplier);
		for (ServerPlayer p : online) {
			writeFood(p, team.hunger.food(), team.hunger.saturation());
		}
	}

	/** Merges one container (inventory or ender chest) across the online members and writes the result back. */
	private void syncContainer(Team team, List<ServerPlayer> online, boolean ender) {
		List<List<ItemStack>> views = new ArrayList<>(online.size());
		for (ServerPlayer p : online) {
			views.add(viewOf(container(p, ender)));
		}
		int size = views.get(0).size();
		for (int m = 0; m < views.size(); m++) {
			views.set(m, resized(views.get(m), size));
		}
		List<ItemStack> canonical = resized(ender ? team.canonicalEnder : team.canonicalInv, size);
		int start = Math.floorMod(team.rotation++, views.size());
		List<ItemStack> merged = copyList(SlotSync.merge(canonical, views, start, SAME));
		// Two members changing the same slot in one tick: the loser's stack would be overwritten and vanish.
		// Re-insert it elsewhere when it is a different item than what won the slot.
		for (List<ItemStack> view : views) {
			for (int i = 0; i < size; i++) {
				ItemStack mine = view.get(i);
				if (mine.isEmpty() || SAME.test(mine, canonical.get(i))) {
					continue; // untouched by this member
				}
				ItemStack winner = merged.get(i);
				if (!winner.isEmpty() && ItemStack.isSameItemSameComponents(winner, mine)) {
					continue; // same item, only the count differs: keep the winner's count
				}
				if (winner.isEmpty() || !SAME.test(mine, winner)) {
					insertIntoCanonical(merged, mine.copy());
				}
			}
		}
		if (ender) {
			team.canonicalEnder = merged;
		} else {
			team.canonicalInv = merged;
		}
		writeDiff(online, merged, ender);
	}

	/** Writes the whole current team state to the online members right now (used by the test lab hooks). */
	private void pushToMembers(Team team) {
		if (team.downReported) {
			return;
		}
		Settings s = game.settings();
		List<ServerPlayer> online = online(team);
		for (ServerPlayer p : online) {
			if (p.isDeadOrDying()) {
				continue;
			}
			applyMaxHealth(p, team.health.max());
			writeHealth(p, team.health.current());
			if (s.shareHunger) {
				writeFood(p, team.hunger.food(), team.hunger.saturation());
			}
		}
		if (team.inventoryReady) {
			if (s.shareInventory) {
				writeDiff(online, team.canonicalInv, false);
			}
			if (s.shareEnderChest) {
				writeDiff(online, team.canonicalEnder, true);
			}
		}
	}

	/** Brings the team up to date with the players first, so injections start from the real state. */
	private Team absorbedTeam(String teamId) {
		Team team = teams.get(teamId);
		if (team == null || team.downReported) {
			return null;
		}
		tickTeam(team);
		if (teams.get(teamId) != team || team.downReported) {
			return null;
		}
		return team;
	}

	private void reportDown(Team team) {
		if (team.downReported) {
			return;
		}
		team.downReported = true;
		game.onRunnerTeamDown(team.id);
	}

	// ------------------------------------------------------------------ canonical state

	/**
	 * Sets the canonical inventory and ender chest of a team. With clearInventoryOnStart every player in
	 * {@code players} is emptied and the canonical state is empty; otherwise the first player's state is adopted.
	 */
	private void initCanonical(Team team, List<ServerPlayer> players) {
		Settings s = game.settings();
		ServerPlayer first = players.get(0);
		if (s.clearInventoryOnStart) {
			for (ServerPlayer p : players) {
				p.getInventory().clearContent(); // VERIFY: Container#clearContent() on Inventory (reference only shows the interface default)
				p.getEnderChestInventory().clearContent(); // VERIFY: Player#getEnderChestInventory()
			}
			team.canonicalInv = blank(first.getInventory().getContainerSize());
			team.canonicalEnder = blank(first.getEnderChestInventory().getContainerSize());
		} else {
			team.canonicalInv = copyList(viewOf(first.getInventory()));
			team.canonicalEnder = copyList(viewOf(first.getEnderChestInventory()));
		}
		team.inventoryReady = true;
	}

	/** Puts {@code rest} into canonical main slots (0..INVENTORY_SIZE-1): top up matching stacks first, then empty slots. */
	private static void insertIntoCanonical(List<ItemStack> canonical, ItemStack rest) {
		int limit = Math.min(canonical.size(), Inventory.INVENTORY_SIZE);
		for (int i = 0; i < limit && !rest.isEmpty(); i++) {
			ItemStack cur = canonical.get(i);
			if (cur.isEmpty() || !ItemStack.isSameItemSameComponents(cur, rest)) {
				continue;
			}
			int room = cur.getMaxStackSize() - cur.getCount();
			if (room <= 0) {
				continue;
			}
			int move = Math.min(room, rest.getCount());
			ItemStack grown = cur.copy();
			grown.setCount(cur.getCount() + move);
			canonical.set(i, grown);
			rest.shrink(move);
		}
		for (int i = 0; i < limit && !rest.isEmpty(); i++) {
			if (!canonical.get(i).isEmpty()) {
				continue;
			}
			int move = Math.min(rest.getMaxStackSize(), rest.getCount());
			ItemStack placed = rest.copy();
			placed.setCount(move);
			canonical.set(i, placed);
			rest.shrink(move);
		}
		// Anything left over has no room in the main inventory and is discarded (test lab only).
	}

	// ------------------------------------------------------------------ player writes

	/** Writes the canonical list into the live container of each target where a slot differs. */
	private static void writeDiff(List<ServerPlayer> targets, List<ItemStack> canonical, boolean ender) {
		for (ServerPlayer p : targets) {
			Container c = container(p, ender);
			int size = c.getContainerSize();
			if (canonical.size() != size) {
				continue; // never write a mismatched list: that would wipe slots
			}
			for (int i = 0; i < size; i++) {
				ItemStack want = canonical.get(i);
				if (!SAME.test(c.getItem(i), want)) {
					c.setItem(i, copyOf(want));
				}
			}
		}
	}

	private static void writeHealth(ServerPlayer p, float poolValue) {
		if (p.isDeadOrDying()) {
			return;
		}
		float target = Math.min(poolValue, (float) p.getAttributeValue(Attributes.MAX_HEALTH));
		if (Math.abs(p.getHealth() - target) > 1e-3f) {
			p.setHealth(target);
		}
	}

	private static void writeFood(ServerPlayer p, int food, float sat) {
		var fd = p.getFoodData();
		if (fd.getFoodLevel() != food) {
			fd.setFoodLevel(food); // VERIFY: FoodData#setFoodLevel(int)
		}
		if (Math.abs(fd.getSaturationLevel() - sat) > 1e-4f) {
			fd.setSaturation(sat); // VERIFY: FoodData#setSaturation(float)
		}
	}

	/** Sets the MAX_HEALTH base value only when it differs (an attribute write sends a packet). */
	private static void applyMaxHealth(ServerPlayer p, float max) {
		if (Math.abs(p.getAttributeBaseValue(Attributes.MAX_HEALTH) - max) < 1e-6) {
			return;
		}
		AttributeInstance attr = p.getAttribute(Attributes.MAX_HEALTH); // VERIFY: LivingEntity#getAttribute(Holder<Attribute>), nullable
		if (attr != null) {
			attr.setBaseValue(max); // VERIFY: AttributeInstance#setBaseValue(double)
		}
	}

	/** Undoes what this class changed on a player: vanilla max health, health clamped to 20, food full. */
	private static void restoreVanilla(ServerPlayer p) {
		applyMaxHealth(p, VANILLA_MAX_HEALTH);
		if (!p.isDeadOrDying()) {
			float hp = Math.min(p.getHealth(), VANILLA_MAX_HEALTH);
			if (Math.abs(p.getHealth() - hp) > 1e-3f) {
				p.setHealth(hp);
			}
		}
		writeFood(p, FULL_FOOD, FULL_SATURATION);
	}

	// ------------------------------------------------------------------ helpers

	/** Online members of the team, in insertion order. */
	private List<ServerPlayer> online(Team team) {
		List<ServerPlayer> out = new ArrayList<>(team.members.size());
		for (UUID id : team.members) {
			ServerPlayer p = game.player(id);
			if (p != null) {
				out.add(p);
			}
		}
		return out;
	}

	private static boolean anyDown(List<ServerPlayer> online) {
		for (ServerPlayer p : online) {
			if (p.isDeadOrDying() || !p.isAlive()) { // VERIFY: Entity#isAlive()
				return true;
			}
		}
		return false;
	}

	/**
	 * Only the first online member's small heals count as natural regeneration (every member regenerates on its
	 * own timer, so counting all of them would multiply regen by the team size). A member with the Regeneration
	 * effect (golden apple, potion) always counts.
	 */
	private static boolean[] regenOkOf(List<ServerPlayer> online) {
		boolean[] ok = new boolean[online.size()];
		for (int i = 0; i < ok.length; i++) {
			ok[i] = i == 0 || online.get(i).hasEffect(MobEffects.REGENERATION);
		}
		return ok;
	}

	private static float[] healthsOf(List<ServerPlayer> online) {
		float[] hp = new float[online.size()];
		for (int i = 0; i < hp.length; i++) {
			hp[i] = online.get(i).getHealth();
		}
		return hp;
	}

	private static Container container(ServerPlayer p, boolean ender) {
		if (ender) {
			return p.getEnderChestInventory(); // VERIFY: Player#getEnderChestInventory()
		}
		return p.getInventory();
	}

	private static List<ItemStack> viewOf(Container c) {
		int size = c.getContainerSize();
		List<ItemStack> out = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			out.add(copyOf(c.getItem(i)));
		}
		return out;
	}

	private static List<ItemStack> resized(List<ItemStack> src, int size) {
		List<ItemStack> out = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			out.add(i < src.size() ? src.get(i) : ItemStack.EMPTY);
		}
		return out;
	}

	private static List<ItemStack> copyList(List<ItemStack> src) {
		List<ItemStack> out = new ArrayList<>(src.size());
		for (ItemStack s : src) {
			out.add(copyOf(s));
		}
		return out;
	}

	private static List<ItemStack> blank(int size) {
		List<ItemStack> out = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			out.add(ItemStack.EMPTY);
		}
		return out;
	}

	private static ItemStack copyOf(ItemStack s) {
		return s == null || s.isEmpty() ? ItemStack.EMPTY : s.copy();
	}

	// ------------------------------------------------------------------ state

	private static final class Team {
		final String id;
		final HealthPool health;
		final HungerPool hunger;
		final LinkedHashSet<UUID> members = new LinkedHashSet<>();
		List<ItemStack> canonicalInv = new ArrayList<>();
		List<ItemStack> canonicalEnder = new ArrayList<>();
		/** False until the canonical inventory has been initialised from a real player. */
		boolean inventoryReady;
		boolean downReported;
		int rotation;

		Team(String id, float max) {
			this.id = id;
			this.health = new HealthPool(max, max);
			this.hunger = new HungerPool(FULL_FOOD, FULL_SATURATION);
		}
	}
}
