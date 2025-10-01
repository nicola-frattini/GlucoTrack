package it.glucotrack.util;

import it.glucotrack.model.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/*
* Alert Management
*/

public class AlertManagement {


    // DATA
    private static final int DAYS_WITHOUT_MEASUREMENT = 7;
    private static final int GLUCOSE_MIN = 70;
    private static final int GLUCOSE_ELEVATED = 140;
    private static final int GLUCOSE_MAX = 180;
    private static final int MEDICATION_ALERT_MINUTES = 60;


    // Generate the alert for a patient
    public static List<Alert> generatePatientAlerts(Patient patient) throws SQLException {

        List<Alert> alerts = new ArrayList<>();
        List<Alert> alertsForFunction = new ArrayList<>();

        alertsForFunction = glucoseOutOfRange(patient);
        if(alertsForFunction != null)
            alerts.addAll(alertsForFunction);

        alertsForFunction = missingsGlucoseMeasurements(patient);
        if(alertsForFunction != null)
            alerts.addAll(alertsForFunction);

        alertsForFunction = nonLoggedMedications(patient);
        if(alertsForFunction != null)
            alerts.addAll(alertsForFunction);

        alertsForFunction = medicationToGetInTheNextHour(patient);
        if(alertsForFunction != null)
            alerts.addAll(alertsForFunction);

        return alerts;
    }

    public static List<Alert> glucoseOutOfRange(Patient patient){
        List<Alert> alerts = new ArrayList<>();
        GlucoseMeasurement lastMeasurement = patient.getLastGlucoseMeasurement();
        if (lastMeasurement != null) {
            float value = lastMeasurement.getGlucoseLevel();
            LocalDateTime measurementDate = lastMeasurement.getDateAndTime();
            if (value < GLUCOSE_MIN) {
                alerts.add(new Alert("Glicemy Low: " + value, AlertType.CRITICAL, patient, measurementDate));
            }else if(value >= GLUCOSE_ELEVATED && value <= GLUCOSE_MAX){
                alerts.add(new Alert("Glicemia Elevated: " + value, AlertType.WARNING, patient, measurementDate));
            }else if(value > GLUCOSE_MAX){
                alerts.add(new Alert("Glicemia High: " + value, AlertType.CRITICAL, patient, measurementDate));
            }
        }
        return alerts;
    }

    public static List<Alert> missingsGlucoseMeasurements(Patient patient) {
        GlucoseMeasurement last = patient.getLastGlucoseMeasurement();
        if (last == null) {
            return null;
        }

        List<Alert> alerts = new ArrayList<>();
        LocalDateTime lastMeasurementDate = patient.getLastGlucoseMeasurement().getDateAndTime();
        if (isTooOld(lastMeasurementDate, DAYS_WITHOUT_MEASUREMENT)) {
            alerts.add(new Alert("No misuration for more than " + DAYS_WITHOUT_MEASUREMENT + " days",
                    AlertType.WARNING, patient, LocalDateTime.now()));
        }

        return alerts;
    }

    public static List<Alert> medicationToGetInTheNextHour(Patient patient) throws SQLException {
        List<Alert> alerts = new ArrayList<>();
        List<LogMedication> upcomingMeds = patient.getUpcomingMedications(MEDICATION_ALERT_MINUTES);
        for (LogMedication med : upcomingMeds) {
            Medication medication = MedicationDAO.getMedicationById(med.getMedication_id());
            alerts.add(new Alert("Looking forward to take " + medication.getName_medication(),
                    AlertType.INFO, patient, med.getDateAndTime()));
        }
        return alerts;
    }

    public static List<Alert> nonLoggedMedications(Patient patient) throws SQLException {
        List<Alert> alerts = new ArrayList<>();
        List<LogMedication> notTaken = patient.getAllMedicationLogsNotTaken();
        for(LogMedication log : notTaken) {
            if (log.getDateAndTime().isBefore(LocalDateTime.now())) {
                Medication medication = MedicationDAO.getMedicationById(log.getMedication_id());
                alerts.add(new Alert(medication.getName_medication() + " missed assumption at " + log.getDateAndTime().toLocalDate() + " " +
                        log.getDateAndTime().toLocalTime(), AlertType.WARNING, patient, log.getDateAndTime()));
            }
        }
        return alerts;
    }

    // Generate alert for all doctor's patients
    public static List<Alert> generateDoctorAlerts(int doctorId) {
        List<Alert> alerts = new ArrayList<>();
        try {
            List<Patient> patients = PatientDAO.getPatientsByDoctorId(doctorId);
            for (Patient patient : patients) {
                alerts.addAll(glucoseOutOfRange(patient));
                alerts.addAll(missingsGlucoseMeasurements(patient));
                alerts.addAll(nonLoggedMedications(patient));
            }
        } catch (Exception e) {
            System.err.println("Error during the check for the doctor ID " + doctorId + ": " + e.getMessage());
        }
        return alerts;
    }

    private static boolean isTooOld(LocalDateTime dateTime, int maxDays) {
        if (dateTime == null) return true;
        long days = ChronoUnit.DAYS.between(dateTime, LocalDateTime.now());
        return days > maxDays;
    }
}
