package io.github.flick256.manhunt.core;

/**
 * Shared health of one runner team.
 *
 * <p>{@link #merge(float[])} takes the health each member has NOW and compares it with the value we last wrote:
 * damage taken by several members in the same tick is summed, healing (the largest single increase) counts once.
 */
public final class HealthPool {
    private static final float EPS = 1e-4f;
    private static final float DEAD = 1e-4f;

    private float current;
    private float max;

    public HealthPool(float current, float max) {
        this.max = nonNegative(max);
        this.current = clamp(current);
    }

    public float current() {
        return current;
    }

    public float max() {
        return max;
    }

    /** Sets a new maximum; current health is clamped to it. */
    public void setMax(float max) {
        this.max = nonNegative(max);
        this.current = Math.min(this.current, this.max);
    }

    /** Sets current health, clamped to 0..max. */
    public void set(float value) {
        this.current = clamp(value);
    }

    /** Heals of at most this many hp can be natural regeneration; see {@link #merge(float[], boolean[])}. */
    static final float SMALL_HEAL = 1.0001f;

    /** Same as {@link #merge(float[], boolean[])} with every member allowed to regenerate. */
    public float merge(float[] memberHealth) {
        return merge(memberHealth, null);
    }

    /**
     * Merges the members' observed health against the last written value.
     * The largest drop of a single member counts once per tick (one explosion hitting two runners costs the pool
     * once, not twice); the largest heal counts once. A small heal (natural regeneration is at most 1 hp per
     * event) is only counted when {@code regenOk[i]} is true for that member, otherwise every extra teammate
     * would multiply the team's regeneration rate. Bigger heals (potions, golden apples) always count.
     * The result is stored and returned.
     */
    public float merge(float[] memberHealth, boolean[] regenOk) {
        if (memberHealth == null || memberHealth.length == 0) {
            return current;
        }
        float last = current;
        float worstDrop = 0f;
        float gain = 0f;
        for (int i = 0; i < memberHealth.length; i++) {
            float m = memberHealth[i];
            if (m < last - EPS) {
                worstDrop = Math.max(worstDrop, last - m);
            } else if (m > last + EPS) {
                float g = m - last;
                boolean allowed = regenOk == null || i >= regenOk.length || regenOk[i] || g > SMALL_HEAL;
                if (allowed) {
                    gain = Math.max(gain, g);
                }
            }
        }
        current = clamp(last - worstDrop + gain);
        return current;
    }

    /** True when current health is at or below zero (with a small tolerance). */
    public boolean isDead() {
        return current <= DEAD;
    }

    /**
     * Applies damage. Non-lethal damage never takes health below 1.0 (health already below 1.0 is left unchanged).
     * Lethal damage can reach 0.
     */
    public void damage(float amt, boolean lethal) {
        float a = Float.isNaN(amt) ? 0f : Math.max(0f, amt);
        if (a == 0f) {
            return;
        }
        if (lethal) {
            current = Math.max(0f, current - a);
        } else {
            current = Math.max(Math.min(current, 1f), current - a);
        }
    }

    /** Heals, capped at max. */
    public void heal(float amt) {
        float a = Float.isNaN(amt) ? 0f : Math.max(0f, amt);
        current = Math.min(max, current + a);
    }

    private float clamp(float value) {
        if (Float.isNaN(value)) {
            return 0f;
        }
        return Math.max(0f, Math.min(max, value));
    }

    private static float nonNegative(float value) {
        return Float.isNaN(value) ? 0f : Math.max(0f, value);
    }
}
