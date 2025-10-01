package it.glucotrack;

import it.glucotrack.util.DatabaseInteraction;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import it.glucotrack.model.*;

import static org.junit.jupiter.api.Assertions.*;

public class ModelTest {



    // ===== Test User Class =====
    @Nested
    @DisplayName("User Model Tests")
    class UserTest {
        private User user;

        @BeforeEach
        void setUp() {
            user = new User("Mario", "Rossi", "mario.rossi@email.com", "password123",
                    LocalDate.of(1990, 1, 1), Gender.MALE, "1234567890",
                    "Roma", "RSSMRA90A01H501X", "PATIENT");
        }

        @Test
        @DisplayName("Test User creation with full constructor")
        void testUserCreation() {
            assertNotNull(user);
            assertEquals("Mario", user.getName());
            assertEquals("Rossi", user.getSurname());
            assertEquals("mario.rossi@email.com", user.getEmail());
            assertEquals("PATIENT", user.getType());
        }

        @Test
        @DisplayName("Test getFullName")
        void testGetFullName() {
            assertEquals("Mario Rossi", user.getFullName());
        }

        @Test
        @DisplayName("Test password check")
        void testCheckPassword() {
            assertTrue(user.checkPassword("password123"));
            assertFalse(user.checkPassword("wrongPassword"));
        }

        @Test
        @DisplayName("Test equals based on email")
        void testEquals() {
            User sameUser = new User("Different", "Name", "mario.rossi@email.com", "different",
                    LocalDate.of(2000, 1, 1), Gender.FEMALE, "9876543210",
                    "Milano", "DFFNME00A01F205X", "DOCTOR");
            assertEquals(user, sameUser);
        }

        @Test
        @DisplayName("Test default constructor")
        void testDefaultConstructor() {
            User defaultUser = new User();
            assertEquals(-1, defaultUser.getId());
            assertEquals("", defaultUser.getName());
            assertNotNull(defaultUser.getBornDate());
        }
    }


    // ===== Test Patient Class =====
    @Nested
    @DisplayName("Patient Model Tests")
    class PatientTest {
        private Patient patient;

        @BeforeEach
        void setUp() {
            patient = new Patient("Giuseppe", "Verdi", "giuseppe.verdi@email.com", "pass456",
                    LocalDate.of(1985, 5, 15), Gender.MALE, "0987654321",
                    "Milano", "VRDGPP85E15F205X", 1);
        }

        @Test
        @DisplayName("Test Patient creation")
        void testPatientCreation() {
            assertNotNull(patient);
            assertEquals(1, patient.getDoctorId());
            assertNotNull(patient.getGlucoseReadings());
            assertNotNull(patient.getSymptoms());
            assertNotNull(patient.getRiskFactors());
            assertNotNull(patient.getMedications());
        }

        @Test
        @DisplayName("Test getLastGlucoseMeasurement")
        void testGetLastGlucoseMeasurement() {
            List<GlucoseMeasurement> measurements = new ArrayList<>();
            measurements.add(new GlucoseMeasurement(1, LocalDateTime.of(2024, 1, 1, 8, 0), 120f, "Before Breakfast", ""));
            measurements.add(new GlucoseMeasurement(1, LocalDateTime.of(2024, 1, 2, 8, 0), 130f, "Before Breakfast", ""));
            patient.setGlucoseReadings(measurements);

            GlucoseMeasurement last = patient.getLastGlucoseMeasurement();
            assertNotNull(last);
            assertEquals(130f, last.getGlucoseLevel());
        }

        @Test
        @DisplayName("Test getUpcomingMedications")
        void testGetUpcomingMedications() {
            List<Medication> medications = new ArrayList<>();
            Medication med = new Medication(1, 1, "Insulin", "10 units", Frequency.TWICE_A_DAY,
                    LocalDate.now(), LocalDate.now().plusDays(30), "Take with meals");
            
            List<LogMedication> logs = new ArrayList<>();
            logs.add(new LogMedication(1, 1, LocalDateTime.now().plusMinutes(15), false));
            logs.add(new LogMedication(2, 1, LocalDateTime.now().plusHours(2), false));
            med.setLogMedications(logs);
            
            medications.add(med);
            patient.setMedications(medications);

            List<LogMedication> upcoming = patient.getUpcomingMedications(30);
            assertEquals(1, upcoming.size());
        }

        @Test
        @DisplayName("Test copy constructor")
        void testCopyConstructor() {
            Patient copiedPatient = new Patient(patient);
            assertEquals(patient.getName(), copiedPatient.getName());
            assertEquals(patient.getDoctorId(), copiedPatient.getDoctorId());
        }
    }

    // ===== Test Doctor Class =====
    @Nested
    @DisplayName("Doctor Model Tests")
    class DoctorTest {
        private Doctor doctor;

        @BeforeEach
        void setUp() {
            doctor = new Doctor("Luigi", "Bianchi", "luigi.bianchi@email.com", "docpass",
                    LocalDate.of(1975, 3, 20), Gender.MALE, "1112223333",
                    "Napoli", "BNCLGU75C20F839X", "Endocrinology");
        }

        @Test
        @DisplayName("Test Doctor creation with specialization")
        void testDoctorCreation() {
            assertNotNull(doctor);
            assertEquals("Endocrinology", doctor.getSpecialization());
            assertEquals("DOCTOR", doctor.getType());
        }

        @Test
        @DisplayName("Test Doctor toString")
        void testToString() {
            String str = doctor.toString();
            assertTrue(str.contains("Luigi Bianchi"));
            assertTrue(str.contains("Endocrinology"));
        }
    }

    // ===== Test GlucoseMeasurement Class =====
    @Nested
    @DisplayName("GlucoseMeasurement Model Tests")
    class GlucoseMeasurementTest {
        private GlucoseMeasurement measurement;

        @BeforeEach
        void setUp() {
            measurement = new GlucoseMeasurement(1, LocalDateTime.of(2024, 1, 15, 8, 30),
                    110f, "Before Breakfast", "Normal reading");
        }

        @Test
        @DisplayName("Test glucose measurement creation")
        void testCreation() {
            assertNotNull(measurement);
            assertEquals(110f, measurement.getGlucoseLevel());
            assertEquals("Before Breakfast", measurement.getType());
        }

        @Test
        @DisplayName("Test isBeforeMeal")
        void testIsBeforeMeal() {
            assertTrue(measurement.isBeforeMeal());
            
            GlucoseMeasurement afterMeal = new GlucoseMeasurement(1, LocalDateTime.now(),
                    140f, "After Lunch", "");
            assertFalse(afterMeal.isBeforeMeal());
        }

        @Test
        @DisplayName("Test getStatus")
        void testGetStatus() {
            // Test different glucose levels
            measurement.setGlucoseLevel(70f);
            assertEquals(Status.LOW, measurement.getStatus());
            
            measurement.setGlucoseLevel(100f);
            assertEquals(Status.NORMAL, measurement.getStatus());
            
            measurement.setGlucoseLevel(180f);
            assertEquals(Status.ELEVATED, measurement.getStatus());
            
            measurement.setGlucoseLevel(250f);
            assertEquals(Status.HIGH, measurement.getStatus());
        }

        @Test
        @DisplayName("Test getDate")
        void testGetDate() {
            LocalDate expectedDate = LocalDate.of(2024, 1, 15);
            assertEquals(expectedDate, measurement.getDate());
        }
    }

    // ===== Test Medication Class =====
    @Nested
    @DisplayName("Medication Model Tests")
    class MedicationTest {
        private Medication medication;

        @BeforeEach
        void setUp() {
            medication = new Medication(1, 1, "Metformin", "500mg", Frequency.TWICE_A_DAY,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), "Take with food");
        }

        @Test
        @DisplayName("Test medication creation")
        void testCreation() {
            assertNotNull(medication);
            assertEquals("Metformin", medication.getName_medication());
            assertEquals("500mg", medication.getDose());
            assertEquals(Frequency.TWICE_A_DAY, medication.getFreq());
        }

        @Test
        @DisplayName("Test isActive")
        void testIsActive() {
            // Test with current date medication
            Medication activeMed = new Medication(1, 1, "Active", "100mg", Frequency.ONCE_A_DAY,
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), "");
            assertTrue(activeMed.isActive());
            
            // Test with expired medication
            Medication expiredMed = new Medication(2, 1, "Expired", "100mg", Frequency.ONCE_A_DAY,
                    LocalDate.now().minusDays(30), LocalDate.now().minusDays(5), "");
            assertFalse(expiredMed.isActive());
        }

        @Test
        @DisplayName("Test generateLogMedications")
        void testGenerateLogMedications() {
            Medication testMed = new Medication(1, 1, "Test", "100mg", Frequency.TWICE_A_DAY,
                    LocalDate.now(), LocalDate.now().plusDays(2), "");
            testMed.setId(1);
            
            List<LogMedication> logs = testMed.generateLogMedications();
            assertNotNull(logs);
            // 3 days * 2 times per day = 6 logs
            assertEquals(6, logs.size());
            
            // Check that all logs have correct medication_id
            for (LogMedication log : logs) {
                assertEquals(1, log.getMedication_id());
                assertFalse(log.isTaken());
            }
        }
    }

    // ===== Test LogMedication Class =====
    @Nested
    @DisplayName("LogMedication Model Tests")
    class LogMedicationTest {
        private LogMedication log;

        @BeforeEach
        void setUp() {
            log = new LogMedication(1, 10, LocalDateTime.of(2024, 1, 15, 9, 0), false);
        }

        @Test
        @DisplayName("Test log medication creation")
        void testCreation() {
            assertNotNull(log);
            assertEquals(10, log.getMedication_id());
            assertFalse(log.isTaken());
        }

        @Test
        @DisplayName("Test getDate")
        void testGetDate() {
            assertEquals(LocalDate.of(2024, 1, 15), log.getDate());
        }

        @Test
        @DisplayName("Test setTaken")
        void testSetTaken() {
            log.setTaken(true);
            assertTrue(log.isTaken());
        }
    }

    // ===== Test Symptom Class =====
    @Nested
    @DisplayName("Symptom Model Tests")
    class SymptomTest {
        private Symptom symptom;

        @BeforeEach
        void setUp() {
            symptom = new Symptom(1, 1, LocalDateTime.of(2024, 1, 15, 14, 30),
                    "Headache", "Moderate", LocalTime.of(2, 30), "Took paracetamol");
        }

        @Test
        @DisplayName("Test symptom creation")
        void testCreation() {
            assertNotNull(symptom);
            assertEquals("Headache", symptom.getSymptomName());
            assertEquals("Moderate", symptom.getGravity());
            assertEquals(LocalTime.of(2, 30), symptom.getDuration());
        }

        @Test
        @DisplayName("Test default constructor")
        void testDefaultConstructor() {
            Symptom defaultSymptom = new Symptom();
            assertEquals(-1, defaultSymptom.getId());
            assertEquals("", defaultSymptom.getSymptomName());
            assertNotNull(defaultSymptom.getDateAndTime());
        }
    }

    // ===== Test RiskFactor Class =====
    @Nested
    @DisplayName("RiskFactor Model Tests")
    class RiskFactorTest {
        private RiskFactor riskFactor;

        @BeforeEach
        void setUp() {
            riskFactor = new RiskFactor("Obesity", Gravity.HIGH, 1);
        }

        @Test
        @DisplayName("Test risk factor creation")
        void testCreation() {
            assertNotNull(riskFactor);
            assertEquals("Obesity", riskFactor.getType());
            assertEquals(Gravity.HIGH, riskFactor.getGravity());
        }

        @Test
        @DisplayName("Test setters")
        void testSetters() {
            riskFactor.setType("Smoking");
            riskFactor.setGravity(Gravity.MEDIUM);
            assertEquals("Smoking", riskFactor.getType());
            assertEquals(Gravity.MEDIUM, riskFactor.getGravity());
        }
    }

    // ===== Test Alert Class =====
    @Nested
    @DisplayName("Alert Model Tests")
    class AlertTest {
        private Alert alert;
        private Patient patient;

        @BeforeEach
        void setUp() {
            patient = new Patient();
            alert = new Alert("High glucose detected", AlertType.WARNING, patient);
        }

        @Test
        @DisplayName("Test alert creation")
        void testCreation() {
            assertNotNull(alert);
            assertEquals("High glucose detected", alert.getMessage());
            assertEquals(AlertType.WARNING, alert.getType());
            assertEquals(patient, alert.getPatient());
            assertNotNull(alert.getDateAndTime());
        }

        @Test
        @DisplayName("Test alert with custom datetime")
        void testWithCustomDateTime() {
            LocalDateTime customTime = LocalDateTime.of(2024, 1, 15, 10, 30);
            Alert customAlert = new Alert("Test", AlertType.INFO, patient, customTime);
            assertEquals(customTime, customAlert.getDateAndTime());
        }
    }

    // ===== Test MedicationEdit Class =====
    @Nested
    @DisplayName("MedicationEdit Model Tests")
    class MedicationEditTest {
        private MedicationEdit edit;
        private Medication medication;

        @BeforeEach
        void setUp() {
            medication = new Medication(1, 1, "Insulin", "10 units", Frequency.ONCE_A_DAY,
                    LocalDate.now(), LocalDate.now().plusDays(30), "Morning");
            edit = new MedicationEdit(1, medication);
        }

        @Test
        @DisplayName("Test medication edit creation")
        void testCreation() {
            assertNotNull(edit);
            assertEquals(1, edit.getDoctorId());
            assertEquals(medication, edit.getMedication());
            assertNotNull(edit.getEditTimestamp());
        }

        @Test
        @DisplayName("Test blank constructor")
        void testBlankConstructor() {
            MedicationEdit blankEdit = new MedicationEdit();
            assertEquals(-1, blankEdit.getMedicationId());
            assertEquals(-1, blankEdit.getDoctorId());
        }
    }

    // ===== Test Enum Classes =====
    @Nested
    @DisplayName("Enum Tests")
    class EnumTest {
        
        @Test
        @DisplayName("Test Gender enum")
        void testGenderEnum() {
            assertEquals(Gender.MALE, Gender.valueOf("MALE"));
            assertEquals(Gender.FEMALE, Gender.valueOf("FEMALE"));
        }

        @Test
        @DisplayName("Test Frequency enum")
        void testFrequencyEnum() {
            assertEquals(Frequency.ONCE_A_DAY, Frequency.valueOf("ONCE_A_DAY"));
            assertEquals(Frequency.TWICE_A_DAY, Frequency.valueOf("TWICE_A_DAY"));
            assertEquals(Frequency.THREE_TIMES_A_DAY, Frequency.valueOf("THREE_TIMES_A_DAY"));
            
            // Test getHours method if it exists
            assertNotNull(Frequency.ONCE_A_DAY.getHours());
            assertTrue(Frequency.TWICE_A_DAY.getHours().length > 1);
        }

        @Test
        @DisplayName("Test Gravity enum")
        void testGravityEnum() {
            assertEquals(Gravity.LOW, Gravity.valueOf("LOW"));
            assertEquals(Gravity.MEDIUM, Gravity.valueOf("MEDIUM"));
            assertEquals(Gravity.HIGH, Gravity.valueOf("HIGH"));
        }

        @Test
        @DisplayName("Test AlertType enum")
        void testAlertTypeEnum() {
            assertNotNull(AlertType.INFO);
            assertNotNull(AlertType.WARNING);
            assertNotNull(AlertType.CRITICAL);
        }

        @Test
        @DisplayName("Test Status enum")
        void testStatusEnum() {
            // Test Status.fromGlucoseValue if it exists
            assertEquals(Status.LOW, Status.fromGlucoseValue(60f));
            assertEquals(Status.NORMAL, Status.fromGlucoseValue(100f));
            assertEquals(Status.ELEVATED, Status.fromGlucoseValue(180f));
            assertEquals(Status.HIGH, Status.fromGlucoseValue(250f));
        }
    }
}