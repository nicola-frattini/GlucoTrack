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
import it.glucotrack.util.PasswordEncryption;


public class DatabaseMockData {

    private static final Random random = new Random();

    public static void populateDatabase() {
        try {
            System.out.println("🚀 Popolamento database con dati mock...");

            // Inizializza i DAO
            UserDAO userDAO = new UserDAO();

            // Controlla se il database è già popolato
            if (!userDAO.getAllUsers().isEmpty()) {
                System.out.println("✅ Database già popolato, saltando l'inserimento");
                return;
            }

            PatientDAO patientDAO = new PatientDAO();
            DoctorDAO doctorDAO = new DoctorDAO();
            AdminDAO adminDAO = new AdminDAO();
            MedicationDAO medicationDAO = new MedicationDAO();
            GlucoseMeasurementDAO glucoseDAO = new GlucoseMeasurementDAO();

            // 1. Crea Admins
            createMockAdmins(adminDAO);

            // 2. Crea Doctors
            createMockDoctors(doctorDAO);

            // 3. Crea Patients
            createMockPatients(patientDAO);

            // 4. Crea Medications per i pazienti
            createMockMedications(medicationDAO, userDAO);

            // 5. Crea Log Medications (pianificazioni assunzioni)
            LogMedicationDAO logMedicationDAO = new LogMedicationDAO();
            createMockLogMedications(logMedicationDAO, medicationDAO, userDAO);

            // 6. Crea Glucose Measurements
            createMockGlucoseMeasurements(glucoseDAO, userDAO);

            // 7. Crea Symptoms per i pazienti
            SymptomDAO symptomDAO = new SymptomDAO();
            createMockSymptoms(symptomDAO, userDAO);

            // 8. Crea Risk Factors per i pazienti
            RiskFactorDAO riskFactorDAO = new RiskFactorDAO();
            createMockRiskFactors(riskFactorDAO, userDAO);

            System.out.println("🎉 Database popolato con successo!");
            printDatabaseStats(userDAO, medicationDAO);

        } catch (SQLException e) {
            System.err.println("❌ Errore durante il popolamento: " + e.getMessage());
        }
    }

    private static void createMockAdmins(AdminDAO adminDAO) throws SQLException {
        System.out.println("  📋 Creazione Admins...");

        Admin admin1 = new Admin("Mario", "Rossi", "admin@glucotrack.com", "admin123",
                LocalDate.of(1980, 5, 15), Gender.MALE, "3331234567",
                "Roma", "RSSMRA80E15H501X", "SUPER_ADMIN");

        Admin admin2 = new Admin("Laura", "Bianchi", "laura.admin@glucotrack.com", "admin456",
                LocalDate.of(1975, 8, 22), Gender.FEMALE, "3339876543",
                "Milano", "BNCLRA75M62F205Y", "SYSTEM_ADMIN");

        adminDAO.insertAdmin(admin1);
        adminDAO.insertAdmin(admin2);

        System.out.println("  ✅ 2 Admins creati");
    }

    private static void createMockDoctors(DoctorDAO doctorDAO) throws SQLException {
        System.out.println("  👨‍⚕️ Creazione Doctors...");

        Doctor doctor1 = new Doctor("Giuseppe", "Verdi", "dr.verdi@glucotrack.com", "doctor123",
                LocalDate.of(1970, 3, 10), Gender.MALE, "3351234567",
                "Torino", "VRDGPP70C10L219X", "Endocrinologia");

        Doctor doctor2 = new Doctor("Anna", "Neri", "dr.neri@glucotrack.com", "doctor456",
                LocalDate.of(1965, 11, 5), Gender.FEMALE, "3359876543",
                "Napoli", "NRANNA65S45F839Y", "Diabetologia");

        doctorDAO.insertDoctor(doctor1);
        doctorDAO.insertDoctor(doctor2);

        System.out.println("  ✅ 2 Doctors creati");
    }

    private static void createMockPatients(PatientDAO patientDAO) throws SQLException {
        System.out.println("  🤒 Creazione Patients...");

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

            // Assegna ai dottori (ID 1 o 2)
            int doctorId = (i % 2) + 3;

            Patient patient = new Patient(nome, cognome, email, password, birthDate, gender,
                    phone, birthPlace, fiscalCode, doctorId);

            patientDAO.insertPatient(patient);
        }

        System.out.println("  ✅ 10 Patients creati");
    }


    private static void createMockMedications(MedicationDAO medicationDAO, UserDAO userDAO) throws SQLException {
        System.out.println("Creazione Medications...");

        // Prendi tutti i pazienti
        System.out.println("Recupero pazienti per medications...");
        List<User> patients = userDAO.getUsersByType("PATIENT");
        System.out.println("Trovati " + patients.size() + " pazienti");

        String[] farmaci = {"Metformina", "Insulina Rapida", "Insulina Lenta", "Glibenclamide",
                           "Gliclazide", "Sitagliptin", "Canagliflozin", "Empagliflozin", "Linagliptin"};
        String[] dosiStr = {"500mg", "10 unità", "20 unità", "5mg",
                           "30mg", "100mg", "100mg", "10mg", "5mg"}; // Dosi come stringhe
        Frequency[] frequenze = {Frequency.TWICE_A_DAY, Frequency.THREE_TIMES_A_DAY, Frequency.ONCE_A_DAY,
                               Frequency.EVERY_TWELVE_HOURS, Frequency.FOUR_TIMES_A_DAY};

        int medicationCount = 0;
        for (User patient : patients) {
            System.out.println("Creando medications per: " + patient.getName() + " " + patient.getSurname());

            // Ogni paziente ha 2-4 farmaci (almeno 2)
            int numFarmaci = 2 + random.nextInt(3);

            for (int i = 0; i < numFarmaci; i++) {
                try {
                    int farmacoIndex = random.nextInt(farmaci.length);
                    String farmaco = farmaci[farmacoIndex];
                    String dose = dosiStr[farmacoIndex];
                    Frequency frequency = frequenze[random.nextInt(frequenze.length)];

                    // Date di inizio e fine che possono partire massimo fino al 19/09/2025
                    LocalDate startDate = LocalDate.now().minusDays(random.nextInt(60));
                    LocalDate endDate = startDate.plusMonths(1 + random.nextInt(6));

                    String instructions = "Assumere " + frequency.getDisplayName().toLowerCase() + " prima dei pasti";

                    // Usa il costruttore che accetta dose come String
                    Medication medication = new Medication(patient.getId(), farmaco, dose, frequency,
                                                         startDate, endDate, instructions);

                    medicationDAO.insertMedication(medication, (i%2)+3);
                    medicationCount++;
                    System.out.println("    Medication creata: " + farmaco + " per paziente " + patient.getName());

                } catch (Exception e) {
                    System.err.println("    Errore creando medication per paziente " + patient.getName() + ": " + e.getMessage());
                    throw e; // Re-throw per fermare il processo
                }
            }
        }

        System.out.println(medicationCount + " Medications creati");
    }

    private static void createMockGlucoseMeasurements(GlucoseMeasurementDAO glucoseDAO, UserDAO userDAO) throws SQLException {
        System.out.println("Creazione Glucose Measurements...");

        List<User> patients = userDAO.getUsersByType("PATIENT");

        int measurementCount = 0;
        for (User patient : patients) {
            // Ogni paziente ha 20-50 misurazioni negli ultimi 30 giorni
            int numMeasurements = 20 + random.nextInt(31);

            for (int i = 0; i < numMeasurements; i++) {
                LocalDateTime measurementTime = LocalDateTime.now().minusDays(random.nextInt(30))
                                                                   .minusHours(random.nextInt(24))
                                                                   .minusMinutes(random.nextInt(60));

                // Valori realistici di glicemia (70-300 mg/dL)
                float value = 70.0f + random.nextFloat() * 231.0f;

                // Seleziona un tipo di misurazione casuale
                String[] measurementTypes = {
                    "Before Breakfast", "After Breakfast",
                    "Before Lunch", "After Lunch",
                    "Before Dinner", "After Dinner",
                    "Before Sleep", "Fasting", "Random"
                };
                String type = measurementTypes[random.nextInt(measurementTypes.length)];
                String notes = type.toLowerCase().contains("before") ? "Prima del pasto" : "Dopo il pasto";

                GlucoseMeasurement measurement = new GlucoseMeasurement(patient.getId(), measurementTime, value, type, notes);
                glucoseDAO.insertGlucoseMeasurement(measurement);
                measurementCount++;
            }
        }

        System.out.println("  ✅ " + measurementCount + " Glucose Measurements creati");
    }

    private static void printDatabaseStats(UserDAO userDAO, MedicationDAO medicationDAO) throws SQLException {
        System.out.println("\n📊 Statistiche Database:");
        System.out.println("- Admins: " + userDAO.getUserCountByType("ADMIN"));
        System.out.println("- Doctors: " + userDAO.getUserCountByType("DOCTOR"));
        System.out.println("- Patients: " + userDAO.getUserCountByType("PATIENT"));
        System.out.println("- Medications: " + medicationDAO.getAllMedications().size());
    }

    public static void printDatabaseContents() {
        System.out.println("\n🔍 === CONTENUTO DATABASE ===");
        try {
            printUsersTable();
            printMedicationsTable();
            printGlucoseMeasurementsTable();
        } catch (Exception e) {
            System.err.println("❌ Errore durante la stampa del database: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("🔍 === FINE CONTENUTO DATABASE ===\n");
    }

    private static void printUsersTable() throws SQLException {
        System.out.println("\n👥 TABELLA USERS:");
        String sql = "SELECT id, name, surname, email, born_date, gender, type FROM users ORDER BY type, id";
        try (java.sql.ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            System.out.println("ID | Nome | Cognome | Email | Data Nascita | Genere | Tipo");
            System.out.println("---|------|---------|-------|--------------|--------|------");
            while (rs.next()) {
                System.out.printf("%2d | %-8s | %-10s | %-25s | %-12s | %-6s | %-7s%n",
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("surname"),
                    rs.getString("email"),
                    rs.getObject("born_date"), // Uso getObject per vedere il tipo effettivo
                    rs.getString("gender"),
                    rs.getString("type")
                );
            }
        }
    }

    private static void printMedicationsTable() throws SQLException {
        System.out.println("\n💊 TABELLA MEDICATIONS:");
        String sql = "SELECT id, name, dose, frequency FROM medications LIMIT 5";
        try (java.sql.ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            System.out.println("ID | Nome | Dosaggio | Frequenza");
            System.out.println("---|------|----------|----------");
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
        System.out.println("\n📊 TABELLA GLUCOSE_MEASUREMENTS (prime 5):");
        String sql = "SELECT id, patient_id, value, measurement_time FROM glucose_measurements LIMIT 5";
        try (java.sql.ResultSet rs = DatabaseInteraction.executeQuery(sql)) {
            System.out.println("ID | Patient ID | Livello | Timestamp");
            System.out.println("---|------------|---------|----------");
            while (rs.next()) {
                System.out.printf("%2d | %10d | %7d | %-20s%n",
                    rs.getInt("id"),
                    rs.getInt("patient_id"),
                    rs.getInt("value"),
                    rs.getObject("measurement_time") // Uso getObject per vedere il tipo effettivo
                );
            }
        }
    }

    private static void createMockSymptoms(SymptomDAO symptomDAO, UserDAO userDAO) throws SQLException {
        System.out.println("  🤒 Creazione Symptoms...");

        List<User> patients = userDAO.getUsersByType("PATIENT");

        String[] symptomNames = {"Mal di testa", "Nausea", "Vertigini", "Stanchezza", "Visione offuscata",
                                "Sete eccessiva", "Fame eccessiva", "Perdita di peso", "Minzione frequente",
                                "Crampi alle gambe", "Formicolio", "Dolore addominale", "Sudorazione eccessiva"};

        int symptomCount = 0;
        for (User patient : patients) {
            // Ogni paziente ha 2-8 sintomi negli ultimi 60 giorni
            int numSymptoms = 2 + random.nextInt(7);

            for (int i = 0; i < numSymptoms; i++) {
                try {
                    LocalDateTime symptomDateTime = LocalDateTime.now().minusDays(random.nextInt(60))
                                                                       .minusHours(random.nextInt(24));

                    String symptomName = symptomNames[random.nextInt(symptomNames.length)];

                    // Genera dati casuali per severity, duration e notes
                    String[] severities = {"Mild", "Moderate", "Severe", "VerySevere"};
                    String severity = severities[random.nextInt(severities.length)];

                    // Genera durata casuale (30 minuti a 4 ore)
                    int hours = random.nextInt(5); // 0-4 ore
                    int minutes = random.nextInt(4) * 15; // 0, 15, 30, 45 minuti
                    String duration = String.format("%02d:%02d", hours, minutes);

                    String[] noteOptions = {"", "Sintomo lieve", "Migliorato con riposo", "Peggiorato dopo i pasti", "Sintomo ricorrente"};
                    String notes = noteOptions[random.nextInt(noteOptions.length)];

                    // Crea oggetto Symptom usando il modello
                    it.glucotrack.model.Symptom symptom = new it.glucotrack.model.Symptom();
                    symptom.setSymptomName(symptomName);
                    symptom.setGravity(severity);
                    symptom.setDuration(java.time.LocalTime.parse(duration));
                    symptom.setNotes(notes);
                    symptom.setDateAndTime(symptomDateTime);

                    // Usa il nuovo metodo che inserisce tutti i campi
                    symptomDAO.insertSymptom(patient.getId(), symptom);
                    symptomCount++;

                } catch (Exception e) {
                    System.err.println("    ❌ Errore creando symptom per paziente " + patient.getName() + ": " + e.getMessage());
                    throw e;
                }
            }
        }

        System.out.println("  ✅ " + symptomCount + " Symptoms creati");
    }

    private static void createMockRiskFactors(RiskFactorDAO riskFactorDAO, UserDAO userDAO) throws SQLException {
        System.out.println("  ⚠️ Creazione Risk Factors...");

        String[] riskTypes = {"Smoking", "Obesity", "Family History", "High Blood Pressure", "High Cholesterol",
                "Sedentary Lifestyle", "Poor Diet", "Stress", "Age Factor", "Genetic Predisposition"};
        Gravity[] gravityLevels = {Gravity.LOW, Gravity.MEDIUM, Gravity.HIGH};

        int riskFactorCount = 0;

        // Prendi tutti i pazienti
        List<User> patients = userDAO.getUsersByType("PATIENT");

        // Inserisci dai 2 ai 4 risk factor per paziente
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
                    System.err.println("    ❌ Errore creando risk factor per paziente " + patient.getName() + ": " + e.getMessage());
                    throw e;
                }
            }
        }

        System.out.println("  ✅ " + riskFactorCount + " Risk Factors creati");
    }


    private static void createMockLogMedications(LogMedicationDAO logMedicationDAO, MedicationDAO medicationDAO, UserDAO userDAO) throws SQLException {
        System.out.println("  📅 Creazione Log Medications (pianificazioni)...");

        // Prendi tutte le medications esistenti
        List<User> patients = userDAO.getUsersByType("PATIENT");
        int logCount = 0;

        for (User patient : patients) {
            try {
                // Prendi tutti i farmaci del paziente
                List<Medication> medications = medicationDAO.getMedicationsByPatientId(patient.getId());

                for (Medication medication : medications) {
                    // Crea log per gli ultimi 30 giorni e i prossimi 7 giorni
                    LocalDate startDate = LocalDate.now().minusDays(30);
                    LocalDate endDate = LocalDate.now().plusDays(7);

                    // Determina quante volte al giorno in base alla frequenza
                    int timesPerDay = getTimesPerDay(medication.getFreq());

                    // Genera log per ogni giorno nel range
                    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                        // Crea più log al giorno in base alla frequenza
                        for (int i = 0; i < timesPerDay; i++) {
                            LocalTime time = getTimeForDose(i, timesPerDay);
                            LocalDateTime dateTime = LocalDateTime.of(date, time);

                            // Determina se è stato preso (passato) o è pianificato (futuro)
                            boolean taken = dateTime.isBefore(LocalDateTime.now()) && random.nextDouble() > 0.15; // 85% di aderenza

                            // Debug: controlla che medication ID sia valido
                            if (medication.getId() <= 0) {
                                System.err.println("      ⚠️ Medication ID non valido: " + medication.getId() + " per " + medication.getName_medication());
                                continue;
                            }

                            LogMedication logMedication = new LogMedication(
                                -1, // ID auto-generato
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
                System.err.println("    ❌ Errore creando log medications per paziente " + patient.getName() + ": " + e.getMessage());
                throw e;
            }
        }

        System.out.println("  ✅ " + logCount + " Log Medications creati");
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
        // Distribuisce le dosi durante la giornata
        switch (totalDoses) {
            case 1: return LocalTime.of(8, 0); // Una volta al giorno - mattina
            case 2:
                return doseIndex == 0 ? LocalTime.of(8, 0) : LocalTime.of(20, 0); // Mattina e sera
            case 3:
                switch (doseIndex) {
                    case 0: return LocalTime.of(8, 0);  // Mattina  
                    case 1: return LocalTime.of(13, 0); // Pranzo
                    case 2: return LocalTime.of(20, 0); // Sera
                    default: return LocalTime.of(8, 0);
                }
            case 4:
                switch (doseIndex) {
                    case 0: return LocalTime.of(8, 0);  // Mattina
                    case 1: return LocalTime.of(12, 0); // Pranzo
                    case 2: return LocalTime.of(17, 0); // Pomeriggio
                    case 3: return LocalTime.of(21, 0); // Sera
                    default: return LocalTime.of(8, 0);
                }
            default: return LocalTime.of(8, 0);
        }
    }
}