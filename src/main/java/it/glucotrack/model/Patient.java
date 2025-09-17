package it.glucotrack.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Patient extends User {

    private int doctorId;
    private List<GlucoseMeasurement> glucoseReadings;
    private List<String> symptoms;
    private List<RiskFactor> riskFactors;
    private List<Medication> medications;

    // ===== Default constructor =====
    public Patient() {
        super();
        this.doctorId = -1;
        this.glucoseReadings = new ArrayList<>();
        this.symptoms = new ArrayList<>();
        this.riskFactors = new ArrayList<>();
        this.medications = new ArrayList<>();
    }

    // Questo costruttore è specifico per caricare i dati dal database
    public Patient(int id, String name, String surname, String email, String password, LocalDate bornDate,
                   Gender gender, String phone, String birthPlace, String fiscalCode, int doctorId) {
        super(id, name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode);
        this.doctorId = doctorId;
        this.glucoseReadings = new ArrayList<>();
        this.symptoms = new ArrayList<>();
        this.riskFactors = new ArrayList<>();
        this.medications = new ArrayList<>();
    }

    // ===== Constructor for new records =====
    public Patient(String name, String surname, String email, String password, LocalDate bornDate,
                   Gender gender, String phone, String birthPlace, String fiscalCode, int doctorId) {
        super(name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode);
        this.doctorId = doctorId;
        this.glucoseReadings = new ArrayList<>();
        this.symptoms = new ArrayList<>();
        this.riskFactors = new ArrayList<>();
        this.medications = new ArrayList<>();
    }

    // ===== Full constructor =====
    public Patient(int id, String name, String surname, String email, String password, LocalDate bornDate,
                   Gender gender, String phone, String birthPlace, String fiscalCode, int doctorId,
                   List<GlucoseMeasurement> glucoseReadings, List<String> symptoms, List<RiskFactor> riskFactors, List<Medication> medications) {
        super(id, name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode);
        this.doctorId = doctorId;
        this.glucoseReadings = (glucoseReadings != null) ? glucoseReadings : new ArrayList<>();
        this.symptoms = (symptoms != null) ? symptoms : new ArrayList<>();
        this.riskFactors = (riskFactors != null) ? riskFactors : new ArrayList<>();
        this.medications = (medications != null) ? medications : new ArrayList<>();
    }

    // ===== Getters and setters =====
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public List<GlucoseMeasurement> getGlucoseReadings() { return glucoseReadings; }
    public void setGlucoseReadings(List<GlucoseMeasurement> glucoseReadings) { this.glucoseReadings = glucoseReadings; }

    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }

    public List<RiskFactor> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<RiskFactor> riskFactors) { this.riskFactors = riskFactors; }

    public List<Medication> getMedications() { return medications; }
    public void setMedications(List<Medication> medications) { this.medications = medications; }

    @Override
    public String toString() {
        return "Patient{" +
                "fullName='" + getFullName() + '\'' +
                ", doctorId=" + doctorId +
                ", glucoseReadings=" + glucoseReadings.size() +
                ", symptoms=" + symptoms +
                ", riskFactors=" + riskFactors.size() +
                '}';
    }
}