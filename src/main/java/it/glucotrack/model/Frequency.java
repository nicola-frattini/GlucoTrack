package it.glucotrack.model;

public enum Frequency {
    ONCE_A_DAY("Once a day", new int[]{8}),
    TWICE_A_DAY("Twice a day", new int[]{8, 20}),
    THREE_TIMES_A_DAY("Three times a day", new int[]{8, 14, 20}),
    FOUR_TIMES_A_DAY("Four times a day", new int[]{8, 12, 16, 20}),
    EVERY_SIX_HOURS("Every six hours", new int[]{0, 6, 12, 18}),
    EVERY_EIGHT_HOURS("Every eight hours", new int[]{6, 14, 22}),
    EVERY_TWELVE_HOURS("Every twelve hours", new int[]{8, 20}),
    AS_NEEDED("As needed", new int[]{});

    private final String displayName;
    private final int[] defaultHours;


    Frequency(String displayName) {
        this.displayName = displayName;
        this.defaultHours = defaultHours;
    }

    public String getDisplayName() { return displayName; }
    public int[] getHours() { return defaultHours; }
    public int timesPerDay() { return defaultHours.length; }


    @Override
    public String toString() { return displayName; }

    public static Frequency fromString(String text) {
        for (Frequency freq : Frequency.values()) {
            if (freq.displayName.equalsIgnoreCase(text)) {
                return freq;
            }
        }
        throw new IllegalArgumentException("No frequency with text '" + text + "' found");
    }
    public static int timesPerDay(Frequency freq) {
        switch (freq) {
            case ONCE_A_DAY: return 1;
            case TWICE_A_DAY: return 2;
            case THREE_TIMES_A_DAY: return 3;
            case FOUR_TIMES_A_DAY: return 4;
            case EVERY_SIX_HOURS: return 4;
            case EVERY_EIGHT_HOURS: return 3;
            case EVERY_TWELVE_HOURS: return 2;
            case AS_NEEDED: return 0; // Variable, depends on patient needs
            default: return 0;
        }
    }

}
