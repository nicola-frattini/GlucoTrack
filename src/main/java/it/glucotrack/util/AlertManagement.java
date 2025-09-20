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
    private static final int GLUCOSE_MIN = 70;
    private static final int GLUCOSE_MAX = 180;
    private static final int MEDICATION_ALERT_MINUTES = 60;

    /** Genera alert per un singolo paziente */
    public static List<Alert> generatePatientAlerts(Patient patient) throws SQLException {
        List<Alert> alerts = new ArrayList<>();

        // 1. Misurazioni fuori soglia
        alerts.addAll(glucoseOutOfRange(patient));

        // 2. Misurazioni mancanti
        alerts.addAll(missingsGlucoseMeasurements(patient));


        // 3. Farmaci non assunti
        alerts.addAll(nonLoggedMedications(patient));


        // 4. Farmaci imminenti
        alerts.addAll(medicationToGetInTheNextHour(patient));

        return alerts;
    }

    public static List<Alert> glucoseOutOfRange(Patient patient){
        List<Alert> alerts = new ArrayList<>();
        GlucoseMeasurement lastMeasurement = patient.getLastGlucoseMeasurement();
        if (lastMeasurement != null) {
            float value = lastMeasurement.getGlucoseLevel();
            LocalDateTime measurementDate = lastMeasurement.getDateAndTime();
            if (value < GLUCOSE_MIN) {
                alerts.add(new Alert("Glicemia troppo bassa: " + value, AlertType.CRITICAL, patient, measurementDate));
            }else if(value < GLUCOSE_MAX){
                alerts.add(new Alert("Glicemia alta: " + value, AlertType.WARNING, patient, measurementDate));
            }else{

                alerts.add(new Alert("Glicemia troppo alta: " + value, AlertType.CRITICAL, patient, measurementDate));
            }
        }
        return alerts;
    }

    public static List<Alert> missingsGlucoseMeasurements(Patient patient) {
        List<Alert> alerts = new ArrayList<>();
        LocalDateTime lastMeasurementDate = patient.getLastGlucoseMeasurement().getDateAndTime();
        if (isTooOld(lastMeasurementDate, DAYS_WITHOUT_MEASUREMENT)) {
            alerts.add(new Alert("Non ci sono misurazioni da più di " + DAYS_WITHOUT_MEASUREMENT + " giorni",
                    AlertType.WARNING, patient, LocalDateTime.now()));
        }
        return alerts;
    }

    public static List<Alert> medicationToGetInTheNextHour(Patient patient) throws SQLException {
        List<Alert> alerts = new ArrayList<>();
        List<LogMedication> upcomingMeds = patient.getUpcomingMedications(MEDICATION_ALERT_MINUTES);
        for (LogMedication med : upcomingMeds) {
            Medication medication = MedicationDAO.getMedicationById(med.getMedication_id());
            alerts.add(new Alert("Assunzione a breve di " + medication.getName_medication(),
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
                alerts.add(new Alert(medication.getName_medication() + " mancata assunzione il " + log.getDateAndTime().toLocalDate() + " " +
                        log.getDateAndTime().toLocalTime(), AlertType.WARNING, patient, log.getDateAndTime()));
            }
        }
        return alerts;
    }

    /** Genera alert per tutti i pazienti di un dottore */
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
            System.err.println("Errore durante il controllo degli alert per il dottore ID " + doctorId + ": " + e.getMessage());
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
