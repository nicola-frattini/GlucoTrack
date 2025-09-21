package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.Admin;
import it.glucotrack.model.Doctor;
import it.glucotrack.model.Gender;

public class AdminDAO {

    public static Admin getAdminById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND type = 'ADMIN'";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToAdmin(rs);
            }
        }
        return null;
    }

    public boolean insertAdmin(Admin admin) throws SQLException {
        String sql = "INSERT INTO users (name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ADMIN', ?)";
        int rows = DatabaseInteraction.executeUpdate(sql,
                admin.getName(), admin.getSurname(), admin.getEmail(), admin.getPassword(),
                admin.getBornDate(), admin.getGender().toString(), admin.getPhone(),
                admin.getBirthPlace(), admin.getFiscalCode(), admin.getRole());
        return rows > 0;
    }


    public List<Admin> getAllAdmins() throws SQLException {
        String sql = "SELECT * FROM users WHERE type = 'ADMIN' ORDER BY surname, name";
        List<Admin> admins = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                admins.add(mapResultSetToAdmin(rs));
            }
        }
        return admins;
    }

    public Admin getAdminByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND type = 'ADMIN'";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, email)) {
            if (rs.next()) {
                return mapResultSetToAdmin(rs);
            }
        }
        return null;
    }

    public List<Admin> getAdminsByRole(String role) throws SQLException {
        String sql = "SELECT * FROM users WHERE type = 'ADMIN' AND role = ? ORDER BY surname, name";
        List<Admin> admins = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, role)) {
            while (rs.next()) {
                admins.add(mapResultSetToAdmin(rs));
            }
        }
        return admins;
    }



    public static boolean updateAdmin(Admin admin) throws SQLException {
        String sql = "UPDATE users SET name=?, surname=?, email=?, password=?, born_date=?, gender=?, phone=?, birth_place=?, fiscal_code=?, role=? WHERE id=? AND type='ADMIN'";
        int rows = DatabaseInteraction.executeUpdate(sql,
                admin.getName(), admin.getSurname(), admin.getEmail(), admin.getPassword(),
                admin.getBornDate(), admin.getGender().toString(), admin.getPhone(),
                admin.getBirthPlace(), admin.getFiscalCode(), admin.getRole(), admin.getId());
        return rows > 0;
    }

    public boolean deleteAdmin(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ? AND type = 'ADMIN'";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }

    public List<Admin> searchAdmins(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM users WHERE type = 'ADMIN' AND (name LIKE ? OR surname LIKE ? OR email LIKE ? OR role LIKE ?) ORDER BY surname, name";
        String searchPattern = "%" + searchTerm + "%";
        List<Admin> admins = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, searchPattern, searchPattern, searchPattern, searchPattern)) {
            while (rs.next()) {
                admins.add(mapResultSetToAdmin(rs));
            }
        }
        return admins;
    }

    // Gestione delle relazioni admin-utenti gestiti
    public boolean addManagedUser(int adminId, int userId) throws SQLException {
        String sql = "INSERT INTO admin_managed_users (admin_id, user_id) VALUES (?, ?)";
        int rows = DatabaseInteraction.executeUpdate(sql, adminId, userId);
        return rows > 0;
    }

    public boolean removeManagedUser(int adminId, int userId) throws SQLException {
        String sql = "DELETE FROM admin_managed_users WHERE admin_id = ? AND user_id = ?";
        int rows = DatabaseInteraction.executeUpdate(sql, adminId, userId);
        return rows > 0;
    }

    public List<Integer> getManagedUserIds(int adminId) throws SQLException {
        String sql = "SELECT user_id FROM admin_managed_users WHERE admin_id = ?";
        List<Integer> userIds = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, adminId)) {
            while (rs.next()) {
                userIds.add(rs.getInt("user_id"));
            }
        }
        return userIds;
    }

    public int getManagedUserCount(int adminId) throws SQLException {
        String sql = "SELECT COUNT(*) as user_count FROM admin_managed_users WHERE admin_id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, adminId)) {
            if (rs.next()) {
                return rs.getInt("user_count");
            }
        }
        return 0;
    }

    public List<String> getUniqueRoles() throws SQLException {
        String sql = "SELECT DISTINCT role FROM users WHERE type = 'ADMIN' AND role IS NOT NULL ORDER BY role";
        List<String> roles = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(rs.getString("role"));
            }
        }
        return roles;
    }

    private static Admin mapResultSetToAdmin(ResultSet rs) throws SQLException {
        return new Admin(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("surname"),
            rs.getString("email"),
            rs.getString("password"),
            rs.getDate("born_date").toLocalDate(),
            Gender.valueOf(rs.getString("gender")),
            rs.getString("phone"),
            rs.getString("birth_place"),
            rs.getString("fiscal_code"),
            rs.getString("role")
        );
    }
}