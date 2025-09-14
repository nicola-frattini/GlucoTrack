package it.glucotrack.model;

public enum Status {
    LOW, // <70 mg/dL
    NORMAL, // 70-130 mg/dL
    ELEVATED, // 130-180 mg/dL
    HIGH; // >180 mg/dL

    public static Status fromGlucoseValue(float value) {
        if (value < 70) return LOW;
        else if (value <= 140) return NORMAL;
        else if (value <= 180) return ELEVATED;
        else return HIGH;
    }

    @Override
    public String toString() {
        switch (this) {
            case LOW: return "Low";
            case NORMAL: return "Normal";
            case ELEVATED: return "Elevated";
            case HIGH: return "High";
            default: return super.toString();
        }
    }
}