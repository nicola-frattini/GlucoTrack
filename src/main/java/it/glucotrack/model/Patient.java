package it.glucotrack.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.util.MedicationDAO;
import it.glucotrack.util.RiskFactorDAO;
import it.glucotrack.util.SymptomDAO;
import it.glucotrack.util.GlucoseMeasurementDAO;
import it.glucotrack.util.LogMedicationDAO;

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
        super(id, name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode, "PATIENT");
        this.doctorId = doctorId;
        this.glucoseReadings = glucoseReadingsSetup();
        this.symptoms = symptomsSetup();
        this.riskFactors = riskFactorsSetup();
        this.medications = medicationsSetup();
    }



    // ===== Constructor for new records =====
    public Patient(String name, String surname, String email, String password, LocalDate bornDate,
                   Gender gender, String phone, String birthPlace, String fiscalCode, int doctorId) {
        super(name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode, "PATIENT");
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
        super(id, name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode, "PATIENT");
        this.doctorId = doctorId;
        this.glucoseReadings = (glucoseReadings != null) ? glucoseReadings : glucoseReadingsSetup();
        this.symptoms = (symptoms != null) ? symptoms : symptomsSetup();
        this.riskFactors = (riskFactors != null) ? riskFactors : riskFactorsSetup();
        this.medications = (medications != null) ? medications : getMedications();
    }




    public LogMedication getLastMedicationLog() {
        if (medications.isEmpty()) return null;
        LogMedication latestLog = null;
        for (Medication med : medications) {
            for (LogMedication log : med.getLogMedications()) {
                if (latestLog == null || log.getDateAndTime().isAfter(latestLog.getDateAndTime())) {
                    latestLog = log;
                }
            }
        }
        return latestLog;
    }

    public List<LogMedication> getAllMedicationLogsNotTaken(){
        if(medications.isEmpty()) return new ArrayList<>();
        List<LogMedication> notTakenLogs = new ArrayList<>();
        for(Medication med : medications) {
            for (LogMedication log : med.getLogMedications()) {
                if (!log.isTaken()) {
                    notTakenLogs.add(log);
                }
            }
        }
        return notTakenLogs;
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

    public GlucoseMeasurement getLastGlucoseMeasurement() {

        if (glucoseReadings.isEmpty()) return null;
        GlucoseMeasurement latestMeasurement = null;
        for (GlucoseMeasurement gm : glucoseReadings) {
            if (latestMeasurement == null || gm.getDateAndTime().isAfter(latestMeasurement.getDateAndTime())) {
                latestMeasurement = gm;
            }
        }
        return latestMeasurement;


    }

    public List<LogMedication> getUpcomingMedications(int medicationAlertMinutes) {

        List<LogMedication> upcoming = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime alertThreshold = now.plusMinutes(medicationAlertMinutes);

        for (Medication med : medications) {
            for (LogMedication log : med.getLogMedications()) {
                if (!log.isTaken() && !log.getDateAndTime().isBefore(now) && !log.getDateAndTime().isAfter(alertThreshold)) {
                    upcoming.add(log);
                }
            }
        }
        return upcoming;

    }

    private List<GlucoseMeasurement> glucoseReadingsSetup() {
        // Interroga l'sql per il suo id, se non ci sono misurazioni ritorna lista vuota
        try {
            return it.glucotrack.util.GlucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(this.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

    }

    private List <String> symptomsSetup() {
        // Interroga l'sql per il suo id, se non ci sono sintomi ritorna lista vuota
        try {
            return it.glucotrack.util.SymptomDAO.getSymptomsByPatientId(this.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    private List <RiskFactor> riskFactorsSetup() {
        // Interroga l'sql per il suo id, se non ci sono fattori di rischio ritorna lista vuota
        try {
            return it.glucotrack.util.RiskFactorDAO.getRiskFactorsByPatientId(this.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List <Medication> medicationsSetup() {
        // Interroga l'sql per il suo id, se non ci sono farmaci ritorna lista vuota
        try {
            return it.glucotrack.util.MedicationDAO.getMedicationsByPatientId(this.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }



}