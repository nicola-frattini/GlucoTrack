package it.glucotrack.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import it.glucotrack.model.User;
import it.glucotrack.model.Gender;
import it.glucotrack.util.InputCheck;
import it.glucotrack.view.ViewNavigator;

import java.io.IOException;
import java.time.LocalDate;

public class RegisterController {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

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

    @FXML
    private CheckBox termsCheckBox;

    @FXML
    private Button registerButton;

    @FXML
    private Label loginLabel;

    public void initialize() {
        setupComboBoxes();
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        registerButton.setOnAction(this::handleRegister);
        loginLabel.setOnMouseClicked(this::handleLoginLink);
    }

    private void setupComboBoxes() {
        // Popola gender ComboBox
        genderComboBox.getItems().addAll("Male", "Female");

        // Popola account type ComboBox
        accountTypeComboBox.getItems().addAll("Patient", "Doctor", "Administrator");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        if (!validateInput()) {
            return;
        }
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String genderStr = genderComboBox.getValue();
            LocalDate birthDate = birthDatePicker.getValue();
            String accountType = accountTypeComboBox.getValue();

            // Converti la stringa gender in enum usando il metodo fromString
            Gender gender = Gender.fromString(genderStr);

            User newUser = new User();
            newUser.setName(firstName);
            newUser.setSurname(lastName);
            newUser.setEmail(email);
            newUser.setPassword(password);
            newUser.setGender(gender);
            newUser.setBornDate(birthDate);

            showSuccessAlert("Registration Successful", "Your account has been created successfully!");
            navigateToLogin();

        } catch (Exception e) {
            showErrorAlert("Registration Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Valida firstName
        String firstName = firstNameField.getText().trim();
        if (!InputCheck.isValidString(firstName)) {
            errors.append("• First name is required\n");
        } else if (!InputCheck.isAlphabetic(firstName)) {
            errors.append("• First name must contain only letters\n");
        }

        // Valida lastName
        String lastName = lastNameField.getText().trim();
        if (!InputCheck.isValidString(lastName)) {
            errors.append("• Last name is required\n");
        } else if (!InputCheck.isAlphabetic(lastName)) {
            errors.append("• Last name must contain only letters\n");
        }

        // Valida email
        String email = emailField.getText().trim();
        if (!InputCheck.isValidString(email)) {
            errors.append("• Email is required\n");
        } else if (!InputCheck.isValidEmail(email)) {
            errors.append("• Please enter a valid email address\n");
        }

        // Valida password
        String password = passwordField.getText();
        if (!InputCheck.isValidString(password)) {
            errors.append("• Password is required\n");
        } else if (!InputCheck.hasLengthBetween(password, 6, 50)) {
            errors.append("• Password must be between 6 and 50 characters long\n");
        }

        // Valida confirm password
        String confirmPassword = confirmPasswordField.getText();
        if (!InputCheck.isValidString(confirmPassword)) {
            errors.append("• Please confirm your password\n");
        } else if (!password.equals(confirmPassword)) {
            errors.append("• Passwords do not match\n");
        }

        // Valida gender
        if (genderComboBox.getValue() == null) {
            errors.append("• Please select your gender\n");
        }

        // Valida birth date
        if (birthDatePicker.getValue() == null) {
            errors.append("• Please select your birth date\n");
        }

        // Valida account type
        if (accountTypeComboBox.getValue() == null) {
            errors.append("• Please select an account type\n");
        }

        // Valida terms checkbox
        if (!termsCheckBox.isSelected()) {
            errors.append("• You must accept the Terms and Conditions\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Validation Error", errors.toString());
            return false;
        }

        return true;
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