package it.glucotrack.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import it.glucotrack.util.SessionManager;

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
    private Button appointmentsBtn;
    @FXML
    private Button messageBtn;

    @FXML
    public void initialize() {
        // Imposta il nome del medico/paziente se necessario
        doctorNameLabel.setText("Amelia Chen"); // We'll get dynamically the name of the doctor
        doctorRoleLabel.setText("Endocrinologist"); // We'll get dynamically the role of the doctor
        // Carica la dashboard di default
        loadCenterContent("DoctorDashboardHome.fxml");
        setActiveButton(dashboardBtn); // Active the dashboard button by default

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
    private void onAppointmentsClick() {
        loadCenterContent("DoctorDashboardMessages.fxml");
        setActiveButton(appointmentsBtn);
    }

    @FXML
    private void onMessagesClick() {
        loadCenterContent("PatientMessages.fxml");
        setActiveButton(messageBtn);
    }

    @FXML
    private void onSettingsClick() {
        // Logica per aprire le impostazioni, oppure lascia vuoto se non ti serve
    }

    private void loadCenterContent(String fxmlFile) {
        try {
            Node node = FXMLLoader.load(getClass().getResource("/assets/fxml/" + fxmlFile));
            contentPane.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
            // Puoi mostrare un messaggio di errore all'utente
        }
    }

    @FXML
    private void onLogoutClick() {
        try {
            SessionManager.getInstance().logout();
            System.out.println("👋 Logout eseguito con successo");
        } catch (Exception e) {
            System.err.println("❌ Errore durante il logout: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void setActiveButton(Button activeBtn) {
        // Cambia lo stile dei pulsanti per evidenziare quello attivo
        dashboardBtn.setStyle(getButtonStyle(dashboardBtn == activeBtn));
        patientsBtn.setStyle(getButtonStyle(patientsBtn == activeBtn));
        appointmentsBtn.setStyle(getButtonStyle(appointmentsBtn == activeBtn));
        messageBtn.setStyle(getButtonStyle(messageBtn == activeBtn));
    }

    private String getButtonStyle(boolean isActive) {
        if (isActive) {
            return "-fx-background-color: #64b5f6; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14px; -fx-padding: 12 20; -fx-cursor: hand; -fx-alignment: center-left;";
        } else {
            return "-fx-background-color: transparent; -fx-text-fill: #8892b0; -fx-background-radius: 8; -fx-font-size: 14px; -fx-padding: 12 20; -fx-cursor: hand; -fx-alignment: center-left;";
        }
    }
}