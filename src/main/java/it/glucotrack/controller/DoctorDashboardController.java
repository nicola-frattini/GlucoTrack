package it.glucotrack.controller;

import it.glucotrack.util.DoctorDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.sql.SQLException;

import it.glucotrack.util.SessionManager;
import it.glucotrack.model.User;
import it.glucotrack.model.Doctor;

public class DoctorDashboardController {

    @FXML
    private StackPane contentPane;

    @FXML
    private Label doctorNameLabel;

    @FXML
    private Label doctorRoleLabel;

    @FXML
    private Button dashboardBtn;
    @FXML
    private Button patientsBtn;
    @FXML
    private Button medicationsBtn;

    private Doctor doctor;



    public void initialize() throws SQLException {

        loadDoctorInfo();

        this.doctor = DoctorDAO.getDoctorById(SessionManager.getInstance().getCurrentUserId());


        loadCenterContent("DoctorDashboardHome.fxml");
        setActiveButton(dashboardBtn);

    }

    private void loadDoctorInfo() {
        try {
            User currentUser = DoctorDAO.getDoctorById(SessionManager.getCurrentUser().getId());
            if (currentUser != null) {
                String displayName = currentUser.getName() + " " + currentUser.getSurname();
                doctorNameLabel.setText(displayName);



                if (currentUser instanceof Doctor) {
                    Doctor currentDoctor = (Doctor) currentUser;
                    doctorRoleLabel.setText(currentDoctor.getSpecialization());
                } else {
                    doctorRoleLabel.setText("Doctor");
                }
            } else {
                doctorNameLabel.setText("Doctor not found");
                doctorRoleLabel.setText("");
            }
        } catch (Exception e) {
            doctorNameLabel.setText("Errore caricamento");
            doctorRoleLabel.setText("");
            e.printStackTrace();
        }
    }

    @FXML
    private void onDashboardClick() {
        loadCenterContent("DoctorDashboardHome.fxml");
        setActiveButton(dashboardBtn);
    }

    @FXML
    private void onPatientsClick() {
        loadCenterContent("DoctorDashboardPatients.fxml");
        setActiveButton(patientsBtn);
    }

    @FXML
    private void onMedicationsClick() {
        loadCenterContent("DoctorDashboardMedications.fxml");
        setActiveButton(medicationsBtn);
    }

    @FXML
    private void onProfileClick() throws IOException, SQLException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/ProfileView.fxml"));
        Parent profileRoot = loader.load();

        ProfileViewController profileController = loader.getController();
        profileController.setUserRole(ProfileViewController.UserRole.DOCTOR_OWN_PROFILE, null);

        profileController.setParentContentPane(contentPane);
        loadCenterContentDirect(profileRoot);


        clearActiveButtons();
    }

    void loadCenterContent(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/" + fxmlFile));
            Node node = loader.load();
            contentPane.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void loadCenterContentDirect(Node content) {
        contentPane.getChildren().setAll(content);
    }


    @FXML
    private void onLogoutClick() {
        try {
            SessionManager.getInstance().logout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button activeBtn) {

        dashboardBtn.setStyle(getButtonStyle(dashboardBtn == activeBtn));
        patientsBtn.setStyle(getButtonStyle(patientsBtn == activeBtn));
        medicationsBtn.setStyle(getButtonStyle(medicationsBtn == activeBtn));
    }

    private void clearActiveButtons() {

        dashboardBtn.setStyle(getButtonStyle(false));
        patientsBtn.setStyle(getButtonStyle(false));
        medicationsBtn.setStyle(getButtonStyle(false));
    }



    private String getButtonStyle(boolean isActive) {
        if (isActive) {
            return "-fx-background-color: #64b5f6; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14px; -fx-padding: 12 20; -fx-cursor: hand; -fx-alignment: center-left;";
        } else {
            return "-fx-background-color: transparent; -fx-text-fill: #8892b0; -fx-background-radius: 8; -fx-font-size: 14px; -fx-padding: 12 20; -fx-cursor: hand; -fx-alignment: center-left;";
        }
    }
}