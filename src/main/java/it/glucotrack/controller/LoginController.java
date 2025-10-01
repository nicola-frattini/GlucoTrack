package it.glucotrack.controller;

import it.glucotrack.model.User;
import it.glucotrack.util.InputCheck;
import it.glucotrack.util.SessionManager;
import it.glucotrack.view.ViewNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label signUpLabel;

    private SessionManager sessionManager = SessionManager.getInstance();

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

        if (emailField == null || passwordField == null) {
            System.err.println("FXML fields not injected properly!");
            showErrorAlert("Error", "Application not initialized correctly.");
            return;
        }

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (validateInput(email, password)) {
            if (authenticateUser(email, password)) {

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

        boolean loginSuccess = sessionManager.login(email, password);

        if (loginSuccess) {

            String userType = sessionManager.getCurrentUserType();
            navigateBasedOnUserType(userType);
            return true;
        } else {
            showErrorAlert("Login Failed", "Invalid email or password.");
            return false;
        }
    }

    private void navigateBasedOnUserType(String userType) {
        switch (userType.toUpperCase()) {
            case "PATIENT":
                ViewNavigator.getInstance().navigateTo(ViewNavigator.PATIENT_DASHBOARD);
                break;
            case "DOCTOR":
                ViewNavigator.getInstance().navigateTo(ViewNavigator.DOCTOR_DASHBOARD);
                break;
            case "ADMIN": {
                ViewNavigator.getInstance().navigateTo(ViewNavigator.ADMIN_DASHBOARD);

                break;
            }
            default:
                showErrorAlert("Error", "Unknown user type");
        }
    }

    private void handleSignUp() {
        ViewNavigator.getInstance().navigateTo(ViewNavigator.REGISTER_VIEW);
    }

    protected void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }




    public static User getCurrentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }

    public static String getCurrentUserType() {
        return SessionManager.getInstance().getCurrentUserType();
    }

    public static boolean isLoggedIn() {
        return SessionManager.getInstance().isLoggedIn();
    }

    public static void logout() {
        SessionManager.getInstance().logout();
    }

    public static int getCurrentUserId() {
        return SessionManager.getInstance().getCurrentUserId();
    }

    public static String getCurrentUserFullName() {
        return SessionManager.getInstance().getCurrentUserFullName();
    }
}