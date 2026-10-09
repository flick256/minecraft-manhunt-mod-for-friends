package io.github.flick256.manhunt.core;

import java.util.ArrayList;
import java.util.List;

/** Persisted configuration: settings, owner names/UUIDs and the game kind. */
public final class ManhuntConfig {
    public Settings settings = new Settings();
    public List<String> owners = new ArrayList<>();
    public GameKind kind = GameKind.CLASSIC;
}
