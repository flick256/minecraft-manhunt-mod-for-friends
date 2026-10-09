package io.github.flick256.manhunt.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/** Generic canonical merge for shared inventories. Pure, never mutates its inputs. */
public final class SlotSync {
    private SlotSync() {}

    /**
     * Returns a new list equal to {@code canonical}, except that for each slot the first member (scanning member
     * views from {@code startIndex}, wrapping around) whose slot is not {@code same} as the canonical slot provides
     * the new value.
     *
     * @throws IllegalArgumentException if a member view has a different size from the canonical list
     */
    public static <T> List<T> merge(List<T> canonical, List<List<T>> memberViews, int startIndex, BiPredicate<T, T> same) {
        if (canonical == null || memberViews == null) {
            throw new IllegalArgumentException("canonical and memberViews must not be null");
        }
        int size = canonical.size();
        for (List<T> view : memberViews) {
            if (view == null || view.size() != size) {
                throw new IllegalArgumentException("member view size mismatch, expected " + size);
            }
        }
        List<T> result = new ArrayList<>(canonical);
        int n = memberViews.size();
        if (n == 0) {
            return result;
        }
        for (int slot = 0; slot < size; slot++) {
            T base = canonical.get(slot);
            for (int k = 0; k < n; k++) {
                T value = memberViews.get(Math.floorMod(startIndex + k, n)).get(slot);
                if (!same.test(value, base)) {
                    result.set(slot, value);
                    break;
                }
            }
        }
        return result;
    }
}
