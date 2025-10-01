    package it.glucotrack.util;

    import java.sql.SQLException;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.LocalTime;
    import java.util.List;
    import java.util.Random;

    import it.glucotrack.model.Admin;
    import it.glucotrack.model.Doctor;
    import it.glucotrack.model.Frequency;
    import it.glucotrack.model.Gender;
    import it.glucotrack.model.GlucoseMeasurement;
    import it.glucotrack.model.Gravity;
    import it.glucotrack.model.Medication;
    import it.glucotrack.model.Patient;
    import it.glucotrack.model.User;
    import it.glucotrack.model.LogMedication;


    public class DatabaseMockData {

        private static final Random random = new Random();

        public static void populateDatabase() {
            try {
                System.out.println("Populate Database with mock data");

                UserDAO userDAO = new UserDAO();

                if (!userDAO.getAllUsers().isEmpty()) {
                    return;
                }

                PatientDAO patientDAO = new PatientDAO();
                DoctorDAO doctorDAO = new DoctorDAO();
                AdminDAO adminDAO = new AdminDAO();
                MedicationDAO medicationDAO = new MedicationDAO();
                GlucoseMeasurementDAO glucoseDAO = new GlucoseMeasurementDAO();

                // 1. Create Admins
                createMockAdmins(adminDAO);

                // 2. Create Doctors
                createMockDoctors(doctorDAO);

                // 3. Create Patients
                createMockPatients(patientDAO);

                // 4. Create Medications
                createMockMedications(medicationDAO, userDAO);

                // 5. Create Log Medications
                LogMedicationDAO logMedicationDAO = new LogMedicationDAO();
                createMockLogMedications(logMedicationDAO, medicationDAO, userDAO);

                // 6. Create Glucose Measurements
                createMockGlucoseMeasurements(glucoseDAO, userDAO);

                // 7. Create Symptoms
                SymptomDAO symptomDAO = new SymptomDAO();
                createMockSymptoms(symptomDAO, userDAO);

                // 8. Create Risk Factors
                RiskFactorDAO riskFactorDAO = new RiskFactorDAO();
                createMockRiskFactors(riskFactorDAO, userDAO);

                System.out.println("Database populated");
                printDatabaseStats(userDAO, medicationDAO);

            } catch (SQLException e) {
                System.err.println("Error during population: " + e.getMessage());
            }
        }

        private static void createMockAdmins(AdminDAO adminDAO) throws SQLException {

            Admin admin1 = new Admin("Mario", "Rossi", "admin@glucotrack.com", "admin123",
                    LocalDate.of(1980, 5, 15), Gender.MALE, "3331234567",
                    "Roma", "RSSMRA80E15H501X", "SUPER_ADMIN");

            Admin admin2 = new Admin("Laura", "Bianchi", "laura.admin@glucotrack.com", "admin456",
                    LocalDate.of(1975, 8, 22), Gender.FEMALE, "3339876543",
                    "Milano", "BNCLRA75M62F205Y", "SYSTEM_ADMIN");

            adminDAO.insertAdmin(admin1);
            adminDAO.insertAdmin(admin2);

            System.out.println("2 Admins created");


        }

        private static void createMockDoctors(DoctorDAO doctorDAO) throws SQLException {

            Doctor doctor1 = new Doctor("Giuseppe", "Verdi", "dr.verdi@glucotrack.com", "doctor123",
                    LocalDate.of(1970, 3, 10), Gender.MALE, "3351234567",
                    "Torino", "VRDGPP70C10L219X", "Endocrinologia");

            Doctor doctor2 = new Doctor("Anna", "Neri", "dr.neri@glucotrack.com", "doctor456",
                    LocalDate.of(1965, 11, 5), Gender.FEMALE, "3359876543",
                    "Napoli", "NRANNA65S45F839Y", "Diabetologia");

            doctorDAO.insertDoctor(doctor1);
            doctorDAO.insertDoctor(doctor2);

            System.out.println("2 Doctor created");

        }

        private static void createMockPatients(PatientDAO patientDAO) throws SQLException {

            String[] nomi = {"Francesco", "Maria", "Antonio", "Giulia", "Marco", "Elena", "Luca", "Sara", "Paolo", "Chiara"};
            String[] cognomi = {"Rossi", "Bianchi", "Verdi", "Neri", "Ferrari", "Romano", "Gallo", "Conti", "Ricci", "Marino"};

            for (int i = 0; i < 10; i++) {
                String nome = nomi[i];
                String cognome = cognomi[i];
                String email = nome.toLowerCase() + "." + cognome.toLowerCase() + "@email.com";
                String password = "patient" + (i + 1);

                LocalDate birthDate = LocalDate.of(1950 + random.nextInt(50),
                        1 + random.nextInt(12),
                        1 + random.nextInt(28));

                Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
                String phone = "33" + (10000000 + random.nextInt(90000000));
                String birthPlace = "Città" + (i + 1);
                String fiscalCode = "FISCAL" + String.format("%02d", i + 1) + "X";

                // Assign to doctors (ID 1 o 2)
                int doctorId = (i % 2) + 3;

                Patient patient = new Patient(nome, cognome, email, password, birthDate, gender,
                        phone, birthPlace, fiscalCode, doctorId);

                patientDAO.insertPatient(patient);
            }

            System.out.println("10 Patients created");
        }


        private static void createMockMedications(MedicationDAO medicationDAO, UserDAO userDAO) throws SQLException {

            List<User> patients = userDAO.getUsersByType("PATIENT");

            String[] farmaci = {"Metformina", "Insulina Rapida", "Insulina Lenta", "Glibenclamide",
                               "Gliclazide", "Sitagliptin", "Canagliflozin", "Empagliflozin", "Linagliptin"};
            String[] dosiStr = {"500mg", "10 unit", "20 unit", "5mg",
                               "30mg", "100mg", "100mg", "10mg", "5mg"}; // Dosi come stringhe
            Frequency[] frequenze = {Frequency.TWICE_A_DAY, Frequency.THREE_TIMES_A_DAY, Frequency.ONCE_A_DAY,
                                   Frequency.EVERY_TWELVE_HOURS, Frequency.FOUR_TIMES_A_DAY};

            int medicationCount = 0;
            for (User patient : patients) {

                // Every patient 2/4
                int numFarmaci = 2 + random.nextInt(3);

                for (int i = 0; i < numFarmaci; i++) {
                    try {
                        int farmacoIndex = random.nextInt(farmaci.length);
                        String farmaco = farmaci[farmacoIndex];
                        String dose = dosiStr[farmacoIndex];
                        Frequency frequency = frequenze[random.nextInt(frequenze.length)];

                        // Start date within last 30 days, end date 1-6 months after start
                        LocalDate startDate = LocalDate.now().minusDays(random.nextInt(30));
                        LocalDate endDate = startDate.plusMonths(1 + random.nextInt(6));

                        String instructions = "Take " + frequency.getDisplayName().toLowerCase() + " with this istruction";

                        Medication medication = new Medication(patient.getId(), farmaco, dose, frequency,
                                                             startDate, endDate, instructions);

                        medicationDAO.insertMedication(medication, (i%2)+3);
                        medicationCount++;

                    } catch (Exception e) {
                        System.err.println("Error creating medication for" + patient.getName() + ": " + e.getMessage());
                        throw e; // Re-throw to stop the process
                    }
                }
            }

            System.out.println(medicationCount + " Medications created");
        }

        private static void createMockGlucoseMeasurements(GlucoseMeasurementDAO glucoseDAO, UserDAO userDAO) throws SQLException {

            List<User> patients = userDAO.getUsersByType("PATIENT");

            int measurementCount = 0;
            for (User patient : patients) {
                // Ogni paziente ha 100-160 misurazioni negli ultimi 90 giorni
                int numMeasurements = 100 + random.nextInt(61);

                for (int i = 0; i < numMeasurements; i++) {
                    LocalDateTime measurementTime = LocalDateTime.now().minusDays(random.nextInt(90))
                                                                       .minusHours(random.nextInt(24))
                                                                       .minusMinutes(random.nextInt(60));

                    // (70-250 mg/dL)
                    float value = 70.0f + random.nextFloat() * 181.0f;

                    String[] measurementTypes = {
                        "Before Breakfast", "After Breakfast",
                        "Before Lunch", "After Lunch",
                        "Before Dinner", "After Dinner",
                        "Before Sleep", "Fasting", "Random"
                    };
                    String type = measurementTypes[random.nextInt(measurementTypes.length)];
                    String notes = type.toLowerCase().contains("before") ? "Before meal" : "After meal";

                    GlucoseMeasurement measurement = new GlucoseMeasurement(patient.getId(), measurementTime, value, type, notes);
                    glucoseDAO.insertGlucoseMeasurement(measurement);
                    measurementCount++;
                }
            }

            System.out.println(measurementCount + " Glucose Measurements created");
        }

        private static void printDatabaseStats(UserDAO userDAO, MedicationDAO medicationDAO) throws SQLException {
            System.out.println("\nDatabase Stats:");
            System.out.println("- Admins: " + userDAO.getUserCountByType("ADMIN"));
            System.out.println("- Doctors: " + userDAO.getUserCountByType("DOCTOR"));
            System.out.println("- Patients: " + userDAO.getUserCountByType("PATIENT"));
            System.out.println("- Medications: " + medicationDAO.getAllMedications().size());
        }

        public static void printDatabaseContents() {
            System.out.println("\n=== DATABASE CONTENTS ===");
            try {
                printUsersTable();
                printMedicationsTable();
                printGlucoseMeasurementsTable();
            } catch (Exception e) {
                System.err.println("Error during database print: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("=== END DATABASE ===\n");
        }

        private static void printUsersTable() throws SQLException {
            System.out.println("\nUSER TABLE:");
            String sql = "SELECT id, name, surname,password, email, born_date, gender, type FROM users ORDER BY type, id";
            try (java.sql.ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
                System.out.println("ID | Name | Surname | Email | Password | Birth Date | Gender | Type");
                System.out.println("---|------|---------|-------|----------|------------|--------|------");
                while (rs.next()) {
                    System.out.printf("%2d | %-8s | %-10s | %-25s | %-25s | %-12s | %-6s | %-7s%n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getObject("born_date"),
                        rs.getString("gender"),
                        rs.getString("type")
                    );
                }
            }
        }

        private static void printMedicationsTable() throws SQLException {
            System.out.println("\nMEDICATIONS TABLE:");
            String sql = "SELECT id, name, dose, frequency FROM medications LIMIT 5";
            try (java.sql.ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
                System.out.println("ID | Name | Dose | Frequency");
                System.out.println("---|------|------|----------");
                while (rs.next()) {
                    System.out.printf("%2d | %-15s | %-10s | %-10s%n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("dose"),
                        rs.getString("frequency")
                    );
                }
            }
        }

        private static void printGlucoseMeasurementsTable() throws SQLException {
            System.out.println("\nGLUCOSE_MEASUREMENTS TABLE (first 10):");
            String sql = "SELECT id, patient_id, value, measurement_time FROM glucose_measurements LIMIT 10";
            try (java.sql.ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
                System.out.println("ID | Patient ID | Value | Timestamp");
                System.out.println("---|------------|-------|----------");
                while (rs.next()) {
                    System.out.printf("%2d | %10d | %7d | %-20s%n",
                        rs.getInt("id"),
                        rs.getInt("patient_id"),
                        rs.getInt("value"),
                        rs.getObject("measurement_time")
                    );
                }
            }
        }

        private static void createMockSymptoms(SymptomDAO symptomDAO, UserDAO userDAO) throws SQLException {

            List<User> patients = userDAO.getUsersByType("PATIENT");

            String[] symptomNames = {
                    "Headache", "Nausea", "Dizziness", "Fatigue", "Blurred vision",
                    "Excessive thirst", "Excessive hunger", "Weight loss", "Frequent urination",
                    "Leg cramps", "Tingling", "Abdominal pain", "Excessive sweating"
            };

            int symptomCount = 0;
            for (User patient : patients) {
                //  2-8 symptoms in the last 60 days
                int numSymptoms = 2 + random.nextInt(7);

                for (int i = 0; i < numSymptoms; i++) {
                    try {
                        LocalDateTime symptomDateTime = LocalDateTime.now().minusDays(random.nextInt(60))
                                                                           .minusHours(random.nextInt(24));

                        String symptomName = symptomNames[random.nextInt(symptomNames.length)];

                        String[] severities = {"Mild", "Moderate", "Severe", "VerySevere"};
                        String severity = severities[random.nextInt(severities.length)];

                        // 30 minutes to 4 hours)
                        int hours = random.nextInt(5); // 0-4 ore
                        int minutes = random.nextInt(4) * 15; // 0, 15, 30, 45 minuti
                        String duration = String.format("%02d:%02d", hours, minutes);

                        String[] noteOptions = {
                                "",
                                "Mild symptom",
                                "Improved with rest",
                                "Worsened after meals",
                                "Recurring symptom"
                        };

                        String notes = noteOptions[random.nextInt(noteOptions.length)];

                        it.glucotrack.model.Symptom symptom = new it.glucotrack.model.Symptom();
                        symptom.setSymptomName(symptomName);
                        symptom.setGravity(severity);
                        symptom.setDuration(java.time.LocalTime.parse(duration));
                        symptom.setNotes(notes);
                        symptom.setDateAndTime(symptomDateTime);

                        symptomDAO.insertSymptom(patient.getId(), symptom);
                        symptomCount++;

                    } catch (Exception e) {
                        System.err.println("Error during symptom's creation for " + patient.getName() + ": " + e.getMessage());
                        throw e;
                    }
                }
            }

            System.out.println(symptomCount + " Symptoms created");
        }

        private static void createMockRiskFactors(RiskFactorDAO riskFactorDAO, UserDAO userDAO) throws SQLException {

            String[] riskTypes = {"Smoking", "Obesity", "Family History", "High Blood Pressure", "High Cholesterol",
                    "Sedentary Lifestyle", "Poor Diet", "Stress", "Age Factor", "Genetic Predisposition"};
            Gravity[] gravityLevels = {Gravity.LOW, Gravity.MEDIUM, Gravity.HIGH};

            int riskFactorCount = 0;

            List<User> patients = userDAO.getUsersByType("PATIENT");

            // 2/4 risk factor for patient
            for(User patient : patients) {
                int numRiskFactors = 2 + random.nextInt(3); // 2-4 risk factors

                for (int i = 0; i < numRiskFactors; i++) {
                    try {
                        String riskType = riskTypes[random.nextInt(riskTypes.length)];
                        Gravity gravity = gravityLevels[random.nextInt(gravityLevels.length)];
                        LocalDate recordedDate = LocalDate.now().minusDays(random.nextInt(365)); // Ultimo anno

                        it.glucotrack.model.RiskFactor riskFactor = new it.glucotrack.model.RiskFactor();
                        riskFactor.setType(riskType);
                        riskFactor.setGravity(gravity);

                        riskFactorDAO.insertRiskFactor(patient.getId(), riskFactor);
                        riskFactorCount++;

                    } catch (Exception e) {
                        System.err.println("Error during risk factor creation for patient " + patient.getName() + ": " + e.getMessage());
                        throw e;
                    }
                }
            }

            System.out.println(riskFactorCount + " Risk Factors created");
        }


        private static void createMockLogMedications(LogMedicationDAO logMedicationDAO, MedicationDAO medicationDAO, UserDAO userDAO) throws SQLException {

            List<User> patients = userDAO.getUsersByType("PATIENT");
            int logCount = 0;

            for (User patient : patients) {
                try {

                    List<Medication> medications = medicationDAO.getMedicationsByPatientId(patient.getId());

                    for (Medication medication : medications) {
                        LocalDate startDate = LocalDate.now().minusDays(30);
                        LocalDate endDate = LocalDate.now().plusDays(7);

                        int timesPerDay = getTimesPerDay(medication.getFreq());

                        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

                            for (int i = 0; i < timesPerDay; i++) {
                                LocalTime time = getTimeForDose(i, timesPerDay);
                                LocalDateTime dateTime = LocalDateTime.of(date, time);

                                boolean taken = dateTime.isBefore(LocalDateTime.now()) && random.nextDouble() > 0.02; // 98% di aderenza

                                // Debug
                                if (medication.getId() <= 0) {
                                    System.err.println("Medication ID not valid: " + medication.getId() + " for " + medication.getName_medication());
                                    continue;
                                }

                                LogMedication logMedication = new LogMedication(
                                    -1, // ID auto-generated
                                    medication.getId(),
                                    dateTime,
                                    taken
                                );

                                logMedicationDAO.insertLogMedication(logMedication);
                                logCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error during the log medications creation for patient " + patient.getName() + ": " + e.getMessage());
                    throw e;
                }
            }

            System.out.println(logCount + " Log Medications created");
        }

        private static int getTimesPerDay(Frequency frequency) {
            switch (frequency) {
                case ONCE_A_DAY: return 1;
                case TWICE_A_DAY: return 2;
                case THREE_TIMES_A_DAY: return 3;
                case FOUR_TIMES_A_DAY: return 4;
                case EVERY_TWELVE_HOURS: return 2;
                default: return 1;
            }
        }

        private static LocalTime getTimeForDose(int doseIndex, int totalDoses) {

            switch (totalDoses) {
                case 1: return LocalTime.of(8, 0);
                case 2:
                    return doseIndex == 0 ? LocalTime.of(8, 0) : LocalTime.of(20, 0);
                case 3:
                    switch (doseIndex) {
                        case 0: return LocalTime.of(8, 0);
                        case 1: return LocalTime.of(13, 0);
                        case 2: return LocalTime.of(20, 0);
                        default: return LocalTime.of(8, 0);
                    }
                case 4:
                    switch (doseIndex) {
                        case 0: return LocalTime.of(8, 0);
                        case 1: return LocalTime.of(12, 0);
                        case 2: return LocalTime.of(17, 0);
                        case 3: return LocalTime.of(21, 0);
                        default: return LocalTime.of(8, 0);
                    }
                default: return LocalTime.of(8, 0);
            }
        }
    }