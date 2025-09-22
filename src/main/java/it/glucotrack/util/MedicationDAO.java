package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.Frequency;
import it.glucotrack.model.Medication;
import it.glucotrack.model.MedicationEdit;

/*
* MEDICATION DAO
*/


public class MedicationDAO {

    //========================
    //==== GET OPERATIONS ====
    //========================

    public static Medication getMedicationById(int id) throws SQLException {
        String sql = "SELECT * FROM medications WHERE id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToMedication(rs);
            }
        }
        return null;
    }

    public static List<Medication> getMedicationsByPatientId(int patientId) throws SQLException {
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

    public static List<MedicationEdit> getMedicationEditsByMedicationId(int medicationId) throws SQLException {
        String sql = "SELECT * FROM medication_edits WHERE medication_id = ? ORDER BY edit_time DESC";
        List<MedicationEdit> edits = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, medicationId)) {
            while (rs.next()) {
                edits.add(mapResultSetToMedicationEdit(rs));
            }
        }
        return edits;
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

    public List<MedicationEdit> getAllMedicationEdits() {
        String sql = "SELECT * FROM medication_edits ORDER BY edit_time DESC";
        List<MedicationEdit> edits = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                edits.add(mapResultSetToMedicationEdit(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching medication edits: " + e.getMessage());
        }
        return edits;
    }


    //===========================
    //==== INSERT OPERATIONS ====
    //===========================

    public boolean insertMedication(Medication med, int doctorId) throws SQLException {
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

           createMedicationsEdit(med.getPatient_id(), doctorId, med);
           return rows > 0;

    }

    public int insertMedicationAndGetId(Medication med,int doctorId) throws SQLException {
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
            createMedicationsEdit(med.getPatient_id(), doctorId, med);


            // Get the last inserted row ID using SQLite's last_insert_rowid()
            String getIdSql = "SELECT last_insert_rowid()";
            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(getIdSql)) {
                if (rs.next()) {
                    med=getMedicationById(rs.getInt(1));
                    createMedicationsEdit(med.getPatient_id(), doctorId, med);
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Creating medication failed, no ID obtained.");
                }
            }
        }
    }


    //===========================
    //==== UPDATE OPERATIONS ====
    //===========================

    public boolean updateMedication(Medication med, int doctorId) throws SQLException {
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
        createMedicationsEdit(med.getPatient_id(), doctorId, med);


        return rows > 0;
    }


    //===========================
    //==== DELETE OPERATIONS ====
    //===========================

    public static boolean deleteMedication(int id) throws SQLException {
        String sql = "DELETE FROM medications WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);

        return rows > 0;
    }

    public void deleteMedicationsByPatientId(int patientId) throws SQLException {
        String sql = "DELETE FROM medications WHERE patient_id = ?";
        DatabaseInteraction.executeUpdate(sql, patientId);
    }


    //===========================
    //==== CREATE OPERATIONS ====
    //===========================

    public void createMedicationsEdit(int patientId, int doctorId, Medication med) throws SQLException {

        // Inserisce nel DB i MedicationEdit
        String sql = "INSERT INTO medication_edits (medication_id, edited_by, medication_name, dose, frequency, start_date, end_date, instructions,edit_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // Convert LocalDate to java.sql.Date for proper database storage
        java.sql.Date startDate = java.sql.Date.valueOf(med.getStart_date());
        java.sql.Date endDate = med.getEnd_date() != null ? java.sql.Date.valueOf(med.getEnd_date()) : null;
        DatabaseInteraction.executeUpdate(sql,
                med.getId(),
                doctorId,
                med.getName_medication(),
                med.getDose(),
                med.getFreq().name(), // Use enum name for consistency
                startDate,           // Use java.sql.Date
                endDate,            // Use java.sql.Date (can be null)
                med.getInstructions(),
                java.sql.Timestamp.valueOf(LocalDateTime.now()));

    };


    //===============================
    //==== ADDITIONAL OPERATIONS ====
    //===============================

    private static MedicationEdit mapResultSetToMedicationEdit(ResultSet rs) throws SQLException {

        return new MedicationEdit(
            rs.getInt("id"),
            rs.getInt("medication_id"),
            rs.getInt("edited_by"),
            rs.getString("medication_name"),
            rs.getString("dose"),
            Frequency.valueOf(rs.getString("frequency")), // Assuming frequency is stored as enum name
            rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
            rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null,
            rs.getString("instructions"),
            rs.getTimestamp("edit_time") != null ? rs.getTimestamp("edit_time").toLocalDateTime() : null
        );

    }


    private static Medication mapResultSetToMedication(ResultSet rs) throws SQLException {
        String frequencyStr = rs.getString("frequency");

        // Try to parse as enum name first
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