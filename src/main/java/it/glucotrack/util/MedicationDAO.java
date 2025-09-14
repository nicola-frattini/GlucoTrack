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
        int rows = DatabaseInteraction.executeUpdate(sql,
                med.getPatient_id(), med.getName_medication(), med.getDose(), med.getFreq().name(),
                med.getStart_date(), med.getEnd_date(), med.getInstructions());
        return rows > 0;
    }

    public boolean updateMedication(Medication med) throws SQLException {
        String sql = "UPDATE medications SET patient_id=?, name=?, dose=?, frequency=?, start_date=?, end_date=?, instructions=? WHERE id=?";
        int rows = DatabaseInteraction.executeUpdate(sql,
                med.getPatient_id(), med.getName_medication(), med.getDose(), med.getFreq().name(),
                med.getStart_date(), med.getEnd_date(), med.getInstructions(), med.getId());
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
        
        // Handle potentially null dates
        java.sql.Date startDateSql = rs.getDate("start_date");
        java.sql.Date endDateSql = rs.getDate("end_date");
        
        LocalDate startDate = (startDateSql != null) ? startDateSql.toLocalDate() : LocalDate.now();
        LocalDate endDate = (endDateSql != null) ? endDateSql.toLocalDate() : LocalDate.now().plusMonths(1);
        
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