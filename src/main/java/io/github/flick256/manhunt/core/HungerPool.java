package io.github.flick256.manhunt.core;

/**
 * Shared food and saturation of one runner team.
 *
 * <p>Internally the pool keeps exact doubles. Food and saturation written to the players are derived from them:
 * written food = ceil(exactFood - 1e-9), written saturation = min(exactSaturation, writtenFood).
 *
 * <p>{@link #merge} compares observed member values with the last written ones. Food gain and saturation gain are
 * the largest single increase among members and are added unscaled. Food drain and saturation drain are the largest
 * single decrease and are divided by the drain multiplier. With multiplier 2 the first lost food point is invisible
 * (exact 19.5 is shown as 20) and the second one shows, so the bar lasts twice as long.
 */
public final class HungerPool {
    private static final int MAX_FOOD = 20;

    private double exactFood;
    private double exactSat;
    private int lastFood;
    private float lastSat;

    public HungerPool(int food, float sat) {
        set(food, sat);
    }

    /** Food value last written to the players. */
    public int food() {
        return lastFood;
    }

    /** Saturation value last written to the players. */
    public float saturation() {
        return lastSat;
    }

    public void set(int food, float sat) {
        exactFood = clampFood(food);
        exactSat = clampSat(sat, exactFood);
        write();
    }

    /**
     * Merges observed member food and saturation against the last written values.
     * {@code drainMultiplier} scales drain only; values below 1 are treated as 1.
     */
    public void merge(int[] memberFood, float[] memberSat, double drainMultiplier) {
        if (memberFood == null || memberSat == null || memberFood.length == 0 || memberSat.length == 0) {
            return;
        }
        if (memberFood.length != memberSat.length) {
            throw new IllegalArgumentException("memberFood and memberSat differ in length");
        }
        double mult = drainMultiplier > 0 ? drainMultiplier : 1.0;

        int foodDrop = 0;
        int foodGain = 0;
        double satDrop = 0;
        double satGain = 0;
        for (int i = 0; i < memberFood.length; i++) {
            int f = memberFood[i];
            double s = memberSat[i];
            foodDrop = Math.max(foodDrop, lastFood - f);
            foodGain = Math.max(foodGain, f - lastFood);
            satDrop = Math.max(satDrop, lastSat - s);
            satGain = Math.max(satGain, s - lastSat);
        }

        exactFood = clampFood(exactFood + foodGain - foodDrop / mult);
        exactSat = clampSat(exactSat + satGain - satDrop / mult, exactFood);
        write();
    }

    /** Direct drain for the test lab. Not scaled by the multiplier. */
    public void drain(int foodPoints) {
        exactFood = clampFood(exactFood - Math.max(0, foodPoints));
        exactSat = clampSat(exactSat, exactFood);
        write();
    }

    private void write() {
        lastFood = (int) clampFood(Math.ceil(exactFood - 1e-9));
        lastSat = (float) Math.min(exactSat, lastFood);
    }

    private static double clampFood(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return Math.max(0, Math.min(MAX_FOOD, value));
    }

    private static double clampSat(double value, double cap) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return Math.max(0, Math.min(cap, value));
    }
}
