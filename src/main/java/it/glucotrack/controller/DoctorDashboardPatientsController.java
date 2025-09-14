package it.glucotrack.controller;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import it.glucotrack.model.Gender;
import it.glucotrack.model.GlucoseMeasurement;
import it.glucotrack.model.Patient;
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
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

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
    @FXML private TableColumn<PatientTableData, String> lastVisitColumn;
    @FXML private TableColumn<PatientTableData, Integer> ageColumn;
    @FXML private TableColumn<PatientTableData, Void> actionsColumn;

    // Context menu
    @FXML private ContextMenu tableContextMenu;
    @FXML private MenuItem viewPatientMenuItem;
    @FXML private MenuItem editPatientMenuItem;
    @FXML private MenuItem scheduleAppointmentMenuItem;
    @FXML private MenuItem sendMessageMenuItem;
    @FXML private MenuItem deletePatientMenuItem;

    // Status elements
    @FXML private Label statusLabel;
    @FXML private Label totalPatientsLabel;

    // Data
    private ObservableList<PatientTableData> patientTableData;
    private FilteredList<PatientTableData> filteredPatients;
    private PatientTableData selectedPatient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupSearch();
        setupButtons();
        setupContextMenu();
        loadPatientsData();
        updateStatusBar();
    }

    private void setupTable() {
        // Initialize data structures
        patientTableData = FXCollections.observableArrayList();
        filteredPatients = new FilteredList<>(patientTableData);
        patientsTable.setItems(filteredPatients);

        // Setup columns
        patientNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        lastGlucoseColumn.setCellValueFactory(new PropertyValueFactory<>("lastGlucoseReading"));
        riskStatusColumn.setCellValueFactory(new PropertyValueFactory<>("riskStatus"));
        lastVisitColumn.setCellValueFactory(new PropertyValueFactory<>("lastVisitFormatted"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));

        // Setup custom cell factories for styling
        setupRiskStatusColumn();
        setupActionsColumn();

        // Table styling
        patientsTable.setRowFactory(tv -> {
            TableRow<PatientTableData> row = new TableRow<PatientTableData>() {
                @Override
                protected void updateItem(PatientTableData patientData, boolean empty) {
                    super.updateItem(patientData, empty);
                    if (empty || patientData == null) {
                        setStyle("");
                        getStyleClass().removeAll("table-row-high-risk", "table-row-moderate-risk", "table-row-normal");
                    } else {
                        // Apply CSS classes based on risk status
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

            // Double-click to view patient
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    viewPatientProfile(getSelectedPatient());
                }
            });

            return row;
        });

        // Selection listener
        patientsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> selectedPatient = newSelection);

        // Apply custom CSS styling
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

                    // Apply CSS classes based on risk level
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

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> new TableCell<PatientTableData, Void>() {
            private final Button viewButton = new Button("View");

            {
                viewButton.getStyleClass().addAll("btn", "btn-primary", "btn-small");
                viewButton.setPrefWidth(60);
                viewButton.setOnAction(event -> {
                    PatientTableData patientData = getTableView().getItems().get(getIndex());
                    viewPatientProfile(patientData);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox actionBox = new HBox(viewButton);
                    actionBox.setAlignment(Pos.CENTER);
                    setGraphic(actionBox);
                }
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterPatients(newValue);
        });

        // Apply search field styling
        searchField.getStyleClass().add("search-field");
    }

    private void setupButtons() {
        addPatientBtn.setOnAction(e -> handleAddPatient());
        filterBtn.setOnAction(e -> handleFilter());

        // Apply button styling
        addPatientBtn.getStyleClass().addAll("btn", "btn-primary");
        filterBtn.getStyleClass().addAll("btn", "btn-secondary");
    }

    private void setupContextMenu() {
        viewPatientMenuItem.setOnAction(e -> {
            if (selectedPatient != null) {

            }
        });

        editPatientMenuItem.setOnAction(e -> {
            if (selectedPatient != null) {
                editPatient(selectedPatient);
            }
        });

        scheduleAppointmentMenuItem.setOnAction(e -> {
            if (selectedPatient != null) {
                scheduleAppointment(selectedPatient);
            }
        });

        sendMessageMenuItem.setOnAction(e -> {
            if (selectedPatient != null) {
                sendMessage(selectedPatient);
            }
        });

        deletePatientMenuItem.setOnAction(e -> {
            if (selectedPatient != null) {
                deletePatient(selectedPatient);
            }
        });

        patientsTable.setContextMenu(tableContextMenu);

        // Apply context menu styling
        tableContextMenu.getStyleClass().add("context-menu");
    }

    private void loadPatientsData() {
        // Create sample patients using the Patient model
        List<Patient> samplePatients = createSamplePatients();

        // Convert to table data and add to observable list
        for (Patient patient : samplePatients) {
            PatientTableData tableData = new PatientTableData(patient);
            patientTableData.add(tableData);
        }

        statusLabel.setText("Patients loaded successfully");
    }

    private List<Patient> createSamplePatients() {
        return Arrays.asList(
                createPatientWithData(1, "Sophia", "Clark", "sophia.clark@email.com",
                        LocalDate.of(1990, 3, 15), "+1234567890", 1, 120, "Normal"),
                createPatientWithData(2, "Ethan", "Harris", "ethan.harris@email.com",
                        LocalDate.of(1982, 7, 22), "+1234567891", 1, 250, "High"),
                createPatientWithData(3, "Olivia", "Turner", "olivia.turner@email.com",
                        LocalDate.of(1996, 11, 8), "+1234567892", 1, 180, "Moderate"),
                createPatientWithData(4, "Liam", "Foster", "liam.foster@email.com",
                        LocalDate.of(1969, 4, 12), "+1234567893", 1, 110, "Normal"),
                createPatientWithData(5, "Ava", "Bennett", "ava.bennett@email.com",
                        LocalDate.of(1986, 9, 25), "+1234567894", 1, 300, "High"),
                createPatientWithData(6, "Noah", "Peterson", "noah.peterson@email.com",
                        LocalDate.of(1993, 1, 18), "+1234567895", 1, 95, "Normal"),
                createPatientWithData(7, "Isabella", "Rodriguez", "isabella.rodriguez@email.com",
                        LocalDate.of(1979, 6, 30), "+1234567896", 1, 165, "Moderate"),
                createPatientWithData(8, "Mason", "Taylor", "mason.taylor@email.com",
                        LocalDate.of(1972, 12, 5), "+1234567897", 1, 275, "High"),
                createPatientWithData(9, "Emma", "Wilson", "emma.wilson@email.com",
                        LocalDate.of(1995, 8, 14), "+1234567898", 1, 135, "Normal"),
                createPatientWithData(10, "William", "Brown", "william.brown@email.com",
                        LocalDate.of(1977, 2, 28), "+1234567899", 1, 195, "Moderate"),
                createPatientWithData(11, "Charlotte", "Davis", "charlotte.davis@email.com",
                        LocalDate.of(1988, 5, 10), "+1234567800", 1, 160, "Moderate"),
                createPatientWithData(12, "James", "Miller", "james.miller@email.com",
                        LocalDate.of(1983, 10, 3), "+1234567801", 1, 85, "Normal"),
                createPatientWithData(13, "Amelia", "Garcia", "amelia.garcia@email.com",
                        LocalDate.of(1991, 12, 20), "+1234567802", 1, 320, "High"),
                createPatientWithData(14, "Benjamin", "Wilson", "benjamin.wilson@email.com",
                        LocalDate.of(1995, 4, 7), "+1234567803", 1, 145, "Normal"),
                createPatientWithData(15, "Mia", "Martinez", "mia.martinez@email.com",
                        LocalDate.of(1980, 8, 16), "+1234567804", 1, 210, "Moderate")
        );
    }

    private Patient createPatientWithData(int id, String name, String surname, String email,
                                          LocalDate birthDate, String phone, int doctorId,
                                          int glucoseReading, String riskStatus) {
        Patient patient = new Patient(id, name, surname, email, "defaultPassword",
                birthDate, Gender.MALE, phone, "Unknown", "FC" + id);
        patient.setDoctorId(doctorId);

        // Add sample glucose reading
        GlucoseMeasurement measurement = new GlucoseMeasurement();
        measurement.setGlucoseLevel(glucoseReading);
        measurement.setDateAndTime(LocalDateTime.now().minusDays((int)(Math.random() * 14)));
        patient.getGlucoseReadings().add(measurement);

        // Add sample symptoms based on risk status
        switch (riskStatus) {
            case "High":
                patient.getSymptoms().addAll(Arrays.asList("Frequent urination", "Excessive thirst", "Fatigue"));
                break;
            case "Moderate":
                patient.getSymptoms().addAll(Arrays.asList("Mild fatigue", "Occasional dizziness"));
                break;
            case "Normal":
                patient.getSymptoms().add("No significant symptoms");
                break;
        }

        return patient;
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

            // Passa i dati al controller del profilo
            Object controller = loader.getController();
            if (controller != null && controller.getClass().getMethod("setPatient", Patient.class) != null) {
                controller.getClass().getMethod("setPatient", Patient.class).invoke(controller, patientData.getPatient());
            }

            // Sostituisci la root della scena
            Scene scene = addPatientBtn.getScene();
            scene.setRoot(profileRoot);
            statusLabel.setText("Opened profile for " + patientData.getFullName());
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Errore nel caricamento del profilo");
        }
    }

    private void editPatient(PatientTableData patientData) {
        System.out.println("Edit patient: " + patientData.getFullName());
        statusLabel.setText("Edit functionality for " + patientData.getFullName() + " coming soon...");
    }

    private void scheduleAppointment(PatientTableData patientData) {
        System.out.println("Schedule appointment for: " + patientData.getFullName());
        statusLabel.setText("Scheduling appointment for " + patientData.getFullName() + "...");
    }

    private void sendMessage(PatientTableData patientData) {
        System.out.println("Send message to: " + patientData.getFullName());
        statusLabel.setText("Composing message to " + patientData.getFullName() + "...");
    }

    private void deletePatient(PatientTableData patientData) {
        System.out.println("Delete patient: " + patientData.getFullName());
        statusLabel.setText("Delete functionality not available yet");
    }

    // Public methods for external use
    public void refreshPatientsList() {
        patientTableData.clear();
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
        private final SimpleStringProperty lastVisitFormatted;
        private final SimpleIntegerProperty age;

        public PatientTableData(Patient patient) {
            this.patient = patient;
            this.fullName = new SimpleStringProperty(patient.getFullName());

            // Get last glucose reading
            String glucoseDisplay = "No readings";
            String riskLevel = "Unknown";
            if (!patient.getGlucoseReadings().isEmpty()) {
                GlucoseMeasurement lastReading = patient.getGlucoseReadings().get(patient.getGlucoseReadings().size() - 1);
                glucoseDisplay = lastReading.getGlucoseLevel() + " mg/dL";
                riskLevel = calculateRiskStatus((int) lastReading.getGlucoseLevel());
            }

            this.lastGlucoseReading = new SimpleStringProperty(glucoseDisplay);
            this.riskStatus = new SimpleStringProperty(riskLevel);

            // Calculate age
            int calculatedAge = Period.between(patient.getBornDate(), LocalDate.now()).getYears();
            this.age = new SimpleIntegerProperty(calculatedAge);

            // Format last visit (using last glucose measurement date as proxy)
            String lastVisit = "Never";
            if (!patient.getGlucoseReadings().isEmpty()) {
                GlucoseMeasurement lastReading = patient.getGlucoseReadings().get(patient.getGlucoseReadings().size() - 1);
                lastVisit = formatLastVisit(lastReading.getDateAndTime());
            }
            this.lastVisitFormatted = new SimpleStringProperty(lastVisit);
        }

        private String calculateRiskStatus(int glucoseLevel) {
            if (glucoseLevel < 80 || glucoseLevel > 200) {
                return "High";
            } else if (glucoseLevel < 90 || glucoseLevel > 160) {
                return "Moderate";
            } else {
                return "Normal";
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

        // Getters for table binding
        public String getFullName() { return fullName.get(); }
        public String getLastGlucoseReading() { return lastGlucoseReading.get(); }
        public String getRiskStatus() { return riskStatus.get(); }
        public String getLastVisitFormatted() { return lastVisitFormatted.get(); }
        public int getAge() { return age.get(); }
        public Patient getPatient() { return patient; }

        // Property getters for TableView
        public SimpleStringProperty fullNameProperty() { return fullName; }
        public SimpleStringProperty lastGlucoseReadingProperty() { return lastGlucoseReading; }
        public SimpleStringProperty riskStatusProperty() { return riskStatus; }
        public SimpleStringProperty lastVisitFormattedProperty() { return lastVisitFormatted; }
        public SimpleIntegerProperty ageProperty() { return age; }
    }
}