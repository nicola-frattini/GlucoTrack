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
import it.glucotrack.model.Doctor; // Importa la classe Doctor se esiste

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

    @FXML
    public void initialize() throws SQLException {
        // Carica le informazioni del medico
        loadDoctorInfo();

        this.doctor = DoctorDAO.getDoctorById(SessionManager.getInstance().getCurrentUserId());

        // Carica la dashboard di default
        loadCenterContent("DoctorDashboardHome.fxml");
        setActiveButton(dashboardBtn); // Attiva il pulsante della dashboard per default
    }

    private void loadDoctorInfo() {
        try {
            User currentUser = DoctorDAO.getDoctorById(SessionManager.getCurrentUser().getId());
            if (currentUser != null) {
                String displayName = currentUser.getName() + " " + currentUser.getSurname();
                doctorNameLabel.setText(displayName);

                // Assicurati che il ruolo sia disponibile nell'oggetto User o Doctor
                // Se hai una classe Doctor, puoi fare un cast
                if (currentUser instanceof Doctor) {
                    Doctor currentDoctor = (Doctor) currentUser;
                    doctorRoleLabel.setText(currentDoctor.getSpecialization()); // Assumendo che esista un metodo getRole()
                } else {
                    // Imposta un ruolo di default se non è un medico o se il ruolo non è specificato
                    doctorRoleLabel.setText("Medico generico");
                }
                System.out.println("📊 Dashboard caricato per medico: " + displayName);
            } else {
                doctorNameLabel.setText("Dottore non trovato");
                doctorRoleLabel.setText("");
                System.err.println("❌ Nessun utente in sessione nel DoctorDashboard!");
            }
        } catch (Exception e) {
            doctorNameLabel.setText("Errore caricamento");
            doctorRoleLabel.setText("");
            System.err.println("❌ Errore nel caricamento info medico: " + e.getMessage());
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
        loadCenterContent("DoctorDashboarMedications.fxml");
        setActiveButton(medicationsBtn);
    }

    @FXML void onProfileClick() throws IOException, SQLException {
        System.out.println("🔄 Caricamento profilo paziente...");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/ProfileView.fxml"));
        Parent profileRoot = loader.load();
        System.out.println("✅ FXML ProfileView caricato con successo");
        ProfileViewController profileController = loader.getController();
        profileController.setUserRole(ProfileViewController.UserRole.DOCTOR_OWN_PROFILE, null);

        profileController.setParentContentPane(contentPane);
        loadCenterContentDirect(profileRoot);


        System.out.println("🔄 Impostazione contenuto profilo nel contentPane...");
        // Non imposto nessun pulsante come attivo per il profilo
        clearActiveButtons();
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

    public void loadCenterContentDirect(Node content) {
        contentPane.getChildren().setAll(content);
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
        medicationsBtn.setStyle(getButtonStyle(medicationsBtn == activeBtn));
    }

    private void clearActiveButtons() {
        // Imposta tutti i pulsanti come non attivi
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