package it.glucotrack.model;

import java.time.LocalDate;

public class User {


    private String name;
    private String surname;
    private Gender gender;
    private String email;
    private String password;
    private LocalDate bornDate;
    private String phone;
    private String birthPlace;
    private String fiscalCode;

    // ===== Default Costructor =====
    public User() {
        this.name = "";
        this.surname = "";
        this.gender = Gender.MALE;
        this.email = "";
        this.password = "";
        this.bornDate = LocalDate.now();
        this.phone = "";
        this.birthPlace = "";
        this.fiscalCode = "";
    }

    // ===== Complete Costructor =====
    public User(String name, String surname, String email, String password, LocalDate bornDate,
                Gender gender, String phone, String birthPlace, String fiscalCode) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.bornDate = bornDate;
        this.gender = gender;
        this.phone = phone;
        this.birthPlace = birthPlace;
        this.fiscalCode = fiscalCode;
    }

    // =====  getFullName =====
    public String getFullName() {
        return name + " " + surname;
    }

    // ===== Static creation method =====
    public static User createUser(String name, String surname, String email, String password, LocalDate bornDate,
                                  Gender gender, String phone, String birthPlace, String fiscalCode) {
        return new User(name, surname, email, password, bornDate, gender, phone, birthPlace, fiscalCode);
    }

    public static User createUser(User user) {
        return new User(user.name, user.surname, user.email, user.password, user.bornDate, user.gender,
                user.phone, user.birthPlace, user.fiscalCode);
    }

    // ===== Password verify =====
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
}
