package it.glucotrack.controller;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.ResourceBundle;

import it.glucotrack.model.Doctor;
import it.glucotrack.model.Gender;
import it.glucotrack.model.GlucoseMeasurement;
import it.glucotrack.model.Patient;
import it.glucotrack.util.GlucoseMeasurementDAO;
import it.glucotrack.util.PatientDAO;
import it.glucotrack.util.DoctorDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class DoctorDashboardPatientsController implements Initializable {

    // Search and filter elements
    @FXML private TextField searchField;
    @FXML private Button filterBtn;
    @FXML private Button addPatientBtn;

    // Table elements
    @FXML private TableView<PatientTableData> patientsTable;
    @FXML private TableColumn<PatientTableData, String> patientNameColumn;
    @FXML private TableColumn<PatientTableData, String> lastGlucoseColumn;
    @FXML private TableColumn<PatientTableData, String> riskStatusColumn;
    @FXML private TableColumn<PatientTableData, String> lastReadingColumn;
    @FXML private TableColumn<PatientTableData, Integer> ageColumn;

    // Context menu
    @FXML private ContextMenu tableContextMenu;
    @FXML private MenuItem viewPatientMenuItem;
    @FXML private MenuItem editPatientMenuItem;
    @FXML private MenuItem deletePatientMenuItem;


    // Status elements
    @FXML private Label statusLabel;
    @FXML private Label totalPatientsLabel;

    // Data and DAOs
    private ObservableList<PatientTableData> patientTableData;
    private FilteredList<PatientTableData> filteredPatients;
    private PatientTableData selectedPatient;
    private PatientDAO patientDAO;
    private GlucoseMeasurementDAO glucoseMeasurementDAO;
    private Doctor doctorUser;
    private int doctorId; // ID of the logged-in doctor

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.patientDAO = new PatientDAO();
        this.glucoseMeasurementDAO = new GlucoseMeasurementDAO();
        // The doctorId should be set by the calling controller, e.g., after login
        // For demonstration, let's use a placeholder value
        this.doctorId = it.glucotrack.util.SessionManager.getInstance().getCurrentUser().getId();
        try {
            this.doctorUser = DoctorDAO.getDoctorById(it.glucotrack.util.SessionManager.getInstance().getCurrentUser().getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        setupTable();
        setupSearch();
        setupButtons();
        setupContextMenu();
        loadPatientsData();
        updateStatusBar();
    }



    private void setupTable() {
        patientTableData = FXCollections.observableArrayList();
        filteredPatients = new FilteredList<>(patientTableData);
        patientsTable.setItems(filteredPatients);

        patientNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        lastGlucoseColumn.setCellValueFactory(new PropertyValueFactory<>("lastGlucoseReading"));
        riskStatusColumn.setCellValueFactory(new PropertyValueFactory<>("riskStatus"));
        lastReadingColumn.setCellValueFactory(new PropertyValueFactory<>("lastReadingFormatted"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));

        setupRiskStatusColumn();

        patientsTable.setRowFactory(tv -> {
            TableRow<PatientTableData> row = new TableRow<PatientTableData>() {
                @Override
                protected void updateItem(PatientTableData patientData, boolean empty) {
                    super.updateItem(patientData, empty);
                    if (empty || patientData == null) {
                        setStyle("");
                        getStyleClass().removeAll("table-row-high-risk", "table-row-moderate-risk", "table-row-normal");
                    } else {
                        getStyleClass().removeAll("table-row-high-risk", "table-row-moderate-risk", "table-row-normal");
                        switch (patientData.getRiskStatus()) {
                            case "High":
                                getStyleClass().add("table-row-high-risk");
                                break;
                            case "Moderate":
                                getStyleClass().add("table-row-moderate-risk");
                                break;
                            case "Normal":
                                getStyleClass().add("table-row-normal");
                                break;
                        }
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    viewPatientProfile(getSelectedPatient());
                }
            });

            return row;
        });

        patientsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> selectedPatient = newSelection);

        patientsTable.getStyleClass().add("patients-table");
    }

    private void setupRiskStatusColumn() {
        riskStatusColumn.setCellFactory(column -> new TableCell<PatientTableData, String>() {
            @Override
            protected void updateItem(String riskStatus, boolean empty) {
                super.updateItem(riskStatus, empty);
                if (empty || riskStatus == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label statusLabel = new Label(riskStatus);
                    statusLabel.setPrefWidth(80);
                    statusLabel.setAlignment(Pos.CENTER);
                    statusLabel.getStyleClass().add("risk-status-label");

                    switch (riskStatus) {
                        case "Normal":
                            statusLabel.getStyleClass().add("risk-normal");
                            break;
                        case "Moderate":
                            statusLabel.getStyleClass().add("risk-moderate");
                            break;
                        case "High":
                            statusLabel.getStyleClass().add("risk-high");
                            break;
                        default:
                            statusLabel.getStyleClass().add("risk-unknown");
                    }

                    setGraphic(statusLabel);
                    setText(null);
                }
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterPatients(newValue);
        });

        searchField.getStyleClass().add("search-field");
    }

    private void setupButtons() {
        addPatientBtn.setOnAction(e -> handleAddPatient());
        filterBtn.setOnAction(e -> handleFilter());

        addPatientBtn.getStyleClass().addAll("btn", "btn-primary");
        filterBtn.getStyleClass().addAll("btn", "btn-secondary");
    }

    private void setupContextMenu() {
        viewPatientMenuItem.setOnAction(e -> {
            if (selectedPatient != null) {
                viewPatientProfile(selectedPatient);
            }
        });

        editPatientMenuItem.setOnAction(e -> {
            if (selectedPatient != null) {
                editPatient(selectedPatient);
            }
        });

        deletePatientMenuItem.setOnAction(e -> {
            if (selectedPatient != null) {
                deletePatient(selectedPatient);
            }
        });

        patientsTable.setContextMenu(tableContextMenu);
        tableContextMenu.getStyleClass().add("context-menu");
    }

    private void loadPatientsData() {
        patientTableData.clear();
        try {
            // Fetch patients from the database assigned to this doctor
            List<Patient> patients = patientDAO.getPatientsByDoctorId(this.doctorId);
            if (patients != null) {
                for (Patient patient : patients) {
                    // Fetch the latest glucose measurement for each patient
                    GlucoseMeasurement lastMeasurement = glucoseMeasurementDAO.getLatestMeasurementByPatientId(patient.getId());
                    if (lastMeasurement != null) {
                        patient.getGlucoseReadings().add(lastMeasurement);
                    }
                    PatientTableData tableData = new PatientTableData(patient);
                    patientTableData.add(tableData);
                }
            }
            statusLabel.setText("Patients loaded successfully from the database");
        } catch (SQLException e) {
            statusLabel.setText("Error loading patients: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterPatients(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredPatients.setPredicate(null);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredPatients.setPredicate(patientData ->
                    patientData.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                            patientData.getRiskStatus().toLowerCase().contains(lowerCaseFilter) ||
                            patientData.getLastGlucoseReading().contains(searchText) ||
                            patientData.getPatient().getEmail().toLowerCase().contains(lowerCaseFilter)
            );
        }
        updateStatusBar();
    }

    private void updateStatusBar() {
        int totalPatients = patientTableData.size();
        int filteredCount = filteredPatients.size();

        if (filteredCount == totalPatients) {
            totalPatientsLabel.setText("Total: " + totalPatients + " patients");
            statusLabel.setText("All patients displayed");
        } else {
            totalPatientsLabel.setText("Showing: " + filteredCount + " of " + totalPatients + " patients");
            statusLabel.setText("Search results filtered");
        }
    }

    // Action handlers - just make buttons clickable without implementing full functionality
    private void handleAddPatient() {
        System.out.println("Add Patient button clicked - functionality not implemented yet");
        statusLabel.setText("Add Patient functionality coming soon...");
    }

    private void handleFilter() {
        System.out.println("Filter button clicked - functionality not implemented yet");
        statusLabel.setText("Filter functionality coming soon...");
    }

    private void viewPatientProfile(PatientTableData patientData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/ProfileView.fxml"));
            Parent profileRoot = loader.load();

            ProfileViewController profileController = loader.getController();
            profileController.setUserRole(ProfileViewController.UserRole.DOCTOR_VIEWING_PATIENT, selectedPatient.getPatient());


            Scene scene = addPatientBtn.getScene();
            BorderPane rootPane = (BorderPane) scene.getRoot();
            StackPane contentPane = (StackPane) rootPane.getCenter();

            profileController.setParentContentPane(contentPane);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(profileRoot);

            statusLabel.setText("Opened profile for " + patientData.getFullName());
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Errore nel caricamento del profilo: " + e.getMessage());
        }
    }

    private void editPatient(PatientTableData patientData) {
        System.out.println("Edit patient: " + patientData.getFullName());
        statusLabel.setText("Edit functionality for " + patientData.getFullName() + " coming soon...");
    }



    private void deletePatient(PatientTableData patientData) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete " + patientData.getFullName() + "?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Confirm Deletion");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    boolean success = patientDAO.deletePatient(patientData.getPatient().getId());
                    if (success) {
                        refreshPatientsList();
                        statusLabel.setText(patientData.getFullName() + " deleted successfully.");
                    } else {
                        statusLabel.setText("Failed to delete " + patientData.getFullName() + ".");
                    }
                } catch (SQLException e) {
                    statusLabel.setText("Error deleting patient: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // Public methods for external use
    public void refreshPatientsList() {
        loadPatientsData();
        updateStatusBar();
        statusLabel.setText("Patients list refreshed");
    }

    public void setSelectedPatient(PatientTableData patientData) {
        patientsTable.getSelectionModel().select(patientData);
        selectedPatient = patientData;
    }

    public void clearSelection() {
        patientsTable.getSelectionModel().clearSelection();
        selectedPatient = null;
    }

    public PatientTableData getSelectedPatient() {
        return selectedPatient;
    }

    public ObservableList<PatientTableData> getPatientsList() {
        return patientTableData;
    }

    // Wrapper class for table display
    public static class PatientTableData {
        private final Patient patient;
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty lastGlucoseReading;
        private final SimpleStringProperty riskStatus;
        private final SimpleStringProperty lastReadingFormatted;
        private final SimpleIntegerProperty age;

        public PatientTableData(Patient patient) {
            this.patient = patient;
            this.fullName = new SimpleStringProperty(patient.getFullName());

            String glucoseDisplay = "No readings";
            String riskLevel = "Unknown";
            LocalDateTime lastReadingDateTime = null;
            if (patient.getGlucoseReadings() != null && !patient.getGlucoseReadings().isEmpty()) {
                GlucoseMeasurement lastReading = patient.getGlucoseReadings().get(patient.getGlucoseReadings().size() - 1);
                if (lastReading != null) {
                    glucoseDisplay = lastReading.getGlucoseLevel() + " mg/dL";
                    riskLevel = calculateRiskStatus((int) lastReading.getGlucoseLevel());
                    lastReadingDateTime = lastReading.getDateAndTime();
                }
            }

            this.lastGlucoseReading = new SimpleStringProperty(glucoseDisplay);
            this.riskStatus = new SimpleStringProperty(riskLevel);

            int calculatedAge = Period.between(patient.getBornDate(), LocalDate.now()).getYears();
            this.age = new SimpleIntegerProperty(calculatedAge);

            String lastReading = "Never";
            if (lastReadingDateTime != null) {
                lastReading = formatlastReading(lastReadingDateTime);
            }
            this.lastReadingFormatted = new SimpleStringProperty(lastReading);
        }

        private String calculateRiskStatus(int glucoseLevel) {
            if (glucoseLevel < 70 || glucoseLevel > 180) {
                return "High";
            } else if (glucoseLevel > 140 && glucoseLevel < 180) {
                return "Moderate";
            } else {
                return "Normal";
            }
        }

        private String formatlastReading(LocalDateTime lastReading) {
            if (lastReading == null) return "Never";

            long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(lastReading.toLocalDate(), LocalDate.now());
            if (daysAgo == 0) return "Today";
            if (daysAgo == 1) return "Yesterday";
            if (daysAgo < 7) return daysAgo + " days ago";
            if (daysAgo < 14) return "1 week ago";
            if (daysAgo < 30) return (daysAgo / 7) + " weeks ago";
            return (daysAgo / 30) + " months ago";
        }

        public String getFullName() { return fullName.get(); }
        public String getLastGlucoseReading() { return lastGlucoseReading.get(); }
        public String getRiskStatus() { return riskStatus.get(); }
        public String getlastReadingFormatted() { return lastReadingFormatted.get(); }
        public int getAge() { return age.get(); }
        public Patient getPatient() { return patient; }

        public SimpleStringProperty fullNameProperty() { return fullName; }
        public SimpleStringProperty lastGlucoseReadingProperty() { return lastGlucoseReading; }
        public SimpleStringProperty riskStatusProperty() { return riskStatus; }
        public SimpleStringProperty lastReadingFormattedProperty() { return lastReadingFormatted; }
        public SimpleIntegerProperty ageProperty() { return age; }
    }
}