package it.glucotrack.model;

import java.time.LocalDateTime;

public class Reminder {
    private int id;
    private int patientId;
    private String title;           // Es: "Medication Reminder"
    private String message;         // Es: "Take Metformin 500mg"
    private LocalDateTime dateTime; // Quando mostrare l’avviso
    private boolean recurring;      // Se è ricorrente (es: ogni giorno)
    private String recurrenceRule;  // Es: "DAILY", "WEEKLY", "MONDAY", ecc.
    private boolean seen;           // Se l’utente ha visualizzato l’avviso

    public Reminder() {}

    public Reminder(int id, int patientId, String title, String message, LocalDateTime dateTime,
                    boolean recurring, String recurrenceRule, boolean seen) {
        this.id = id;
        this.patientId = patientId;
        this.title = title;
        this.message = message;
        this.dateTime = dateTime;
        this.recurring = recurring;
        this.recurrenceRule = recurrenceRule;
        this.seen = seen;
    }

    // Getter e setter...

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public String getRecurrenceRule() { return recurrenceRule; }
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }

    @Override
    public String toString() {
        return "Reminder{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", dateTime=" + dateTime +
                ", recurring=" + recurring +
                ", recurrenceRule='" + recurrenceRule + '\'' +
                ", seen=" + seen +
                '}';
    }
}