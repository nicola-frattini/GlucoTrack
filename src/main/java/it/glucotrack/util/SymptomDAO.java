package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
}