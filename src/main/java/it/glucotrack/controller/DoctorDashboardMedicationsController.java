package it.glucotrack.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import it.glucotrack.model.Medication;
import it.glucotrack.model.Patient;
import it.glucotrack.model.Frequency;
import it.glucotrack.util.MedicationDAO;
import it.glucotrack.util.PatientDAO;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class DoctorDashboardMedicationsController {

    @FXML private ComboBox<String> patientFilterComboBox;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Button addMedicationButton;
    @FXML private Button refreshButton;

    // Statistics labels
    @FXML private Label totalMedicationsLabel;
    @FXML private Label activeMedicationsLabel;
    @FXML private Label expiredMedicationsLabel;
    @FXML private Label totalPatientsLabel;

    // Table and columns
    @FXML private TableView<MedicationDisplayData> medicationsTable;
    @FXML private TableColumn<MedicationDisplayData, String> patientNameColumn;
    @FXML private TableColumn<MedicationDisplayData, String> medicationNameColumn;
    @FXML private TableColumn<MedicationDisplayData, String> dosageColumn;
    @FXML private TableColumn<MedicationDisplayData, String> frequencyColumn;
    @FXML private TableColumn<MedicationDisplayData, String> startDateColumn;
    @FXML private TableColumn<MedicationDisplayData, String> endDateColumn;
    @FXML private TableColumn<MedicationDisplayData, String> statusColumn;
    @FXML private TableColumn<MedicationDisplayData, String> instructionsColumn;
    @FXML private TableColumn<MedicationDisplayData, Void> actionsColumn;

    private MedicationDAO medicationDAO;
    private PatientDAO patientDAO;
    private ObservableList<MedicationDisplayData> allMedications;
    private ObservableList<MedicationDisplayData> filteredMedications;

    @FXML
    public void initialize() {
        medicationDAO = new MedicationDAO();
        patientDAO = new PatientDAO();
        allMedications = FXCollections.observableArrayList();
        filteredMedications = FXCollections.observableArrayList();

        setupTable();
        setupFilters();
        loadData();
    }

    private void setupTable() {
        // Configure table columns
        patientNameColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        medicationNameColumn.setCellValueFactory(new PropertyValueFactory<>("medicationName"));
        dosageColumn.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        frequencyColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        instructionsColumn.setCellValueFactory(new PropertyValueFactory<>("instructions"));

        // Setup actions column with edit/delete buttons
        actionsColumn.setCellFactory(param -> new TableCell<MedicationDisplayData, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttonsBox = new HBox(5);

            {
                editButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 8;");
                deleteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 8;");
                
                editButton.setOnAction(event -> {
                    MedicationDisplayData data = getTableView().getItems().get(getIndex());
                    handleEditMedication(data);
                });
                
                deleteButton.setOnAction(event -> {
                    MedicationDisplayData data = getTableView().getItems().get(getIndex());
                    handleDeleteMedication(data);
                });
                
                buttonsBox.getChildren().addAll(editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsBox);
                }
            }
        });

        medicationsTable.setItems(filteredMedications);
    }

    private void setupFilters() {
        // Setup status filter
        statusFilterComboBox.getItems().addAll("All", "Active", "Expired", "Ending Soon");
        statusFilterComboBox.setValue("All");

        // Setup patient filter
        patientFilterComboBox.setValue("All Patients");
        loadPatientFilter();
    }

    private void loadPatientFilter() {
        try {
            List<Patient> patients = patientDAO.getAllPatients();
            ObservableList<String> patientNames = FXCollections.observableArrayList();
            patientNames.add("All Patients");
            patientNames.addAll(patients.stream()
                    .map(p -> p.getName() + " " + p.getSurname())
                    .collect(Collectors.toList()));
            patientFilterComboBox.setItems(patientNames);
        } catch (SQLException e) {
            System.err.println("Error loading patients for filter: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            List<Medication> medications = medicationDAO.getAllMedications();
            allMedications.clear();
            
            for (Medication med : medications) {
                try {
                    Patient patient = patientDAO.getPatientById(med.getPatient_id());
                    String patientName = patient != null ? patient.getName() + " " + patient.getSurname() : "Unknown Patient";
                    
                    MedicationDisplayData displayData = new MedicationDisplayData(
                            med.getId(),
                            med.getPatient_id(),
                            patientName,
                            med.getName_medication(),
                            med.getDose(),
                            med.getFreq().getDisplayName(),
                            med.getStart_date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            med.getEnd_date() != null ? med.getEnd_date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A",
                            getMedicationStatus(med),
                            med.getInstructions() != null ? med.getInstructions() : ""
                    );
                    
                    allMedications.add(displayData);
                } catch (SQLException e) {
                    System.err.println("Error loading patient data for medication " + med.getId() + ": " + e.getMessage());
                }
            }
            
            applyFilters();
            updateStatistics();
            
        } catch (SQLException e) {
            System.err.println("Error loading medications: " + e.getMessage());
            showError("Error loading medications", e.getMessage());
        }
    }

    private String getMedicationStatus(Medication med) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = med.getEnd_date();
        
        if (endDate != null && today.isAfter(endDate)) {
            return "Expired";
        } else if (endDate != null && today.plusDays(7).isAfter(endDate)) {
            return "Ending Soon";
        } else {
            return "Active";
        }
    }

    private void updateStatistics() {
        int total = allMedications.size();
        long active = allMedications.stream().filter(m -> "Active".equals(m.getStatus())).count();
        long expired = allMedications.stream().filter(m -> "Expired".equals(m.getStatus())).count();
        long uniquePatients = allMedications.stream().mapToInt(MedicationDisplayData::getPatientId).distinct().count();

        totalMedicationsLabel.setText(String.valueOf(total));
        activeMedicationsLabel.setText(String.valueOf(active));
        expiredMedicationsLabel.setText(String.valueOf(expired));
        totalPatientsLabel.setText(String.valueOf(uniquePatients));
    }

    @FXML
    private void handleAddMedication() {
        navigateToInsertView();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handlePatientFilter() {
        applyFilters();
    }

    @FXML
    private void handleStatusFilter() {
        applyFilters();
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    private void applyFilters() {
        filteredMedications.clear();
        
        String selectedPatient = patientFilterComboBox.getValue();
        String selectedStatus = statusFilterComboBox.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        filteredMedications.addAll(allMedications.stream()
                .filter(med -> selectedPatient.equals("All Patients") || med.getPatientName().equals(selectedPatient))
                .filter(med -> selectedStatus.equals("All") || med.getStatus().equals(selectedStatus))
                .filter(med -> searchText.isEmpty() || 
                        med.getMedicationName().toLowerCase().contains(searchText) ||
                        med.getPatientName().toLowerCase().contains(searchText))
                .collect(Collectors.toList()));
    }

    private void handleEditMedication(MedicationDisplayData data) {
        // TODO: Navigate to edit medication view
        System.out.println("Edit medication: " + data.getMedicationName() + " for patient: " + data.getPatientName());
    }

    private void handleDeleteMedication(MedicationDisplayData data) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Medication");
        confirmAlert.setHeaderText("Are you sure you want to delete this medication?");
        confirmAlert.setContentText("Patient: " + data.getPatientName() + "\nMedication: " + data.getMedicationName());

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                medicationDAO.deleteMedication(data.getId());
                loadData(); // Refresh the table
                showSuccess("Medication deleted successfully!");
            } catch (SQLException e) {
                System.err.println("Error deleting medication: " + e.getMessage());
                showError("Error deleting medication", e.getMessage());
            }
        }
    }

    private void navigateToInsertView() {
        try {
            StackPane contentPane = findContentPane();
            if (contentPane != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/DoctorDashboardMedicationsInsert.fxml"));
                Node insertView = loader.load();
                contentPane.getChildren().clear();
                contentPane.getChildren().add(insertView);
            }
        } catch (IOException e) {
            System.err.println("Error navigating to insert view: " + e.getMessage());
            showError("Navigation Error", "Could not load the medication insert form.");
        }
    }

    private StackPane findContentPane() {
        Node current = medicationsTable.getScene().getRoot();
        return findStackPaneRecursively(current);
    }

    private StackPane findStackPaneRecursively(Node node) {
        if (node instanceof StackPane && 
            ((StackPane) node).getId() != null && 
            ((StackPane) node).getId().equals("contentPane")) {
            return (StackPane) node;
        }
        
        if (node instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                StackPane result = findStackPaneRecursively(child);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for table display data
    public static class MedicationDisplayData {
        private final int id;
        private final int patientId;
        private final SimpleStringProperty patientName;
        private final SimpleStringProperty medicationName;
        private final SimpleStringProperty dosage;
        private final SimpleStringProperty frequency;
        private final SimpleStringProperty startDate;
        private final SimpleStringProperty endDate;
        private final SimpleStringProperty status;
        private final SimpleStringProperty instructions;

        public MedicationDisplayData(int id, int patientId, String patientName, String medicationName, 
                                   String dosage, String frequency, String startDate, String endDate, 
                                   String status, String instructions) {
            this.id = id;
            this.patientId = patientId;
            this.patientName = new SimpleStringProperty(patientName);
            this.medicationName = new SimpleStringProperty(medicationName);
            this.dosage = new SimpleStringProperty(dosage);
            this.frequency = new SimpleStringProperty(frequency);
            this.startDate = new SimpleStringProperty(startDate);
            this.endDate = new SimpleStringProperty(endDate);
            this.status = new SimpleStringProperty(status);
            this.instructions = new SimpleStringProperty(instructions);
        }

        // Getters
        public int getId() { return id; }
        public int getPatientId() { return patientId; }
        public String getPatientName() { return patientName.get(); }
        public String getMedicationName() { return medicationName.get(); }
        public String getDosage() { return dosage.get(); }
        public String getFrequency() { return frequency.get(); }
        public String getStartDate() { return startDate.get(); }
        public String getEndDate() { return endDate.get(); }
        public String getStatus() { return status.get(); }
        public String getInstructions() { return instructions.get(); }

        // Property getters for TableView
        public SimpleStringProperty patientNameProperty() { return patientName; }
        public SimpleStringProperty medicationNameProperty() { return medicationName; }
        public SimpleStringProperty dosageProperty() { return dosage; }
        public SimpleStringProperty frequencyProperty() { return frequency; }
        public SimpleStringProperty startDateProperty() { return startDate; }
        public SimpleStringProperty endDateProperty() { return endDate; }
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleStringProperty instructionsProperty() { return instructions; }
    }
}