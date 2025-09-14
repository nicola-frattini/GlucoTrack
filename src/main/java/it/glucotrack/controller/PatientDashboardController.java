package it.glucotrack.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import it.glucotrack.view.ViewNavigator;

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
    private Button messageBtn;

    @FXML
    public void initialize() {
        // Imposta il nome del medico/paziente se necessario
        patientNameLabel.setText("Mario Rossi"); // We'll get dynamically the name of the doctor
        // Carica la dashboard di default
        loadCenterContent("PatientDashboardHome.fxml");
        setActiveButton(dashboardBtn); // Active the dashboard button by default

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
        // Qui puoi mettere la logica per il logout, ad esempio tornare alla schermata di login
        ViewNavigator.getInstance().navigateTo(ViewNavigator.LOGIN_VIEW, "GlucoTrack - Login");
    }



    private void setActiveButton(Button activeBtn) {
        // Cambia lo stile dei pulsanti per evidenziare quello attivo
        dashboardBtn.setStyle(getButtonStyle(dashboardBtn == activeBtn));
        readingsBtn.setStyle(getButtonStyle(readingsBtn == activeBtn));
        medicationBtn.setStyle(getButtonStyle(medicationBtn == activeBtn));
        symptomsBtn.setStyle(getButtonStyle(symptomsBtn == activeBtn));
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