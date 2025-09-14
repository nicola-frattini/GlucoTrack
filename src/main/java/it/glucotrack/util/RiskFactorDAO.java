package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.Gravity;
import it.glucotrack.model.RiskFactor;

public class RiskFactorDAO {

    public List<RiskFactor> getRiskFactorsByPatientId(int patientId) throws SQLException {
        String sql = "SELECT * FROM risk_factors WHERE patient_id = ? ORDER BY detected_date DESC";
        List<RiskFactor> riskFactors = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            while (rs.next()) {
                riskFactors.add(mapResultSetToRiskFactor(rs));
            }
        }
        return riskFactors;
    }

    public RiskFactor getRiskFactorById(int id) throws SQLException {
        String sql = "SELECT * FROM risk_factors WHERE id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToRiskFactor(rs);
            }
        }
        return null;
    }

    public boolean insertRiskFactor(RiskFactor riskFactor) throws SQLException {
        String sql = "INSERT INTO risk_factors (type, gravity) VALUES (?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql, riskFactor.getType(), riskFactor.getGravity().toString());
        return rows > 0;
    }
    
    public boolean insertRiskFactor(String type, Gravity gravity) throws SQLException {
        String sql = "INSERT INTO risk_factors (type, gravity) VALUES (?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql, type, gravity.toString());
        return rows > 0;
    }

    public boolean updateRiskFactor(int id, int patientId, String factor, String description, LocalDate detectedDate) throws SQLException {
        String sql = "UPDATE risk_factors SET patient_id=?, factor=?, description=?, detected_date=? WHERE id=?";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId, factor, description, detectedDate, id);
        return rows > 0;
    }

    public boolean deleteRiskFactor(int id) throws SQLException {
        String sql = "DELETE FROM risk_factors WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }

    public boolean deleteRiskFactorsByPatientId(int patientId) throws SQLException {
        String sql = "DELETE FROM risk_factors WHERE patient_id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId);
        return rows > 0;
    }

    public List<String> getUniqueRiskFactors() throws SQLException {
        String sql = "SELECT DISTINCT factor FROM risk_factors ORDER BY factor";
        List<String> factors = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                factors.add(rs.getString("factor"));
            }
        }
        return factors;
    }

    private RiskFactor mapResultSetToRiskFactor(ResultSet rs) throws SQLException {
        // Mappa i campi del DB alla classe RiskFactor esistente
        // Nota: c'è un disallineamento tra schema DB e classe Java
        RiskFactor riskFactor = new RiskFactor();
        riskFactor.setId(rs.getInt("id"));
        riskFactor.setType(rs.getString("factor")); // factor -> type
        
        // La classe RiskFactor non ha gravity nel DB, usa un default
        String description = rs.getString("description");
        if (description != null && description.toLowerCase().contains("high")) {
            riskFactor.setGravity(Gravity.HIGH);
        } else if (description != null && description.toLowerCase().contains("medium")) {
            riskFactor.setGravity(Gravity.MEDIUM);
        } else {
            riskFactor.setGravity(Gravity.LOW);
        }
        
        return riskFactor;
    }
}