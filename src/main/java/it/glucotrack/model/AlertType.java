package it.glucotrack.model;

public class AlertType {

    private int id;
    private String name;
    private String description;

    // ===== Default constructor =====
    public AlertType() {
        this.id = -1;
        this.name = "";
        this.description = "";
    }

    // ===== Full constructor =====
    public AlertType(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // ===== Getters and Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

}
