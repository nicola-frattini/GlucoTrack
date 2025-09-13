package it.glucotrack.model;

import java.time.LocalDate;

public class Admin extends User {

    private String role; // e.g., "System Administrator", "Clinic Manager"

    // ===== Default constructor =====
    public Admin() {
        super();
        this.role = "Administrator";
    }

    // ===== Full constructor =====
    public Admin(int id, String name, String surname, String email, String password, LocalDate bornDate,
                 Gender gender, String phone, String birthPlace, String fiscalCode, String role) {
        super(id, name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode);
        this.role = role;
    }

    // ===== Getter and setter =====
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "Admin{" +
                "fullName='" + getFullName() + '\'' +
                ", role='" + role + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }
}
