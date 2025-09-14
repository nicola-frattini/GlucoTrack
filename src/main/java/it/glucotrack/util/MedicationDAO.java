package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.Frequency;
import it.glucotrack.model.Medication;

public class MedicationDAO {

    public Medication getMedicationById(int id) throws SQLException {
        String sql = "SELECT * FROM medications WHERE id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToMedication(rs);
            }
        }
        return null;
    }

    public List<Medication> getMedicationsByPatientId(int patientId) throws SQLException {
        String sql = "SELECT * FROM medications WHERE patient_id = ?";
        List<Medication> meds = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            while (rs.next()) {
                meds.add(mapResultSetToMedication(rs));
            }
        }
        return meds;
    }

    public List<Medication> getActiveMedicationsByPatientId(int patientId) throws SQLException {
        String sql = "SELECT * FROM medications WHERE patient_id = ? AND end_date >= ?";
        List<Medication> meds = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId, LocalDate.now())) {
            while (rs.next()) {
                meds.add(mapResultSetToMedication(rs));
            }
        }
        return meds;
    }

    public List<Medication> getAllMedications() throws SQLException {
        String sql = "SELECT * FROM medications ORDER BY name";
        List<Medication> meds = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                meds.add(mapResultSetToMedication(rs));
            }
        }
        return meds;
    }

   public boolean insertMedication(Medication med) throws SQLException {
    String sql = "INSERT INTO medications (patient_id, name, dose, frequency, start_date, end_date, instructions) VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    // Convert LocalDate to java.sql.Date for proper database storage
    java.sql.Date startDate = java.sql.Date.valueOf(med.getStart_date());
    java.sql.Date endDate = med.getEnd_date() != null ? java.sql.Date.valueOf(med.getEnd_date()) : null;
    
    int rows = DatabaseInteraction.executeUpdate(sql,
            med.getPatient_id(), 
            med.getName_medication(), 
            med.getDose(), 
            med.getFreq().name(), // Use enum name for consistency
            startDate,           // Use java.sql.Date
            endDate,            // Use java.sql.Date (can be null)
            med.getInstructions());
    return rows > 0;
}

    public int insertMedicationAndGetId(Medication med) throws SQLException {
        String sql = "INSERT INTO medications (patient_id, name, dose, frequency, start_date, end_date, instructions) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        // Convert LocalDate to java.sql.Date for proper database storage
        java.sql.Date startDate = java.sql.Date.valueOf(med.getStart_date());
        java.sql.Date endDate = med.getEnd_date() != null ? java.sql.Date.valueOf(med.getEnd_date()) : null;
        
        try (java.sql.Connection conn = DatabaseInteraction.connect();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, med.getPatient_id());
            pstmt.setString(2, med.getName_medication());
            pstmt.setString(3, med.getDose());
            pstmt.setString(4, med.getFreq().name());
            pstmt.setDate(5, startDate);
            pstmt.setDate(6, endDate);
            pstmt.setString(7, med.getInstructions());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating medication failed, no rows affected.");
            }
            
            // Get the last inserted row ID using SQLite's last_insert_rowid()
            String getIdSql = "SELECT last_insert_rowid()";
            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(getIdSql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Creating medication failed, no ID obtained.");
                }
            }
        }
    }

public boolean updateMedication(Medication med) throws SQLException {
    String sql = "UPDATE medications SET patient_id=?, name=?, dose=?, frequency=?, start_date=?, end_date=?, instructions=? WHERE id=?";
    
    // Convert LocalDate to java.sql.Date for proper database storage
    java.sql.Date startDate = java.sql.Date.valueOf(med.getStart_date());
    java.sql.Date endDate = med.getEnd_date() != null ? java.sql.Date.valueOf(med.getEnd_date()) : null;
    
    int rows = DatabaseInteraction.executeUpdate(sql,
            med.getPatient_id(), 
            med.getName_medication(), 
            med.getDose(), 
            med.getFreq().name(), // Use enum name for consistency
            startDate,           // Use java.sql.Date
            endDate,            // Use java.sql.Date (can be null)
            med.getInstructions(), 
            med.getId());
    return rows > 0;
}

    public boolean deleteMedication(int id) throws SQLException {
        String sql = "DELETE FROM medications WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }

    private Medication mapResultSetToMedication(ResultSet rs) throws SQLException {
    String frequencyStr = rs.getString("frequency");
    
    // Try to parse as enum name first, then as display name for backward compatibility
    Frequency frequency;
    try {
        frequency = Frequency.valueOf(frequencyStr);
    } catch (IllegalArgumentException e) {
        // If that fails, try parsing as display name
        frequency = Frequency.fromString(frequencyStr);
    }
    
    // Handle dates more robustly
    LocalDate startDate;
    LocalDate endDate;
    
    try {
        // Try getting as Date first
        java.sql.Date startDateSql = rs.getDate("start_date");
        startDate = (startDateSql != null) ? startDateSql.toLocalDate() : LocalDate.now();
    } catch (SQLException e) {
        // If that fails, try getting as String and parse
        try {
            String startDateStr = rs.getString("start_date");
            startDate = (startDateStr != null && !startDateStr.isEmpty()) ? 
                       LocalDate.parse(startDateStr) : LocalDate.now();
        } catch (Exception ex) {
            System.err.println("Warning: Could not parse start_date, using current date");
            startDate = LocalDate.now();
        }
    }
    
    try {
        // Try getting as Date first
        java.sql.Date endDateSql = rs.getDate("end_date");
        endDate = (endDateSql != null) ? endDateSql.toLocalDate() : LocalDate.now().plusMonths(1);
    } catch (SQLException e) {
        // If that fails, try getting as String and parse
        try {
            String endDateStr = rs.getString("end_date");
            endDate = (endDateStr != null && !endDateStr.isEmpty()) ? 
                     LocalDate.parse(endDateStr) : LocalDate.now().plusMonths(1);
        } catch (Exception ex) {
            System.err.println("Warning: Could not parse end_date, using current date + 1 month");
            endDate = LocalDate.now().plusMonths(1);
        }
    }
    
    return new Medication(
        rs.getInt("id"),
        rs.getInt("patient_id"),
        rs.getString("name"),
        rs.getString("dose"),
        frequency,
        startDate,
        endDate,
        rs.getString("instructions")
    );
}
}