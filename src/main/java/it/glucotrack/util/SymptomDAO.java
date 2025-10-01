package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import it.glucotrack.model.Symptom;

/*
* SYMPTOM DAO
* Data Access Object for Symptom entity
* Handles all database operations related to symptoms
*/

public class SymptomDAO {

    //================================
    //==== GENERIC GET OPERATIONS ====
    //================================

    public static List<Symptom> getSymptomsByPatientId(int patientId) throws SQLException {
        String sql = "SELECT * FROM patient_symptoms WHERE patient_id = ? ORDER BY symptom_date DESC";
        List<Symptom> symptoms = new ArrayList<>();

        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            while (rs.next()) {
                Symptom symptom = new Symptom();
                symptom.setPatient_id(patientId);
                symptom.setSymptomName(rs.getString("symptom"));
                symptom.setGravity(rs.getString("severity"));
                symptom.setDuration(LocalTime.parse(rs.getString("duration")));
                symptom.setNotes(rs.getString("notes"));
                String dateTimeStr = rs.getString("symptom_date");
                if (dateTimeStr != null && !dateTimeStr.isEmpty()) {
                    // Use OffsetDateTime to handle nanoseconds safely, then convert to LocalDateTime
                    symptom.setDateAndTime(LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } else {
                    symptom.setDateAndTime(LocalDateTime.now());
                }
                symptoms.add(symptom);
            }
        }

        return symptoms;
    }

    public List<String> getUniqueSymptoms() throws SQLException {
        String sql = "SELECT DISTINCT symptom FROM patient_symptoms ORDER BY symptom";
        List<String> symptoms = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                symptoms.add(rs.getString("symptom"));
            }
        }
        return symptoms;
    }


    public List<String> getSymptomsByPatientIdAndDateRange(int patientId, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT symptom FROM patient_symptoms WHERE patient_id = ? AND symptom_date BETWEEN ? AND ? ORDER BY symptom_date DESC";
        List<String> symptoms = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId, startDate, endDate)) {
            while (rs.next()) {
                symptoms.add(rs.getString("symptom"));
            }
        }
        return symptoms;
    }

    // Used to set a list of Symptoms for a table
    public static List<Symptom> getSymptomsForTable(int patientId) throws SQLException {
        String sql = "SELECT id, symptom, severity, duration, notes, symptom_date FROM patient_symptoms WHERE patient_id = ? ORDER BY symptom_date DESC";
        List<Symptom> symptoms = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("symptom");
                String severity = rs.getString("severity");
                String duration = rs.getString("duration");
                String notes = rs.getString("notes");

                // Timestamp handling
                LocalDateTime dateTime;
                try {
                    Timestamp timestamp = rs.getTimestamp("symptom_date");
                    if (timestamp != null) {
                        dateTime = timestamp.toLocalDateTime();
                    } else {
                        dateTime = LocalDateTime.now();
                    }
                } catch (SQLException e) {

                    // If timestamp fails, try with getString and parsing
                    String dateStr = rs.getString("symptom_date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            // Check for hours time
                            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                                dateTime = LocalDate.parse(dateStr).atTime(12, 0); // Mezzogiorno di default
                            } else {
                                // Normal parse
                                dateTime = LocalDateTime.parse(dateStr.replace(" ", "T"));
                            }
                        } catch (Exception parseEx) {
                            System.err.println("Error parsing data '" + dateStr + "': " + parseEx.getMessage());
                            dateTime = LocalDateTime.now();
                        }
                    } else {
                        dateTime = LocalDateTime.now();
                    }
                }

                // Build a Symptom object
                Symptom symptom = new Symptom();
                symptom.setId(id);
                symptom.setPatient_id(patientId);
                symptom.setSymptomName(name);
                symptom.setGravity(severity != null ? severity : "Mild");  // Default if null
                symptom.setNotes(notes != null ? notes : "");  // Default if null
                symptom.setDateAndTime(dateTime);

                try {
                    if (duration != null && !duration.isEmpty()) {

                        symptom.setDuration(LocalTime.parse(duration));
                    } else {
                        symptom.setDuration(LocalTime.of(0, 0));  // Default if null
                    }
                } catch (Exception e) {
                    symptom.setDuration(LocalTime.of(0, 0));
                }

                symptoms.add(symptom);
            }
        }
        return symptoms;
    }


    //===========================
    //==== INSERT OPERATIONS ====
    //===========================

    public static boolean insertSymptom(Symptom symptom) throws SQLException {
        String sql = "INSERT INTO patient_symptoms (patient_id, symptom, severity, duration, notes, symptom_date) VALUES (?, ?, ?, ?, ?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql,
                symptom.getPatient_id(),
                symptom.getSymptomName(),
                symptom.getGravity(),
                symptom.getDuration().toString(),
                symptom.getNotes(),
                symptom.getDateAndTime());
            return rows > 0;
    }

    public static boolean insertSymptom(int PatiendId, Symptom symptom) throws SQLException {
        {
        String sql = "INSERT INTO patient_symptoms (patient_id, symptom, severity, duration, notes, symptom_date) VALUES (?, ?, ?, ?, ?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql,
                PatiendId,
                symptom.getSymptomName(),
                symptom.getGravity(),
                symptom.getDuration().toString(),
                symptom.getNotes(),
                symptom.getDateAndTime());
        return rows > 0;
        }
    }


    //===========================
    //==== DELETE OPERATIONS ====
    //===========================

    public boolean deleteSymptom(int patientId, String symptom) throws SQLException {
        String sql = "DELETE FROM patient_symptoms WHERE patient_id = ? AND symptom = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId, symptom);
        return rows > 0;
    }

    public boolean deleteSymptomsByPatientId(int patientId) throws SQLException {
        String sql = "DELETE FROM patient_symptoms WHERE patient_id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId);
        return rows > 0;
    }

    public boolean deleteSymptomById(int id) throws SQLException {
        String sql = "DELETE FROM patient_symptoms WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }

    //===========================
    //==== UPDATE OPERATIONS ====
    //===========================

    public boolean updateSymptom(Symptom symptom) throws SQLException {
        String sql = "UPDATE patient_symptoms SET symptom=?, severity=?, duration=?, notes=?, symptom_date=? WHERE id=?";
        int rows = DatabaseInteraction.executeUpdate(sql,
            symptom.getSymptomName(),
            symptom.getGravity(),
            symptom.getDuration().toString(),
            symptom.getNotes(),
            symptom.getDateAndTime(),
            symptom.getId());
        return rows > 0;
    }


    //==============================
    //=== ADDITIONAL OPERATIONS ====
    //==============================

    public Symptom findSymptomById(int id) throws SQLException {
        String sql = "SELECT id, patient_id, symptom, severity, duration, notes, symptom_date FROM patient_symptoms WHERE id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToSymptom(rs);
            }
        }
        return null;
    }
    

    private Symptom mapResultSetToSymptom(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int patientId = rs.getInt("patient_id");
        String name = rs.getString("symptom");
        String severity = rs.getString("severity");
        String duration = rs.getString("duration");
        String notes = rs.getString("notes");
        

        LocalDateTime dateTime;
        try {
            Timestamp timestamp = rs.getTimestamp("symptom_date");
            if (timestamp != null) {
                dateTime = timestamp.toLocalDateTime();
            } else {
                dateTime = LocalDateTime.now();
            }
        } catch (SQLException e) {
            String dateStr = rs.getString("symptom_date");
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        dateTime = LocalDate.parse(dateStr).atTime(12, 0);
                    } else {
                        dateTime = LocalDateTime.parse(dateStr.replace(" ", "T"));
                    }
                } catch (Exception parseEx) {
                    dateTime = LocalDateTime.now();
                }
            } else {
                dateTime = LocalDateTime.now();
            }
        }
        
        Symptom symptom = new Symptom();
        symptom.setId(id);
        symptom.setPatient_id(patientId);
        symptom.setSymptomName(name);
        symptom.setGravity(severity != null ? severity : "Mild");
        symptom.setNotes(notes != null ? notes : "");
        symptom.setDateAndTime(dateTime);
        
        try {
            if (duration != null && !duration.isEmpty()) {
                symptom.setDuration(LocalTime.parse(duration));
            } else {
                symptom.setDuration(LocalTime.of(0, 0));
            }
        } catch (Exception e) {
            symptom.setDuration(LocalTime.of(0, 0));
        }
        
        return symptom;
    }
}