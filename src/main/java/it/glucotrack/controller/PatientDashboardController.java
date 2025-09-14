package it.glucotrack.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;
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
    private Button messageBtn;

    @FXML
    public void initialize() {
        // Imposta il nome del paziente dalla sessione corrente
        loadPatientInfo();
        
        // Carica la dashboard di default
        loadCenterContent("PatientDashboardHome.fxml");
        setActiveButton(dashboardBtn); // Active the dashboard button by default
    }
    
    private void loadPatientInfo() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                String displayName = currentUser.getName() + " " + currentUser.getSurname();
                patientNameLabel.setText(displayName);
                System.out.println("📊 Dashboard caricato per paziente: " + displayName);
            } else {
                patientNameLabel.setText("Paziente non trovato");
                System.err.println("❌ Nessun utente in sessione nel PatientDashboard!");
            }
        } catch (Exception e) {
            patientNameLabel.setText("Errore caricamento");
            System.err.println("❌ Errore nel caricamento info paziente: " + e.getMessage());
            e.printStackTrace();
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
            // Esegui il logout dalla sessione
            SessionManager.getInstance().logout();
            System.out.println("👋 Logout eseguito con successo");
            
            // Torna alla schermata di login
            ViewNavigator.getInstance().navigateTo(ViewNavigator.LOGIN_VIEW, "GlucoTrack - Login");
        } catch (Exception e) {
            System.err.println("❌ Errore durante il logout: " + e.getMessage());
            e.printStackTrace();
        }
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