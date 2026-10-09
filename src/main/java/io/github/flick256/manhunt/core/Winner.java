package io.github.flick256.manhunt.core;

/** Result of a finished game. {@code teamId} is null unless the side is TEAM (or RUNNERS in classic mode). */
public record Winner(Side side, String teamId) {}
