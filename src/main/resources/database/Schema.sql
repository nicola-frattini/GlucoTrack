-- ============================
-- Table: users
-- ============================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    born_date DATE NOT NULL,
    gender ENUM('MALE','FEMALE','OTHER') NOT NULL,
    phone VARCHAR(20),
    birth_place VARCHAR(100),
    fiscal_code VARCHAR(50) UNIQUE,
    type ENUM('PATIENT','DOCTOR','ADMIN') NOT NULL,
    role VARCHAR(50) DEFAULT NULL,          -- For Admins
    specialization VARCHAR(100) DEFAULT NULL, -- For Doctors
    doctor_id INT DEFAULT NULL,             -- For Patients: FK to doctors
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ============================
-- Table: glucose_measurements
-- ============================
CREATE TABLE glucose_measurements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    value INT NOT NULL,
    measurement_time DATETIME NOT NULL,
    before_meal BOOLEAN NOT NULL,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================
-- Table: medications
-- ============================
CREATE TABLE medications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    dose DECIMAL(5,2) NOT NULL,
    times_per_day INT NOT NULL,
    instructions VARCHAR(255),
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================
-- Table: patient_symptoms
-- ============================
CREATE TABLE patient_symptoms (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    symptom VARCHAR(255) NOT NULL,
    symptom_date DATE NOT NULL,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================
-- Table: admin_managed_users
-- ============================
-- Optional: if you want to track which users each admin manages
CREATE TABLE admin_managed_users (
    admin_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (admin_id, user_id),
    FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================
-- Indexes
-- ============================
CREATE INDEX idx_patient_doctor ON users(doctor_id);
CREATE INDEX idx_glucose_patient ON glucose_measurements(patient_id);
CREATE INDEX idx_medication_patient ON medications(patient_id);
CREATE INDEX idx_symptom_patient ON patient_symptoms(patient_id);


-- !!! STILL IL PROGRESS !!!