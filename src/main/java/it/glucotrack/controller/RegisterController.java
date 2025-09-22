package it.glucotrack.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import it.glucotrack.model.Patient;
import it.glucotrack.model.Doctor;
import it.glucotrack.model.Gender;
import it.glucotrack.util.InputCheck;
import it.glucotrack.util.UserDAO;
import it.glucotrack.util.PatientDAO;
import it.glucotrack.util.DoctorDAO;
import it.glucotrack.view.ViewNavigator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RegisterController {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField birthPlaceField;

    @FXML
    private TextField fiscalCodeField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<String> genderComboBox;

    @FXML
    private DatePicker birthDatePicker;

    @FXML
    private ComboBox<String> accountTypeComboBox;

    // Doctor-specific fields
    @FXML
    private VBox doctorFieldsContainer;

    @FXML
    private TextField specializationField;

    // Patient-specific fields
    @FXML
    private VBox patientFieldsContainer;

    @FXML
    private ComboBox<Doctor> referringDoctorComboBox;

    @FXML
    private CheckBox termsCheckBox;

    @FXML
    private Button registerButton;

    @FXML
    private Label loginLabel;

    private UserDAO userDAO;
    private DoctorDAO doctorDAO;
    private PatientDAO patientDAO;

    public void initialize() {
        userDAO = new UserDAO();
        doctorDAO = new DoctorDAO();
        patientDAO = new PatientDAO();

        setupComboBoxes();
        setupEventHandlers();
        loadAvailableDoctors();
    }

    private void setupEventHandlers() {
        registerButton.setOnAction(this::handleRegister);
        loginLabel.setOnMouseClicked(this::handleLoginLink);

        // Add listener for account type changes
        accountTypeComboBox.setOnAction(e -> handleAccountTypeChange());
    }

    private void setupComboBoxes() {
        // Populate gender ComboBox
        genderComboBox.getItems().addAll("Male", "Female");

        // Populate account type ComboBox
        accountTypeComboBox.getItems().addAll("Patient", "Doctor");

        // Setup doctor ComboBox display format
        referringDoctorComboBox.setCellFactory(listView -> new ListCell<Doctor>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);
                if (empty || doctor == null) {
                    setText(null);
                } else {
                    setText(doctor.getFullName() + " - " + doctor.getSpecialization());
                }
            }
        });

        referringDoctorComboBox.setButtonCell(new ListCell<Doctor>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);
                if (empty || doctor == null) {
                    setText("Select Referring Doctor");
                } else {
                    setText(doctor.getFullName() + " - " + doctor.getSpecialization());
                }
            }
        });
    }

    private void loadAvailableDoctors() {
        try {
            List<Doctor> doctors = doctorDAO.getAllDoctors();
            referringDoctorComboBox.getItems().clear();
            referringDoctorComboBox.getItems().addAll(doctors);
        } catch (SQLException e) {
            System.err.println("Error loading doctors: " + e.getMessage());
            showErrorAlert("Error", "Could not load available doctors.");
        }
    }

    private void handleAccountTypeChange() {
        String selectedType = accountTypeComboBox.getValue();

        if (selectedType == null) {
            hideAllSpecificFields();
            return;
        }

        switch (selectedType) {
            case "Doctor":
                showDoctorFields();
                hidePatientFields();
                break;
            case "Patient":
                showPatientFields();
                hideDoctorFields();
                break;
            default:
                hideAllSpecificFields();
                break;
        }
    }

    private void showDoctorFields() {
        doctorFieldsContainer.setVisible(true);
        doctorFieldsContainer.setManaged(true);
    }

    private void hideDoctorFields() {
        doctorFieldsContainer.setVisible(false);
        doctorFieldsContainer.setManaged(false);
    }

    private void showPatientFields() {
        patientFieldsContainer.setVisible(true);
        patientFieldsContainer.setManaged(true);
    }

    private void hidePatientFields() {
        patientFieldsContainer.setVisible(false);
        patientFieldsContainer.setManaged(false);
    }

    private void hideAllSpecificFields() {
        hideDoctorFields();
        hidePatientFields();
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        try {
            String accountType = accountTypeComboBox.getValue();

            // Check if email already exists
            if (userDAO.emailExists(emailField.getText().trim())) {
                showErrorAlert("Registration Error", "An account with this email already exists.");
                return;
            }

            boolean success = false;

            switch (accountType) {
                case "Doctor":
                    success = registerDoctor();
                    break;
                case "Patient":
                    success = registerPatient();
                    break;
            }

            if (success) {
                showSuccessAlert("Registration Successful",
                        "Your " + accountType.toLowerCase() + " account has been created successfully!");
                clearForm();
                navigateToLogin();
            } else {
                showErrorAlert("Registration Error", "Failed to create account. Please try again.");
            }

        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            showErrorAlert("Registration Error", "Database error occurred. Please try again later.");
        } catch (Exception e) {
            System.err.println("Unexpected error during registration: " + e.getMessage());
            showErrorAlert("Registration Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean registerDoctor() throws SQLException {
        String specialization = specializationField.getText().trim();

        Doctor doctor = new Doctor(
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText(), // Will be encrypted by DAO
                birthDatePicker.getValue(),
                Gender.fromString(genderComboBox.getValue()),
                phoneField.getText().trim(),
                birthPlaceField.getText().trim(),
                fiscalCodeField.getText().trim(),
                specialization
        );

        return doctorDAO.insertDoctor(doctor);
    }

    private boolean registerPatient() throws SQLException {
        Doctor selectedDoctor = referringDoctorComboBox.getValue();
        int doctorId = selectedDoctor != null ? selectedDoctor.getId() : -1;

        Patient patient = new Patient(
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText(), // Will be encrypted by DAO
                birthDatePicker.getValue(),
                Gender.fromString(genderComboBox.getValue()),
                phoneField.getText().trim(),
                birthPlaceField.getText().trim(),
                fiscalCodeField.getText().trim(),
                doctorId
        );

        return patientDAO.insertPatient(patient);
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Validate account type
        if (accountTypeComboBox.getValue() == null) {
            errors.append("• Please select an account type\n");
            if (errors.length() > 0) {
                showErrorAlert("Validation Error", errors.toString());
                return false;
            }
        }

        // Validate basic fields
        validateBasicFields(errors);

        // Validate account type specific fields
        String accountType = accountTypeComboBox.getValue();
        if ("Doctor".equals(accountType)) {
            validateDoctorFields(errors);
        } else if ("Patient".equals(accountType)) {
            validatePatientFields(errors);
        }

        // Validate password
        validatePasswordFields(errors);

        // Validate terms checkbox
        if (!termsCheckBox.isSelected()) {
            errors.append("• You must accept the Terms and Conditions\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Validation Error", errors.toString());
            return false;
        }

        return true;
    }

    private void validateBasicFields(StringBuilder errors) {
        // Validate firstName
        String firstName = firstNameField.getText().trim();
        if (!InputCheck.isValidString(firstName)) {
            errors.append("• First name is required\n");
        } else if (!InputCheck.isAlphabetic(firstName)) {
            errors.append("• First name must contain only letters\n");
        }

        // Validate lastName
        String lastName = lastNameField.getText().trim();
        if (!InputCheck.isValidString(lastName)) {
            errors.append("• Last name is required\n");
        } else if (!InputCheck.isAlphabetic(lastName)) {
            errors.append("• Last name must contain only letters\n");
        }

        // Validate email
        String email = emailField.getText().trim();
        if (!InputCheck.isValidString(email)) {
            errors.append("• Email is required\n");
        } else if (!InputCheck.isValidEmail(email)) {
            errors.append("• Please enter a valid email address\n");
        }

        // Validate phone
        String phone = phoneField.getText().trim();
        if (!InputCheck.isValidString(phone)) {
            errors.append("• Phone number is required\n");
        }

        // Validate birth place
        String birthPlace = birthPlaceField.getText().trim();
        if (!InputCheck.isValidString(birthPlace)) {
            errors.append("• Birth place is required\n");
        }

        // Validate fiscal code
        String fiscalCode = fiscalCodeField.getText().trim();
        if (!InputCheck.isValidString(fiscalCode)) {
            errors.append("• Fiscal code is required\n");
        }

        // Validate gender
        if (genderComboBox.getValue() == null) {
            errors.append("• Please select your gender\n");
        }

        // Validate birth date
        if (birthDatePicker.getValue() == null) {
            errors.append("• Please select your birth date\n");
        } else {
            LocalDate birthDate = birthDatePicker.getValue();
            LocalDate today = LocalDate.now();
            if (birthDate.isAfter(today)) {
                errors.append("• Birth date cannot be in the future\n");
            }
            // Check if user is at least 13 years old
            LocalDate minDate = today.minusYears(13);
            if (birthDate.isAfter(minDate)) {
                errors.append("• You must be at least 13 years old to register\n");
            }
        }
    }

    private void validateDoctorFields(StringBuilder errors) {
        String specialization = specializationField.getText().trim();
        if (!InputCheck.isValidString(specialization)) {
            errors.append("• Medical specialization is required\n");
        }
    }

    private void validatePatientFields(StringBuilder errors) {
        if (referringDoctorComboBox.getValue() == null) {
            errors.append("• Please select a referring doctor\n");
        }
    }

    private void validatePasswordFields(StringBuilder errors) {
        // Validate password
        String password = passwordField.getText();
        if (!InputCheck.isValidString(password)) {
            errors.append("• Password is required\n");
        } else if (!InputCheck.hasLengthBetween(password, 6, 50)) {
            errors.append("• Password must be between 6 and 50 characters long\n");
        }

        // Validate confirm password
        String confirmPassword = confirmPasswordField.getText();
        if (!InputCheck.isValidString(confirmPassword)) {
            errors.append("• Please confirm your password\n");
        } else if (!password.equals(confirmPassword)) {
            errors.append("• Passwords do not match\n");
        }
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        birthPlaceField.clear();
        fiscalCodeField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        genderComboBox.setValue(null);
        birthDatePicker.setValue(null);
        accountTypeComboBox.setValue(null);
        specializationField.clear();
        referringDoctorComboBox.setValue(null);
        termsCheckBox.setSelected(false);
        hideAllSpecificFields();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLoginLink(MouseEvent event) {
        navigateToLogin();
    }

    private void navigateToLogin() {
        ViewNavigator.getInstance().navigateTo(ViewNavigator.LOGIN_VIEW);
    }
}