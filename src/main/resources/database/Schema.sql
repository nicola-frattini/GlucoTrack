        -- ============================
        -- Table: users (base for all)
        -- ============================
        CREATE TABLE users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name VARCHAR(100) NOT NULL,
            surname VARCHAR(100) NOT NULL,
            email VARCHAR(150) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            born_date DATE NOT NULL,
            gender VARCHAR(10) NOT NULL,
            phone VARCHAR(20),
            birth_place VARCHAR(100),
            fiscal_code VARCHAR(50) UNIQUE,
            type VARCHAR(20) NOT NULL, -- 'PATIENT', 'DOCTOR', 'ADMIN'
            role VARCHAR(50),          -- For Admins
            specialization VARCHAR(100), -- For Doctors
            doctor_id INTEGER          -- For Patients: FK to doctors
        );

        -- ============================
        -- Table: glucose_measurements
        -- ============================
        CREATE TABLE glucose_measurements (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER NOT NULL,
            value INTEGER NOT NULL,
            measurement_time DATETIME NOT NULL,
            type VARCHAR(50) NOT NULL, -- 'Before Breakfast', 'After Breakfast', etc.
            notes TEXT,
            FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
        );

        -- ============================
        -- Table: medications
        -- ============================
        CREATE TABLE medications (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER NOT NULL,
            name VARCHAR(100) NOT NULL,
            dose VARCHAR(50) NOT NULL,
            frequency VARCHAR(50) NOT NULL, -- es: 'ONCE_A_DAY', 'TWICE_A_DAY'
            start_date DATE NOT NULL,
            end_date DATE NOT NULL,
            instructions VARCHAR(255),
            FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
        );

        -- ============================
        -- Table: log_medications
        -- ============================
        CREATE TABLE log_medications (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            medication_id INTEGER NOT NULL,
            date_time DATETIME NOT NULL,
            taken BOOLEAN NOT NULL DEFAULT 0,
            FOREIGN KEY (medication_id) REFERENCES medications(id) ON DELETE CASCADE
        );

        -- ============================
        -- Table: patient_symptoms
        -- ============================
        CREATE TABLE patient_symptoms (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER NOT NULL,
            symptom VARCHAR(255) NOT NULL,
            severity VARCHAR(50),     -- 'Mild', 'Moderate', 'Severe', 'Very Severe'
            duration VARCHAR(100),    -- Duration description (optional)
            notes TEXT,              -- Additional notes (optional)
            symptom_date DATETIME NOT NULL,  -- Changed to DATETIME to include time
            FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
        );

        -- ============================
        -- Table: risk_factors
        -- ============================
        CREATE TABLE risk_factors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER NOT NULL,
            type VARCHAR(100) NOT NULL,
            gravity VARCHAR(10) NOT NULL,
            FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
        );

        -- ============================
        -- Table: medication_edits
        -- ============================
        CREATE TABLE medication_edits (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            medication_id INTEGER NOT NULL,
            edited_by INTEGER NOT NULL, -- FK to user(id)
            medication_name VARCHAR(100) NOT NULL,
            dose VARCHAR(50) NOT NULL,
            frequency VARCHAR(50) NOT NULL, -- es: 'ONCE_A_DAY', 'TWICE_A_DAY'
            start_date DATE NOT NULL,
            end_date DATE NOT NULL,
            instructions VARCHAR(255),
            edit_time DATETIME NOT NULL,
            FOREIGN KEY (medication_id) REFERENCES medications(id) ON DELETE CASCADE,
            FOREIGN KEY (edited_by) REFERENCES user(id) ON DELETE CASCADE
        );

        -- ============================
        -- Indexes
        -- ============================
        CREATE INDEX idx_patient_doctor ON users(doctor_id);
        CREATE INDEX idx_glucose_patient ON glucose_measurements(patient_id);
        CREATE INDEX idx_medication_patient ON medications(patient_id);
        CREATE INDEX idx_log_medication ON log_medications(medication_id);
        CREATE INDEX idx_symptom_patient ON patient_symptoms(patient_id);
        CREATE INDEX idx_risk_factor_patient ON risk_factors(patient_id);
        CREATE INDEX idx_medication_edit ON medication_edits(medication_id);
        CREATE INDEX idx_medication_edit_by ON medication_edits(edited_by);
