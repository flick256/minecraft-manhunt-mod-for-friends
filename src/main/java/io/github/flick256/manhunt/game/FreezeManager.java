package io.github.flick256.manhunt.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps players in place (head start / countdown). Frozen players are invulnerable and get a
 * slowness effect; any movement beyond {@link #MAX_DRIFT} is undone by teleporting them back.
 * Offline players keep their entry so that they are frozen again when they rejoin.
 */
public final class FreezeManager {

    private static final double MAX_DRIFT = 0.05;
    private static final int EFFECT_DURATION = 40;
    private static final int EFFECT_AMPLIFIER = 6;
    private static final long EFFECT_INTERVAL = 20;

    private record FrozenState(double x, double y, double z, float yRot, float xRot, boolean wasInvulnerable) {}

    private final Map<UUID, FrozenState> frozen = new HashMap<>();
    private long tickCount;

    public FreezeManager() {
    }

    public void freeze(ServerPlayer p) {
        if (p == null) return;
        // Already frozen: keep the original spot (a rejoining player is pulled back to it).
        if (!frozen.containsKey(p.getUUID())) {
            frozen.put(p.getUUID(), new FrozenState(
                    p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(),
                    p.isInvulnerable())); // VERIFY: Entity#isInvulnerable() (not in local reference)
        }
        p.setInvulnerable(true);
        applySlowness(p);
    }

    public void unfreeze(ServerPlayer p) {
        if (p == null) return;
        FrozenState state = frozen.remove(p.getUUID());
        if (state != null) restore(p, state);
    }

    public boolean isFrozen(UUID id) {
        return id != null && frozen.containsKey(id);
    }

    public void tick(MinecraftServer server) {
        if (frozen.isEmpty()) return;
        tickCount++;
        boolean refreshEffect = tickCount % EFFECT_INTERVAL == 0;
        for (Map.Entry<UUID, FrozenState> entry : frozen.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            if (p == null || !p.isAlive()) continue;
            FrozenState s = entry.getValue();
            boolean drifted = Math.abs(p.getX() - s.x()) > MAX_DRIFT
                    || Math.abs(p.getY() - s.y()) > MAX_DRIFT
                    || Math.abs(p.getZ() - s.z()) > MAX_DRIFT;
            if (drifted) snapBack(p, s);
            p.setDeltaMovement(Vec3.ZERO);
            if (refreshEffect) applySlowness(p);
        }
    }

    public void clear(MinecraftServer server) {
        for (Map.Entry<UUID, FrozenState> entry : frozen.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            if (p != null) restore(p, entry.getValue());
        }
        frozen.clear();
    }

    private static void snapBack(ServerPlayer p, FrozenState s) {
        ServerLevel level = (ServerLevel) p.level();
        // teleport(TeleportTransition) and TeleportTransition(level, pos, speed, yRot, xRot, post) are
        // used this way in the Fabric 26.2 reference (AttachmentCopyTests).
        p.teleport(new TeleportTransition(level, new Vec3(s.x(), s.y(), s.z()), Vec3.ZERO,
                s.yRot(), s.xRot(), TeleportTransition.DO_NOTHING));
    }

    private static void applySlowness(ServerPlayer p) {
        // VERIFY: MobEffects.SLOWNESS constant name and the 6-arg MobEffectInstance constructor.
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EFFECT_DURATION, EFFECT_AMPLIFIER,
                false, false, false));
    }

    private static void restore(ServerPlayer p, FrozenState state) {
        p.setInvulnerable(state.wasInvulnerable());
        p.removeEffect(MobEffects.SLOWNESS); // VERIFY: SLOWNESS constant (removeEffect(Holder) is used with SATURATION in the reference)
    }
}
