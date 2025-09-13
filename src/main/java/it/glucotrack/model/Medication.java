package it.glucotrack.model;

public class Medication {

    private String name;           // Medication name
    private double dose;           // Dose amount per intake
    private int timesPerDay;       // Number of daily intakes
    private String instructions;   // Any instructions (e.g., "after meals", "before meals")

    // ===== Default constructor =====
    public Medication() {
        this.name = "";
        this.dose = 0;
        this.timesPerDay = 1;
        this.instructions = "";
    }

    // ===== Full constructor =====
    public Medication(String name, double dose, int timesPerDay, String instructions) {
        this.name = name;
        this.dose = dose;
        this.timesPerDay = timesPerDay;
        this.instructions = instructions;
    }

    // ===== Getters and setters =====
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDose() {
        return dose;
    }

    public void setDose(double dose) {
        this.dose = dose;
    }

    public int getTimesPerDay() {
        return timesPerDay;
    }

    public void setTimesPerDay(int timesPerDay) {
        this.timesPerDay = timesPerDay;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    @Override
    public String toString() {
        return "Medication{" +
                "name='" + name + '\'' +
                ", dose=" + dose +
                ", timesPerDay=" + timesPerDay +
                ", instructions='" + instructions + '\'' +
                '}';
    }
}
