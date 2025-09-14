package it.glucotrack.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Patient extends User {

    private int doctorId;                       // DB reference to assigned doctor
    private List<GlucoseMeasurement> glucoseReadings; // Daily glucose readings
    private List<String> symptoms;              // Symptoms reported
    private List<RiskFactor> riskFactors;       // Patient risk factors


    // ===== Default constructor =====
    public Patient() {
        super();
        this.doctorId = -1;
        this.glucoseReadings = new ArrayList<>();
        this.symptoms = new ArrayList<>();
        this.riskFactors = new ArrayList<>();
    }

    // ===== Constructor without ID (for new records) =====
    public Patient(String name, String surname, String email, String password, LocalDate bornDate,
                   Gender gender, String phone, String birthPlace, String fiscalCode, int doctorId) {
        super(name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode);
        this.doctorId = doctorId;
        this.glucoseReadings = new ArrayList<>();
        this.symptoms = new ArrayList<>();
        this.riskFactors = new ArrayList<>();
    }

    // ===== Full constructor =====
    public Patient(int id, String name, String surname, String email, String password, LocalDate bornDate,
                   Gender gender, String phone, String birthPlace, String fiscalCode, int doctorId) {
        super(id, name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode);
        this.doctorId = doctorId;
        this.glucoseReadings = new ArrayList<>();
        this.symptoms = new ArrayList<>();
        this.riskFactors = new ArrayList<>();
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
