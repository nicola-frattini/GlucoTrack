package it.glucotrack.model;

public class RiskFactor {

    private int id;
    private int patient_id;
    private String type; // e.g., "Smoking", "Obesity", "Family History"
    private Gravity gravity;

    // ===== Default constructor =====
    public RiskFactor() {
        this.id = -1;
        this.patient_id = -1;
        this.type = "";
        this.gravity = Gravity.LOW;
    }

    // ===== Constructor without ID (for new records) =====
    public RiskFactor(String type, Gravity gravity, int patient_id) {
        this.id = -1; // Will be set by database
        this.patient_id = patient_id;
        this.type = type;
        this.gravity = gravity;
    }

    // ===== Full constructor =====
    public RiskFactor(int id, String type, Gravity gravity, int patient_id) {
        this.id = id;
        this.patient_id = patient_id;
        this.type = type;
        this.gravity = gravity;
    }

    // ===== Getters and Setters =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Gravity getGravity() { return gravity; }
    public void setGravity(Gravity gravity) { this.gravity = gravity; }

    @Override
    public String toString() {
        return "RiskFactor{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", gravity=" + gravity +
                '}';
    }


}
