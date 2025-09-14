package it.glucotrack.model;

import java.time.LocalDateTime;


public class GlucoseMeasurement {

    int id;
    int patientId;
    LocalDateTime dateAndTime;
    float glucoseLevel; // in mg/dL
    boolean beforeMeal; // true if before meal, false if after meal
    String notes;


    public GlucoseMeasurement(int id, int patientId, LocalDateTime dateAndTime, float glucoseLevel, boolean beforeMeal, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.dateAndTime = dateAndTime;
        this.glucoseLevel = glucoseLevel;
        this.beforeMeal = beforeMeal;
        this.notes = notes;
    }

    public GlucoseMeasurement() {
        this.id = -1;
        this.patientId = -1;
        this.dateAndTime = LocalDateTime.now();
        this.glucoseLevel = 0.0f;
        this.beforeMeal = true;
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

    public boolean isBeforeMeal() {return beforeMeal;}
    public void setBeforeMeal(boolean beforeMeal) {this.beforeMeal = beforeMeal;}

    public String getNotes() {return notes;}
    public void setNotes(String notes) {this.notes = notes;}

    public Status getStatus() {
        return Status.fromGlucoseValue(glucoseLevel);
    }
    public String getStatusString(){
        return Status.fromGlucoseValue(glucoseLevel).toString();
    }

    @Override
    public String toString() {
        return "GlucoseMesourament{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", dateAndTime=" + dateAndTime +
                ", glucoseLevel=" + glucoseLevel +
                ", beforeMeal=" + beforeMeal +
                ", notes='" + notes + '\'' +
                '}';
    }




}
