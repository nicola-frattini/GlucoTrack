package it.glucotrack.controller;

import it.glucotrack.util.PatientDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.sql.SQLException;

import it.glucotrack.view.ViewNavigator;
import it.glucotrack.util.SessionManager;
import it.glucotrack.model.User;
import it.glucotrack.model.Patient;

public class PatientDashboardController {


    @FXML
    private StackPane contentPane;

    @FXML
    private Label patientNameLabel;

    @FXML
    private Button dashboardBtn;
    @FXML
    private Button readingsBtn;
    @FXML
    private Button medicationBtn;
    @FXML
    private Button symptomsBtn;

    @FXML
    private Button contactBtn;

    private Patient patient;

    @FXML
    public void initialize() {

        instance = this;

        this.patient = loadPatientInfo();


        loadCenterContent("PatientDashboardHome.fxml");
        setActiveButton(dashboardBtn); // Active the dashboard button by default

    }
    
    private Patient loadPatientInfo() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                String displayName = currentUser.getName() + " " + currentUser.getSurname();
                patientNameLabel.setText(displayName);
            } else {
                patientNameLabel.setText("Patient not found");
            }
            return new Patient(PatientDAO.getPatientById(currentUser.getId()));
        } catch (Exception e) {
            patientNameLabel.setText("Loading error");
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void onDashboardClick() {
        loadCenterContent("PatientDashboardHome.fxml");
        setActiveButton(dashboardBtn);
    }

    @FXML
    private void onReadingsClick() {
        loadCenterContent("PatientDashboardReadings.fxml");
        setActiveButton(readingsBtn);
    }


    @FXML
    private void onMedicationClick() {
        loadCenterContent("PatientDashboardMedications.fxml");
        setActiveButton(medicationBtn);
    }

    @FXML
    private void onSymptomsClick() {
        loadCenterContent("PatientDashboardSymptoms.fxml");
        setActiveButton(symptomsBtn);
    }


    @FXML
    private void onProfileClick() throws IOException, SQLException {


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/ProfileView.fxml"));
        Parent profileRoot = loader.load();

        ProfileViewController profileController = loader.getController();

        profileController.setUserRole(ProfileViewController.UserRole.PATIENT_OWN_PROFILE, null);

        profileController.setParentContentPane(contentPane);
        loadCenterContentDirect(profileRoot);

        clearActiveButtons();
    }

    public void loadCenterContent(String fxmlFile) {
        try {

            Node node = FXMLLoader.load(getClass().getResource("/assets/fxml/" + fxmlFile));

            contentPane.getChildren().setAll(node);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public void loadCenterContentDirect(Node content) {
        contentPane.getChildren().setAll(content);
    }
    

    private static PatientDashboardController instance;
    

    public static PatientDashboardController getInstance() {
        return instance;
    }

    @FXML
    private void onLogoutClick() {
        try {
            SessionManager.getInstance().logout();
            ViewNavigator.getInstance().navigateTo(ViewNavigator.LOGIN_VIEW, "GlucoTrack - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void setActiveButton(Button activeBtn) {
        dashboardBtn.setStyle(getButtonStyle(dashboardBtn == activeBtn));
        readingsBtn.setStyle(getButtonStyle(readingsBtn == activeBtn));
        medicationBtn.setStyle(getButtonStyle(medicationBtn == activeBtn));
        symptomsBtn.setStyle(getButtonStyle(symptomsBtn == activeBtn));
    }

    private void clearActiveButtons() {

        dashboardBtn.setStyle(getButtonStyle(false));
        readingsBtn.setStyle(getButtonStyle(false));
        medicationBtn.setStyle(getButtonStyle(false));
        symptomsBtn.setStyle(getButtonStyle(false));
    }

    private String getButtonStyle(boolean isActive) {
        if (isActive) {
            return "-fx-background-color: #64b5f6; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14px; -fx-padding: 12 20; -fx-cursor: hand; -fx-alignment: center-left;";
        } else {
            return "-fx-background-color: transparent; -fx-text-fill: #8892b0; -fx-background-radius: 8; -fx-font-size: 14px; -fx-padding: 12 20; -fx-cursor: hand; -fx-alignment: center-left;";
        }
    }
}