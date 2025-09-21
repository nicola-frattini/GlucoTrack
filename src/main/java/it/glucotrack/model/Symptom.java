package it.glucotrack.model;

import java.time.*;

public class Symptom {

    int id;
    int patient_id;
    LocalDateTime dateAndTime;
    String symptomName;
    String gravity;
    LocalTime duration;
    String notes;

    // ===== Default constructor =====
    public Symptom() {
        this.id = -1;
        this.patient_id = -1;
        this.dateAndTime = LocalDateTime.now();
        this.symptomName = "";
        this.gravity = "";
        this.duration = LocalTime.of(0, 0);
        this.notes = "";

    }

    // ===== Full constructor =====
    public Symptom(int id, int patient_id, LocalDateTime dateAndTime, String symptomName, String gravity, LocalTime duration, String notes) {
        this.id = id;
        this.patient_id = patient_id;
        this.dateAndTime = dateAndTime;
        this.symptomName = symptomName;
        this.gravity = gravity;
        this.duration = duration;
        this.notes = notes;
    }


    // ===== Getters and Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatient_id() { return patient_id; }
    public void setPatient_id(int patient_id) { this.patient_id = patient_id; }

    public LocalDateTime getDateAndTime() { return dateAndTime; }
    public void setDateAndTime(LocalDateTime dateAndTime) { this.dateAndTime= dateAndTime; }

    public String getSymptomName() { return symptomName; }
    public void setSymptomName(String symptomName) { this.symptomName = symptomName; }

    public String getGravity() { return gravity; }
    public void setGravity(String gravity) { this.gravity = gravity; }

    public LocalTime getDuration() { return duration; }
    public void setDuration(LocalTime duration) { this.duration = duration; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Symptom{" +
                "id=" + id +
                ", patient_id=" + patient_id +
                ", dateAndTime=" + dateAndTime +
                ", symptomName='" + symptomName + '\'' +
                ", gravity='" + gravity + '\'' +
                ", duration=" + duration +
                ", notes='" + notes + '\'' +
                '}';
    }



}
