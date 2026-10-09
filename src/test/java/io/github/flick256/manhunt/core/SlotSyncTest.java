package io.github.flick256.manhunt.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlotSyncTest {

    private static List<String> l(String... s) {
        return new ArrayList<>(Arrays.asList(s));
    }

    @Test
    void noDifferenceKeepsCanonical() {
        List<String> canon = l("a", "b");
        List<String> out = SlotSync.merge(canon, List.of(l("a", "b"), l("a", "b")), 0, Objects::equals);
        assertEquals(l("a", "b"), out);
    }

    @Test
    void changedSlotIsTakenFromMember() {
        List<String> canon = l("a", "b", "c");
        List<String> out = SlotSync.merge(canon, List.of(l("a", "x", "c")), 0, Objects::equals);
        assertEquals(l("a", "x", "c"), out);
    }

    @Test
    void firstMemberInRotatedOrderWins() {
        List<String> canon = l("a");
        List<List<String>> views = List.of(l("x"), l("y"));
        assertEquals(l("x"), SlotSync.merge(canon, views, 0, Objects::equals));
        assertEquals(l("y"), SlotSync.merge(canon, views, 1, Objects::equals));
    }

    @Test
    void startIndexWrapsAroundAndNegativeIsHandled() {
        List<String> canon = l("a");
        List<List<String>> views = List.of(l("x"), l("y"), l("z"));
        assertEquals(l("z"), SlotSync.merge(canon, views, 2, Objects::equals));
        assertEquals(l("y"), SlotSync.merge(canon, views, 4, Objects::equals));
        assertEquals(l("z"), SlotSync.merge(canon, views, -1, Objects::equals));
    }

    @Test
    void unchangedMembersDoNotOverrideChangedOne() {
        // member 0 equals canonical, member 1 changed the slot: member 1 wins regardless of order
        List<String> canon = l("a");
        List<List<String>> views = List.of(l("a"), l("q"));
        assertEquals(l("q"), SlotSync.merge(canon, views, 0, Objects::equals));
        assertEquals(l("q"), SlotSync.merge(canon, views, 1, Objects::equals));
    }

    @Test
    void differentSlotsCanComeFromDifferentMembers() {
        List<String> canon = l("a", "b", "c", "d");
        List<List<String>> views = List.of(
                l("A", "b", "c", "d"),
                l("a", "B", "c", "D"));
        assertEquals(l("A", "B", "c", "D"), SlotSync.merge(canon, views, 0, Objects::equals));
    }

    @Test
    void inputsAreNotMutatedAndResultIsNewList() {
        List<String> canon = l("a", "b");
        List<String> member = l("x", "b");
        List<String> out = SlotSync.merge(canon, List.of(member), 0, Objects::equals);
        assertEquals(l("a", "b"), canon);
        assertEquals(l("x", "b"), member);
        assertNotSame(canon, out);
    }

    @Test
    void customPredicateIsUsed() {
        // treat strings as equal when they match ignoring case
        List<String> canon = l("Apple");
        List<List<String>> views = List.of(l("APPLE"));
        assertEquals(l("Apple"), SlotSync.merge(canon, views, 0, (m, c) -> m.equalsIgnoreCase(c)));
        assertEquals(l("APPLE"), SlotSync.merge(canon, views, 0, Objects::equals));
    }

    @Test
    void noMembersReturnsCanonicalCopy() {
        List<String> canon = l("a", "b");
        List<String> out = SlotSync.merge(canon, List.of(), 0, Objects::equals);
        assertEquals(l("a", "b"), out);
        assertNotSame(canon, out);
    }

    @Test
    void nullValuesAreHandledByPredicate() {
        List<String> canon = l(null, "b");
        List<List<String>> views = List.of(l("x", "b"));
        assertEquals(l("x", "b"), SlotSync.merge(canon, views, 0, Objects::equals));
        assertEquals(l(null, "b"), SlotSync.merge(canon, List.of(l(null, "b")), 0, Objects::equals));
    }

    @Test
    void sizeMismatchThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> SlotSync.merge(l("a", "b"), List.of(l("a")), 0, Objects::equals));
    }
}
