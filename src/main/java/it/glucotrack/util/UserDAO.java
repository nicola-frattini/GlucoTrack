
package it.glucotrack.util;

import it.glucotrack.model.User;
import it.glucotrack.model.Gender;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDAO{

    public List<User> getPatientsByDoctorId(int doctorId) throws SQLException {
        String sql = "SELECT id, name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id FROM users WHERE type = 'PATIENT' AND doctor_id = ? ORDER BY surname, name";
        List<User> users = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, doctorId)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    // Metodi generici per tutti i tipi di utenti
    public User getUserById(int id) throws SQLException {
        String sql = "SELECT id, name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id FROM users WHERE id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    public User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT id, name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id FROM users WHERE email = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, email)) {
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    public User authenticateUser(String email, String password) throws SQLException {
        System.out.println("🔍 Attempting authentication for: " + email);
        String sql = "SELECT id, name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id FROM users WHERE email = ? AND password = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, email, password)) {
            if (rs.next()) {
                System.out.println("✅ User found: " + rs.getString("name") + " " + rs.getString("surname") + " (" + rs.getString("type") + ")");
                return mapResultSetToUser(rs);
            } else {
                System.out.println("❌ No user found with email: " + email + " and provided password");
                // Let's check if user exists with different password
                String checkSql = "SELECT email, password FROM users WHERE email = ?";
                try (ResultSet checkRs = DatabaseInteraction.executeQuery(checkSql, email)) {
                    if (checkRs.next()) {
                        System.out.println("📧 User exists but password mismatch. Stored password: " + checkRs.getString("password"));
                    } else {
                        System.out.println("📧 User with email " + email + " does not exist");
                    }
                }
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT id, name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id FROM users ORDER BY type, surname, name";
        List<User> users = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    public List<User> getUsersByType(String type) throws SQLException {
        String sql = "SELECT id, name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id FROM users WHERE type = ? ORDER BY surname, name";
        List<User> users = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, type)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM users WHERE email = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, email)) {
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }

    public boolean fiscalCodeExists(String fiscalCode) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM users WHERE fiscal_code = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, fiscalCode)) {
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }

    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, newPassword, userId);
        return rows > 0;
    }

    public boolean updateEmail(int userId, String newEmail) throws SQLException {
        String sql = "UPDATE users SET email = ? WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, newEmail, userId);
        return rows > 0;
    }

    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }

    public List<User> searchUsers(String searchTerm) throws SQLException {
        String sql = "SELECT id, name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id FROM users WHERE name LIKE ? OR surname LIKE ? OR email LIKE ? ORDER BY type, surname, name";
        String searchPattern = "%" + searchTerm + "%";
        List<User> users = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, searchPattern, searchPattern, searchPattern)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    public int getUserCountByType(String type) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM users WHERE type = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, type)) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    public List<User> getRecentUsers(int limit) throws SQLException {
        String sql = "SELECT id, name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id FROM users ORDER BY id DESC LIMIT ?";
        List<User> users = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, limit)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        System.out.println("🔍 Mapping user: " + rs.getString("name") + " " + rs.getString("surname"));
        
        // Handle potentially problematic born_date parsing
        LocalDate bornDate = null;
        try {
            Object dateObj = rs.getObject("born_date");
            System.out.println("🔍 born_date raw value: " + dateObj + " (type: " + (dateObj != null ? dateObj.getClass().getSimpleName() : "null") + ")");
            
            if (dateObj != null) {
                if (dateObj instanceof String) {
                    // If it's a string, try to parse it
                    String dateStr = (String) dateObj;
                    bornDate = LocalDate.parse(dateStr);
                } else {
                    // If it's already a Date, convert it
                    java.sql.Date sqlDate = rs.getDate("born_date");
                    bornDate = sqlDate.toLocalDate();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error parsing born_date: " + e.getMessage() + ". Setting to null.");
            bornDate = null;
        }
        
        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("surname"),
            rs.getString("email"),
            rs.getString("password"),
            bornDate,
            Gender.valueOf(rs.getString("gender").toUpperCase()),
            rs.getString("phone"),
            rs.getString("birth_place"),
            rs.getString("fiscal_code"), rs.getString("type")
        );
    }
}