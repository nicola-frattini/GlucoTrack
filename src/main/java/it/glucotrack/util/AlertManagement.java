package it.glucotrack.util;

import it.glucotrack.model.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AlertManagement {

    // Soglie parametriche
    private static final int DAYS_WITHOUT_MEASUREMENT = 7;
    private static final int DAYS_WITHOUT_MEDICATION_LOG = 2;
    private static final int GLUCOSE_MIN = 70;
    private static final int GLUCOSE_MAX = 180;
    private static final int MEDICATION_ALERT_MINUTES = 30;

    /** Genera alert per un singolo paziente */
    public static List<Alert> generatePatientAlerts(Patient patient) throws SQLException {
        List<Alert> alerts = new ArrayList<>();

        // 1. Controllo ultima misurazione glicemia
        GlucoseMeasurement lastMeasurement = patient.getLastGlucoseMeasurement();
        if (lastMeasurement != null) {
            float value = lastMeasurement.getGlucoseLevel();
            LocalDateTime measurementDate = lastMeasurement.getDateAndTime();
            if (value < GLUCOSE_MIN) {
                alerts.add(new Alert("Glicemia troppo bassa: " + value, AlertType.CRITICAL, patient, measurementDate));
            } else if (value > GLUCOSE_MAX) {
                alerts.add(new Alert("Glicemia troppo alta: " + value, AlertType.CRITICAL, patient, measurementDate));
            }
        }

        // 2. Misurazioni mancanti
        try {
            LocalDateTime lastMeasurementDate = patient.getLastGlucoseMeasurement().getDateAndTime();
            if (isTooOld(lastMeasurementDate, DAYS_WITHOUT_MEASUREMENT)) {
                alerts.add(new Alert("Non ci sono misurazioni da più di " + DAYS_WITHOUT_MEASUREMENT + " giorni",
                        AlertType.WARNING, patient, LocalDateTime.now()));
            }

        }catch (Exception e) {
            // Nessuna misurazione presente
        }
        // 3. Farmaci non loggati
        LogMedication lastLog = patient.getLastMedicationLog();
        if (lastLog != null) {
            LocalDateTime dateTime = lastLog.getDateAndTime();
            if (isTooOld(dateTime, DAYS_WITHOUT_MEDICATION_LOG)) {
                alerts.add(new Alert("Farmaci non registrati da più di " + DAYS_WITHOUT_MEDICATION_LOG + " giorni",
                        AlertType.WARNING, patient, LocalDateTime.now()));
            }        } else {
            // Nessun log presente, puoi saltare o fare un default
        }

        // 4. Farmaci imminenti
        List<LogMedication> upcomingMeds = patient.getUpcomingMedications(MEDICATION_ALERT_MINUTES);
        for (LogMedication med : upcomingMeds) {
            Medication medication = MedicationDAO.getMedicationById(med.getMedication_id());
            alerts.add(new Alert("Tra " + MEDICATION_ALERT_MINUTES + " minuti prendi: " + medication.getName_medication(),
                    AlertType.INFO, patient, med.getDateAndTime()));
        }

        return alerts;
    }

    /** Genera alert per tutti i pazienti di un dottore */
    public static List<Alert> generateDoctorAlerts(int doctorId) {
        List<Alert> alerts = new ArrayList<>();
        try {
            List<Patient> patients = PatientDAO.getPatientsByDoctorId(doctorId);
            for (Patient patient : patients) {
                alerts.addAll(generatePatientAlerts(patient));
            }
        } catch (Exception e) {
            System.err.println("❌ Errore durante il controllo degli alert per il dottore ID " + doctorId + ": " + e.getMessage());
        }
        return alerts;
    }

    /** Controllo se la data è troppo vecchia */
    private static boolean isTooOld(LocalDateTime dateTime, int maxDays) {
        if (dateTime == null) return true;
        long days = ChronoUnit.DAYS.between(dateTime, LocalDateTime.now());
        return days > maxDays;
    }
}
