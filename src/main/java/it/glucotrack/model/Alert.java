package it.glucotrack.model;

public class Alert {

    private String message;
    private AlertType type;

    public Alert(String message, AlertType type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }


    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }


}
