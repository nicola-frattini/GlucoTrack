package it.glucotrack.model;

public enum AlertType {
    INFO("blue"),
    WARNING("orange"),
    CRITICAL("red");

    private final String color;

    AlertType(String color) {
        this.color = color;
    }

    public String getColor() { return color; }
}
