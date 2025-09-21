package it.glucotrack.controller;

import it.glucotrack.model.*;
import it.glucotrack.util.*;
import it.glucotrack.model.Medication;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @FXML private Label patientNameLabel;
    @FXML private Label patientIdLabel;

    // Tab buttons
    @FXML private Button overviewTab;
    @FXML private Button medicationTab;
    @FXML private Button notesTab;


    // Content areas
    @FXML private VBox overviewContent;
    @FXML private VBox trendsContent;
    @FXML private VBox medicationContent;
    @FXML private VBox notesContent;

    // Medication Table
    @FXML
    private TableView<Medication> prescribedMedicationsTable;

    @FXML
    private TableColumn<Medication, String> drugNameColumn;

    @FXML
    private TableColumn<Medication, String> dosageColumn;

    @FXML
    private TableColumn<Medication, String> frequencyColumn;

    @FXML
    private TableColumn<Medication, String> instructionsColumn;

    private ObservableList<Medication> prescribedMedications;



    // Overview content elements - Fixed to match FXML
    @FXML private Label currentGlucoseLabel;
    @FXML private Label trendLabel;
    @FXML private Label statusLabel;
    @FXML private LineChart<String, Number> glucoseChart;
    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private Label averageGlucoseLabel;
    @FXML private Label averageGlucoseChangeLabel;
    @FXML private LineChart<String, Number> glucoseTrendsChart;
    @FXML private Label adherancePercentageLabel;
    @FXML private ProgressBar adheranceProgressBar;

    @FXML private VBox symptomsContainer;
    @FXML private VBox riskFactorsContainer;
    @FXML private VBox therapyModificationsContainer;


    // Action buttons
    @FXML private Button modifyTherapyBtn;
    @FXML private Button addRiskBtn;
    @FXML private Button deleteUserBtn;
    @FXML private Button exportDataBtn;
    @FXML private Button sendMessageBtn;

    // Data models
    private Patient currentPatient;
    private User currentUser;
    private UserRole currentUserRole;
    private ObservableList<MedicationEdit> therapyModifications;
    private StackPane parentContentPane;
    private String currentUserType;

    // DAOs
    private GlucoseMeasurementDAO glucoseMeasurementDAO;
    private SymptomDAO symptomDAO;
    private MedicationDAO medicationDAO;

    private List<GlucoseMeasurement> filterMeasurementsByPeriod(List<GlucoseMeasurement> measurements, int daysBack) {
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(daysBack);
        return measurements.stream()
                .filter(m -> m.getDateAndTime().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize DAOs
        this.glucoseMeasurementDAO = new GlucoseMeasurementDAO();
        this.symptomDAO = new SymptomDAO();
        this.medicationDAO = new MedicationDAO();

        this.currentUser = SessionManager.getCurrentUser();
        this.currentUserType = currentUser.getType();

    }

    public void refreshInitialize() {
        initializeComponents();
        setupTabs();
        setupTable();
        setupCharts();
        loadTrendsContent();
        setupButtons();
        loadRiskFactors();
        initializeAdditionalButtons();

    }

    public void setParentContentPane(StackPane contentPane) {
        this.parentContentPane = contentPane;
    }

    private void initializeComponents() {
        // Initialize ComboBox
        loadRiskFactors();

        timeRangeCombo.getItems().addAll("Ultimi 7 giorni", "Ultimi 30 giorni", "Ultimo anno");

        timeRangeCombo.setOnAction(e -> {
            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            System.out.println("Cambio periodo: " + selectedPeriod);

            // Pausa breve per evitare conflitti nel refresh del grafico
            javafx.application.Platform.runLater(() -> {
                try {
                    // Aggiorna sia i dati numerici che il grafico quando cambia il periodo
                    updateGlucoseData();
                    updateChart();
                    System.out.println("Aggiornamento completato per periodo: " + selectedPeriod);
                } catch (Exception ex) {
                    System.err.println("Errore durante il cambio periodo: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        });
        // Initialize therapy modifications list
        therapyModifications = FXCollections.observableArrayList();

        // Seleziona il default (7 giorni) e trigger del listener
        timeRangeCombo.getSelectionModel().select("Ultimi 7 giorni");

        // Inizializza i dati della dashboard
        updateGlucoseData();
        updateChart();
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
     */
    public void setUserRole(UserRole role, User viewedPatient) throws SQLException {
        System.out.println("DEBUG - Role: " + role);
        System.out.println("DEBUG - ViewedPatient: " + (viewedPatient != null ? "not null" : "null"));

        this.currentUserRole = role;

        if (viewedPatient != null) {
            this.currentPatient = PatientDAO.getPatientById(viewedPatient.getId());
        } else {
            this.currentPatient = null;
            System.out.println("DEBUG - User is not a Patient instance!");
        }

        refreshInitialize();

        updateViewForUserRole();

        if (currentPatient != null && currentUserRole == UserRole.DOCTOR_VIEWING_PATIENT) {
            try {
                updatePatientSpecificData();
                loadTherapyModifications();
            } catch (SQLException e) {
                showError("Database Error", "Failed to load patient data", e.getMessage());
            }
        } else {
            updateNonPatientProfile();
        }
    }

    private void updateNonPatientProfile() {
        if (currentUser != null) {
            String userName = getUserName(currentUser);
            String userId = getUserId(currentUser);

            if (patientNameLabel != null) {
                patientNameLabel.setText(userName);
            }
            if (patientIdLabel != null) {
                patientIdLabel.setText("User ID: " + userId);
            }

        }
        System.out.println("Nascondo elementi medici");
        hideMedicalElements();
    }

    private String getUserName(Object user) {
        if (user instanceof Patient) {
            return ((Patient) user).getFullName();
        }
        try {
            if (user.getClass().getMethod("getFullName") != null) {
                return (String) user.getClass().getMethod("getFullName").invoke(user);
            } else if (user.getClass().getMethod("getName") != null) {
                return (String) user.getClass().getMethod("getName").invoke(user);
            }
        } catch (Exception e) {
            return user.getClass().getSimpleName() + " User";
        }
        return "Unknown User";
    }

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

    private String getUserRole(Object user) {
        return user.getClass().getSimpleName();
    }

    private void hideMedicalElements() {
        if (glucoseChart != null) {
            glucoseChart.setVisible(false);
            glucoseChart.setManaged(false);
        }
        if (averageGlucoseLabel != null) {
            averageGlucoseLabel.setVisible(false);
        }
        if (averageGlucoseChangeLabel != null) {
            averageGlucoseChangeLabel.setVisible(false);
        }
        if (adheranceProgressBar != null) {
            adheranceProgressBar.setVisible(false);
        }
        if (adherancePercentageLabel != null) {
            adherancePercentageLabel.setVisible(false);
        }
        if (symptomsContainer != null) {
            symptomsContainer.setVisible(false);
            symptomsContainer.setManaged(false);
        }
        if (prescribedMedicationsTable != null) {
            prescribedMedicationsTable.setVisible(false);
            prescribedMedicationsTable.setManaged(false);
        }

        if(medicationTab != null) {
            medicationTab.setVisible(false);
        }

        if (overviewTab != null) {
            overviewTab.setVisible(false);
            overviewTab.setManaged(false);
            overviewContent.setManaged(false);
        }
    }

    private void updateViewForUserRole() {
        if (currentUserRole == null) {
            return;
        }

        switch (currentUserRole) {
            case DOCTOR_VIEWING_PATIENT:
                setupDoctorViewingPatient();
                loadRiskFactors();
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

    private void setupDoctorViewingPatient() {
        showMedicalCharts(true);

        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(true);
            modifyTherapyBtn.setDisable(false);
            modifyTherapyBtn.setText("Modify Therapy");
        }

        if (addRiskBtn != null) {
            addRiskBtn.setVisible(true);
            addRiskBtn.setDisable(false);
        }


        if (sendMessageBtn != null) {
            sendMessageBtn.setVisible(true);
            sendMessageBtn.setText("Send Message to Patient");
        }

        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(false);
        }

        showMedicalTabs(true);
        enableNotesEditing(true);
    }

    private void setupDoctorOwnProfile() {
        showMedicalCharts(false);

        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(false);
        }
        if (addRiskBtn != null) {
            addRiskBtn.setVisible(false);
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

    private void setupPatientOwnProfile() {
        showMedicalCharts(false);

        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(false);
            modifyTherapyBtn.setDisable(true);
        }
        if (addRiskBtn != null) {
            addRiskBtn.setVisible(false);
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

        showMedicalTabs(true);
        enableNotesEditing(false);
    }

    private void setupAdminViewingUser() {
        showMedicalCharts(false);

        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(true);
            modifyTherapyBtn.setDisable(false);
            modifyTherapyBtn.setText("Modify Therapy");
        }

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

    private void setupAdminOwnProfile() {
        showMedicalCharts(false);

        if (modifyTherapyBtn != null) {
            modifyTherapyBtn.setVisible(false);
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
        if (glucoseChart != null) {
            glucoseChart.setVisible(show);
            glucoseChart.setManaged(show);
        }
    }

    private void showMedicalTabs(boolean showAll) {
        if (medicationTab != null) {
            medicationTab.setVisible(showAll);
            medicationTab.setManaged(showAll);
        }
    }

    private void enableNotesEditing(boolean enable) {
        // This will be used in loadNotesContent
    }

    // Event handlers
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
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("User Deleted");
                successAlert.setHeaderText("Success");
                successAlert.setContentText("User account has been successfully deleted.");
                successAlert.showAndWait();

                handleBackToPatientsList();
            }
        });
    }

    private void handleBackToPatientsList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboardView.fxml"));
            Parent adminDashboard = loader.load();
            AdminDashboardController controller = loader.getController();
            if (controller != null && parentContentPane != null) {
                controller.setContentPane(parentContentPane);
                parentContentPane.getChildren().setAll(adminDashboard);
            }
        } catch (Exception e) {
            showError("Navigation Error", "Failed to navigate back to Admin Dashboard", e.getMessage());
        }
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

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Tab and UI setup methods
    private void setupTabs() {
        if (overviewTab != null) overviewTab.setOnAction(e -> switchToTab("Overview"));
        if (medicationTab != null) medicationTab.setOnAction(e -> switchToTab("Medication"));
        if (notesTab != null) notesTab.setOnAction(e -> switchToTab("Personal Data"));
        if(currentUserRole == UserRole.DOCTOR_VIEWING_PATIENT) {
            switchToTab("Overview");
        }else{
            switchToTab("Personal Data");
        }


    }

    private void switchToTab(String tabName) {
        // Hide all content
        if (overviewContent != null) overviewContent.setVisible(false);
        if (medicationContent != null) medicationContent.setVisible(false);
        if (notesContent != null) notesContent.setVisible(false);

        resetTabStyles();

        switch (tabName) {
            case "Overview":
                if (overviewContent != null) overviewContent.setVisible(true);
                setActiveTabStyle(overviewTab);
                if (currentPatient != null) {
                    updatePatientSpecificData();
                    loadRiskFactors();
                }
                break;
            case "Medication":
                if (medicationContent != null) medicationContent.setVisible(true);
                setActiveTabStyle(medicationTab);
                loadMedicationContent();
                break;
            case "Personal Data":
                if (notesContent != null) notesContent.setVisible(true);
                setActiveTabStyle(notesTab);
                loadNotesContent();
                break;
        }
    }

    private void resetTabStyles() {
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-border-width: 0 0 1 0; -fx-border-color: #2C3E50;";
        if (overviewTab != null) overviewTab.setStyle(inactiveStyle);
        if (medicationTab != null) medicationTab.setStyle(inactiveStyle);
        if (notesTab != null) notesTab.setStyle(inactiveStyle);
    }

    private void setActiveTabStyle(Button activeTab) {
        if (activeTab != null) {
            String activeStyle = "-fx-background-color: #3498DB; -fx-text-fill: white; -fx-border-width: 0 0 3 0; -fx-border-color: #3498DB;";
            activeTab.setStyle(activeStyle);
        }
    }

    private void setupTable() {
        if (prescribedMedicationsTable != null) {
            if (drugNameColumn != null) drugNameColumn.setCellValueFactory(new PropertyValueFactory<>("drugName"));
            if (dosageColumn != null) dosageColumn.setCellValueFactory(new PropertyValueFactory<>("dosage"));
            if (frequencyColumn != null) frequencyColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));
            if (instructionsColumn != null) instructionsColumn.setCellValueFactory(new PropertyValueFactory<>("instructions"));

            prescribedMedicationsTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white;");
        }
    }

    private void setupCharts() {
        // Setup main glucose chart from FXML
        if (glucoseChart != null) {
            glucoseChart.setAnimated(false);
            glucoseChart.setLegendVisible(false);
            glucoseChart.setCreateSymbols(true);
        }

        // Setup trends chart if it exists
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
    }

    private void updatePatientInfo() {
        if (currentPatient != null) {
            if (patientNameLabel != null) {
                patientNameLabel.setText(currentPatient.getFullName());
            }
            if (patientIdLabel != null) {
                patientIdLabel.setText("Patient ID: " + currentPatient.getId());
            }

        }
    }

    private void updatePatientSpecificData() {
        if (currentPatient != null) {
            updateGlucoseStatistics();
            loadRealSymptoms();
            updateChart();
            updateMedicationProgress();
        }
    }

    private void updateGlucoseData() {
            if (currentPatient != null) {
                List<GlucoseMeasurement> allMeasurements = currentPatient.getGlucoseReadings();

                if (!allMeasurements.isEmpty()) {
                    // Filtra i dati in base al periodo selezionato
                    String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
                    int daysBack = getDaysFromPeriod(selectedPeriod);
                    List<GlucoseMeasurement> filteredMeasurements = filterMeasurementsByPeriod(allMeasurements, daysBack);

                    if (!filteredMeasurements.isEmpty()) {
                        // Calcola statistiche sui dati filtrati
                        calculateAndDisplayStatistics(filteredMeasurements);
                    } else {
                        // Nessun dato nel periodo selezionato, usa l'ultima misurazione disponibile
                        GlucoseMeasurement latest = allMeasurements.get(0);
                        currentGlucoseLabel.setText(String.format("%.0f", latest.getGlucoseLevel()));
                        setStatusWithColor(latest.getGlucoseLevel());
                        trendLabel.setText("N/A (nessun dato nel periodo)");
                    }
                } else {
                    currentGlucoseLabel.setText("N/A");
                    trendLabel.setText("N/A");
                    statusLabel.setText("Nessun dato");
                }
            }

    }

    private void calculateAndDisplayStatistics(List<GlucoseMeasurement> measurements) {
        if (measurements.isEmpty()) return;

        // Ordina per data (più recente per primo)
        measurements.sort((a, b) -> b.getDateAndTime().compareTo(a.getDateAndTime()));

        // Valore corrente (più recente nel periodo)
        GlucoseMeasurement latest = measurements.get(0);
        currentGlucoseLabel.setText(String.format("%.0f", latest.getGlucoseLevel()));

        // Status basato sul valore più recente con colore
        setStatusWithColor(latest.getGlucoseLevel());

        // Calcola trend confrontando prima e ultima misurazione del periodo
        if (measurements.size() > 1) {
            GlucoseMeasurement oldest = measurements.get(measurements.size() - 1);
            double change = ((double)(latest.getGlucoseLevel() - oldest.getGlucoseLevel()) / oldest.getGlucoseLevel()) * 100;

            String trendText;
            String trendColor;
            if (Math.abs(change) < 1.0) {
                trendText = "Stabile";
                trendColor = "-fx-text-fill: #8892b0;"; // Grigio per stabile
            } else if (change > 0) {
                trendText = String.format("↑ %.1f%%", change);
                trendColor = "-fx-text-fill: #f44336;"; // Rosso per trend positivo (peggioramento)
            } else {
                trendText = String.format("↓ %.1f%%", Math.abs(change));
                trendColor = "-fx-text-fill: #4caf50;"; // Verde per trend negativo (miglioramento)
            }
            trendLabel.setText(trendText);
            trendLabel.setStyle(trendColor);
        } else {
            trendLabel.setText("N/A");
            trendLabel.setStyle("-fx-text-fill: #8892b0;");
        }
    }


    private void setStatusWithColor(float glucose) {
        String statusText;
        String colorStyle;

        if (glucose < 70) {
            statusText = "Low";
            colorStyle = "-fx-text-fill: #f44336;"; // Rosso per valori bassi (stesso del High)
        } else if (glucose <= 140) {
            statusText = "Normal";
            colorStyle = "-fx-text-fill: #4caf50;"; // Verde per valori normali (70-140)
        } else if (glucose <= 180) {
            statusText = "Elevated";
            colorStyle = "-fx-text-fill: #ff9800;"; // Arancione per valori elevati (140-180)
        } else {
            statusText = "High";
            colorStyle = "-fx-text-fill: #f44336;"; // Rosso per valori alti (>180)
        }

        statusLabel.setText(statusText);
        statusLabel.setStyle(colorStyle);
    }

    private void updateGlucoseStatistics() {
        if (currentPatient == null || currentPatient.getGlucoseReadings() == null || currentPatient.getGlucoseReadings().isEmpty()) {
            if (currentGlucoseLabel != null) currentGlucoseLabel.setText("--");
            if (averageGlucoseLabel != null) averageGlucoseLabel.setText("No data");
            if (averageGlucoseChangeLabel != null) averageGlucoseChangeLabel.setText("--");
            if (statusLabel != null) statusLabel.setText("No data");
            if (trendLabel != null) trendLabel.setText("--");
            return;
        }

        List<GlucoseMeasurement> readings = currentPatient.getGlucoseReadings();
        double average = readings.stream()
                .mapToDouble(GlucoseMeasurement::getGlucoseLevel)
                .average()
                .orElse(0.0);

        // Update current glucose (most recent reading)
        if (currentGlucoseLabel != null && !readings.isEmpty()) {
            GlucoseMeasurement mostRecent = readings.stream()
                    .max((r1, r2) -> r1.getDateAndTime().compareTo(r2.getDateAndTime()))
                    .orElse(readings.get(0));
            currentGlucoseLabel.setText(String.format("%.0f", mostRecent.getGlucoseLevel()));
        }

        // Update average glucose
        if (averageGlucoseLabel != null) {
            averageGlucoseLabel.setText(String.format("%.1f mg/dL", average));
        }

        // Update status
        if (statusLabel != null) {
            if (average <= 140) {
                statusLabel.setText("Normal");
                statusLabel.setTextFill(Color.web("#4caf50"));
            } else if (average <= 180) {
                statusLabel.setText("Elevated");
                statusLabel.setTextFill(Color.web("#ff9800"));
            } else {
                statusLabel.setText("High");
                statusLabel.setTextFill(Color.web("#f44336"));
            }
        }

        // Update trend
        List<GlucoseMeasurement> sortedReadings = readings.stream()
                .sorted((r1, r2) -> r2.getDateAndTime().compareTo(r1.getDateAndTime()))
                .collect(Collectors.toList());

        if (sortedReadings.size() >= 2) {
            double recent = sortedReadings.get(0).getGlucoseLevel();
            double previous = sortedReadings.get(sortedReadings.size() - 1).getGlucoseLevel();
            double change = recent - previous;
            double percentChange = (change / previous) * 100;

            if (trendLabel != null) {
                String trendText;
                if (Math.abs(percentChange) < 1) {
                    trendText = "No change";
                    trendLabel.setTextFill(Color.web("#8892b0"));
                } else if (percentChange > 0) {
                    trendText = String.format("↑ %.1f%%", Math.abs(percentChange));
                    trendLabel.setTextFill(Color.web("#f44336"));
                } else {
                    trendText = String.format("↓ %.1f%%", Math.abs(percentChange));
                    trendLabel.setTextFill(Color.web("#4caf50"));
                }
                trendLabel.setText(trendText);
            }

            if (averageGlucoseChangeLabel != null) {
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
            }
        } else {
            if (trendLabel != null) {
                trendLabel.setText("--");
                trendLabel.setTextFill(Color.web("#8892b0"));
            }
            if (averageGlucoseChangeLabel != null) {
                averageGlucoseChangeLabel.setText("Insufficient data");
            }
        }
    }

    private void loadRealSymptoms() {
        System.out.println("Sono in loadRealSymptoms");

        if (symptomsContainer != null) {
            symptomsContainer.getChildren().clear();
            System.out.println("Sono nel primo if: " + currentPatient.getFullName()+ "  e i suoi sintomi sono: " + currentPatient.getSymptoms());
            if (currentPatient == null || currentPatient.getSymptoms() == null || currentPatient.getSymptoms().isEmpty()) {
                Label noSymptomsLabel = new Label("No symptoms reported");
                noSymptomsLabel.setTextFill(Color.web("#BDC3C7"));
                noSymptomsLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
                noSymptomsLabel.setPadding(new Insets(10));
                symptomsContainer.getChildren().add(noSymptomsLabel);
                return;
            }
            System.out.println("Symptoms: " + currentPatient.getSymptoms());
            for (Symptom symptom : currentPatient.getSymptoms()) {
                System.out.println("Loading symptom: " + symptom.getSymptomName() + " with severity " + symptom.getGravity());
                HBox symptomBox = new HBox(10);
                symptomBox.setPadding(new Insets(8, 12, 8, 12));
                symptomBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4;");

                Label symptomLabel = new Label(symptom.getSymptomName());
                symptomLabel.setTextFill(Color.WHITE);
                symptomLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

                String severityColor = getSeverityColor(symptom.getGravity());

                Label severityLabel = new Label(symptom.getGravity());severityLabel.setTextFill(Color.web(severityColor));
                severityLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                severityLabel.setStyle("-fx-background-color: " + severityColor + "33; -fx-background-radius: 3; -fx-padding: 2 6 2 6;");

                symptomBox.getChildren().addAll(symptomLabel, severityLabel);
                symptomsContainer.getChildren().add(symptomBox);
            }
        }
    }

    private String getSeverityColor(String severity) {
        switch (severity) {
            case "Mild": return "#F1C40F";
            case "Moderate": return "#E67E22";
            case "Severe": return "#E74C3C";
            case "Critical": return "#8E44AD";
            default: return "#3498DB";
        }
    }


    private void updateChart() {

            if (currentPatient == null) return;

            // Pulizia completa del grafico
            glucoseChart.getData().clear();
            glucoseChart.getXAxis().setAnimated(false);
            glucoseChart.getYAxis().setAnimated(false);
            glucoseChart.setAnimated(false);

            // Ottieni i dati dal database
            List<GlucoseMeasurement> measurements = currentPatient.getGlucoseReadings();
            System.out.println("Totale misurazioni caricate: " + measurements.size());
            if (measurements.isEmpty()) {
                System.out.println("⚠Nessuna misurazione trovata per il grafico");
                return;
            }

            // Filtra i dati in base al periodo selezionato
            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            int daysBack = getDaysFromPeriod(selectedPeriod);

            // Filtra e ordina i dati per il periodo
            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(daysBack);
            List<GlucoseMeasurement> filteredMeasurements = measurements.stream()
                    .filter(m -> m.getDateAndTime().isAfter(cutoffDate))
                    .sorted((a, b) -> a.getDateAndTime().compareTo(b.getDateAndTime()))
                    .collect(Collectors.toList());

            // Crea serie dati per il grafico
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Glicemia (mg/dL)");

            // Limita il numero di punti visualizzati per evitare sovrapposizioni delle date
            int maxPoints = getMaxPointsForPeriod(selectedPeriod);

            if (filteredMeasurements.size() > maxPoints) {
                // Campionamento uniforme per distribuire i punti nel tempo
                double step = (double) filteredMeasurements.size() / maxPoints;
                for (int i = 0; i < maxPoints; i++) {
                    int index = (int) Math.round(i * step);
                    if (index >= filteredMeasurements.size()) {
                        index = filteredMeasurements.size() - 1;
                    }
                    GlucoseMeasurement measurement = filteredMeasurements.get(index);
                    String dateStr = formatDateForChart(measurement.getDateAndTime(), selectedPeriod);
                    series.getData().add(new XYChart.Data<>(dateStr, measurement.getGlucoseLevel()));
                }
            } else {
                // Se ci sono pochi punti, mostra tutti ma con spaziatura minima
                for (int i = 0; i < filteredMeasurements.size(); i++) {
                    GlucoseMeasurement measurement = filteredMeasurements.get(i);
                    String dateStr = formatDateForChart(measurement.getDateAndTime(), selectedPeriod);
                    series.getData().add(new XYChart.Data<>(dateStr, measurement.getGlucoseLevel()));
                }
            }

            glucoseChart.getData().add(series);

            System.out.println("Grafico aggiornato - Periodo: " + selectedPeriod +
                    ", Punti visualizzati: " + series.getData().size() +
                    "/" + filteredMeasurements.size());

            // Forza il refresh completo del grafico
            javafx.application.Platform.runLater(() -> {
                glucoseChart.requestLayout();
                glucoseChart.autosize();
            });

    }

    private int getMaxPointsForPeriod(String period) {
        switch (period) {
            case "Ultimi 7 giorni": return 15;
            case "Ultimi 30 giorni": return 20;
            case "Ultimo anno": return 25;
            default: return 15;
        }
    }

    private String formatDateForChart(java.time.LocalDateTime dateTime, String period) {
        try {
            switch (period) {
                case "Ultimi 7 giorni":
                    // Formato compatto per 7 giorni
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                case "Ultimi 30 giorni":
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                case "Ultimo anno":
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
                default:
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
            }
        } catch (Exception e) {
            System.err.println("Errore nel formato data: " + e.getMessage());
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
        }
    }


    private int getDaysFromPeriod(String period) {
        switch (period) {
            case "Ultimi 7 giorni": return 7;
            case "Ultimi 30 giorni": return 30;
            case "Ultimo anno": return 365;
            default: return 7;
        }
    }


    private int getMaxPointsForTimeRange(String timeRange) {
        switch (timeRange) {
            case "Last 7 days": return 7;
            case "Last 30 days": return 30;
            case "Last 3 months": return 90;
            default: return 10;
        }
    }

    private void updateMedicationProgress() {
        if (currentPatient != null && currentPatient.getGlucoseReadings() != null) {
            int readingsCount = currentPatient.getGlucoseReadings().size();
            double compliance = Math.min(1.0, readingsCount / 30.0 * 0.85);

            if (adheranceProgressBar != null) {
                adheranceProgressBar.setProgress(compliance);
            }
            if (adherancePercentageLabel != null) {
                adherancePercentageLabel.setText(String.format("%.0f%%", compliance * 100));
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

    private void loadTherapyModifications() throws SQLException {
        if (therapyModifications != null) {
            therapyModifications.clear();
            if (currentPatient != null) {
                try {
                    List<Medication> medications = currentPatient.getMedications();
                    for(Medication med : medications) {
                        List<MedicationEdit> edits = medicationDAO.getMedicationEditsByMedicationId(med.getId());
                        therapyModifications.addAll(edits);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading therapy modifications: " + e.getMessage());
                }
            }
        }
        loadTherapyModificationsUI();
    }

    private void loadTherapyModificationsUI() {
        if (therapyModificationsContainer != null) {
            therapyModificationsContainer.getChildren().clear();

            if (therapyModifications == null || therapyModifications.isEmpty()) {
                Label noModsLabel = new Label("No therapy modifications recorded");
                noModsLabel.setTextFill(Color.web("#BDC3C7"));
                noModsLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
                noModsLabel.setPadding(new Insets(10));
                therapyModificationsContainer.getChildren().add(noModsLabel);
            } else {
                for (MedicationEdit edit : therapyModifications) {
                    HBox modBox = createModificationBox(edit);
                    therapyModificationsContainer.getChildren().add(modBox);
                }
            }
        }
    }

    private HBox createModificationBox(MedicationEdit edit) {
        HBox modBox = new HBox(15);
        modBox.setPadding(new Insets(10));
        modBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 5;");

        VBox modInfo = new VBox(5);

        Label dateLabel = new Label("Date: " + (edit.getEditTimestamp() != null ?
                edit.getEditTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Unknown"));
        dateLabel.setTextFill(Color.web("#BDC3C7"));
        dateLabel.setFont(Font.font(12));

        Label changeLabel = new Label("Change: " + (edit.getMedication().getInstructions() != null ?
                edit.getMedication().getInstructions() : "No description"));
        changeLabel.setTextFill(Color.WHITE);
        changeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        changeLabel.setWrapText(true);

        modInfo.getChildren().addAll(dateLabel, changeLabel);
        modBox.getChildren().add(modInfo);

        return modBox;
    }

    private void loadRiskFactors() {
        System.out.println("This is for test before the if in loadRiskFactors");
        if (riskFactorsContainer != null) {
            riskFactorsContainer.getChildren().clear();
            System.out.println("This is for test inside loadRiskFactors");
            // Sample risk factors - in a real app, these would come from the database
            if (currentPatient != null) {
                List<RiskFactor> riskFactors = currentPatient.getRiskFactors();
                for(RiskFactor factor : riskFactors) {
                    String color;
                    switch (factor.getGravity()) {
                        case LOW: color = "#E74C3C"; break;
                        case MEDIUM: color = "#F39C12"; break;
                        case HIGH: color = "#2ECC71"; break;
                        default: color = "#3498DB"; break;
                    }
                    System.out.println("Adding risk factor: " + factor.getType() + " with gravity " + factor.getGravity());
                    addRiskFactor(factor.getType(), factor.getGravity().toString(), color);
                }
            } else {
                Label noRiskLabel = new Label("No risk factors assessed");
                noRiskLabel.setTextFill(Color.web("#BDC3C7"));
                noRiskLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
                noRiskLabel.setPadding(new Insets(10));
                riskFactorsContainer.getChildren().add(noRiskLabel);
            }
        }
    }

    private void addRiskFactor(String factor, String risk, String color) {
        HBox riskBox = new HBox(10);
        riskBox.setPadding(new Insets(8, 12, 8, 12));
        riskBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4;");

        VBox factorInfo = new VBox(2);
        Label factorLabel = new Label(factor);
        factorLabel.setTextFill(Color.WHITE);
        factorLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        factorInfo.getChildren().addAll(factorLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label riskLabel = new Label(risk);
        riskLabel.setTextFill(Color.web(color));
        riskLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        riskLabel.setStyle("-fx-background-color: " + color + "33; -fx-background-radius: 3; -fx-padding: 2 6 2 6;");

        riskBox.getChildren().addAll(factorInfo, spacer, riskLabel);
        riskFactorsContainer.getChildren().add(riskBox);
    }

    private void loadTrendsContent() {
        if (trendsContent != null && !trendsContent.getChildren().isEmpty()) return;

        VBox trendsBox = new VBox(15);
        trendsBox.setPadding(new Insets(20));

        Label trendsTitle = new Label("Glucose Trends Analysis");
        trendsTitle.setTextFill(Color.WHITE);
        trendsTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

        if (currentPatient != null && currentPatient.getGlucoseReadings() != null && !currentPatient.getGlucoseReadings().isEmpty()) {
            Label trendAnalysis = createTrendAnalysis();
            trendsBox.getChildren().addAll(trendsTitle, trendAnalysis);
        } else {
            Label noDataLabel = new Label("No glucose data available for trend analysis");
            noDataLabel.setTextFill(Color.web("#BDC3C7"));
            noDataLabel.setFont(Font.font(16));
            trendsBox.getChildren().addAll(trendsTitle, noDataLabel);
        }

        if (trendsContent != null) {
            trendsContent.getChildren().clear();
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
        }else if(average <80){
            analysis.append("\nRecommendation: Glucose levels are low. Ensure regular meals and monitor for hypoglycemia.");
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

        if (currentPatient != null && currentPatient.getMedications() != null && !currentPatient.getMedications().isEmpty()) {
            ObservableList<Medication> meds = FXCollections.observableArrayList(currentPatient.getMedications());
            prescribedMedicationsTable.setItems(meds);
            medicationBox.getChildren().addAll(medicationTitle, prescribedMedicationsTable);
        } else {
            Label noMedsLabel = new Label("No medications currently prescribed.");
            noMedsLabel.setTextFill(Color.web("#BDC3C7"));
            noMedsLabel.setFont(Font.font(14));
            medicationBox.getChildren().addAll(medicationTitle, noMedsLabel);
        }

        if (medicationContent != null) {
            medicationContent.getChildren().clear();
            medicationContent.getChildren().add(medicationBox);
        }
    }

    private void loadNotesContent() {
        if (notesContent != null && !notesContent.getChildren().isEmpty()) return;

        VBox notesBox = new VBox(15);
        notesBox.setPadding(new Insets(20));
        notesBox.setStyle("-fx-background-color: #34495E;");

        // Determine if editing should be allowed
        System.out.println(currentUserRole);
        boolean canEdit = (currentUserRole == UserRole.DOCTOR_OWN_PROFILE ||
                currentUserRole == UserRole.ADMIN_VIEWING_USER ||
                currentUserRole == UserRole.PATIENT_OWN_PROFILE);

        System.out.println(currentPatient);
        System.out.println(canEdit);

        if (currentPatient != null || canEdit) {

            User utente;
            if(canEdit) {
                System.out.println(currentUser);
                utente = currentUser;
                System.out.println("Ora i dati sono di: " + utente.getFullName());
            }else {
                utente = currentPatient;
                System.out.println("Ora i dati sono di: " + utente.getFullName());
            }

            // Create form fields for patient data
            VBox formContainer = new VBox(12);
            formContainer.setStyle("-fx-background-color: #2C3E50; -fx-background-radius: 8; -fx-padding: 20;");

            // Full Name
            VBox nameSection = createFormField("Name:", utente.getName(), canEdit);
            VBox surnameSection = createFormField("Surname:", utente.getSurname(), canEdit);
            // Email
            VBox emailSection = createFormField("Email:", utente.getEmail(), canEdit);

            // Phone (if available)
            String phone = utente.getPhone() != null ? utente.getPhone() : "Not specified";
            VBox phoneSection = createFormField("Phone:", phone, canEdit);

            // Date of Birth (if available)
            String dateOfBirth = utente.getBornDate() != null ?
                    utente.getBornDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Not specified";
            VBox dobSection = createFormField("Date of Birth:", dateOfBirth, canEdit);

            // Gender (if available)
            String gender = utente.getGender() != null ? utente.getGender().getDisplayName() : "Not specified";
            VBox genderSection = createFormField("Gender:", gender, canEdit);

            // Bitrth City
            String birthCity = utente.getBirthPlace() != null ? utente.getBirthPlace() : "Not specified";
            VBox birthCitySection = createFormField("Address:", birthCity, canEdit);

            String fiscalCode = utente.getFiscalCode() != null ? utente.getFiscalCode() : "Not specified";
            VBox fiscalCodeSection = createFormField("Fiscal Code:", fiscalCode, canEdit);


            formContainer.getChildren().addAll(
                    nameSection,
                    surnameSection,
                    emailSection,
                    phoneSection,
                    dobSection,
                    genderSection,
                    birthCitySection,
                    fiscalCodeSection

            );

            notesBox.getChildren().addAll(formContainer);

            // Add save button if user can edit
            if (canEdit) {
                HBox buttonContainer = new HBox(15);
                buttonContainer.setAlignment(Pos.CENTER_LEFT);

                Button saveButton = new Button("Save Changes");
                saveButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 10 20;");
                saveButton.setOnAction(e -> handleSaveUserInfo(formContainer));

                Button cancelButton = new Button("Cancel");
                cancelButton.setStyle("-fx-background-color: transparent; -fx-border-color: #3498DB; -fx-border-width: 1; -fx-text-fill: #3498DB; -fx-background-radius: 5; -fx-padding: 10 20;");
                cancelButton.setOnAction(e -> loadNotesContent()); // Reload to cancel changes

                buttonContainer.getChildren().addAll(saveButton, cancelButton);
                notesBox.getChildren().add(buttonContainer);
            }
        } else {
            Label noDataLabel = new Label("No patient data available");
            noDataLabel.setTextFill(Color.web("#BDC3C7"));
            noDataLabel.setFont(Font.font(14));
            notesBox.getChildren().addAll(noDataLabel);
        }

        if (notesContent != null) {
            notesContent.getChildren().clear();
            notesContent.getChildren().add(notesBox);
        }
    }

    private VBox createFormField(String labelText, String value, boolean editable) {
        VBox fieldContainer = new VBox(5);

        Label fieldLabel = new Label(labelText);
        fieldLabel.setTextFill(Color.web("#BDC3C7"));
        fieldLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        if (editable) {
            TextField textField = new TextField(value);
            textField.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-border-color: #3498DB; -fx-border-radius: 3; -fx-background-radius: 3;");
            textField.setPrefHeight(30);

            // Store the original value as user data for validation/reset purposes
            textField.setUserData(value);

            fieldContainer.getChildren().addAll(fieldLabel, textField);
        } else {
            Label valueLabel = new Label(value);
            valueLabel.setTextFill(Color.WHITE);
            valueLabel.setFont(Font.font(14));
            valueLabel.setWrapText(true);
            valueLabel.setStyle("-fx-background-color: #16213e; -fx-padding: 8; -fx-background-radius: 3;");

            fieldContainer.getChildren().addAll(fieldLabel, valueLabel);
        }

        return fieldContainer;
    }

    private VBox createDoctorSelectionField(String labelText, Integer currentDoctorId, boolean editable) throws SQLException {
        VBox fieldContainer = new VBox(5);

        Label fieldLabel = new Label(labelText);
        fieldLabel.setTextFill(Color.web("#BDC3C7"));
        fieldLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        if (editable) {
            ComboBox<Doctor> doctorComboBox = new ComboBox<>();
            doctorComboBox.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-border-color: #3498DB; -fx-border-radius: 3; -fx-background-radius: 3;");
            doctorComboBox.setPrefHeight(30);
            doctorComboBox.setPrefWidth(200);

            // Load doctors list
            loadDoctorsIntoComboBox(doctorComboBox);

            // Set current selection
            if (currentDoctorId != null) {
                doctorComboBox.getItems().stream()
                        .filter(doctor -> doctor.getId() == currentDoctorId)
                        .findFirst()
                        .ifPresent(doctorComboBox::setValue);
            }

            // Store field identifier for form processing
            doctorComboBox.setUserData("Assigned Doctor");

            fieldContainer.getChildren().addAll(fieldLabel, doctorComboBox);
        } else {
            String doctorInfo = currentDoctorId != null ?
                    DoctorDAO.getDoctorById(currentDoctorId).getFullName() : "No doctor assigned";

            Label valueLabel = new Label(doctorInfo);
            valueLabel.setTextFill(Color.WHITE);
            valueLabel.setFont(Font.font(14));
            valueLabel.setWrapText(true);
            valueLabel.setStyle("-fx-background-color: #16213e; -fx-padding: 8; -fx-background-radius: 3;");

            fieldContainer.getChildren().addAll(fieldLabel, valueLabel);
        }

        return fieldContainer;
    }

    private void loadDoctorsIntoComboBox(ComboBox<Doctor> comboBox) {
        try {

            // Load doctors from database/service
            List<Doctor> doctors = DoctorDAO.getAllDoctors(); // Implement this method
            comboBox.getItems().addAll(doctors);

            // Custom cell factory to display doctor names
            comboBox.setCellFactory(listView -> new ListCell<Doctor>() {
                @Override
                protected void updateItem(Doctor doctor, boolean empty) {
                    super.updateItem(doctor, empty);
                    if (empty || doctor == null) {
                        setText(null);
                    } else {
                        if (doctor.getId() == -1) {
                            setText("No Doctor");
                            setStyle("-fx-text-fill: #BDC3C7;");
                        } else {
                            setText(doctor.getName() + " " + doctor.getSurname() +
                                    (doctor.getSpecialization() != null ? " (" + doctor.getSpecialization() + ")" : ""));
                            setStyle("-fx-text-fill: white;");
                        }
                    }
                }
            });

            // Custom button cell for selected value display
            comboBox.setButtonCell(new ListCell<Doctor>() {
                @Override
                protected void updateItem(Doctor doctor, boolean empty) {
                    super.updateItem(doctor, empty);
                    if (empty || doctor == null) {
                        setText(null);
                    } else {
                        if (doctor.getId()==-1) {
                            setText("No Doctor");
                        } else {
                            setText(doctor.getName() + " " + doctor.getSurname());
                        }
                    }
                    setStyle("-fx-text-fill: white;");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading doctors: " + e.getMessage());
        }
    }

    /**
     * Handles saving of patient information from the form
     */
    private void handleSaveUserInfo(VBox formContainer) {
        try {
            // Extract data from form fields
            Map<String, String> updatedData = new HashMap<>();
            Map<String, Object> specialFields = new HashMap<>();

            for (Node child : formContainer.getChildren()) {
                if (child instanceof VBox) {
                    VBox fieldContainer = (VBox) child;
                    if (fieldContainer.getChildren().size() >= 2) {
                        Node labelNode = fieldContainer.getChildren().get(0);
                        Node inputNode = fieldContainer.getChildren().get(1);

                        if (labelNode instanceof Label) {
                            Label label = (Label) labelNode;
                            String fieldName = label.getText().replace(":", "");

                            if (inputNode instanceof TextField) {
                                TextField textField = (TextField) inputNode;
                                updatedData.put(fieldName, textField.getText());
                            } else if (inputNode instanceof ComboBox) {
                                // Handle ComboBox for doctor assignment or other dropdowns
                                ComboBox<?> comboBox = (ComboBox<?>) inputNode;
                                Object selectedValue = comboBox.getValue();

                                if (fieldName.equals("Doctor") && selectedValue instanceof Doctor) {
                                    specialFields.put("doctorId", ((Doctor) selectedValue).getId());
                                    updatedData.put(fieldName, ((Doctor) selectedValue).getName() + " " + ((Doctor) selectedValue).getSurname());
                                } else if (selectedValue != null) {
                                    updatedData.put(fieldName, selectedValue.toString());
                                }
                            }
                        }
                    }
                }
            }

            // Update user object with new data based on role
            User toUpdate = updateUserFromFormData(updatedData, specialFields);

            if (toUpdate == null) {
                showErrorAlert("Failed to prepare user data for update");
                return;
            }

            // Save to database or service
            boolean success = saveUserToDatabase(toUpdate);

            if (success) {
                showSuccessAlert("User information updated successfully");
                // Refresh the form to show updated data
                notesContent.getChildren().clear();
                loadNotesContent();
            } else {
                showErrorAlert("Failed to update user information");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error occurred while saving: " + e.getMessage());
        }
    }

    private boolean saveUserToDatabase(User toUpdate) {
        try {
            if (toUpdate == null) {
                return false;
            }

            System.out.println(toUpdate);
            System.out.println(toUpdate.getType());
            System.out.println("Ora mettiamo a confronto: '"+toUpdate.getType() +"' e 'DOCTOR'");
            System.out.println(toUpdate.getType().equals("DOCTOR"));
            if (toUpdate.getType().equals("DOCTOR")){
                Doctor doctor = DoctorDAO.getDoctorById(toUpdate.getId());
                doctor.setName(toUpdate.getName());
                doctor.setSurname(toUpdate.getSurname());
                doctor.setEmail(toUpdate.getEmail());
                doctor.setBornDate(toUpdate.getBornDate());
                doctor.setGender(toUpdate.getGender());
                doctor.setPhone(toUpdate.getPhone());
                doctor.setBirthPlace(toUpdate.getBirthPlace());
                doctor.setFiscalCode(toUpdate.getFiscalCode());
                DoctorDAO.updateDoctor(doctor);
            } else if (toUpdate.getType().equals("PATIENT")) {
                System.out.println("Sono dentro i pazienti " + toUpdate );
                Patient patient = PatientDAO.getPatientById(toUpdate.getId());
                patient.setName(toUpdate.getName());
                patient.setSurname(toUpdate.getSurname());
                patient.setEmail(toUpdate.getEmail());
                patient.setBornDate(toUpdate.getBornDate());
                patient.setGender(toUpdate.getGender());
                patient.setPhone(toUpdate.getPhone());
                patient.setBirthPlace(toUpdate.getBirthPlace());
                patient.setFiscalCode(toUpdate.getFiscalCode());
                System.out.println(patient);
                PatientDAO.updatePatient(patient);
            } else if (toUpdate.getType().equals("ADMIN")) {
                Admin admin = AdminDAO.getAdminById(toUpdate.getId());
                admin.setName(toUpdate.getName());
                admin.setSurname(toUpdate.getSurname());
                admin.setEmail(toUpdate.getEmail());
                admin.setBornDate(toUpdate.getBornDate());
                admin.setGender(toUpdate.getGender());
                admin.setPhone(toUpdate.getPhone());
                admin.setBirthPlace(toUpdate.getBirthPlace());
                admin.setFiscalCode(toUpdate.getFiscalCode());
                AdminDAO.updateAdmin(admin);
            } else {
                System.err.println("Unknown user type: " + toUpdate.getClass().getSimpleName());
                return false;
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates user object with form data and returns the updated user
     */
    private User updateUserFromFormData(Map<String, String> formData, Map<String, Object> specialFields) {
        // Determine which object to update based on the current user role
        User userToUpdate = null;

        switch (currentUserRole) {
            case PATIENT_OWN_PROFILE:
                userToUpdate = (User) currentUser; // Patient viewing own profile
                break;
            case DOCTOR_OWN_PROFILE:
                userToUpdate = (User) currentUser; // Doctor viewing own profile
                break;
            case ADMIN_OWN_PROFILE:
                userToUpdate = (User) currentUser; // Admin viewing own profile
                break;
            case DOCTOR_VIEWING_PATIENT:
            case ADMIN_VIEWING_USER:
                userToUpdate = currentPatient; // Doctor/Admin viewing another user
                break;
            default:
                System.err.println("Unknown user role: " + currentUserRole);
                return null;
        }

        if (userToUpdate == null) {
            System.err.println("No user to update");
            return null;
        }

        // Update common fields for all user types
        updateCommonUserFields(userToUpdate, formData);

        // Update specific fields based on user type
        if (userToUpdate instanceof Patient) {
            updatePatientSpecificFields((Patient) userToUpdate, formData, specialFields);
        } else if (userToUpdate instanceof Doctor) {
            updateDoctorSpecificFields((Doctor) userToUpdate, formData);
        } else if (userToUpdate instanceof Admin) {
            updateAdminSpecificFields((Admin) userToUpdate, formData);
        }

        return userToUpdate;
    }

    private void updateCommonUserFields(User user, Map<String, String> formData) {
        if (formData.containsKey("Name")) {
            user.setName(formData.get("Name").trim());
        }
        if (formData.containsKey("Surname")) {
            user.setSurname(formData.get("Surname").trim());
        }
        if (formData.containsKey("Email")) {
            user.setEmail(formData.get("Email").trim());
        }
        if (formData.containsKey("Phone")) {
            String phone = formData.get("Phone").trim();
            user.setPhone(phone.equals("Not specified") ? null : phone);
        }
        if (formData.containsKey("Date of Birth")) {
            String dobStr = formData.get("Date of Birth").trim();
            if (!dobStr.equals("Not specified")) {
                try {
                    LocalDate dob = LocalDate.parse(dobStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    user.setBornDate(dob);
                } catch (Exception e) {
                    System.err.println("Invalid date format for Date of Birth: " + dobStr);
                }
            } else {
                user.setBornDate(null);
            }
        }
        if (formData.containsKey("Gender")) {
            String genderStr = formData.get("Gender").trim();
            if (!genderStr.equals("Not specified")) {
                try {
                    Gender gender = Gender.valueOf(genderStr.toUpperCase());
                    user.setGender(gender);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid gender value: " + genderStr);
                }
            }
        }
        if (formData.containsKey("Birth City")) {
            String birthCity = formData.get("Birth City").trim();
            user.setBirthPlace(birthCity.equals("Not specified") ? null : birthCity);
        }
        if (formData.containsKey("Fiscal Code")) {
            String fiscalCode = formData.get("Fiscal Code").trim();
            user.setFiscalCode(fiscalCode.equals("Not specified") ? null : fiscalCode);
        }
    }

    private void updatePatientSpecificFields(Patient patient, Map<String, String> formData, Map<String, Object> specialFields) {
        // Update doctor assignment from ComboBox
        if (specialFields.containsKey("doctorId")) {
            Integer doctorId = (Integer) specialFields.get("doctorId");
            patient.setDoctorId(doctorId);
        }
    }

    private void updateDoctorSpecificFields(Doctor doctor, Map<String, String> formData) {
        if (formData.containsKey("Specialization")) {
            String specialization = formData.get("Specialization").trim();
            doctor.setSpecialization(specialization.equals("Not specified") ? null : specialization);
        }
    }

    /**
     * Updates Admin-specific fields
     */
    private void updateAdminSpecificFields(Admin admin, Map<String, String> formData) {
        if (formData.containsKey("Role")) {
            String role = formData.get("Role").trim();
            admin.setRole(role.equals("Not specified") ? null : role);
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}