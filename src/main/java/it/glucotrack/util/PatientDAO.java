package it.glucotrack.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.model.Admin;
import it.glucotrack.model.Gender;
import it.glucotrack.model.Patient;

/*
* PATIENT DAO
*/


public class PatientDAO {

    //========================
    //==== GET OPERATIONS ====
    //========================

    public static Patient getPatientById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND type = 'PATIENT'";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, id)) {
            if (rs.next()) {
                return mapResultSetToPatient(rs);
            }
        }
        return null;
    }

    public static List<Patient> getAllPatients() throws SQLException {

        String sql = "SELECT * FROM users WHERE type = 'PATIENT' ORDER BY surname, name";
        List<Patient> patients = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            while (rs.next()) {
                patients.add(mapResultSetToPatient(rs));
            }
        }
        return patients;
    }

    public static List<Patient> getPatientsByDoctorId(int doctorId) throws SQLException {
        String sql = "SELECT * FROM users WHERE type = 'PATIENT' AND doctor_id = ? ORDER BY surname, name";
        List<Patient> patients = new ArrayList<>();


        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, doctorId)) {

            int count = 0;
            while (rs.next()) {
                count++;
                patients.add(mapResultSetToPatient(rs));
            }
        }

        return patients;
    }

    public Patient getPatientByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND type = 'PATIENT'";
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, email)) {
            if (rs.next()) {
                return mapResultSetToPatient(rs);
            }
        }
        return null;
    }


    //===========================
    //==== INSERT OPERATIONS ====
    //===========================

    public boolean insertPatient(Patient patient) throws SQLException {
        String sql = "INSERT INTO users (name, surname, email, password, born_date, gender, phone, birth_place, fiscal_code, type, doctor_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PATIENT', ?)";
        int rows = DatabaseInteraction.executeUpdate(sql,
                patient.getName(), patient.getSurname(), patient.getEmail(), PasswordUtils.encryptPassword(patient.getPassword(), patient.getEmail()),
                patient.getBornDate(), patient.getGender().toString(), patient.getPhone(),
                patient.getBirthPlace(), patient.getFiscalCode(), patient.getDoctorId());
        System.out.println("PASSWORD CRIPTATA: " +  PasswordUtils.encryptPassword(patient.getPassword(), patient.getEmail()));
        return rows > 0;
    }


    //===========================
    //==== UPDATE OPERATIONS ====
    //===========================


    public static boolean updatePatient(Patient patient) throws SQLException {
        String sql = "UPDATE users SET name=?, surname=?, email=?, password=?, born_date=?, gender=?, phone=?, birth_place=?, fiscal_code=?, doctor_id=? WHERE id=? AND type='PATIENT'";
        int rows = DatabaseInteraction.executeUpdate(sql,
                patient.getName(), patient.getSurname(), patient.getEmail(), PasswordUtils.encryptPassword(patient.getPassword(), patient.getEmail()),
                patient.getBornDate(), patient.getGender().toString(), patient.getPhone(),
                patient.getBirthPlace(), patient.getFiscalCode(), patient.getDoctorId(), patient.getId());
        return rows > 0;
    }


    //===========================
    //==== DELETE OPERATIONS ====
    //===========================

    public boolean deletePatient(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ? AND type = 'PATIENT'";
        int rows = DatabaseInteraction.executeUpdate(sql, id);
        return rows > 0;
    }


    //===============================
    //==== ADDITIONAL OPERATIONS ====
    //===============================

    public List<Patient> searchPatients(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM users WHERE type = 'PATIENT' AND (name LIKE ? OR surname LIKE ? OR email LIKE ?) ORDER BY surname, name";
        String searchPattern = "%" + searchTerm + "%";
        List<Patient> patients = new ArrayList<>();
        try (ResultSet rs = DatabaseInteraction.executeQuery(sql, searchPattern, searchPattern, searchPattern)) {
            while (rs.next()) {
                patients.add(mapResultSetToPatient(rs));
            }
        }
        return patients;
    }

    private static Patient mapResultSetToPatient(ResultSet rs) throws SQLException {

        // Parse born_date as string since it's stored as ISO date string in database
        String bornDateStr = rs.getString("born_date");
        java.time.LocalDate bornDate = null;
        if (bornDateStr != null && !bornDateStr.isEmpty()) {
            try {
                bornDate = java.time.LocalDate.parse(bornDateStr);
            } catch (Exception e) {
                System.err.println("Error parsing born_date: " + bornDateStr + " - " + e.getMessage());
                bornDate = java.time.LocalDate.now(); // fallback
            }
        }
        
        // Parse gender with error handling
        Gender gender = Gender.MALE; // default fallback
        String genderStr = rs.getString("gender");
        if (genderStr != null && !genderStr.isEmpty()) {
            try {
                gender = Gender.fromString(genderStr);
            } catch (Exception e) {
                System.err.println("Error parsing gender: " + genderStr + " - " + e.getMessage());
                // Use default MALE as fallback
            }
        }
        
        return new Patient(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("surname"),
            rs.getString("email"),
            PasswordUtils.decryptPassword(rs.getString("password"), rs.getString("email")),
            bornDate,
            gender,
            rs.getString("phone"),
            rs.getString("birth_place"),
            rs.getString("fiscal_code"),
            rs.getInt("doctor_id")
        );
    }
}
