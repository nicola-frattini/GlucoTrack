package it.glucotrack.model;

//Questa classe serve per salvare le modifiche effettuate su una terapia di un paziente
//in modo tale da sapere cosa è stato cambiato e chi l'ha fatto


import java.time.LocalDate;
import java.time.LocalDateTime;

public class MedicationEdit {

    private int medicationId;
    private int doctorId;
    private Medication medication;
    private LocalDateTime editTimestamp; // Timestamp of the edit

    // ===== Full constructor =====
    public MedicationEdit(int medicationId, int doctorId, LocalDateTime editTimestamp, Medication medication) {
        this.medicationId = medicationId;
        this.doctorId = doctorId;
        this.editTimestamp = editTimestamp;
        this.medication = medication;
    }

    // ===== Blank constructor =====
    public MedicationEdit() {
        this(-1, -1, null, new Medication());
    }

    public MedicationEdit(int doctorId, LocalDateTime time, Medication medication) {
        this(-1, doctorId, time, medication);
    }

    public MedicationEdit(int doctorId, Medication medication) {
        this(-1, doctorId, LocalDateTime.now(), medication);
    }

    public MedicationEdit(int id, int doctorId, LocalDateTime time, int medicationId,int patientId, String name, String dosage, Frequency frequency, String notes, LocalDate start, LocalDate end) {
        this(id, doctorId, time, new Medication(medicationId, patientId, name, dosage, frequency, start, end, notes));
    }

    // ===== Getters and Setters =====

    public int getMedicationId() { return medicationId; }
    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId;}

    public Medication getMedication(){ return medication; };
    public void setMedication() { this.medication = medication;}

    public LocalDateTime getEditTimestamp() { return editTimestamp; }
    public void setEditTimestamp(LocalDateTime editTimestamp) { this.editTimestamp = editTimestamp; }

    @Override
    public String toString() {
        return "MedicationEdit{" +
                "medicationId=" + medicationId +
                ", doctorId=" + doctorId +
                ", medication=" + medication + '\'' +
                ", editTimestamp='" + editTimestamp + '\'' +
                '}';
    }






}
