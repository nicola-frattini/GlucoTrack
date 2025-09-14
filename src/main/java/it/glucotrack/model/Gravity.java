package it.glucotrack.model;

public enum Gravity {

    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String displayName;

    Gravity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Gravity fromString(String text) {
        for (Gravity gravity : Gravity.values()) {
            if (gravity.displayName.equalsIgnoreCase(text)) {
                return gravity;
            }
        }
        throw new IllegalArgumentException("No gravity with text '" + text + "' found");
    }


}
