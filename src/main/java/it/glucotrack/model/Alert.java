package it.glucotrack.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Alert {

    private String message;
    private AlertType type;
    private Patient patient;
    private LocalDateTime date;

    public Alert(String message, AlertType type, Patient patient, LocalDateTime date) {
        this.message = message;
        this.type = type;
        this.patient = patient;
        this.date = date;
    }

    public Alert(){
        this.message = "";
        this.type = null;
        this.patient = null;
        this.date = null;
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

    public LocalDateTime getDate() {return date;}
    public void setDate(LocalDateTime date){this.date = date;}


    @Override
    public String toString() {
        return "Alert{" +
                "message='" + message + '\'' +
                ", type=" + type + '\'' +
                ", patient=" + patient + '\'' +
                ", date=" + date +
                '}';
    }
}
