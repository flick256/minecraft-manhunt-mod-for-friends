package io.github.flick256.manhunt.core.quiz;

import java.util.Locale;

/** Subjects a hunter can pick for the respawn quiz. {@code id} is what players type in chat or commands. */
public enum Topic {
    MATH("math", "Math", "General arithmetic"),
    METHODS("methods", "Maths Methods", "VCE Mathematical Methods Units 3&4 (easy)"),
    BIOLOGY("biology", "Biology", "VCE Biology Units 3&4 (easy)"),
    CHEMISTRY("chemistry", "Chemistry", "VCE Chemistry Units 3&4 (easy)"),
    PHYSICS("physics", "Physics", "VCE Physics Units 3&4 (easy)"),
    HISTORY("history", "History", "VCE History: Revolutions and Australian history (easy)"),
    FRENCH("french", "French", "VCE French (easy)");

    private final String id;
    private final String display;
    private final String blurb;

    Topic(String id, String display, String blurb) {
        this.id = id;
        this.display = display;
        this.blurb = blurb;
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public String blurb() {
        return blurb;
    }

    /** Parses an id, display name or common short name ("bio", "chem", "phys", "maths", "mm", ...). Null if unknown. */
    public static Topic parse(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return null;
        }
        for (Topic topic : values()) {
            if (topic.id.equals(t) || topic.display.toLowerCase(Locale.ROOT).equals(t)) {
                return topic;
            }
        }
        switch (t) {
            case "maths":
            case "mathematics":
            case "arithmetic":
                return MATH;
            case "mm":
            case "method":
            case "maths methods":
            case "mathematical methods":
            case "math methods":
                return METHODS;
            case "bio":
                return BIOLOGY;
            case "chem":
                return CHEMISTRY;
            case "phys":
                return PHYSICS;
            case "hist":
            case "revolutions":
                return HISTORY;
            case "francais":
            case "français":
                return FRENCH;
            default:
                return null;
        }
    }
}
