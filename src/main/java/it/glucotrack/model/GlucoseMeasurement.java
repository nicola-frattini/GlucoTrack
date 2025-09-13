package it.glucotrack.model;

import java.time.LocalDateTime;

public class GlucoseMeasurement {

    private LocalDateTime timestamp;   // Date and time of the measurement
    private int value;                 // Glucose value in mg/dL
    private boolean beforeMeal;        // True if measurement was before a meal, false if after

    // ===== Default constructor =====
    public GlucoseMeasurement() {
        this.timestamp = LocalDateTime.now();
        this.value = 0;
        this.beforeMeal = true;
    }

    // ===== Full constructor =====
    public GlucoseMeasurement(LocalDateTime timestamp, int value, boolean beforeMeal) {
        this.timestamp = timestamp;
        this.value = value;
        this.beforeMeal = beforeMeal;
    }

    // ===== Getters and setters =====
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isBeforeMeal() {
        return beforeMeal;
    }

    public void setBeforeMeal(boolean beforeMeal) {
        this.beforeMeal = beforeMeal;
    }

    @Override
    public String toString() {
        return "GlucoseMeasurement{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                ", beforeMeal=" + beforeMeal +
                '}';
    }
}
