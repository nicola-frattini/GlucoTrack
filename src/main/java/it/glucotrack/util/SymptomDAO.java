package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import it.glucotrack.model.Symptom;

public class SymptomDAO {

    public List<String> getSymptomsByPatientId(int patientId) throws SQLException {
        String sql = "SELECT symptom FROM patient_symptoms WHERE patient_id = ? ORDER BY symptom_date DESC";
        List<String> symptoms = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
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

    public boolean insertSymptom(int patientId, String symptom, LocalDate symptomDate) throws SQLException {
        String sql = "INSERT INTO patient_symptoms (patient_id, symptom, symptom_date) VALUES (?, ?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId, symptom, symptomDate);
        return rows > 0;
    }

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

    // Nuovo metodo per il controller che usa il modello Symptom
    public List<Symptom> getSymptomsForTable(int patientId) throws SQLException {
        String sql = "SELECT id, symptom, severity, duration, notes, symptom_date FROM patient_symptoms WHERE patient_id = ? ORDER BY symptom_date DESC";
        List<Symptom> symptoms = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("symptom");  // Corretto: "symptom" non "symptom_name"
                String severity = rs.getString("severity");
                String duration = rs.getString("duration");
                String notes = rs.getString("notes");
                // Gestione più robusta del timestamp
                LocalDateTime dateTime;
                try {
                    Timestamp timestamp = rs.getTimestamp("symptom_date");
                    if (timestamp != null) {
                        dateTime = timestamp.toLocalDateTime();
                    } else {
                        dateTime = LocalDateTime.now();
                    }
                } catch (SQLException e) {
                    // Se fallisce con timestamp, prova con getString e parsing
                    String dateStr = rs.getString("symptom_date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            // Se è solo una data (YYYY-MM-DD), aggiungi l'orario di default
                            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                                dateTime = LocalDate.parse(dateStr).atTime(12, 0); // Mezzogiorno di default
                            } else {
                                // Se include già l'ora, prova il parsing normale
                                dateTime = LocalDateTime.parse(dateStr.replace(" ", "T"));
                            }
                        } catch (Exception parseEx) {
                            System.err.println("⚠️ Errore parsing data '" + dateStr + "': " + parseEx.getMessage());
                            dateTime = LocalDateTime.now();
                        }
                    } else {
                        dateTime = LocalDateTime.now();
                    }
                }
                
                // Creiamo un oggetto Symptom usando il costruttore con parametri appropriati
                Symptom symptom = new Symptom();
                symptom.setId(id);
                symptom.setPatient_id(patientId);
                symptom.setSymptomName(name);
                symptom.setGravity(severity != null ? severity : "Mild");  // Default se null
                symptom.setNotes(notes != null ? notes : "");  // Default se null
                symptom.setDateAndTime(dateTime);
                // Per la durata, convertiamo la stringa in LocalTime se possibile
                try {
                    if (duration != null && !duration.isEmpty()) {
                        // Assumiamo formato "HH:mm" per la durata
                        symptom.setDuration(LocalTime.parse(duration));
                    } else {
                        symptom.setDuration(LocalTime.of(0, 0));  // Default se null
                    }
                } catch (Exception e) {
                    symptom.setDuration(LocalTime.of(0, 0));
                }
                
                symptoms.add(symptom);
            }
        }
        return symptoms;
    }

    // Nuovo metodo per inserire usando il modello Symptom
    public boolean insertSymptom(int patientId, Symptom symptom) throws SQLException {
        String sql = "INSERT INTO patient_symptoms (patient_id, symptom, severity, duration, notes, symptom_date) VALUES (?, ?, ?, ?, ?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql, 
            patientId, 
            symptom.getSymptomName(), 
            symptom.getGravity(), 
            symptom.getDuration().toString(), 
            symptom.getNotes(), 
            symptom.getDateAndTime());
        return rows > 0;
    }
    
    // Metodo per aggiornare un sintomo esistente
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
    
    // Metodo per eliminare un sintomo per ID
    public boolean deleteSymptomById(int id) throws SQLException {
        String sql = "DELETE FROM patient_symptoms WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }
    
    // Metodo per trovare un sintomo specifico per ID
    public Symptom findSymptomById(int id) throws SQLException {
        String sql = "SELECT id, patient_id, symptom, severity, duration, notes, symptom_date FROM patient_symptoms WHERE id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToSymptom(rs);
            }
        }
        return null;
    }
    
    // Helper method per mappare ResultSet a Symptom
    private Symptom mapResultSetToSymptom(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int patientId = rs.getInt("patient_id");
        String name = rs.getString("symptom");
        String severity = rs.getString("severity");
        String duration = rs.getString("duration");
        String notes = rs.getString("notes");
        
        // Gestione robusta del timestamp
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
        
        // Creiamo un oggetto Symptom
        Symptom symptom = new Symptom();
        symptom.setId(id);
        symptom.setPatient_id(patientId);
        symptom.setSymptomName(name);
        symptom.setGravity(severity != null ? severity : "Mild");
        symptom.setNotes(notes != null ? notes : "");
        symptom.setDateAndTime(dateTime);
        
        // Gestione della durata
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