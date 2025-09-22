package it.glucotrack.controller;

import it.glucotrack.model.*;
import it.glucotrack.util.*;
import it.glucotrack.model.Medication;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.*;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.control.ButtonBar;
 import javafx.scene.control.ButtonType;
 import javafx.scene.control.Dialog;
 import javafx.scene.control.ListCell;
 import javafx.scene.control.TextArea;
 import javafx.scene.paint.Color;
 import javafx.scene.text.Font;
 import javafx.scene.text.FontWeight;
 import java.util.Optional;

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
    @FXML
    private Label patientNameLabel;
    @FXML
    private Label patientIdLabel;

    // Tab buttons
    @FXML
    private Button overviewTab;
    @FXML
    private Button medicationTab;
    @FXML
    private Button notesTab;


    // Content areas
    @FXML
    private VBox overviewContent;
    @FXML
    private VBox trendsContent;
    @FXML
    private VBox medicationContent;
    @FXML
    private VBox notesContent;

    // Medication Table
    @FXML
    private TableView<Medication> prescribedMedicationsTable;

    @FXML
    private TableColumn<Medication, String> medicationNameColumn;

    @FXML
    private TableColumn<Medication, String> dosageColumn;

    @FXML
    private TableColumn<Medication, String> frequencyColumn;

    @FXML
    private TableColumn<Medication, String> instructionsColumn;

    @FXML
    private TableView<MedicationEdit> therapyModificationsTable;

    @FXML
    private TableColumn<MedicationEdit, String> therapyNameEditColumn;

    @FXML
    private TableColumn<MedicationEdit, String> dosageEditColumn;

    @FXML
    private TableColumn<MedicationEdit, String> frequencyEditColumn;

    @FXML
    private TableColumn<MedicationEdit, String> instructionsEditColumn;

    @FXML
    private TableColumn<MedicationEdit, String> modificationDateColumn;

    @FXML

    private TableColumn<MedicationEdit, String> modifiedByColumn;



    private ObservableList<Medication> prescribedMedications;
    private ObservableList<MedicationEdit> therapyModifications;


    // Overview content elements - Fixed to match FXML
    @FXML
    private Label currentGlucoseLabel;
    @FXML
    private Label trendLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private LineChart<String, Number> glucoseChart;
    @FXML
    private ComboBox<String> timeRangeCombo;
    @FXML
    private Label averageGlucoseLabel;
    @FXML
    private Label averageGlucoseChangeLabel;
    @FXML
    private LineChart<String, Number> glucoseTrendsChart;
    @FXML
    private Label adherancePercentageLabel;
    @FXML
    private ProgressBar adheranceProgressBar;

    @FXML
    private VBox symptomsContainer;
    @FXML
    private VBox riskFactorsContainer;


    // Action buttons
    @FXML
    private Button addRiskBtn;
    @FXML
    private Button deleteUserBtn;
    @FXML
    private Button sendMessageBtn;

    // Data models
    private Patient currentPatient;
    private User currentUser;
    private UserRole currentUserRole;
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
        setupCharts();
        loadTrendsContent();
        setupButtons();
        loadRiskFactors();
        initializeAdditionalButtons();
        setupTherapyTable();
        setupTherapyEditTable();
    }

    @FXML
    private void setupTherapyEditTable(){
        therapyNameEditColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedication().getName_medication()));
        dosageEditColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedication().getDose()));
        frequencyEditColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedication().getFreq().toString()));
        instructionsEditColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedication().getInstructions()));
        modificationDateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEditTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        modifiedByColumn.setCellValueFactory(cellData -> {
            try {
                Doctor doc = DoctorDAO.getDoctorById(cellData.getValue().getDoctorId());
                String name = (doc != null) ? doc.getFullName() : "Unknown Doctor";
                return new SimpleStringProperty(name);
            } catch (SQLException e) {
                e.printStackTrace(); // logga l'errore
                return new SimpleStringProperty("Error");
            }
        });

    }

    @FXML
    private void setupTherapyTable(){
        medicationNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName_medication()));
        dosageColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDose()));
        frequencyColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFreq().toString()));
        instructionsColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getInstructions()));
    };

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

            updatePatientSpecificData();
            loadTherapyTable();
            loadTherapyModificationsTable();
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

        if (medicationTab != null) {
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

        if (addRiskBtn != null) {
            addRiskBtn.setVisible(false);
        }
        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(false);
        }

        showMedicalTabs(false);
        enableNotesEditing(false);
    }

    private void setupPatientOwnProfile() {
        showMedicalCharts(false);

        if (addRiskBtn != null) {
            addRiskBtn.setVisible(false);
        }

        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(false);
        }

        showMedicalTabs(true);
        enableNotesEditing(false);
    }

    private void setupAdminViewingUser() {
        showMedicalCharts(false);

        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(true);
            deleteUserBtn.setDisable(false);
            deleteUserBtn.setText("Delete User");
            deleteUserBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 5;");
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

        if (deleteUserBtn != null) {
            deleteUserBtn.setVisible(false);
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
        String email ="";
        String messageType = "";
        switch (currentUserRole) {
            case DOCTOR_VIEWING_PATIENT:
                messageType = "message to patient";
                email = currentPatient.getEmail();
                break;
            case PATIENT_OWN_PROFILE:
                messageType = "message to doctor";
                break;
            case ADMIN_VIEWING_USER:
                messageType = "admin notification";
                break;
        }
        MailHelper.openMailClient(email);

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
        if (currentUserRole == UserRole.DOCTOR_VIEWING_PATIENT) {
            switchToTab("Overview");
        } else {
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
            loadSymptoms();
            updateChart();
            updateMedicationProgress();
            updatePatientInfo();
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
            double change = ((double) (latest.getGlucoseLevel() - oldest.getGlucoseLevel()) / oldest.getGlucoseLevel()) * 100;

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

    private void loadSymptoms() {

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
                HBox symptomBox = createSymptomBox(symptom);
                symptomsContainer.getChildren().add(symptomBox);
            }
        }
    }

    private HBox createSymptomBox(Symptom symptom) {
        HBox symptomBox = new HBox(10);
        symptomBox.setPadding(new Insets(8, 12, 8, 12));
        symptomBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4; -fx-cursor: hand;");

        Label symptomLabel = new Label(symptom.getSymptomName());
        symptomLabel.setTextFill(Color.WHITE);
        symptomLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

        String severityColor = getSeverityColor(symptom.getGravity());

        Label severityLabel = new Label(symptom.getGravity());
        severityLabel.setTextFill(Color.web(severityColor));
        severityLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        severityLabel.setStyle("-fx-background-color: " + severityColor + "33; -fx-background-radius: 3; -fx-padding: 2 6 2 6;");

        symptomBox.getChildren().addAll(symptomLabel, severityLabel);

        // Add double-click event handler
        symptomBox.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                System.out.println("Double-clicked on symptom: " + symptom.getSymptomName());
                showSymptomDetailsPopup(symptom);
            }
        });

        // Add hover effect
        symptomBox.setOnMouseEntered(e -> symptomBox.setStyle("-fx-background-color: #3A5270; -fx-background-radius: 4; -fx-cursor: hand;"));
        symptomBox.setOnMouseExited(e -> symptomBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4; -fx-cursor: hand;"));

        return symptomBox;
    }

    private void showSymptomDetailsPopup(Symptom symptom) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController popupController = loader.getController();

            // Set popup content
            popupController.setTitle("Symptom Details");
            popupController.setSubtitle("Complete symptom information");

            // Create content for the popup
            VBox content = new VBox(15);
            content.setStyle("-fx-padding: 10;");

            // Symptom name
            addDetailRow(content, "Symptom:", symptom.getSymptomName());

            // Severity
            addDetailRow(content, "Severity:", symptom.getGravity());

            // Duration (if available)
            String duration = (symptom.getDuration() != null && !symptom.getDuration().equals("")) ?
                    symptom.getDuration().toString() : "Not specified";
            addDetailRow(content, "Duration:", duration);

            // Date reported
            String dateReported = symptom.getDateAndTime() != null ?
                    symptom.getDateAndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) :
                    "Not specified";
            addDetailRow(content, "Reported on:", dateReported);

            // Notes (if available)
            if (symptom.getNotes() != null && !symptom.getNotes().isEmpty()) {
                VBox notesSection = new VBox(5);
                Label notesTitle = new Label("Notes:");
                notesTitle.setStyle("-fx-text-fill: #bfc6d1; -fx-font-weight: bold; -fx-font-size: 1.1em;");

                Label notesContent = new Label(symptom.getNotes());
                notesContent.setStyle("-fx-text-fill: #fff; -fx-font-size: 1em; -fx-wrap-text: true;");
                notesContent.setWrapText(true);
                notesContent.setMaxWidth(400);

                notesSection.getChildren().addAll(notesTitle, notesContent);
                content.getChildren().add(notesSection);
            }

            popupController.setContent(content);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setMinWidth(520);
            popupStage.setMinHeight(340);
            popupController.setStage(popupStage);
            popupStage.showAndWait();

            // Show the popup


        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Failed to show symptom details: " + e.getMessage());
        }
    }

    private String getSeverityColor(String severity) {
        switch (severity) {
            case "Mild":
                return "#F1C40F";
            case "Moderate":
                return "#E67E22";
            case "Severe":
                return "#E74C3C";
            case "Critical":
                return "#8E44AD";
            default:
                return "#3498DB";
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
            case "Ultimi 7 giorni":
                return 15;
            case "Ultimi 30 giorni":
                return 20;
            case "Ultimo anno":
                return 25;
            default:
                return 15;
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
            case "Ultimi 7 giorni":
                return 7;
            case "Ultimi 30 giorni":
                return 30;
            case "Ultimo anno":
                return 365;
            default:
                return 7;
        }
    }


    private int getMaxPointsForTimeRange(String timeRange) {
        switch (timeRange) {
            case "Last 7 days":
                return 7;
            case "Last 30 days":
                return 30;
            case "Last 3 months":
                return 90;
            default:
                return 10;
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

    private void loadTherapyTable() {

        ObservableList<Medication> data = FXCollections.observableArrayList();
        try {
            List<Medication> meds = MedicationDAO.getMedicationsByPatientId(currentPatient.getId());
            data.addAll(meds);
            prescribedMedicationsTable.setItems(data);

            if (data.isEmpty()) {
                System.out.println("No prescribed medications found for patient ID: " + currentPatient.getId());
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load prescribed medications", e.getMessage());
        }
    }

    private void loadTherapyModificationsTable() throws SQLException {

        ObservableList<MedicationEdit> data = FXCollections.observableArrayList();

        List<Medication> meds = MedicationDAO.getMedicationsByPatientId(currentPatient.getId());

        for (Medication med : meds) {
            System.out.println("Loading edits for medication: " + med.getName_medication());
            List<MedicationEdit> edits = MedicationDAO.getMedicationEditsByMedicationId(med.getId());
            data.addAll(edits);
        }

        therapyModificationsTable.setItems(data);

        if (data.isEmpty()) {
            System.out.println("Therapy modifications are empty");
        }
    }



    private void loadRiskFactors() {
        System.out.println("This is for test before the if in loadRiskFactors");
        if (riskFactorsContainer != null) {
            riskFactorsContainer.getChildren().clear();
            System.out.println("This is for test inside loadRiskFactors");

            if (currentPatient != null) {
                List<RiskFactor> riskFactors = currentPatient.getRiskFactors();
                if (riskFactors.isEmpty()) {
                    Label noRiskLabel = new Label("No risk factors assessed");
                    noRiskLabel.setTextFill(Color.web("#BDC3C7"));
                    noRiskLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
                    noRiskLabel.setPadding(new Insets(10));
                    riskFactorsContainer.getChildren().add(noRiskLabel);
                    return;
                }

                for(RiskFactor factor : riskFactors) {
                    String color;
                    switch (factor.getGravity()) {
                        case LOW: color = "#2ECC71"; break;
                        case MEDIUM: color = "#F39C12"; break;
                        case HIGH: color = "#E74C3C"; break;
                        default: color = "#3498DB"; break;
                    }
                    System.out.println("Adding risk factor: " + factor.getType() + " with gravity " + factor.getGravity());
                    HBox riskFactorBox = createRiskFactorBox(factor, color);
                    riskFactorsContainer.getChildren().add(riskFactorBox);
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

    private HBox createRiskFactorBox(RiskFactor riskFactor, String color) {
        HBox riskBox = new HBox(10);
        riskBox.setPadding(new Insets(8, 12, 8, 12));
        riskBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4; -fx-cursor: hand;");

        VBox factorInfo = new VBox(2);
        Label factorLabel = new Label(riskFactor.getType());
        factorLabel.setTextFill(Color.WHITE);
        factorLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        factorInfo.getChildren().add(factorLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label riskLabel = new Label(riskFactor.getGravity().toString());
        riskLabel.setTextFill(Color.web(color));
        riskLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        riskLabel.setStyle("-fx-background-color: " + color + "33; -fx-background-radius: 3; -fx-padding: 2 6 2 6;");

        riskBox.getChildren().addAll(factorInfo, spacer, riskLabel);

        // Add double-click event handler for details
        riskBox.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showRiskFactorDetailsPopup(riskFactor);
            }
        });

        // Add right-click context menu
        ContextMenu contextMenu = createRiskFactorContextMenu(riskFactor);
        riskBox.setOnContextMenuRequested(event -> {
            contextMenu.show(riskBox, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        // Add hover effect
        riskBox.setOnMouseEntered(e -> riskBox.setStyle("-fx-background-color: #3A5270; -fx-background-radius: 4; -fx-cursor: hand;"));
        riskBox.setOnMouseExited(e -> riskBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4; -fx-cursor: hand;"));

        return riskBox;
    }

    private void showRiskFactorDetailsPopup(RiskFactor riskFactor) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController popupController = loader.getController();

            // Set popup content
            popupController.setTitle("Risk Factor Details");
            popupController.setSubtitle("Complete risk factor information");

            // Create content for the popup
            VBox content = new VBox(15);
            content.setStyle("-fx-padding: 10;");

            // Risk factor type
            addDetailRow(content, "Risk Factor:", riskFactor.getType());

            // Severity with color
            HBox severityRow = new HBox(10);
            severityRow.setAlignment(Pos.CENTER_LEFT);

            Label severityTitle = new Label("Severity:");
            severityTitle.setStyle("-fx-text-fill: #bfc6d1; -fx-font-weight: bold; -fx-font-size: 1.1em;");

            String color;
            switch (riskFactor.getGravity()) {
                case LOW: color = "#2ECC71"; break;
                case MEDIUM: color = "#F39C12"; break;
                case HIGH: color = "#E74C3C"; break;
                default: color = "#3498DB"; break;
            }

            Label severityValue = new Label(riskFactor.getGravity().toString());
            severityValue.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 1.1em; -fx-font-weight: bold; " +
                    "-fx-background-color: " + color + "33; -fx-background-radius: 3; -fx-padding: 4 8 4 8;");

            severityRow.getChildren().addAll(severityTitle, severityValue);
            content.getChildren().add(severityRow);


            popupController.setContent(content);

            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setMinWidth(400);
            popupStage.setMinHeight(300);
            popupController.setStage(popupStage);
            popupStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Failed to show risk factor details: " + e.getMessage());
        }
    }

    private ContextMenu createRiskFactorContextMenu(RiskFactor riskFactor) {
        ContextMenu contextMenu = new ContextMenu();

        // Edit menu item
        MenuItem editItem = new MenuItem("Edit Risk Factor");
        editItem.setStyle("-fx-text-fill: #3498DB;");
        editItem.setOnAction(e -> showEditRiskFactorDialog(riskFactor));

        // Delete menu item
        MenuItem deleteItem = new MenuItem("Delete Risk Factor");
        deleteItem.setStyle("-fx-text-fill: #E74C3C;");
        deleteItem.setOnAction(e -> showDeleteRiskFactorConfirmation(riskFactor));

        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // Details menu item
        MenuItem detailsItem = new MenuItem("View Details");
        detailsItem.setStyle("-fx-text-fill: #BDC3C7;");
        detailsItem.setOnAction(e -> showRiskFactorDetailsPopup(riskFactor));

        contextMenu.getItems().addAll(editItem, deleteItem, separator, detailsItem);

        // Style the context menu
        contextMenu.setStyle("-fx-background-color: #2C3E50; -fx-border-color: #3498DB; -fx-border-width: 1;");

        return contextMenu;
    }

    private void showEditRiskFactorDialog(RiskFactor riskFactor) {
        Dialog<RiskFactorFormData> dialog = new Dialog<>();
        dialog.setTitle("Edit Risk Factor");
        dialog.setHeaderText("Edit risk factor for " + currentPatient.getFullName());

        ButtonType updateButtonType = new ButtonType("Update Risk Factor", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        // Create form similar to add form but pre-filled
        VBox formContainer = new VBox(15);
        formContainer.setPadding(new Insets(20));
        formContainer.setStyle("-fx-background-color: #2C3E50;");

        // Risk Factor Type section (pre-filled)
        VBox typeSection = new VBox(5);
        Label typeLabel = new Label("Risk Factor Type:");
        typeLabel.setTextFill(Color.WHITE);
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.setPrefWidth(300);
        typeComboBox.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-border-color: #3498DB; -fx-border-radius: 3; -fx-background-radius: 3;");
        typeComboBox.getItems().addAll(
                "Hypertension", "High Cholesterol", "Obesity", "Family History of Diabetes",
                "Sedentary Lifestyle", "Smoking", "Age >45 years", "Previous Gestational Diabetes",
                "Polycystic Ovary Syndrome", "Sleep Apnea", "Cardiovascular Disease", "Kidney Disease", "Other"
        );
        typeComboBox.setEditable(true);
        typeComboBox.setValue(riskFactor.getType()); // Pre-fill current value

        typeSection.getChildren().addAll(typeLabel, typeComboBox);

        // Gravity section (pre-filled)
        VBox gravitySection = new VBox(5);
        Label gravityLabel = new Label("Severity Level:");
        gravityLabel.setTextFill(Color.WHITE);
        gravityLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        ComboBox<Gravity> gravityComboBox = new ComboBox<>();
        gravityComboBox.setPrefWidth(300);
        gravityComboBox.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-border-color: #3498DB; -fx-border-radius: 3; -fx-background-radius: 3;");
        gravityComboBox.getItems().addAll(Gravity.LOW, Gravity.MEDIUM, Gravity.HIGH);
        gravityComboBox.setValue(riskFactor.getGravity()); // Pre-fill current value

        gravitySection.getChildren().addAll(gravityLabel, gravityComboBox);

        formContainer.getChildren().addAll(typeSection, gravitySection);

        Node updateButton = dialog.getDialogPane().lookupButton(updateButtonType);
        updateButton.setDisable(false);

        dialog.getDialogPane().setContent(formContainer);

        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: #2C3E50;");
        dialog.getDialogPane().lookupButton(updateButtonType).setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 5;");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle("-fx-background-color: transparent; -fx-border-color: #3498DB; -fx-border-width: 1; -fx-text-fill: #3498DB; -fx-background-radius: 5;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                String riskType = typeComboBox.getValue();
                return new RiskFactorFormData(riskType, gravityComboBox.getValue(), "");
            }
            return null;
        });

        Optional<RiskFactorFormData> result = dialog.showAndWait();

        result.ifPresent(formData -> {
            try {
                RiskFactorDAO riskFactorDAO = new RiskFactorDAO();
                boolean success = riskFactorDAO.updateRiskFactor(
                        riskFactor.getId(),
                        currentPatient.getId(),
                        formData.getType(),
                        "", // description not used in current schema
                        formData.getGravity()
                );

                if (success) {
                    // Refresh the patient's risk factors list
                    List<RiskFactor> updatedRiskFactors = RiskFactorDAO.getRiskFactorsByPatientId(currentPatient.getId());
                    currentPatient.setRiskFactors(updatedRiskFactors);

                    loadRiskFactors();
                    showSuccessAlert("Risk factor updated successfully.");
                } else {
                    showErrorAlert("Failed to update risk factor.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showErrorAlert("Database error: " + e.getMessage());
            }
        });
    }

    private void showDeleteRiskFactorConfirmation(RiskFactor riskFactor) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Risk Factor");
        confirmAlert.setHeaderText("Confirm Deletion");
        confirmAlert.setContentText("Are you sure you want to delete the risk factor '" +
                riskFactor.getType() + "'? This action cannot be undone.");

        // Style the alert
        confirmAlert.getDialogPane().setStyle("-fx-background-color: #2C3E50;");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    RiskFactorDAO riskFactorDAO = new RiskFactorDAO();
                    boolean success = riskFactorDAO.deleteRiskFactor(riskFactor.getId());

                    if (success) {
                        // Refresh the patient's risk factors list
                        List<RiskFactor> updatedRiskFactors = RiskFactorDAO.getRiskFactorsByPatientId(currentPatient.getId());
                        currentPatient.setRiskFactors(updatedRiskFactors);

                        loadRiskFactors();
                        showSuccessAlert("Risk factor deleted successfully.");
                    } else {
                        showErrorAlert("Failed to delete risk factor.");
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    showErrorAlert("Database error: " + e.getMessage());
                }
            }
        });
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
                    highReadings, (double) highReadings / readings.size() * 100));
            analysis.append(String.format("Normal readings (80-140): %d (%.1f%%)\n",
                    normalReadings, (double) normalReadings / readings.size() * 100));
        } else {
            analysis.append("High readings (>180): 0 (0.0%)\n");
            analysis.append("Normal readings (80-140): 0 (0.0%)\n");
        }

        if (average > 180) {
            analysis.append("\nRecommendation: Glucose levels are consistently high. Consider medication adjustment.");
        } else if (average > 140) {
            analysis.append("\nRecommendation: Glucose levels are moderately elevated. Continue monitoring closely.");
        } else if (average < 80) {
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
            if (canEdit) {
                System.out.println(currentUser);
                utente = currentUser;
                System.out.println("Ora i dati sono di: " + utente.getFullName());
            } else {
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
        Label fieldLabel = new Label(labelText);
        fieldLabel.setTextFill(Color.web("#BDC3C7"));
        fieldLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        fieldLabel.setPrefWidth(120); // stessa larghezza per tutte le etichette

        Node inputNode;

        if (editable) {
            TextField textField = new TextField(value);
            textField.setStyle(
                    "-fx-background-color: #34495E; -fx-text-fill: white; "
                            + "-fx-border-color: #3498DB; -fx-border-radius: 3; -fx-background-radius: 3;"
            );
            textField.setPrefHeight(30);
            textField.setPrefWidth(250); // larghezza fissa o regolabile
            textField.setUserData(value);
            inputNode = textField;
        } else {
            Label valueLabel = new Label(value);
            valueLabel.setTextFill(Color.WHITE);
            valueLabel.setFont(Font.font(14));
            valueLabel.setWrapText(true);
            valueLabel.setStyle("-fx-background-color: #16213e; -fx-padding: 8; -fx-background-radius: 3;");
            valueLabel.setMaxWidth(250); // per andare a capo se troppo lungo
            inputNode = valueLabel;
        }

        VBox fieldContainer = new VBox(15, fieldLabel, inputNode);
        fieldContainer.setAlignment(Pos.CENTER_LEFT);

        return fieldContainer;
    }



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
            System.out.println("Ora mettiamo a confronto: '" + toUpdate.getType() + "' e 'DOCTOR'");
            System.out.println(toUpdate.getType().equals("DOCTOR"));
            if (toUpdate.getType().equals("DOCTOR")) {
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
                System.out.println("Sono dentro i pazienti " + toUpdate);
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

    private void handleAddRisk() {
        if (currentPatient == null) return;

        // Create the dialog
        Dialog<RiskFactorFormData> dialog = new Dialog<>();
        dialog.setTitle("Add Risk Factor");
        dialog.setHeaderText("Add new risk factor for " + currentPatient.getFullName());

        // Set the button types
        ButtonType addButtonType = new ButtonType("Add Risk Factor", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create the form content
        VBox formContainer = new VBox(15);
        formContainer.setPadding(new Insets(20));
        formContainer.setStyle("-fx-background-color: #2C3E50;");

        // Risk Factor Type section
        VBox typeSection = new VBox(5);
        Label typeLabel = new Label("Risk Factor Type:");
        typeLabel.setTextFill(Color.WHITE);
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.setPrefWidth(300);
        typeComboBox.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-border-color: #3498DB; -fx-border-radius: 3; -fx-background-radius: 3;");

        // Populate with common risk factors
        typeComboBox.getItems().addAll(
                "Hypertension",
                "High Cholesterol",
                "Obesity",
                "Family History of Diabetes",
                "Sedentary Lifestyle",
                "Smoking",
                "Age >45 years",
                "Previous Gestational Diabetes",
                "Polycystic Ovary Syndrome",
                "Sleep Apnea",
                "Cardiovascular Disease",
                "Kidney Disease",
                "Other"
        );
        typeComboBox.setEditable(true); // Allow custom entries

        typeSection.getChildren().addAll(typeLabel, typeComboBox);

        // Gravity/Severity section
        VBox gravitySection = new VBox(5);
        Label gravityLabel = new Label("Severity Level:");
        gravityLabel.setTextFill(Color.WHITE);
        gravityLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        ComboBox<Gravity> gravityComboBox = new ComboBox<>();
        gravityComboBox.setPrefWidth(300);
        gravityComboBox.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-border-color: #3498DB; -fx-border-radius: 3; -fx-background-radius: 3;");
        gravityComboBox.getItems().addAll(Gravity.LOW, Gravity.MEDIUM, Gravity.HIGH);
        gravityComboBox.setValue(Gravity.MEDIUM); // Default selection

        // Custom cell factory to display gravity levels with colors
        gravityComboBox.setCellFactory(listView -> new ListCell<Gravity>() {
            @Override
            protected void updateItem(Gravity gravity, boolean empty) {
                super.updateItem(gravity, empty);
                if (empty || gravity == null) {
                    setText(null);
                    setStyle("-fx-text-fill: white;");
                } else {
                    setText(gravity.toString());
                    switch (gravity) {
                        case LOW:
                            setStyle("-fx-text-fill: #2ECC71;"); // Green
                            break;
                        case MEDIUM:
                            setStyle("-fx-text-fill: #F39C12;"); // Orange
                            break;
                        case HIGH:
                            setStyle("-fx-text-fill: #E74C3C;"); // Red
                            break;
                    }
                }
            }
        });

        // Button cell for selected value
        gravityComboBox.setButtonCell(new ListCell<Gravity>() {
            @Override
            protected void updateItem(Gravity gravity, boolean empty) {
                super.updateItem(gravity, empty);
                if (empty || gravity == null) {
                    setText(null);
                } else {
                    setText(gravity.toString());
                    setStyle("-fx-text-fill: white;");
                }
            }
        });

        gravitySection.getChildren().addAll(gravityLabel, gravityComboBox);

        // Additional notes section (optional)
        VBox notesSection = new VBox(5);
        Label notesLabel = new Label("Additional Notes (Optional):");
        notesLabel.setTextFill(Color.WHITE);
        notesLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        TextArea notesTextArea = new TextArea();
        notesTextArea.setPrefRowCount(3);
        notesTextArea.setPrefWidth(300);
        notesTextArea.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-border-color: #3498DB; -fx-border-radius: 3; -fx-background-radius: 3;");
        notesTextArea.setPromptText("Enter any additional information about this risk factor...");

        notesSection.getChildren().addAll(notesLabel, notesTextArea);

        // Add all sections to the form container
        formContainer.getChildren().addAll(typeSection, gravitySection, notesSection);

        // Enable/Disable Add button based on input validation
        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);

        // Validation listener
        typeComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });

        typeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(formContainer);

        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: #2C3E50;");
        dialog.getDialogPane().lookupButton(addButtonType).setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 5;");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle("-fx-background-color: transparent; -fx-border-color: #3498DB; -fx-border-width: 1; -fx-text-fill: #3498DB; -fx-background-radius: 5;");

        // Convert result when Add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String riskType = typeComboBox.getEditor().getText();
                if (riskType == null || riskType.trim().isEmpty()) {
                    riskType = typeComboBox.getValue();
                }

                return new RiskFactorFormData(
                        riskType != null ? riskType.trim() : "",
                        gravityComboBox.getValue(),
                        notesTextArea.getText().trim()
                );
            }
            return null;
        });

        // Request focus on the type field
        Platform.runLater(() -> typeComboBox.requestFocus());

        // Show dialog and process result
        Optional<RiskFactorFormData> result = dialog.showAndWait();

        result.ifPresent(formData -> {
            try {
                // Insert the risk factor into the database
                RiskFactorDAO riskFactorDAO = new RiskFactorDAO();
                boolean success = riskFactorDAO.insertRiskFactor(
                        currentPatient.getId(),
                        formData.getType(),
                        formData.getGravity()
                );

                if (success) {
                    // Refresh the patient's risk factors list
                    List<RiskFactor> updatedRiskFactors = RiskFactorDAO.getRiskFactorsByPatientId(currentPatient.getId());
                    currentPatient.setRiskFactors(updatedRiskFactors);

                    // Refresh the UI
                    loadRiskFactors();

                    // Show success message
                    showSuccessAlert("Risk factor '" + formData.getType() + "' has been successfully added.");
                } else {
                    showErrorAlert("Failed to add risk factor. Please try again.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showErrorAlert("Database error occurred while adding risk factor: " + e.getMessage());
            }
        });
    }

    private void addDetailRow(VBox container, String title, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #bfc6d1; -fx-font-weight: bold; -fx-font-size: 1.1em;");
        titleLabel.setPrefWidth(120);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 1.1em;");
        valueLabel.setWrapText(true);

        row.getChildren().addAll(titleLabel, valueLabel);
        container.getChildren().add(row);
    }

    /**
     * Helper method to show popup with proper styling and positioning
     */
    private void showPopup(StackPane popupRoot) {
        if (parentContentPane != null) {
            // Create overlay background
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
            overlay.setAlignment(Pos.CENTER);

            // Add popup to overlay
            overlay.getChildren().add(popupRoot);

            // Add overlay to parent content pane
            parentContentPane.getChildren().add(overlay);

            // Close popup when clicking on overlay (outside popup)
            overlay.setOnMouseClicked(event -> {
                if (event.getTarget() == overlay) {
                    parentContentPane.getChildren().remove(overlay);
                }
            });
        }
    }

    // Add this inner class to store form data
    private static class RiskFactorFormData {
        private final String type;
        private final Gravity gravity;
        private final String notes;

        public RiskFactorFormData(String type, Gravity gravity, String notes) {
            this.type = type;
            this.gravity = gravity;
            this.notes = notes;
        }

        public String getType() {
            return type;
        }

        public Gravity getGravity() {
            return gravity;
        }

    }

}