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
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (validateInput(email, password)) {
            if (authenticateUser(email, password)) {
                //navigateToMainApp();
                showErrorAlert("Login Successful", "Welcome, " + email + "!");
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
        return "admin".equals(email) && "password".equals(password);
    }

    private void handleSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/RegisterView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) signUpLabel.getScene().getWindow();

            // Salva le dimensioni correnti
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();

            Scene newScene = new Scene(root, width, height);
            stage.setScene(newScene);
            stage.setTitle("GlucoTrack - Register");

            // Ripristina lo stato della finestra
            if (wasMaximized) {
                stage.setMaximized(true);
            }

        } catch (IOException e) {
            showErrorAlert("Navigation Error", "Could not load registration page.");
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}