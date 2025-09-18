package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.GlucoseMeasurement;

public class GlucoseMeasurementDAO {

    public GlucoseMeasurement getGlucoseMeasurementById(int id) throws SQLException {
        String sql = "SELECT * FROM glucose_measurements WHERE id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToGlucoseMeasurement(rs);
            }
        }
        return null;
    }

    public static List<GlucoseMeasurement> getGlucoseMeasurementsByPatientId(int patientId) throws SQLException {
        String sql = "SELECT * FROM glucose_measurements WHERE patient_id = ? ORDER BY measurement_time DESC";
        List<GlucoseMeasurement> measurements = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            while (rs.next()) {
                measurements.add(mapResultSetToGlucoseMeasurement(rs));
            }
        }
        return measurements;
    }

    public List<GlucoseMeasurement> getGlucoseMeasurementsByDateRange(int patientId, LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        String sql = "SELECT * FROM glucose_measurements WHERE patient_id = ? AND measurement_time BETWEEN ? AND ? ORDER BY measurement_time DESC";
        List<GlucoseMeasurement> measurements = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId, startDate, endDate)) {
            while (rs.next()) {
                measurements.add(mapResultSetToGlucoseMeasurement(rs));
            }
        }
        return measurements;
    }

    public GlucoseMeasurement getLatestGlucoseMeasurement(int patientId) throws SQLException {
        String sql = "SELECT * FROM glucose_measurements WHERE patient_id = ? ORDER BY measurement_time DESC LIMIT 1";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            if (rs.next()) {
                return mapResultSetToGlucoseMeasurement(rs);
            }
        }
        return null;
    }

    public double getAverageGlucoseLevel(int patientId, int days) throws SQLException {
        String sql = "SELECT AVG(value) as avg_glucose FROM glucose_measurements WHERE patient_id = ? AND measurement_time >= datetime('now', '-' || ? || ' days')";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId, days)) {
            if (rs.next()) {
                return rs.getDouble("avg_glucose");
            }
        }
        return 0.0;
    }

    public boolean insertGlucoseMeasurement(GlucoseMeasurement measurement) throws SQLException {
        String sql = "INSERT INTO glucose_measurements (patient_id, value, measurement_time, type, notes) VALUES (?, ?, ?, ?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql,
                measurement.getPatientId(), 
                (int) measurement.getGlucoseLevel(), 
                java.sql.Timestamp.valueOf(measurement.getDateAndTime()),
                measurement.getType(),
                measurement.getNotes());
        return rows > 0;
    }

    public boolean updateGlucoseMeasurement(GlucoseMeasurement measurement) throws SQLException {
        String sql = "UPDATE glucose_measurements SET patient_id=?, value=?, measurement_time=?, type=?, notes=? WHERE id=?";
        int rows = DatabaseInteraction.executeUpdate(sql,
                measurement.getPatientId(), 
                (int) measurement.getGlucoseLevel(), 
                java.sql.Timestamp.valueOf(measurement.getDateAndTime()),
                measurement.getType(),
                measurement.getNotes(),
                measurement.getId());
        return rows > 0;
    }

    public boolean deleteGlucoseMeasurement(int id) throws SQLException {
        String sql = "DELETE FROM glucose_measurements WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }
    
    public boolean deleteGlucoseMeasurement(int patientId, LocalDateTime dateTime, float value) throws SQLException {
        String sql = "DELETE FROM glucose_measurements WHERE patient_id = ? AND measurement_time = ? AND value = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, patientId, java.sql.Timestamp.valueOf(dateTime), (int) value);
        return rows > 0;
    }
    
    public GlucoseMeasurement findGlucoseMeasurement(int patientId, LocalDateTime dateTime, float value) throws SQLException {
        String sql = "SELECT * FROM glucose_measurements WHERE patient_id = ? AND measurement_time = ? AND value = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId, java.sql.Timestamp.valueOf(dateTime), (int) value)) {
            if (rs.next()) {
                return mapResultSetToGlucoseMeasurement(rs);
            }
        }
        return null;
    }

    public List<GlucoseMeasurement> getHighGlucoseReadings(int patientId, int threshold) throws SQLException {
        String sql = "SELECT * FROM glucose_measurements WHERE patient_id = ? AND value > ? ORDER BY measurement_time DESC";
        List<GlucoseMeasurement> measurements = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId, threshold)) {
            while (rs.next()) {
                measurements.add(mapResultSetToGlucoseMeasurement(rs));
            }
        }
        return measurements;
    }

    private static GlucoseMeasurement mapResultSetToGlucoseMeasurement(ResultSet rs) throws SQLException {
        GlucoseMeasurement measurement = new GlucoseMeasurement();
        measurement.setId(rs.getInt("id"));
        measurement.setPatientId(rs.getInt("patient_id"));
        measurement.setGlucoseLevel(rs.getFloat("value"));
        measurement.setDateAndTime(rs.getTimestamp("measurement_time").toLocalDateTime());
        
        // Handle type column - default to "Before Breakfast" if null or missing
        try {
            String type = rs.getString("type");
            measurement.setType(type != null ? type : "Before Breakfast");
        } catch (SQLException e) {
            measurement.setType("Before Breakfast");
        }
        
        // Handle notes column - default to empty string if null or missing
        try {
            String notes = rs.getString("notes");
            measurement.setNotes(notes != null ? notes : "");
        } catch (SQLException e) {
            measurement.setNotes("");
        }
        
        return measurement;
    }


    public GlucoseMeasurement getLatestMeasurementByPatientId(int patientId) throws SQLException {
        String sql = "SELECT * FROM glucose_measurements WHERE patient_id = ? ORDER BY measurement_time DESC LIMIT 1";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, patientId)) {
            if (rs.next()) {
                return mapResultSetToGlucoseMeasurement(rs);
            }
        }
        return null;
    }
}