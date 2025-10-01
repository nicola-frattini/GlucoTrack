package it.glucotrack.util;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.LogMedication;

/*
* LOG MEDICATION DAO
*/

public class LogMedicationDAO {

    //========================
    //==== GET OPERATIONS ====
    //========================

    public LogMedication getLogMedicationById(int id) throws SQLException {
        String sql = "SELECT * FROM log_medications WHERE id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToLogMedication(rs);
            }
        }
        return null;
    }

    public static List<LogMedication> getLogMedicationsByMedicationId(int medicationId) throws SQLException {
        String sql = "SELECT * FROM log_medications WHERE medication_id = ? ORDER BY date_time DESC";
        List<LogMedication> logs = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, medicationId)) {
            while (rs.next()) {
                logs.add(mapResultSetToLogMedication(rs));
            }
        }
        return logs;
    }

    public static List<LogMedication> getLogMedicationByPatientId(int patientId) throws SQLException {
        String sql = "SELECT lm.* FROM log_medications lm " +
                     "JOIN medications m ON lm.medication_id = m.id " +
                     "WHERE m.patient_id = ? ORDER BY lm.date_time DESC";
        List<LogMedication> logs = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            while (rs.next()) {
                logs.add(mapResultSetToLogMedication(rs));
            }
        }
        return logs;
    }

    public static List<LogMedication> getLogMedicationsByPatientIdUntillNow(int patientId) throws SQLException {

        String sql = "SELECT lm.* FROM log_medications lm " +
                     "JOIN medications m ON lm.medication_id = m.id " +
                     "WHERE m.patient_id = ? AND lm.date_time <= ? ORDER BY lm.date_time DESC";
        List<LogMedication> logs = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId, Timestamp.valueOf(LocalDateTime.now()))) {
            while (rs.next()) {
                logs.add(mapResultSetToLogMedication(rs));
            }
        }
        return logs;

    }


    public List<LogMedication> getPendingLogMedications(int medicationId) throws SQLException {
        String sql = "SELECT * FROM log_medications WHERE medication_id = ? AND taken = 0 ORDER BY date_time";
        List<LogMedication> logs = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, medicationId)) {
            while (rs.next()) {
                logs.add(mapResultSetToLogMedication(rs));
            }
        }
        return logs;
    }

    public List<LogMedication> getLogMedicationsByMedicationIdUpToNow(int medicationId) throws SQLException {
        String sql = "SELECT * FROM log_medications WHERE medication_id = ? AND date_time <= ? ORDER BY date_time DESC";
        List<LogMedication> logs = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, medicationId, LocalDateTime.now())) {
            while (rs.next()) {
                logs.add(mapResultSetToLogMedication(rs));
            }
        }
        return logs;
    }

    public List<LogMedication> getLogMedicationsByDateRange(int medicationId, LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        String sql = "SELECT * FROM log_medications WHERE medication_id = ? AND date_time BETWEEN ? AND ? ORDER BY date_time";
        List<LogMedication> logs = new ArrayList<>();
        
        // Convert LocalDateTime to java.sql.Timestamp for proper database storage
        java.sql.Timestamp startTimestamp = java.sql.Timestamp.valueOf(startDate);
        java.sql.Timestamp endTimestamp = java.sql.Timestamp.valueOf(endDate);
        
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, medicationId, 
                startTimestamp, 
                endTimestamp)) {
            while (rs.next()) {
                logs.add(mapResultSetToLogMedication(rs));
            }
        }
        return logs;
    }



    //===========================
    //==== INSERT OPERATIONS ====
    //===========================

    public boolean insertLogMedication(LogMedication log) throws SQLException {
        String sql = "INSERT INTO log_medications (medication_id, date_time, taken) VALUES (?, ?, ?)";
        
        // Convert LocalDateTime to java.sql.Timestamp for proper database storage
        java.sql.Timestamp dateTime = java.sql.Timestamp.valueOf(log.getDateAndTime());
        
        int rows = DatabaseInteraction.executeUpdate(sql,
                log.getMedication_id(), 
                dateTime,
                log.isTaken());
        return rows > 0;
    }

    public static void insertLogMedicationStatic(LogMedication log) throws SQLException {
        String sql = "INSERT INTO log_medications (medication_id, date_time, taken) VALUES (?, ?, ?)";

        // Convert LocalDateTime to java.sql.Timestamp for proper database storage
        java.sql.Timestamp dateTime = java.sql.Timestamp.valueOf(log.getDateAndTime());

        DatabaseInteraction.executeUpdate(sql,
                log.getMedication_id(),
                dateTime,
                log.isTaken());
    }


    //===========================
    //==== UPDATE OPERATIONS ====
    //===========================

    public static boolean updateLogMedication(LogMedication log) throws SQLException {
        String sql = "UPDATE log_medications SET medication_id=?, date_time=?, taken=? WHERE id=?";
        
        // Convert LocalDateTime to java.sql.Timestamp for proper database storage
        java.sql.Timestamp dateTime = java.sql.Timestamp.valueOf(log.getDateAndTime());
        
        int rows = DatabaseInteraction.executeUpdate(sql,
                log.getMedication_id(), 
                dateTime,
                log.isTaken(), 
                log.getId());
        return rows > 0;
    }

    public boolean updateLogMedicationStatus(int logId, boolean taken) throws SQLException {
        String sql = "UPDATE log_medications SET taken = ? WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, taken, logId);
        return rows > 0;
    }


    //===========================
    //==== DELETE OPERATIONS ====
    //===========================

    public boolean deleteLogMedication(int id) throws SQLException {
        String sql = "DELETE FROM log_medications WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }

    public static boolean deleteLogsByMedicationId(int id) {
        String sql = "DELETE FROM log_medications WHERE medication_id = ?";
        try {
            int rows = DatabaseInteraction.executeUpdate(sql, id);
            System.out.println("Deleted " + rows + " log medications for medication_id " + id);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting log medications: " + e.getMessage());
            return false;
        }
    }
    public void deleteFutureLogMedications(int id, LocalDateTime today) {

        String sql = "DELETE FROM log_medications WHERE medication_id = ? AND date_time >= ?";
        System.out.println("[DEBUG] Executing SQL: " + sql);
        System.out.println("[DEBUG] Parameters: medication_id=" + id + ", date=" + today);
        try {
            int rows = DatabaseInteraction.executeUpdate(sql, id, Timestamp.valueOf(today));
            System.out.println("Deleted " + rows + " future (and today) log medications for medication_id " + id);
            System.out.println(getPendingLogMedications(id));
        } catch (SQLException e) {
            System.err.println("Error deleting future log medications: " + e.getMessage());
        }

    }


    //===========================
    //==== INSERT OPERATIONS ====
    //===========================

    public boolean insertBatchLogMedications(List<LogMedication> logs) throws SQLException {
        String sql = "INSERT INTO log_medications (medication_id, date_time, taken) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseInteraction.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (LogMedication log : logs) {
                // Convert LocalDateTime to java.sql.Timestamp for proper database storage
                java.sql.Timestamp dateTime = java.sql.Timestamp.valueOf(log.getDateAndTime());
                
                stmt.setInt(1, log.getMedication_id());
                stmt.setTimestamp(2, dateTime);
                stmt.setBoolean(3, log.isTaken());
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            return results.length == logs.size();
        }
    }



    //===============================
    //==== ADDITIONAL OPERATIONS ====
    //===============================

    private static LogMedication mapResultSetToLogMedication(ResultSet rs) throws SQLException {
        LogMedication log = new LogMedication();
        log.setId(rs.getInt("id"));
        log.setMedication_id(rs.getInt("medication_id"));
        
        // Handle timestamp more robustly
        LocalDateTime dateTime;
        try {
            // Try getting as Timestamp first
            java.sql.Timestamp timestamp = rs.getTimestamp("date_time");
            dateTime = (timestamp != null) ? timestamp.toLocalDateTime() : LocalDateTime.now();
        } catch (SQLException e) {
            // If that fails, try getting as String and parse
            try {
                String dateTimeStr = rs.getString("date_time");
                dateTime = (dateTimeStr != null && !dateTimeStr.isEmpty()) ? 
                          LocalDateTime.parse(dateTimeStr) : LocalDateTime.now();
            } catch (Exception ex) {
                System.err.println("Warning: Could not parse date_time, using current time");
                dateTime = LocalDateTime.now();
            }
        }
        
        log.setDateAndTime(dateTime);
        log.setTaken(rs.getBoolean("taken"));
        return log;
    }

    public boolean markAsTaken(int logId) throws SQLException {
        String sql = "UPDATE log_medications SET taken = 1 WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, logId);
        return rows > 0;
    }

}