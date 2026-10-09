package io.github.flick256.manhunt.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Reads and writes {@link ManhuntConfig} as pretty printed JSON.
 * Loading is lenient: every field is read on its own, so missing, mistyped or unknown values fall back to defaults.
 */
public final class ConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigIO() {}

    /**
     * Loads the config. A missing, unreadable or corrupt file gives defaults. Never throws.
     * The returned settings are always clamped.
     */
    public static ManhuntConfig load(Path file) {
        if (file == null) {
            return finish(new ManhuntConfig());
        }
        try {
            if (!Files.isRegularFile(file)) {
                return finish(new ManhuntConfig());
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(text);
            if (root == null || !root.isJsonObject()) {
                return finish(new ManhuntConfig());
            }
            JsonObject obj = root.getAsJsonObject();
            ManhuntConfig cfg = new ManhuntConfig();
            cfg.settings = readSettings(obj.get("settings"));
            cfg.owners = readOwners(obj.get("owners"));
            cfg.kind = enumOr(obj, "kind", GameKind.class, GameKind.CLASSIC);
            return finish(cfg);
        } catch (Exception | StackOverflowError e) {
            return finish(new ManhuntConfig());
        }
    }

    /**
     * Writes the config as pretty printed JSON, creating parent directories. Settings are clamped first.
     *
     * @throws UncheckedIOException if the file cannot be written
     */
    public static void save(Path file, ManhuntConfig cfg) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        ManhuntConfig out = finish(cfg == null ? new ManhuntConfig() : cfg);
        String json = GSON.toJson(out) + System.lineSeparator();
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ManhuntConfig finish(ManhuntConfig cfg) {
        if (cfg.settings == null) {
            cfg.settings = new Settings();
        }
        cfg.settings.clamp();
        if (cfg.owners == null) {
            cfg.owners = new ArrayList<>();
        }
        if (cfg.kind == null) {
            cfg.kind = GameKind.CLASSIC;
        }
        return cfg;
    }

    private static Settings readSettings(JsonElement e) {
        Settings s = new Settings();
        if (e == null || !e.isJsonObject()) {
            return s;
        }
        JsonObject o = e.getAsJsonObject();
        s.poolHearts = intOr(o, "poolHearts", s.poolHearts);
        s.hungerMultiplier = doubleOr(o, "hungerMultiplier", s.hungerMultiplier);
        s.headStartSeconds = intOr(o, "headStartSeconds", s.headStartSeconds);
        s.releaseMode = enumOr(o, "releaseMode", ReleaseMode.class, s.releaseMode);
        s.staggerSeconds = intOr(o, "staggerSeconds", s.staggerSeconds);
        s.mathRespawn = boolOr(o, "mathRespawn", s.mathRespawn);
        s.mathQuestions = intOr(o, "mathQuestions", s.mathQuestions);
        s.mathDifficulty = intOr(o, "mathDifficulty", s.mathDifficulty);
        s.shareInventory = boolOr(o, "shareInventory", s.shareInventory);
        s.shareEnderChest = boolOr(o, "shareEnderChest", s.shareEnderChest);
        s.shareHunger = boolOr(o, "shareHunger", s.shareHunger);
        s.teamSelfSelect = boolOr(o, "teamSelfSelect", s.teamSelfSelect);
        s.clearInventoryOnStart = boolOr(o, "clearInventoryOnStart", s.clearInventoryOnStart);
        s.giveCompass = boolOr(o, "giveCompass", s.giveCompass);
        return s;
    }

    private static List<String> readOwners(JsonElement e) {
        LinkedHashSet<String> owners = new LinkedHashSet<>();
        if (e != null && e.isJsonArray()) {
            JsonArray arr = e.getAsJsonArray();
            for (JsonElement item : arr) {
                if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                    String name = item.getAsString().trim();
                    if (!name.isEmpty()) {
                        owners.add(name);
                    }
                }
            }
        }
        return new ArrayList<>(owners);
    }

    private static JsonPrimitive primitive(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsJsonPrimitive() : null;
    }

    private static Double number(JsonObject o, String key) {
        JsonPrimitive p = primitive(o, key);
        if (p == null) {
            return null;
        }
        if (p.isNumber()) {
            double d = p.getAsDouble();
            return Double.isNaN(d) ? null : d;
        }
        if (p.isString()) {
            try {
                double d = Double.parseDouble(p.getAsString().trim());
                return Double.isNaN(d) ? null : d;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static int intOr(JsonObject o, String key, int def) {
        Double d = number(o, key);
        if (d == null) {
            return def;
        }
        long rounded = Math.round(d);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rounded));
    }

    private static double doubleOr(JsonObject o, String key, double def) {
        Double d = number(o, key);
        return d == null ? def : d;
    }

    private static boolean boolOr(JsonObject o, String key, boolean def) {
        JsonPrimitive p = primitive(o, key);
        if (p == null) {
            return def;
        }
        if (p.isBoolean()) {
            return p.getAsBoolean();
        }
        if (p.isString()) {
            String s = p.getAsString().trim();
            if (s.equalsIgnoreCase("true")) {
                return true;
            }
            if (s.equalsIgnoreCase("false")) {
                return false;
            }
        }
        return def;
    }

    private static <E extends Enum<E>> E enumOr(JsonObject o, String key, Class<E> type, E def) {
        JsonPrimitive p = primitive(o, key);
        if (p == null || !p.isString()) {
            return def;
        }
        try {
            return Enum.valueOf(type, p.getAsString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
}
