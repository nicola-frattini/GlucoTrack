package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.Gravity;
import it.glucotrack.model.RiskFactor;

/**
 * RISK FACTOR DAO
 */

public class RiskFactorDAO {

    //========================
    //==== GET OPERATIONS ====
    //========================

    public static List<RiskFactor> getRiskFactorsByPatientId(int patientId) throws SQLException {
        String sql = "SELECT * FROM risk_factors WHERE patient_id = ? ORDER BY id DESC";
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

    public List<String> getUniqueRiskFactors() throws SQLException {
        String sql = "SELECT DISTINCT type FROM risk_factors ORDER BY type";
        List<String> factors = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                factors.add(rs.getString("type"));
            }
        }
        return factors;
    }


    //===========================
    //==== INSERT OPERATIONS ====
    //===========================

    public boolean insertRiskFactor(int patientId, RiskFactor riskFactor) throws SQLException {
        String sql = "INSERT INTO risk_factors (patient_id, type, gravity) VALUES (?, ?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId, riskFactor.getType(), riskFactor.getGravity().toString());
        return rows > 0;
    }

    public boolean insertRiskFactor(int patientId, String type, Gravity gravity) throws SQLException {
        String sql = "INSERT INTO risk_factors (patient_id, type, gravity) VALUES (?, ?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId, type, gravity.toString());
        return rows > 0;
    }


    //===========================
    //==== UPDATE OPERATIONS ====
    //===========================

    public boolean updateRiskFactor(int id, int patientId, String type, String description, Gravity gravity) throws SQLException {
        String sql = "UPDATE risk_factors SET patient_id=?, type=?, description=?, gravity=? WHERE id=?";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId, type, description, gravity.toString(), id);
        return rows > 0;
    }


    //===========================
    //==== DELETE OPERATIONS ====
    //===========================

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


    //===============================
    //==== ADDITIONAL OPERATIONS ====
    //===============================


    private static RiskFactor mapResultSetToRiskFactor(ResultSet rs) throws SQLException {

        RiskFactor riskFactor = new RiskFactor();
        riskFactor.setId(rs.getInt("id"));
        riskFactor.setType(rs.getString("type"));


        String gravityStr = rs.getString("gravity");
        if (gravityStr != null) {
            switch (gravityStr.toUpperCase()) {
                case "HIGH": riskFactor.setGravity(Gravity.HIGH); break;
                case "MEDIUM": riskFactor.setGravity(Gravity.MEDIUM); break;
                default: riskFactor.setGravity(Gravity.LOW); break;
            }
        } else {
            riskFactor.setGravity(Gravity.LOW);
        }

        return riskFactor;
    }
}
