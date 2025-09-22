package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.Doctor;
import it.glucotrack.model.Gender;

/*
* DOCTOR DAO
*/

public class DoctorDAO {


    //========================
    //==== GET OPERATIONS ====
    //========================

    public static Doctor getDoctorById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND type = 'DOCTOR'";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToDoctor(rs);
            }
        }
        return null;
    }

    public static List<Doctor> getAllDoctors() throws SQLException {
        String sql = "SELECT * FROM users WHERE type = 'DOCTOR' ORDER BY surname, name";
        List<Doctor> doctors = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                doctors.add(mapResultSetToDoctor(rs));
            }
        }
        return doctors;
    }

    public Doctor getDoctorByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND type = 'DOCTOR'";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, email)) {
            if (rs.next()) {
                return mapResultSetToDoctor(rs);
            }
        }
        return null;
    }

    public List<Doctor> getDoctorsBySpecialization(String specialization) throws SQLException {
        String sql = "SELECT * FROM users WHERE type = 'DOCTOR' AND specialization = ? ORDER BY surname, name";
        List<Doctor> doctors = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, specialization)) {
            while (rs.next()) {
                doctors.add(mapResultSetToDoctor(rs));
            }
        }
        return doctors;
    }

    public List<Doctor> searchDoctors(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM users WHERE type = 'DOCTOR' AND (name LIKE ? OR surname LIKE ? OR email LIKE ? OR specialization LIKE ?) ORDER BY surname, name";
        String searchPattern = "%" + searchTerm + "%";
        List<Doctor> doctors = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, searchPattern, searchPattern, searchPattern, searchPattern)) {
            while (rs.next()) {
                doctors.add(mapResultSetToDoctor(rs));
            }
        }
        return doctors;
    }

    public int getPatientCountByDoctorId(int doctorId) throws SQLException {
        String sql = "SELECT COUNT(*) as patient_count FROM users WHERE type = 'PATIENT' AND doctor_id = ?";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, doctorId)) {
            if (rs.next()) {
                return rs.getInt("patient_count");
            }
        }
        return 0;
    }

    public List<String> getUniqueSpecializations() throws SQLException {
        String sql = "SELECT DISTINCT specialization FROM users WHERE type = 'DOCTOR' AND specialization IS NOT NULL ORDER BY specialization";
        List<String> specializations = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                specializations.add(rs.getString("specialization"));
            }
        }
        return specializations;
    }



    //===========================
    //==== INSERT OPERATIONS ====
    //===========================

    public boolean insertDoctor(Doctor doctor) throws SQLException {

        String sql = "INSERT INTO users (name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, specialization) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'DOCTOR', ?)";
        int rows = DatabaseInteraction.executeUpdate(sql,
                doctor.getName(), doctor.getSurname(), doctor.getEmail(), PasswordUtils.encryptPassword(doctor.getPassword(), doctor.getEmail()),
                doctor.getBornDate(), doctor.getGender().toString(), doctor.getPhone(),
                doctor.getBirthPlace(), doctor.getFiscalCode(), doctor.getSpecialization());
        return rows > 0;
    }


    //===========================
    //==== UPDATE OPERATIONS ====
    //===========================

    public static boolean updateDoctor(Doctor doctor) throws SQLException {
        String sql = "UPDATE users SET name=?, surname=?, email=?, password=?, born_date=?, gender=?, phone=?, birth_place=?, fiscal_code=?, specialization=? WHERE id=? AND type='DOCTOR'";
        int rows = DatabaseInteraction.executeUpdate(sql,
                doctor.getName(), doctor.getSurname(), doctor.getEmail(), PasswordUtils.encryptPassword(doctor.getPassword(), doctor.getEmail()),
                doctor.getBornDate(), doctor.getGender().toString(), doctor.getPhone(),
                doctor.getBirthPlace(), doctor.getFiscalCode(), doctor.getSpecialization(), doctor.getId());
        return rows > 0;
    }


    //===========================
    //==== DELETE OPERATIONS ====
    //===========================

    public boolean deleteDoctor(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ? AND type = 'DOCTOR'";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }


    //===============================
    //==== ADDITIONAL OPERATIONS ====
    //===============================

    private static Doctor mapResultSetToDoctor(ResultSet rs) throws SQLException {
        return new Doctor(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("surname"),
            rs.getString("email"),
            PasswordUtils.decryptPassword(rs.getString("password"), rs.getString("email")),
                java.time.LocalDate.parse(rs.getString("born_date")),
            Gender.valueOf(rs.getString("gender").toUpperCase()),
            rs.getString("phone"),
            rs.getString("birth_place"),
            rs.getString("fiscal_code"),
            rs.getString("specialization")
        );
    }
}