package it.glucotrack;

import it.glucotrack.model.*;
import it.glucotrack.util.*;

import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DaoTest {

    private MedicationDAO medicationDAO;
    private int testPatientId;
    private int testDoctorId;

    @BeforeAll
    public void setupDatabase() throws Exception {
        DatabaseInteraction.setDatabasePath("test.db");
        DatabaseInitializer.resetDatabase();
        medicationDAO = new MedicationDAO();

        try {
            List<Medication> meds = medicationDAO.getAllMedications();
            if (!meds.isEmpty()) {
                testPatientId = meds.get(0).getPatient_id();
                testDoctorId = 3; // I dottori mock hanno id 3 e 4
            } else {
                testPatientId = 1;
                testDoctorId = 3;
            }
        } catch (SQLException e) {
            testPatientId = 1;
            testDoctorId = 3;
        }
    }

    // ------------------ USER ------------------
    @Test
    @DisplayName("Test UserDAO")
    void testUserDAO() throws SQLException {
        UserDAO userDAO = new UserDAO();
        List<User> allUsers = userDAO.getAllUsers();
        Assertions.assertFalse(allUsers.isEmpty(), "User list should not be empty");
        User first = allUsers.get(0);
        User byId = userDAO.getUserById(first.getId());
        Assertions.assertEquals(first.getEmail(), byId.getEmail());
        User byEmail = userDAO.getUserByEmail(first.getEmail());
        Assertions.assertEquals(first.getId(), byEmail.getId());
        List<User> patients = userDAO.getUsersByType("PATIENT");
        Assertions.assertFalse(patients.isEmpty());
    }

    // ------------------ PATIENT ------------------
    @Test
    @DisplayName("Test PatientDAO")
    void testPatientDAO() throws SQLException {
        List<Patient> allPatients = PatientDAO.getAllPatients();
        Assertions.assertFalse(allPatients.isEmpty());
        Patient first = allPatients.get(0);
        Patient byId = PatientDAO.getPatientById(first.getId());
        Assertions.assertEquals(first.getEmail(), byId.getEmail());
        List<Patient> byDoctor = PatientDAO.getPatientsByDoctorId(first.getDoctorId());
        Assertions.assertFalse(byDoctor.isEmpty());
    }

    // ------------------ DOCTOR ------------------
    @Test
    @DisplayName("Test DoctorDAO")
    void testDoctorDAO() throws SQLException {
        List<Doctor> allDoctors = DoctorDAO.getAllDoctors();
        Assertions.assertFalse(allDoctors.isEmpty());
        Doctor first = allDoctors.get(0);
        Doctor byId = DoctorDAO.getDoctorById(first.getId());
        Assertions.assertEquals(first.getEmail(), byId.getEmail());
        DoctorDAO dao = new DoctorDAO();
        Doctor byEmail = dao.getDoctorByEmail(first.getEmail());
        Assertions.assertEquals(first.getId(), byEmail.getId());
        List<Doctor> bySpec = dao.getDoctorsBySpecialization(first.getSpecialization());
        Assertions.assertFalse(bySpec.isEmpty());
    }

    // ------------------ ADMIN ------------------
    @Test
    @DisplayName("Test AdminDAO")
    void testAdminDAO() throws SQLException {
        AdminDAO dao = new AdminDAO();
        List<Admin> allAdmins = dao.getAllAdmins();
        Assertions.assertFalse(allAdmins.isEmpty());
        Admin first = allAdmins.get(0);
        Admin byId = AdminDAO.getAdminById(first.getId());
        Assertions.assertEquals(first.getEmail(), byId.getEmail());
        Admin byEmail = dao.getAdminByEmail(first.getEmail());
        Assertions.assertEquals(first.getId(), byEmail.getId());
        List<Admin> byRole = dao.getAdminsByRole(first.getRole());
        Assertions.assertFalse(byRole.isEmpty());
    }

    // ------------------ GLUCOSE ------------------
    @Test
    @DisplayName("Test GlucoseMeasurementDAO")
    void testGlucoseMeasurementDAO() throws SQLException {
        GlucoseMeasurementDAO dao = new GlucoseMeasurementDAO();
        List<GlucoseMeasurement> byPatient = GlucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(testPatientId);
        Assertions.assertFalse(byPatient.isEmpty());
        GlucoseMeasurement first = byPatient.get(0);
        GlucoseMeasurement byId = dao.getGlucoseMeasurementById(first.getId());
        Assertions.assertEquals(first.getGlucoseLevel(), byId.getGlucoseLevel());
        GlucoseMeasurement latest = dao.getLatestMeasurementByPatientId(testPatientId);
        Assertions.assertNotNull(latest);
    }

    // ------------------ LOG MEDICATION ------------------
    @Test
    @DisplayName("Test LogMedicationDAO")
    void testLogMedicationDAO() throws SQLException {
        LogMedicationDAO dao = new LogMedicationDAO();
        List<Medication> meds = medicationDAO.getMedicationsByPatientId(testPatientId);
        Medication med = meds.get(0);
        List<LogMedication> logs = LogMedicationDAO.getLogMedicationsByMedicationId(med.getId());
        Assertions.assertFalse(logs.isEmpty());
        LogMedication first = logs.get(0);
        LogMedication byId = dao.getLogMedicationById(first.getId());
        Assertions.assertEquals(first.getMedication_id(), byId.getMedication_id());
        List<LogMedication> pending = dao.getPendingLogMedications(med.getId());
        Assertions.assertNotNull(pending);
    }

    // ------------------ RISK FACTOR ------------------
    @Test
    @DisplayName("Test RiskFactorDAO")
    void testRiskFactorDAO() throws SQLException {
        RiskFactorDAO dao = new RiskFactorDAO();
        List<RiskFactor> risks = RiskFactorDAO.getRiskFactorsByPatientId(testPatientId);
        Assertions.assertFalse(risks.isEmpty());
        RiskFactor first = risks.get(0);
        RiskFactor byId = dao.getRiskFactorById(first.getId());
        Assertions.assertEquals(first.getType(), byId.getType());
        List<String> unique = dao.getUniqueRiskFactors();
        Assertions.assertFalse(unique.isEmpty());
    }

    // ------------------ SYMPTOM ------------------
    @Test
    @DisplayName("Test SymptomDAO")
    void testSymptomDAO() throws SQLException {
        SymptomDAO dao = new SymptomDAO();
        List<Symptom> symptoms = SymptomDAO.getSymptomsByPatientId(testPatientId);
        Assertions.assertFalse(symptoms.isEmpty());
        Symptom first = symptoms.get(0);
        Assertions.assertNotNull(first.getSymptomName());
        List<String> unique = dao.getUniqueSymptoms();
        Assertions.assertFalse(unique.isEmpty());
    }

    // ------------------ MEDICATION ------------------
    @Test
    @DisplayName("Test getAllMedications")
    void testGetAllMedications() throws SQLException {
        List<Medication> meds = medicationDAO.getAllMedications();
        Assertions.assertFalse(meds.isEmpty(), "Medications should not be empty after mock population");
    }

    @Test
    @DisplayName("Test getMedicationsByPatientId")
    void testGetMedicationsByPatientId() throws SQLException {
        List<Medication> meds = medicationDAO.getMedicationsByPatientId(testPatientId);
        Assertions.assertFalse(meds.isEmpty(), "Patient should have medications");
    }

    @Test
    @DisplayName("Test insertMedication and getMedicationById")
    void testInsertAndGetMedication() throws SQLException {
        Medication med = new Medication(testPatientId, "TestMed", "10mg", Frequency.ONCE_A_DAY,
                LocalDate.now(), LocalDate.now().plusDays(10), "Test instructions");
        boolean inserted = medicationDAO.insertMedication(med, testDoctorId);
        Assertions.assertTrue(inserted, "Medication should be inserted");
        List<Medication> meds = medicationDAO.getMedicationsByPatientId(testPatientId);
        Medication found = meds.stream().filter(m -> "TestMed".equals(m.getName_medication())).findFirst().orElse(null);
        Assertions.assertNotNull(found, "Inserted medication should be found");
        Medication byId = MedicationDAO.getMedicationById(found.getId());
        Assertions.assertEquals("TestMed", byId.getName_medication());
    }

    @Test
    @DisplayName("Test updateMedication")
    void testUpdateMedication() throws SQLException {
        List<Medication> meds = medicationDAO.getMedicationsByPatientId(testPatientId);
        Medication med = meds.get(0);
        med.setDose("20mg");
        boolean updated = medicationDAO.updateMedication(med, testDoctorId);
        Assertions.assertTrue(updated, "Medication should be updated");
        Medication updatedMed = MedicationDAO.getMedicationById(med.getId());
        Assertions.assertEquals("20mg", updatedMed.getDose());
    }

    @Test
    @DisplayName("Test getActiveMedicationsByPatientId")
    void testGetActiveMedicationsByPatientId() throws SQLException {
        List<Medication> activeMeds = medicationDAO.getActiveMedicationsByPatientId(testPatientId);
        Assertions.assertNotNull(activeMeds);
    }

    @Test
    @DisplayName("Test getMedicationEditsByMedicationId")
    void testGetMedicationEditsByMedicationId() throws SQLException {
        List<Medication> meds = medicationDAO.getMedicationsByPatientId(testPatientId);
        Medication med = meds.get(0);
        List<MedicationEdit> edits = MedicationDAO.getMedicationEditsByMedicationId(med.getId());
        Assertions.assertNotNull(edits);
    }

    @Test
    @DisplayName("Test deleteMedication")
    void testDeleteMedication() throws SQLException {
        Medication med = new Medication(testPatientId, "DeleteMed", "5mg", Frequency.ONCE_A_DAY,
                LocalDate.now(), LocalDate.now().plusDays(5), "Delete instructions");
        medicationDAO.insertMedication(med, testDoctorId);
        List<Medication> meds = medicationDAO.getMedicationsByPatientId(testPatientId);
        Medication toDelete = meds.stream().filter(m -> "DeleteMed".equals(m.getName_medication())).findFirst().orElse(null);
        Assertions.assertNotNull(toDelete);
        boolean deleted = MedicationDAO.deleteMedication(toDelete.getId());
        Assertions.assertTrue(deleted, "Medication should be deleted");
    }

    @AfterAll
    public void cleanupDatabase() throws SQLException {
        String[] tables = {
            "medication_edits", "medications", "log_medications", "glucose_measurements",
            "patient_symptoms", "risk_factors", "users"
        };
        for (String table : tables) {
            DatabaseInteraction.executeUpdate("DELETE FROM " + table);
        }
        DatabaseInteraction.setDatabasePath("src/main/resources/database/glucotrack_db.sqlite");
    }
}
