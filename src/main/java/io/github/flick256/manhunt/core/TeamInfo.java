package io.github.flick256.manhunt.core;

import java.util.LinkedHashSet;
import java.util.UUID;

/** A runner team: stable id, display name, chat colour name and member UUIDs in insertion order. */
public final class TeamInfo {
    public String id;
    public String displayName;
    /** Chat colour name, e.g. "red", "light_purple". */
    public String color;
    public LinkedHashSet<UUID> members = new LinkedHashSet<>();

    public TeamInfo(String id, String displayName, String color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }
}
