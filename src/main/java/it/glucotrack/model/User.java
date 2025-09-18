package it.glucotrack.model;

import java.time.LocalDate;

public class User {

    private int id;              // Unique DB ID
    private String name;
    private String surname;
    private Gender gender;
    private String email;
    private String password;
    private LocalDate bornDate;
    private String phone;
    private String birthPlace;
    private String fiscalCode;
    private String type;

    
    // ===== Default constructor =====
    public User() {
        this.id = -1;
        this.name = "";
        this.surname = "";
        this.gender = Gender.MALE;
        this.email = "";
        this.password = "";
        this.bornDate = LocalDate.now();
        this.phone = "";
        this.birthPlace = "";
        this.fiscalCode = "";
        this.type = "";
    }

    // ===== Constructor without ID (for new records) =====
    public User(String name, String surname, String email, String password, LocalDate bornDate,
                Gender gender, String phone, String birthPlace, String fiscalCode) {
        this.id = -1; // Will be set by database
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.bornDate = bornDate;
        this.gender = gender;
        this.phone = phone;
        this.birthPlace = birthPlace;
        this.fiscalCode = fiscalCode;
        this.type = "";
    }

    public User(String name, String surname, String email, String password, LocalDate bornDate,
                Gender gender, String phone, String birthPlace, String fiscalCode,String type) {
        this.id = -1; // Will be set by database
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.bornDate = bornDate;
        this.gender = gender;
        this.phone = phone;
        this.birthPlace = birthPlace;
        this.fiscalCode = fiscalCode;
        this.type = type;
    }


    // ===== Full constructor =====
    public User(int id, String name, String surname, String email, String password, LocalDate bornDate,
                Gender gender, String phone, String birthPlace, String fiscalCode,String type) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.bornDate = bornDate;
        this.gender = gender;
        this.phone = phone;
        this.birthPlace = birthPlace;
        this.fiscalCode = fiscalCode;
        this.type = type;
    }

    // ===== Get full name =====
    public String getFullName() {
        return name + " " + surname;
    }

    // ===== Password check =====
    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    // ===== Equals based on email =====
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User other = (User) obj;
        return email.equals(other.email);
    }

    // ===== Getters and setters =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDate getBornDate() { return bornDate; }
    public void setBornDate(LocalDate bornDate) { this.bornDate = bornDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBirthPlace() { return birthPlace; }
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }

    public String getFiscalCode() { return fiscalCode; }
    public void setFiscalCode(String fiscalCode) { this.fiscalCode = fiscalCode; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", gender=" + gender +
                ", bornDate=" + bornDate +
                ", phone='" + phone + '\'' +
                ", birthPlace='" + birthPlace + '\'' +
                ", fiscalCode='" + fiscalCode + '\'' +
                ", type='" + type + '\'' +
                '}';    
    }

    public String getNameAndSurname() {
        return name + " " + surname;
    }
}
