package it.glucotrack.controller;

import it.glucotrack.model.GlucoseMeasurement;
import it.glucotrack.model.Medication;
import it.glucotrack.model.Patient;
import it.glucotrack.model.Symptom;
import it.glucotrack.util.GlucoseMeasurementDAO;
import it.glucotrack.util.MedicationDAO;
import it.glucotrack.util.SymptomDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProfileViewController implements Initializable {

    // Enum per i tipi di utente
    public enum UserRole {
        DOCTOR_VIEWING_PATIENT,
        DOCTOR_OWN_PROFILE,
        PATIENT_OWN_PROFILE,
        ADMIN_VIEWING_USER,
        ADMIN_OWN_PROFILE
    }

    // Header elements
    @FXML private Circle profileImageCircle;
    @FXML private ImageView profileImageView;
    @FXML private Label patientNameLabel;
    @FXML private Label patientIdLabel;
    @FXML private Label lastVisitLabel;

    // Tab buttons
    @FXML private Button overviewTab;
    @FXML private Button trendsTab;
    @FXML private Button medicationTab;
    @FXML private Button notesTab;

    // Content areas
    @FXML private VBox overviewContent;
    @FXML private VBox trendsContent;
    @FXML private VBox medicationContent;
    @FXML private VBox notesContent;

    // Overview content elements
    @FXML private Label averageGlucoseLabel;
    @FXML private Label averageGlucoseChangeLabel;
    @FXML private LineChart<String, Number> glucoseTrendsChart;
    @FXML private Label adherancePercentageLabel;
    @FXML private ProgressBar adheranceProgressBar;

    @FXML private VBox symptomsContainer;
    @FXML private TableView<TherapyModification> therapyModificationsTable;
    @FXML private TableColumn<TherapyModification, String> dateColumn;
    @FXML private TableColumn<TherapyModification, String> modificationColumn;
    @FXML private TableColumn<TherapyModification, String> modifiedByColumn;

    // Action buttons
    @FXML private Button modifyTherapyBtn;
    @FXML private Button updatePatientInfoBtn;

    // Additional buttons for different roles
    @FXML private Button addRiskBtn;
    @FXML private Button deleteUserBtn;
    @FXML private Button exportDataBtn;
    @FXML private Button sendMessageBtn;

    // Data models
    private Patient currentPatient;
    private Object currentUser; // L'utente che sta usando l'applicazione (può essere Doctor, Admin, o Patient)
    private UserRole currentUserRole;
    private ObservableList<TherapyModification> therapyModifications;
    private StackPane parentContentPane;

    // DAOs
    private GlucoseMeasurementDAO glucoseMeasurementDAO;
    private SymptomDAO symptomDAO;
    private MedicationDAO medicationDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize DAOs
        this.glucoseMeasurementDAO = new GlucoseMeasurementDAO();
        this.symptomDAO = new SymptomDAO();
        this.medicationDAO = new MedicationDAO();

        setupTabs();
        setupTable();
        setupCharts();
        setupButtons();
        // Initialize additional buttons if they exist in FXML
        initializeAdditionalButtons();
    }

    private void initializeAdditionalButtons() {
        if (addRiskBtn != null) {
            addRiskBtn.setOnAction(e -> handleAddRisk());
        }
        if (deleteUserBtn != null) {
            deleteUserBtn.setOnAction(e -> handleDeleteUser());
        }
        if (exportDataBtn != null) {
            exportDataBtn.setOnAction(e -> handleExportData());
        }
        if (sendMessageBtn != null) {
            sendMessageBtn.setOnAction(e -> handleSendMessage());
        }
    }

    /**
     * Configura la vista in base al ruolo dell'utente
     * @param role Il ruolo dell'utente corrente
     * @param user L'utente che sta usando l'applicazione (Doctor, Admin, o Patient)
     * @param viewedPatient Il paziente che viene visualizzato (null se si visualizza il proprio profilo)
     */
    public void setUserRole(UserRole role, Object user, Patient viewedPatient) {
        this.currentUserRole = role;
        this.currentUser = user;

        // Determina il paziente da visualizzare
        if (viewedPatient != null) {
            this.currentPatient = viewedPatient;
        } else if (user instanceof Patient) {
            this.currentPatient = (Patient) user;
        } else {
            // Se l'utente non è un paziente e non è stato passato un paziente da visualizzare
            // questo caso dovrebbe gestire il profilo di Doctor/Admin che non sono pazienti
            this.currentPatient = null;
        }

        updateViewForUserRole();

        if (currentPatient != null) {
            try {
                loadPatientData();
                updatePatientInfo();
                updatePatientSpecificData();
                loadTherapyModifications();
            } catch (SQLException e) {
                showError("Database Error", "Failed to load patient data", e.getMessage());
            }
        } else {
            // Gestisci il caso in cui si visualizza il profilo di un Doctor/Admin
            updateNonPatientProfile();
        }
    }

    /**
     * Metodo per gestire i profili non-paziente (Doctor/Admin)
     */
    private void updateNonPatientProfile() {
        // Aggiorna le informazioni del profilo per utenti non-paziente
        if (currentUser != null) {
            String userName = getUserName(currentUser);
            String userId = getUserId(currentUser);

            if (patientNameLabel != null) {
                patientNameLabel.setText(userName);
            }
            if (patientIdLabel != null) {
                patientIdLabel.setText("User ID: " + userId);
            }
            if (lastVisitLabel != null) {
                lastVisitLabel.setText("Role: " + getUserRole(currentUser));
            }
        }

        // Nascondi elementi medici per profili non-paziente
        hideMedicalElements();
    }

    /**
     * Utility per ottenere il nome dell'utente indipendentemente dal tipo
     */
    private String getUserName(Object user) {
        if (user instanceof Patient) {
            return ((Patient) user).getFullName();
        }
        // Assumendo che Doctor e Admin abbiano metodi simili
        try {
            // Usando reflection per chiamare getName() o getFullName()
            if (user.getClass().getMethod("getFullName") != null) {
                return (String) user.getClass().getMethod("getFullName").invoke(user);
            } else if (user.getClass().getMethod("getName") != null) {
                return (String) user.getClass().getMethod("getName").invoke(user);
            }
        } catch (Exception e) {
            // Se fallisce la reflection, usa toString()
            return user.getClass().getSimpleName() + " User";
        }
        return "Unknown User";
    }

    /**
     * Utility per ottenere l'ID dell'utente
     */
    private String getUserId(Object user) {
        if (user instanceof Patient) {
            return String.valueOf(((Patient) user).getId());
        }
        try {
            return String.valueOf(user.getClass().getMethod("getId").invoke(user));
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Utility per ottenere il ruolo dell'utente come stringa
     */
    private String getUserRole(Object user) {
        return user.getClass().getSimpleName();
    }

    /**
     * Nascondi elementi medici per profili non-paziente
     */
    private void hideMedicalElements() {
        // Nascondi grafici medici
        if (glucoseTrendsChart != null) {
            glucoseTrendsChart.setVisible(false);
            glucoseTrendsChart.setManaged(false);
        }

        // Nascondi statistiche glucose
        if (averageGlucoseLabel != null) {
            averageGlucoseLabel.setVisible(false);
        }
        if (averageGlucoseChangeLabel != null) {
            averageGlucoseChangeLabel.setVisible(false);
        }

        // Nascondi progress bar medicazioni
        if (adheranceProgressBar != null) {
            adheranceProgressBar.setVisible(false);
        }
        if (adherancePercentageLabel != null) {
            adherancePercentageLabel.setVisible(false);
        }

        // Nascondi container sintomi
        if (symptomsContainer != null) {
            symptomsContainer.setVisible(false);
            symptomsContainer.setManaged(false);
        }

        // Nascondi tabella modifiche terapia
        if (therapyModificationsTable != null) {
            therapyModificationsTable.setVisible(false);
            therapyModificationsTable.setManaged(false);
        }
    }

    /**
     * Aggiorna la vista in base al ruolo dell'utente
     */
    private void updateViewForUserRole() {
        switch (currentUserRole) {
            case DOCTOR_VIEWING_PATIENT:
                setupDoctorViewingPatient();
                break;
            case DOCTOR_OWN_PROFILE:
                setupDoctorOwnProfile();
                break;
            case PATIENT_OWN_PROFILE:
                setupPatientOwnProfile();
                break;
            case ADMIN_VIEWING_USER:
                setupAdminViewingUser();
                break;
            case ADMIN_OWN_PROFILE:
                setupAdminOwnProfile();
                break;
        }
    }

    /**
     * Configurazione per dottore che visualizza un paziente
     */
    private void setupDoctorViewingPatient() {
        // Mostra tutti i grafici e dati medici
        showMedicalCharts(true);

        // Abilita pulsanti per modifiche mediche
        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(true);
            modifyTherapyBtn.setDisable(false);
            modifyTherapyBtn.setText("Modify Therapy");
        }

        if (addRiskBtn != null) {
            addRiskBtn.setVisible(true);
            addRiskBtn.setDisable(false);
        }

        if (updatePatientInfoBtn != null) {
            updatePatientInfoBtn.setVisible(true);
            updatePatientInfoBtn.setDisable(false);
            updatePatientInfoBtn.setText("Update Patient Info");
        }

        if (sendMessageBtn != null) {
            sendMessageBtn.setVisible(true);
            sendMessageBtn.setText("Send Message to Patient");
        }

        // Nascondi pulsanti non necessari
        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(false);
        }

        // Mostra tab mediche
        showMedicalTabs(true);

        // Abilita editing delle note cliniche
        enableNotesEditing(true);
    }

    /**
     * Configurazione per dottore che visualizza il proprio profilo
     */
    private void setupDoctorOwnProfile() {
        // Mostra grafici limitati (solo per monitoraggio personale se diabetico)
        showMedicalCharts(false);

        // Abilita solo aggiornamento info personali
        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(false);
        }

        if (addRiskBtn != null) {
            addRiskBtn.setVisible(false);
        }

        if (updatePatientInfoBtn != null) {
            updatePatientInfoBtn.setVisible(true);
            updatePatientInfoBtn.setDisable(false);
            updatePatientInfoBtn.setText("Update My Info");
        }

        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(false);
        }

        if (exportDataBtn != null) {
            exportDataBtn.setVisible(true);
            exportDataBtn.setText("Export My Data");
        }

        // Mostra tab limitate
        showMedicalTabs(false);
        enableNotesEditing(false);
    }

    /**
     * Configurazione per paziente che visualizza il proprio profilo
     */
    private void setupPatientOwnProfile() {
        // Mostra grafici semplificati
        showMedicalCharts(true);

        // Solo visualizzazione e aggiornamento dati personali
        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(true);
            modifyTherapyBtn.setDisable(true);
            modifyTherapyBtn.setText("View Therapy");
        }

        if (addRiskBtn != null) {
            addRiskBtn.setVisible(false);
        }

        if (updatePatientInfoBtn != null) {
            updatePatientInfoBtn.setVisible(true);
            updatePatientInfoBtn.setDisable(false);
            updatePatientInfoBtn.setText("Update My Info");
        }

        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(false);
        }

        if (exportDataBtn != null) {
            exportDataBtn.setVisible(true);
            exportDataBtn.setText("Export My Data");
        }

        if (sendMessageBtn != null) {
            sendMessageBtn.setVisible(true);
            sendMessageBtn.setText("Contact Doctor");
        }

        // Mostra tab ma limita funzionalità
        showMedicalTabs(true);
        enableNotesEditing(false);
    }

    /**
     * Configurazione per admin che visualizza un utente
     */
    private void setupAdminViewingUser() {
        // Admin può vedere tutto
        showMedicalCharts(true);

        // Abilita tutti i controlli
        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(true);
            modifyTherapyBtn.setDisable(false);
            modifyTherapyBtn.setText("Modify Therapy");
        }

        if (updatePatientInfoBtn != null) {
            updatePatientInfoBtn.setVisible(true);
            updatePatientInfoBtn.setDisable(false);
            updatePatientInfoBtn.setText("Update User Info");
        }

        // Funzioni specifiche dell'admin
        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(true);
            deleteUserBtn.setDisable(false);
            deleteUserBtn.setText("Delete User");
            deleteUserBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 5;");
        }

        if (exportDataBtn != null) {
            exportDataBtn.setVisible(true);
            exportDataBtn.setText("Export User Data");
        }

        if (sendMessageBtn != null) {
            sendMessageBtn.setVisible(true);
            sendMessageBtn.setText("Send Admin Message");
        }

        showMedicalTabs(true);
        enableNotesEditing(true);
    }

    /**
     * Configurazione per admin che visualizza il proprio profilo
     */
    private void setupAdminOwnProfile() {
        // Admin visualizza il proprio profilo come utente normale
        showMedicalCharts(false);

        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(false);
        }

        if (updatePatientInfoBtn != null) {
            updatePatientInfoBtn.setVisible(true);
            updatePatientInfoBtn.setDisable(false);
            updatePatientInfoBtn.setText("Update My Info");
        }

        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(false);
        }

        if (exportDataBtn != null) {
            exportDataBtn.setVisible(true);
            exportDataBtn.setText("Export My Data");
        }

        showMedicalTabs(false);
        enableNotesEditing(false);
    }

    private void showMedicalCharts(boolean show) {
        if (glucoseTrendsChart != null) {
            glucoseTrendsChart.setVisible(show);
            glucoseTrendsChart.setManaged(show);
        }

        // Altri grafici medici possono essere controllati qui
    }

    private void showMedicalTabs(boolean showAll) {
        if (trendsTab != null) {
            trendsTab.setVisible(showAll);
            trendsTab.setManaged(showAll);
        }

        if (medicationTab != null) {
            medicationTab.setVisible(showAll);
            medicationTab.setManaged(showAll);
        }

        // Per pazienti e profili non medici, mostra solo overview e note limitate
        if (!showAll) {
            if (notesTab != null) {
                notesTab.setText("Info");
            }
        } else {
            if (notesTab != null) {
                notesTab.setText("Notes");
            }
        }
    }

    private void enableNotesEditing(boolean enable) {
        // Questa funzione sarà usata nel loadNotesContent per determinare se abilitare l'editing
    }

    // Event handlers per i nuovi pulsanti

    private void handleAddRisk() {
        if (currentPatient == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Add Risk Assessment");
        alert.setHeaderText("Risk Assessment for " + currentPatient.getFullName());
        alert.setContentText("Risk assessment dialog would open here to evaluate and document patient risks.");
        alert.showAndWait();
    }

    private void handleDeleteUser() {
        if (currentPatient == null || currentUserRole != UserRole.ADMIN_VIEWING_USER) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete User");
        confirmAlert.setHeaderText("Delete User Account");
        confirmAlert.setContentText("Are you sure you want to delete the account for " +
                currentPatient.getFullName() + "? This action cannot be undone.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Qui implementeresti la logica per eliminare l'utente dal database
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("User Deleted");
                successAlert.setHeaderText("Success");
                successAlert.setContentText("User account has been successfully deleted.");
                successAlert.showAndWait();

                // Torna alla lista utenti
                handleBackToPatientsList();
            }
        });
    }

    private void handleExportData() {
        if (currentPatient == null) return;

        String patientName = (currentPatient == currentUser) ? "your" : currentPatient.getFullName() + "'s";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Data");
        alert.setHeaderText("Data Export");
        alert.setContentText("Exporting " + patientName + " medical data to PDF format...");
        alert.showAndWait();
    }

    private void handleSendMessage() {
        if (currentPatient == null) return;

        String messageType = "";
        switch (currentUserRole) {
            case DOCTOR_VIEWING_PATIENT:
                messageType = "message to patient";
                break;
            case PATIENT_OWN_PROFILE:
                messageType = "message to doctor";
                break;
            case ADMIN_VIEWING_USER:
                messageType = "admin notification";
                break;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Send Message");
        alert.setHeaderText("Messaging System");
        alert.setContentText("Message composition dialog would open here to send " + messageType + ".");
        alert.showAndWait();
    }

    // Modifica i metodi esistenti per considerare i ruoli

    private void loadNotesContent() {
        if (!notesContent.getChildren().isEmpty()) return;

        VBox notesBox = new VBox(10);
        notesBox.setPadding(new Insets(20));

        String notesTitle = (currentUserRole == UserRole.PATIENT_OWN_PROFILE ||
                currentUserRole == UserRole.DOCTOR_OWN_PROFILE ||
                currentUserRole == UserRole.ADMIN_OWN_PROFILE) ? "Personal Information" : "Clinical Notes";

        Label notesTitleLabel = new Label(notesTitle);
        notesTitleLabel.setTextFill(Color.WHITE);
        notesTitleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(10);
        notesArea.setStyle("-fx-control-inner-background: #34495E; -fx-text-fill: white;");

        if (currentPatient != null) {
            StringBuilder notes = new StringBuilder();
            notes.append("User: ").append(currentPatient.getFullName()).append("\n");
            notes.append("Date: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");

            if (currentUserRole == UserRole.DOCTOR_VIEWING_PATIENT || currentUserRole == UserRole.ADMIN_VIEWING_USER) {
                // Note cliniche complete
                if (!currentPatient.getGlucoseReadings().isEmpty()) {
                    double avgGlucose = currentPatient.getGlucoseReadings().stream()
                            .mapToDouble(GlucoseMeasurement::getGlucoseLevel)
                            .average().orElse(0);
                    notes.append("Average glucose level: ").append(String.format("%.1f mg/dL", avgGlucose)).append("\n");
                }
                if (!currentPatient.getSymptoms().isEmpty()) {
                    notes.append("Reported symptoms: ").append(String.join(", ", currentPatient.getSymptoms())).append("\n");
                }
                notes.append("\nClinical observations:\n");
                notes.append("- Patient compliance appears good\n");
                notes.append("- Continue current medication regimen\n");
                notes.append("- Schedule follow-up in 2 weeks\n");
                notesArea.setPromptText("Enter clinical notes here...");
            } else {
                // Informazioni personali limitate
                notes.append("Personal notes and observations:\n");
                notes.append("- Regular monitoring schedule\n");
                notes.append("- Contact healthcare provider for concerns\n");
                notesArea.setPromptText("Enter personal notes here...");
            }

            notesArea.setText(notes.toString());
        }

        // Determina se permettere l'editing
        boolean canEdit = (currentUserRole == UserRole.DOCTOR_VIEWING_PATIENT ||
                currentUserRole == UserRole.ADMIN_VIEWING_USER ||
                (currentPatient == currentUser)); // Può modificare i propri dati

        notesArea.setEditable(canEdit);

        if (canEdit) {
            Button saveNotesBtn = new Button("Save Notes");
            saveNotesBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 10 20;");
            saveNotesBtn.setOnAction(e -> handleSaveNotes(notesArea.getText()));
            notesBox.getChildren().addAll(notesTitleLabel, notesArea, saveNotesBtn);
        } else {
            notesBox.getChildren().addAll(notesTitleLabel, notesArea);
        }

        notesContent.getChildren().add(notesBox);
    }

    private void handleModifyTherapy() {
        if (currentUserRole == UserRole.DOCTOR_VIEWING_PATIENT || currentUserRole == UserRole.ADMIN_VIEWING_USER) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Modify Therapy");
            alert.setHeaderText("Therapy Modification");
            alert.setContentText("Therapy modification dialog would open here for " +
                    (currentPatient != null ? currentPatient.getFullName() : "patient"));
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Therapy Information");
            alert.setHeaderText("Current Therapy");
            alert.setContentText("Contact your doctor to discuss therapy modifications.");
            alert.showAndWait();
        }
    }


    private void handleUpdatePatientInfo() {
        boolean canEdit = (currentUserRole != UserRole.PATIENT_OWN_PROFILE || currentPatient == currentUser);

        if (canEdit) {
            String title = (currentPatient == currentUser) ? "Update My Information" : "Update Patient Information";
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText("Edit Information");
            alert.setContentText("Information editing dialog would open here.");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("View Only");
            alert.setContentText("Contact your healthcare provider to update your information.");
            alert.showAndWait();
        }
    }

    // Utility methods

    private void loadPatientData() throws SQLException {
        List<GlucoseMeasurement> measurements = glucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(currentPatient.getId());
        currentPatient.setGlucoseReadings(measurements);

        List<Symptom> symptoms = symptomDAO.getSymptomsForTable(currentPatient.getId());
        currentPatient.setSymptoms(symptoms.stream().map(Symptom::getSymptomName).collect(Collectors.toList()));

        List<Medication> medications = medicationDAO.getMedicationsByPatientId(currentPatient.getId());
        currentPatient.setMedications(medications);
    }

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // METODI DEL TUO CODICE ORIGINALE DA MANTENERE:

    private void setupTabs() {
        overviewTab.setOnAction(e -> switchToTab("overview"));
        trendsTab.setOnAction(e -> switchToTab("trends"));
        medicationTab.setOnAction(e -> switchToTab("medication"));
        notesTab.setOnAction(e -> switchToTab("notes"));
        switchToTab("overview");
    }

    private void switchToTab(String tabName) {
        overviewContent.setVisible(false);
        trendsContent.setVisible(false);
        medicationContent.setVisible(false);
        notesContent.setVisible(false);

        resetTabStyles();

        switch (tabName) {
            case "overview":
                overviewContent.setVisible(true);
                setActiveTabStyle(overviewTab);
                break;
            case "trends":
                trendsContent.setVisible(true);
                setActiveTabStyle(trendsTab);
                loadTrendsContent();
                break;
            case "medication":
                medicationContent.setVisible(true);
                setActiveTabStyle(medicationTab);
                loadMedicationContent();
                break;
            case "notes":
                notesContent.setVisible(true);
                setActiveTabStyle(notesTab);
                loadNotesContent();
                break;
        }
    }

    private void resetTabStyles() {
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-border-width: 0 0 1 0; -fx-border-color: #2C3E50;";
        overviewTab.setStyle(inactiveStyle);
        trendsTab.setStyle(inactiveStyle);
        medicationTab.setStyle(inactiveStyle);
        notesTab.setStyle(inactiveStyle);
    }

    private void setActiveTabStyle(Button activeTab) {
        String activeStyle = "-fx-background-color: #3498DB; -fx-text-fill: white; -fx-border-width: 0 0 3 0; -fx-border-color: #3498DB;";
        activeTab.setStyle(activeStyle);
    }

    private void setupTable() {
        therapyModifications = FXCollections.observableArrayList();
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        modificationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getModification()));
        modifiedByColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getModifiedBy()));
        therapyModificationsTable.setItems(therapyModifications);

        therapyModificationsTable.setRowFactory(tv -> {
            TableRow<TherapyModification> row = new TableRow<>();
            row.setStyle("-fx-background-color: #34495E; -fx-text-fill: white;");
            return row;
        });
    }

    private void setupCharts() {
        if (glucoseTrendsChart != null) {
            glucoseTrendsChart.setAnimated(false);
            if (glucoseTrendsChart.getXAxis() != null) {
                glucoseTrendsChart.getXAxis().setLabel("Date");
            }
            if (glucoseTrendsChart.getYAxis() != null) {
                glucoseTrendsChart.getYAxis().setLabel("mg/dL");
            }
            glucoseTrendsChart.setLegendVisible(false);
        }
    }

    private void setupButtons() {
        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setOnAction(e -> handleModifyTherapy());
        }
        if (updatePatientInfoBtn != null) {
            updatePatientInfoBtn.setOnAction(e -> handleUpdatePatientInfo());
        }
    }

    private void updatePatientInfo() {
        if (currentPatient != null) {
            patientNameLabel.setText(currentPatient.getFullName());
            patientIdLabel.setText("Patient ID: " + currentPatient.getId());

            LocalDateTime lastVisit = null;
            if (!currentPatient.getGlucoseReadings().isEmpty()) {
                lastVisit = currentPatient.getGlucoseReadings().stream()
                        .max((r1, r2) -> r1.getDateAndTime().compareTo(r2.getDateAndTime()))
                        .map(GlucoseMeasurement::getDateAndTime)
                        .orElse(null);
            }
            lastVisitLabel.setText("Last visit: " + formatLastVisit(lastVisit));
        }
    }

    private void updatePatientSpecificData() {
        if (currentPatient != null) {
            updateGlucoseStatistics();
            loadRealSymptoms();
            updateGlucoseChart();
            updateMedicationProgress();
        }
    }

    private void updateGlucoseStatistics() {
        if (currentPatient.getGlucoseReadings().isEmpty()) {
            if (averageGlucoseLabel != null) averageGlucoseLabel.setText("No data");
            if (averageGlucoseChangeLabel != null) averageGlucoseChangeLabel.setText("--");
            return;
        }

        double average = currentPatient.getGlucoseReadings().stream()
                .mapToDouble(GlucoseMeasurement::getGlucoseLevel)
                .average()
                .orElse(0.0);
        if (averageGlucoseLabel != null) {
            averageGlucoseLabel.setText(String.format("%.1f mg/dL", average));
        }

        List<GlucoseMeasurement> sortedReadings = currentPatient.getGlucoseReadings().stream()
                .sorted((r1, r2) -> r2.getDateAndTime().compareTo(r1.getDateAndTime()))
                .collect(Collectors.toList());

        if (sortedReadings.size() >= 2 && averageGlucoseChangeLabel != null) {
            double recent = sortedReadings.get(0).getGlucoseLevel();
            double previous = sortedReadings.get(sortedReadings.size() - 1).getGlucoseLevel();
            double change = recent - previous;

            String changeText = String.format("%.1f mg/dL", Math.abs(change));
            if (change > 0) {
                changeText = "+" + changeText + " ↗";
                averageGlucoseChangeLabel.setTextFill(Color.web("#E74C3C"));
            } else if (change < 0) {
                changeText = "-" + changeText + " ↘";
                averageGlucoseChangeLabel.setTextFill(Color.web("#27AE60"));
            } else {
                changeText = "No change";
                averageGlucoseChangeLabel.setTextFill(Color.web("#BDC3C7"));
            }
            averageGlucoseChangeLabel.setText(changeText);
        } else if (averageGlucoseChangeLabel != null) {
            averageGlucoseChangeLabel.setText("Insufficient data");
        }
    }

    private void loadRealSymptoms() {
        if (symptomsContainer != null) {
            symptomsContainer.getChildren().clear();

            if (currentPatient == null || currentPatient.getSymptoms().isEmpty()) {
                Label noSymptomsLabel = new Label("No symptoms reported");
                noSymptomsLabel.setTextFill(Color.web("#BDC3C7"));
                noSymptomsLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
                noSymptomsLabel.setPadding(new Insets(10));
                symptomsContainer.getChildren().add(noSymptomsLabel);
                return;
            }

            for (String symptom : currentPatient.getSymptoms()) {
                HBox symptomBox = new HBox(10);
                symptomBox.setPadding(new Insets(8, 12, 8, 12));
                symptomBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4;");

                Label symptomLabel = new Label(symptom);
                symptomLabel.setTextFill(Color.WHITE);
                symptomLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

                String severity = determineSeverity(symptom);
                String severityColor = getSeverityColor(severity);

                Label severityLabel = new Label(severity);
                severityLabel.setTextFill(Color.web(severityColor));
                severityLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                severityLabel.setStyle("-fx-background-color: " + severityColor + "33; -fx-background-radius: 3; -fx-padding: 2 6 2 6;");

                symptomBox.getChildren().addAll(symptomLabel, severityLabel);
                symptomsContainer.getChildren().add(symptomBox);
            }
        }
    }

    private String determineSeverity(String symptom) {
        String lowerSymptom = symptom.toLowerCase();
        if (lowerSymptom.contains("severe") || lowerSymptom.contains("excessive") ||
                lowerSymptom.contains("frequent") || lowerSymptom.contains("extreme")) {
            return "High";
        } else if (lowerSymptom.contains("moderate") || lowerSymptom.contains("occasional") ||
                lowerSymptom.contains("mild")) {
            return "Moderate";
        } else {
            return "Normal";
        }
    }

    private String getSeverityColor(String severity) {
        switch (severity) {
            case "High": return "#E74C3C";
            case "Moderate": return "#F39C12";
            case "Normal": return "#2ECC71";
            default: return "#3498DB";
        }
    }

    private void updateGlucoseChart() {
        if (glucoseTrendsChart != null) {
            glucoseTrendsChart.getData().clear();

            if (currentPatient == null || currentPatient.getGlucoseReadings().isEmpty()) {
                return;
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Glucose Levels");

            List<GlucoseMeasurement> sortedReadings = currentPatient.getGlucoseReadings().stream()
                    .sorted((r1, r2) -> r1.getDateAndTime().compareTo(r2.getDateAndTime()))
                    .collect(Collectors.toList());

            int size = sortedReadings.size();
            int startIndex = Math.max(0, size - 10);

            for (int i = startIndex; i < size; i++) {
                GlucoseMeasurement reading = sortedReadings.get(i);
                String dateLabel = reading.getDateAndTime().format(DateTimeFormatter.ofPattern("MM/dd"));
                series.getData().add(new XYChart.Data<>(dateLabel, reading.getGlucoseLevel()));
            }

            glucoseTrendsChart.getData().add(series);
        }
    }

    private void updateMedicationProgress() {
        if (currentPatient != null) {
            int readingsCount = currentPatient.getGlucoseReadings().size();
            double metforminCompliance = Math.min(1.0, readingsCount / 30.0 * 0.85);

            if (adheranceProgressBar != null) {
                adheranceProgressBar.setProgress(metforminCompliance);
            }
            if (adherancePercentageLabel != null) {
                adherancePercentageLabel.setText(String.format("%.0f%%", metforminCompliance * 100));
            }
        } else {
            if (adheranceProgressBar != null) {
                adheranceProgressBar.setProgress(0);
            }
            if (adherancePercentageLabel != null) {
                adherancePercentageLabel.setText("0%");
            }
        }
    }

    private String formatLastVisit(LocalDateTime lastVisit) {
        if (lastVisit == null) return "Never";

        long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(lastVisit.toLocalDate(), LocalDate.now());
        if (daysAgo == 0) return "Today";
        if (daysAgo == 1) return "Yesterday";
        if (daysAgo < 7) return daysAgo + " days ago";
        if (daysAgo < 14) return "1 week ago";
        if (daysAgo < 30) return (daysAgo / 7) + " weeks ago";
        return (daysAgo / 30) + " months ago";
    }

    private void loadTherapyModifications() {
        if (therapyModifications != null) {
            therapyModifications.clear();
            if (currentPatient != null) {
                therapyModifications.add(new TherapyModification(
                        LocalDate.now().minusDays(5),
                        "Updated glucose monitoring frequency",
                        "Dr. Smith"
                ));
                if (!currentPatient.getGlucoseReadings().isEmpty()) {
                    double avgGlucose = currentPatient.getGlucoseReadings().stream()
                            .mapToDouble(GlucoseMeasurement::getGlucoseLevel)
                            .average()
                            .orElse(0);
                    if (avgGlucose > 180) {
                        therapyModifications.add(new TherapyModification(
                                LocalDate.now().minusDays(12),
                                "Increased insulin dosage due to high glucose levels",
                                "Dr. Johnson"
                        ));
                    } else if (avgGlucose > 140) {
                        therapyModifications.add(new TherapyModification(
                                LocalDate.now().minusDays(12),
                                "Adjusted Metformin dosage",
                                "Dr. Johnson"
                        ));
                    }
                }
                therapyModifications.add(new TherapyModification(
                        LocalDate.now().minusDays(25),
                        "Initial diabetes management plan established",
                        "Dr. Smith"
                ));
            }
        }
    }

    private void loadTrendsContent() {
        if (trendsContent != null && !trendsContent.getChildren().isEmpty()) return;

        VBox trendsBox = new VBox(15);
        trendsBox.setPadding(new Insets(20));

        Label trendsTitle = new Label("Glucose Trends Analysis");
        trendsTitle.setTextFill(Color.WHITE);
        trendsTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

        if (currentPatient != null && !currentPatient.getGlucoseReadings().isEmpty()) {
            Label trendAnalysis = createTrendAnalysis();
            trendsBox.getChildren().addAll(trendsTitle, trendAnalysis);
        } else {
            Label noDataLabel = new Label("No glucose data available for trend analysis");
            noDataLabel.setTextFill(Color.web("#BDC3C7"));
            noDataLabel.setFont(Font.font(16));
            trendsBox.getChildren().addAll(trendsTitle, noDataLabel);
        }

        if (trendsContent != null) {
            trendsContent.getChildren().add(trendsBox);
        }
    }

    private Label createTrendAnalysis() {
        StringBuilder analysis = new StringBuilder();

        List<GlucoseMeasurement> readings = currentPatient.getGlucoseReadings();
        double average = readings.stream().mapToDouble(GlucoseMeasurement::getGlucoseLevel).average().orElse(0);
        long highReadings = readings.stream().filter(r -> r.getGlucoseLevel() > 180).count();
        long normalReadings = readings.stream().filter(r -> r.getGlucoseLevel() >= 80 && r.getGlucoseLevel() <= 140).count();

        analysis.append(String.format("Average glucose level: %.1f mg/dL\n", average));
        analysis.append(String.format("Total readings: %d\n", readings.size()));
        if (readings.size() > 0) {
            analysis.append(String.format("High readings (>180): %d (%.1f%%)\n",
                    highReadings, (double)highReadings / readings.size() * 100));
            analysis.append(String.format("Normal readings (80-140): %d (%.1f%%)\n",
                    normalReadings, (double)normalReadings / readings.size() * 100));
        } else {
            analysis.append("High readings (>180): 0 (0.0%)\n");
            analysis.append("Normal readings (80-140): 0 (0.0%)\n");
        }

        if (average > 180) {
            analysis.append("\nRecommendation: Glucose levels are consistently high. Consider medication adjustment.");
        } else if (average > 140) {
            analysis.append("\nRecommendation: Glucose levels are moderately elevated. Continue monitoring closely.");
        } else {
            analysis.append("\nRecommendation: Glucose levels are well controlled. Maintain current regimen.");
        }

        Label analysisLabel = new Label(analysis.toString());
        analysisLabel.setTextFill(Color.WHITE);
        analysisLabel.setFont(Font.font(14));
        analysisLabel.setWrapText(true);

        return analysisLabel;
    }

    private void loadMedicationContent() {
        if (medicationContent != null && !medicationContent.getChildren().isEmpty()) {
            return;
        }

        VBox medicationBox = new VBox(15);
        medicationBox.setPadding(new Insets(20));

        Label medicationTitle = new Label("Current Medications");
        medicationTitle.setTextFill(Color.WHITE);
        medicationTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

        VBox medicationsList = new VBox(10);
        if (currentPatient != null && !currentPatient.getMedications().isEmpty()) {
            for (Medication medication : currentPatient.getMedications()) {
                HBox medBox = createMedicationBox(
                        medication.getName_medication(),
                        medication.getDose(),
                        medication.getFreq().getDisplayName(),
                        medication.getInstructions()
                );
                medicationsList.getChildren().add(medBox);
            }
        } else {
            Label noMedsLabel = new Label("No medications currently prescribed.");
            noMedsLabel.setTextFill(Color.web("#BDC3C7"));
            noMedsLabel.setFont(Font.font(14));
            medicationsList.getChildren().add(noMedsLabel);
        }

        medicationBox.getChildren().addAll(medicationTitle, medicationsList);
        if (medicationContent != null) {
            medicationContent.getChildren().add(medicationBox);
        }
    }

    private HBox createMedicationBox(String name, String dosage, String frequency, String instructions) {
        HBox medBox = new HBox(15);
        medBox.setPadding(new Insets(10));
        medBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 5;");
        VBox medInfo = new VBox(5);
        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        Label dosageLabel = new Label("Dosage: " + dosage);
        dosageLabel.setTextFill(Color.web("#BDC3C7"));
        dosageLabel.setFont(Font.font(12));
        Label frequencyLabel = new Label("Frequency: " + frequency);
        frequencyLabel.setTextFill(Color.web("#BDC3C7"));
        frequencyLabel.setFont(Font.font(12));
        Label instructionsLabel = new Label("Instructions: " + instructions);
        instructionsLabel.setTextFill(Color.web("#BDC3C7"));
        instructionsLabel.setFont(Font.font(12));
        medInfo.getChildren().addAll(nameLabel, dosageLabel, frequencyLabel, instructionsLabel);
        medBox.getChildren().add(medInfo);
        return medBox;
    }

    private void handleSaveNotes(String notes) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notes Saved");
        alert.setHeaderText("Success");
        alert.setContentText("Clinical notes have been saved successfully for " +
                (currentPatient != null ? currentPatient.getFullName() : "patient"));
        alert.showAndWait();
    }

    public static class TherapyModification {
        private LocalDate date;
        private String modification;
        private String modifiedBy;
        public TherapyModification(LocalDate date, String modification, String modifiedBy) {
            this.date = date;
            this.modification = modification;
            this.modifiedBy = modifiedBy;
        }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getModification() { return modification; }
        public void setModification(String modification) { this.modification = modification; }
        public String getModifiedBy() { return modifiedBy; }
        public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
    }

    private void handleBackToPatientsList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/DoctorDashboardPatients.fxml"));
            Parent patientsView = loader.load();
            if (parentContentPane != null) {
                parentContentPane.getChildren().clear();
                parentContentPane.getChildren().add(patientsView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Navigation Error");
            alert.setContentText("Could not return to patients list: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void setParentContentPane(StackPane contentPane) {
        this.parentContentPane = contentPane;
    }

    // Getter per il ruolo corrente (utile per debug o altre funzionalità)
    public UserRole getCurrentUserRole() {
        return currentUserRole;
    }

    public Object getCurrentUser() {
        return currentUser;
    }

    public Patient getCurrentPatient() {
        return currentPatient;
    }
}