package it.glucotrack.model;

import java.time.LocalDate;
import java.time.LocalDateTime;


public class GlucoseMeasurement {

    int id;
    int patientId;
    LocalDateTime dateAndTime;
    float glucoseLevel; // in mg/dL
    String type; // Type of measurement: "Before Breakfast", "After Lunch", etc.
    String notes;


    public GlucoseMeasurement(int id, int patientId, LocalDateTime dateAndTime, float glucoseLevel, String type, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.dateAndTime = dateAndTime;
        this.glucoseLevel = glucoseLevel;
        this.type = type;
        this.notes = notes;
    }

    // ===== Constructor without ID (for new records) =====
    public GlucoseMeasurement(int patientId, LocalDateTime dateAndTime, float glucoseLevel, String type, String notes) {
        this.id = -1; // Will be set by database
        this.patientId = patientId;
        this.dateAndTime = dateAndTime;
        this.glucoseLevel = glucoseLevel;
        this.type = type;
        this.notes = notes;
    }

    public GlucoseMeasurement() {
        this.id = -1;
        this.patientId = -1;
        this.dateAndTime = LocalDateTime.now();
        this.glucoseLevel = 0.0f;
        this.type = "Before Breakfast";
        this.notes = "";
    }

    public int getId() {return id;}
    public void setId(int id) {this.id = id;}

    public int getPatientId() {return patientId;}
    public void setPatientId(int patientId) {this.patientId = patientId;}

    public LocalDateTime getDateAndTime() {return dateAndTime;}
    public void setDateAndTime(LocalDateTime dateAndTime) {this.dateAndTime = dateAndTime;}

    public float getGlucoseLevel() {return glucoseLevel;}
    public void setGlucoseLevel(float glucoseLevel) {this.glucoseLevel = glucoseLevel;}

    public String getType() {return type;}
    public void setType(String type) {this.type = type;}

    public String getNotes() {return notes;}
    public void setNotes(String notes) {this.notes = notes;}

    // Helper method to check if measurement is before meal
    public boolean isBeforeMeal() {
        return type != null && type.toLowerCase().contains("before");
    }

    public Status getStatus() {
        return Status.fromGlucoseValue(glucoseLevel);
    }
    public String getStatusString(){
        return Status.fromGlucoseValue(glucoseLevel).toString();
    }

    public LocalDate getDate() {return dateAndTime.toLocalDate();}

    @Override
    public String toString() {
        return "GlucoseMeasurement{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", dateAndTime=" + dateAndTime +
                ", glucoseLevel=" + glucoseLevel +
                ", type='" + type + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }




}
