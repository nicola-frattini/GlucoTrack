package it.glucotrack.model;

import java.time.LocalDateTime;

public class Alert {

    private String message;
    private AlertType type;
    private Patient patient;
    private LocalDateTime dateAndTime;

    public Alert(String message, AlertType type, Patient patient, LocalDateTime dateAndTime) {
        this.message = message;
        this.type = type;
        this.patient = patient;
        this.dateAndTime = dateAndTime;
    }

    public Alert(){
        this.message = "";
        this.type = null;
        this.patient = null;
        this.dateAndTime = null;
    }

    public Alert(String message, AlertType type) {
        this(message, type, null, LocalDateTime.now());
    }

    public Alert(String message, AlertType type, Patient patient){
        this(message, type, patient, LocalDateTime.now());
    }


    public String getMessage() {return message;}
    public void setMessage(String message) {this.message = message;}

    public AlertType getType() {return type;}
    public void setType(AlertType type) {this.type = type;}


    public Patient getPatient() {return patient;}
    public void setPatient(Patient patient) {this.patient = patient;}

    public LocalDateTime getDateAndTime() {return dateAndTime;}
    public void setDateAndTime(LocalDateTime dateAndTime){this.dateAndTime = dateAndTime;}


    @Override
    public String toString() {
        return "Alert{" +
                "message='" + message + '\'' +
                ", type=" + type + '\'' +
                ", patient=" + patient + '\'' +
                ", date=" + dateAndTime +
                '}';
    }
}
