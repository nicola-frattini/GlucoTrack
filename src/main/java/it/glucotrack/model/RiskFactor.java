package it.glucotrack.model;

public class RiskFactor {

    private int id;
    private String type; // e.g., "Smoking", "Obesity", "Family History"
    private Gravity gravity; // e.g., "Low", "Medium", "High"

    // ===== Default constructor =====
    public RiskFactor() {
        this.id = -1;
        this.type = "";
        this.gravity = Gravity.LOW;
    }

    // ===== Full constructor =====
    public RiskFactor(int id, String type, Gravity gravity) {
        this.id = id;
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
