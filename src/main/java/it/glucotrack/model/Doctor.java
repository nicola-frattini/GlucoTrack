package it.glucotrack.model;

import java.time.LocalDate;

public class Doctor extends User {

    private String specialization;  // Doctor's specialty

    // ===== Default constructor =====
    public Doctor() {
        super();
        this.specialization = "";
    }

    // ===== Constructor without ID (for new records) =====
    public Doctor(String name, String surname, String email, String password, LocalDate bornDate,
                  Gender gender, String phone, String birthPlace, String fiscalCode, String specialization) {
        super(name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode, "DOCTOR");
        this.specialization = specialization;
    }

    // ===== Full constructor =====
    public Doctor(int id, String name, String surname, String email, String password, LocalDate bornDate,
                  Gender gender, String phone, String birthPlace, String fiscalCode, String specialization) {
        super(id, name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode, "DOCTOR");
        this.specialization = specialization;
    }

    // ===== Getter and setter =====
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    @Override
    public String toString() {
        return "Doctor{" +
                "fullName='" + getFullName() + '\'' +
                ", specialization='" + specialization + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }
}
