
package it.glucotrack.util;

import it.glucotrack.model.User;
import it.glucotrack.model.Gender;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
* USER DAO
* Data Access Object for User entity
* Handles all database operations related to users
*/


public class UserDAO {

    //================================
    //==== GENERIC GET OPERATIONS ====
    //================================
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

    //=============================
    //==== SPECIFIC OPERATIONS ====
    //=============================

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
    //===========================
    //==== UPDATE OPERATIONS ====
    //===========================

    public boolean updateEmail(int userId, String newEmail) throws SQLException {
        String sql = "UPDATE users SET email = ? WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, newEmail, userId);
        return rows > 0;
    }

    public boolean updatePassword(int userId, String email, String newPassword) throws SQLException {
        // Hash the new password before storing it
        String encPsw = PasswordUtils.encryptPassword(newPassword, email);
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, encPsw, userId);
        return rows > 0;
    }

    public boolean updateUser(User user) throws SQLException {
        //Update the user data by id
        String sql = "UPDATE users SET name=?, surname=?, email=?, password=?, born_date=?, gender=?, phone=?, birth_place=?, fiscal_code=?, WHERE id=?";
        int rows = DatabaseInteraction.executeUpdate(sql,
                user.getName(), user.getSurname(), user.getEmail(), PasswordUtils.encryptPassword(user.getPassword(), user.getEmail()),
                user.getBornDate(), user.getGender().toString(), user.getPhone(),
                user.getBirthPlace(), user.getFiscalCode(), user.getId());
        return rows > 0;
    }

    //==========================
    //==== CREATE OPERATION ====
    //==========================
    public boolean createUser(User user, String plainPassword) throws SQLException {
        String sql = "INSERT INTO users (name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // First insert the user to get the generated ID
        String insertSql = "INSERT INTO users (name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role, specialization, doctor_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Use a temporary password first, then update with hashed version
        int rows = DatabaseInteraction.executeUpdate(insertSql,
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                "temp", // Temporary password
                user.getBornDate(),
                user.getGender().toString(),
                user.getPhone(),
                user.getBirthPlace(),
                user.getFiscalCode(),
                user.getType(),
                null, // specialization
                null  // doctor_id
        );


        if (rows > 0) {

            // Get the newly created user to get the ID
            User newUser = getUserByEmail(user.getEmail());
            if (newUser != null) {

                // Now update with the properly hashed password
                return updatePassword(newUser.getId(), newUser.getEmail(), plainPassword);
            }
        }

        return false;
    }

    //==========================
    //==== DELETE OPERATION ====
    //==========================

    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }

    //===============================
    //==== ADDITIONAL OPERATIONS ====
    //===============================

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

        // Handle potentially problematic born_date parsing
        LocalDate bornDate = null;
        try {
            Object dateObj = rs.getObject("born_date");

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
            System.err.println("Error parsing born_date: " + e.getMessage() + ". Setting to null.");
            bornDate = null;
        }

        // Map the ResultSet to a User object
        return new User(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("surname"),
                rs.getString("email"),
                PasswordUtils.decryptPassword(rs.getString("password"), rs.getString("email")),
                bornDate,
                Gender.valueOf(rs.getString("gender").toUpperCase()),
                rs.getString("phone"),
                rs.getString("birth_place"),
                rs.getString("fiscal_code"), rs.getString("type")
        );
    }
}
