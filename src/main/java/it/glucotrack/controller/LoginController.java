package it.glucotrack.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import it.glucotrack.util.InputCheck;
import java.io.IOException;
import it.glucotrack.view.ViewNavigator;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Button loginButton;

    @FXML
    private Label signUpLabel;

    @FXML
    private void initialize() {
        System.out.println("LoginController initialize() called");
        if (signUpLabel != null) {
            signUpLabel.setOnMouseClicked(event -> handleSignUp());
        }
        if (emailField != null) {
            emailField.setOnAction(this::handleLogin);
        }
        if (passwordField != null) {
            passwordField.setOnAction(this::handleLogin);
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        // Controllo che i campi siano stati iniettati correttamente
        if (emailField == null || passwordField == null) {
            System.err.println("FXML fields not injected properly!");
            showErrorAlert("Error", "Application not initialized correctly.");
            return;
        }

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (validateInput(email, password)) {
            if (authenticateUser(email, password)) {
                // Login riuscito - la navigazione è gestita in authenticateUser
            } else {
                showErrorAlert("Login failed", "Invalid email or password.");
            }
        }
    }



    private boolean validateInput(String email, String password) {
        if (!InputCheck.isValidString(email)) {
            showErrorAlert("Validation Error", "Please enter your email or email.");
            emailField.requestFocus();
            return false;
        }

        if (!InputCheck.isValidString(password)) {
            showErrorAlert("Validation Error", "Please enter your password.");
            passwordField.requestFocus();
            return false;
        }
        
        return true;
    }

    public boolean authenticateUser(String email, String password) {
        // TODO: Implementare autenticazione reale con database
        if ("admin".equals(email) && "password".equals(password)) {
            navigateBasedOnUserType("ADMIN");
            return true;
        }
        if("doctor".equals(email) && "password".equals(password)) {
            navigateBasedOnUserType("DOCTOR");
            return true;
        }
        if ("patient".equals(email) && "password".equals(password)) {
            navigateBasedOnUserType("PATIENT");
            return true;
        }
        return false;
    }

    private void navigateBasedOnUserType(String userType) {
        switch (userType.toUpperCase()) {
            case "PATIENT":
                ViewNavigator.getInstance().navigateTo(ViewNavigator.PATIENT_DASHBOARD);
                break;
            case "DOCTOR":
                ViewNavigator.getInstance().navigateTo(ViewNavigator.DOCTOR_DASHBOARD);
                break;
            case "ADMIN":
                ViewNavigator.getInstance().navigateTo(ViewNavigator.ADMIN_DASHBOARD);
                break;
            default:
                showErrorAlert("Error", "Unknown user type");
        }
    }

    private void handleSignUp() {
        ViewNavigator.getInstance().navigateTo(ViewNavigator.REGISTER_VIEW);
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}