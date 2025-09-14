package it.glucotrack.model;

import java.time.LocalDateTime;

public class LogMedication {

    // All the time that a patient has to take a medication from medication class
    private int id;
    private int medication_id;
    private LocalDateTime dateAndTime;
    private boolean taken; // true if taken, false if not taken

    // ===== Default constructor =====
    public LogMedication() {
        this.id = -1;
        this.medication_id = -1;
        this.dateAndTime = LocalDateTime.now();
        this.taken = false;
    }

    // ===== Full constructor =====
    public LogMedication(int id, int medication_id, LocalDateTime dateAndTime, boolean taken) {
        this.id = id;
        this.medication_id = medication_id;
        this.dateAndTime = dateAndTime;
        this.taken = taken;
    }

    // ===== Getters and Setters =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedication_id() { return medication_id; }
    public void setMedication_id(int medication_id) { this.medication_id = medication_id;}

    public LocalDateTime getDateAndTime() { return dateAndTime; }
    public void setDateAndTime(LocalDateTime dateAndTime) { this.dateAndTime = dateAndTime; }

    public boolean isTaken() { return taken; }
    public void setTaken(boolean taken) { this.taken = taken; }

    @Override
    public String toString() {
        return "LogMedication{" +
                "id=" + id +
                ", medication_id=" + medication_id +
                ", dateAndTime=" + dateAndTime +
                ", taken=" + taken +
                '}';

    }




}
