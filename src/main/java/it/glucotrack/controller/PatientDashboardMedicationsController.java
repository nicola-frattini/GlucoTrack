package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Optional;
import it.glucotrack.model.User;
import it.glucotrack.util.MedicationDAO;
import it.glucotrack.util.SessionManager;

public class PatientDashboardMedicationsController implements Initializable {

    @FXML
    private Button logMedicationBtn;

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

    @FXML
    private TableView<MedicationLog> intakeLogTable;

    @FXML
    private TableColumn<MedicationLog, String> dateTimeColumn;

    @FXML
    private TableColumn<MedicationLog, String> medicationColumn;

    @FXML
    private TableColumn<MedicationLog, String> statusColumn;

    private ObservableList<Medication> prescribedMedications;
    private ObservableList<MedicationLog> medicationLogs;

    // Simulazione servizi database - da sostituire con dependency injection
    private MedicationService medicationService;
    private MedicationLogService medicationLogService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializza servizi fittizi
        initializeServices();

        // Setup tabelle
        setupPrescribedMedicationsTable();
        setupIntakeLogTable();

        // Setup eventi
        setupEventHandlers();

        // Carica dati
        loadData();
    }

    private void initializeServices() {
        // Initialize DAO services
        medicationService = new DatabaseMedicationService();
        medicationLogService = new MockMedicationLogService(); // TODO: implement real service
    }

    private void setupPrescribedMedicationsTable() {
        drugNameColumn.setCellValueFactory(new PropertyValueFactory<>("drugName"));
        dosageColumn.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        frequencyColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        instructionsColumn.setCellValueFactory(new PropertyValueFactory<>("instructions"));

        prescribedMedicationsTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white;");
    }

    private void setupIntakeLogTable() {
        dateTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedDateTime()));

        medicationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedicationInfo()));

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom cell factory per colorare lo status
        statusColumn.setCellFactory(new Callback<TableColumn<MedicationLog, String>, TableCell<MedicationLog, String>>() {
            @Override
            public TableCell<MedicationLog, String> call(TableColumn<MedicationLog, String> param) {
                return new TableCell<MedicationLog, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText("● " + item);
                            switch (item.toLowerCase()) {
                                case "taken":
                                    setStyle("-fx-text-fill: #2ECC71;");
                                    break;
                                case "missed":
                                    setStyle("-fx-text-fill: #E74C3C;");
                                    break;
                                default:
                                    setStyle("-fx-text-fill: white;");
                                    break;
                            }
                        }
                    }
                };
            }
        });

        intakeLogTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white;");
    }

    private void setupEventHandlers() {
        logMedicationBtn.setOnAction(e -> handleLogMedicationIntake());
    }

    private void loadData() {
        // Carica farmaci prescritti
        prescribedMedications = FXCollections.observableArrayList(
                medicationService.getPrescribedMedications()
        );
        prescribedMedicationsTable.setItems(prescribedMedications);

        // Carica log delle assunzioni
        medicationLogs = FXCollections.observableArrayList(
                medicationLogService.getMedicationLogs()
        );
        intakeLogTable.setItems(medicationLogs);
    }

    private void handleLogMedicationIntake() {
        // Crea dialog per logging
        Optional<MedicationLog> result = showLogMedicationDialog();
        result.ifPresent(log -> {
            // Salva nel "database"
            medicationLogService.saveMedicationLog(log);

            // Aggiorna UI
            medicationLogs.add(0, log);
            intakeLogTable.refresh();
        });
    }

    private Optional<MedicationLog> showLogMedicationDialog() {
        Dialog<MedicationLog> dialog = new Dialog<>();
        dialog.setTitle("Log Medication Intake");
        dialog.setHeaderText("Record medication intake");

        // Placeholder per dialog - implementazione completa da fare
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Log Medication");
        alert.setHeaderText(null);
        alert.setContentText("Medication logging dialog will be implemented here.\nThis will allow selection of medication and status.");
        alert.showAndWait();

        return Optional.empty(); // Temporary
    }

    // Metodi per future integrazioni
    public void refreshData() {
        loadData();
    }

    public ObservableList<Medication> getPrescribedMedications() {
        return prescribedMedications;
    }

    public ObservableList<MedicationLog> getMedicationLogs() {
        return medicationLogs;
    }

    // ========== CLASSI MODELLO ==========

    public static class Medication {
        private Long id;
        private String drugName;
        private String dosage;
        private String frequency;
        private String instructions;

        public Medication(Long id, String drugName, String dosage, String frequency, String instructions) {
            this.id = id;
            this.drugName = drugName;
            this.dosage = dosage;
            this.frequency = frequency;
            this.instructions = instructions;
        }

        // Getters e Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getDrugName() { return drugName; }
        public void setDrugName(String drugName) { this.drugName = drugName; }

        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public String getInstructions() { return instructions; }
        public void setInstructions(String instructions) { this.instructions = instructions; }
    }

    public static class MedicationLog {
        private Long id;
        private Long medicationId;
        private String drugName;
        private String dosage;
        private LocalDateTime dateTime;
        private String status; // "Taken", "Missed"

        public MedicationLog(Long id, Long medicationId, String drugName, String dosage,
                             LocalDateTime dateTime, String status) {
            this.id = id;
            this.medicationId = medicationId;
            this.drugName = drugName;
            this.dosage = dosage;
            this.dateTime = dateTime;
            this.status = status;
        }

        // Getters e Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getMedicationId() { return medicationId; }
        public void setMedicationId(Long medicationId) { this.medicationId = medicationId; }

        public String getDrugName() { return drugName; }
        public void setDrugName(String drugName) { this.drugName = drugName; }

        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }

        public LocalDateTime getDateTime() { return dateTime; }
        public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        // Metodi per display
        public String getFormattedDateTime() {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a"));
        }

        public String getMedicationInfo() {
            return String.format("%s (%s)", drugName, dosage);
        }
    }

        // ========== SERVIZI DATABASE E MOCK ==========

    // Interfacce dei servizi

    public interface MedicationService {
        java.util.List<Medication> getPrescribedMedications();
        void saveMedication(Medication medication);
    }

    public interface MedicationLogService {
        java.util.List<MedicationLog> getMedicationLogs();
        void saveMedicationLog(MedicationLog log);
    }

    // Implementazione database
    private static class DatabaseMedicationService implements MedicationService {
        private MedicationDAO medicationDAO;
        
        public DatabaseMedicationService() {
            this.medicationDAO = new MedicationDAO();
        }
        
        @Override
        public java.util.List<Medication> getPrescribedMedications() {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    java.util.List<it.glucotrack.model.Medication> dbMedications = 
                        medicationDAO.getMedicationsByPatientId(currentUser.getId());
                    
                    // Convert from model Medication to controller Medication
                    java.util.List<Medication> controllerMedications = new java.util.ArrayList<>();
                    for (it.glucotrack.model.Medication dbMed : dbMedications) {
                        Medication controllerMed = new Medication(
                            (long) dbMed.getId(),
                            dbMed.getName_medication(),
                            dbMed.getDose(),
                            dbMed.getFreq().getDisplayName(),
                            dbMed.getInstructions()
                        );
                        controllerMedications.add(controllerMed);
                    }
                    return controllerMedications;
                }
                return new java.util.ArrayList<>();
            } catch (SQLException e) {
                System.err.println("Errore nel recupero medications: " + e.getMessage());
                return new java.util.ArrayList<>();
            }
        }
        
        @Override
        public void saveMedication(Medication medication) {
            try {
                // Convert from controller Medication to model Medication would be needed here
                // For now, this is just a placeholder
                System.out.println("Save medication called: " + medication.getDrugName());
            } catch (Exception e) {
                System.err.println("Errore nel salvataggio medication: " + e.getMessage());
            }
        }
    }

    // Implementazioni mock
    private static class MockMedicationService implements MedicationService {
        @Override
        public java.util.List<Medication> getPrescribedMedications() {
            return java.util.Arrays.asList(
                    new Medication(1L, "Metformin", "500mg", "Twice a day", "After meals"),
                    new Medication(2L, "Gliclazide", "30mg", "Once a day", "With breakfast"),
                    new Medication(3L, "Insulin Glargine", "10 units", "Once a day", "At bedtime")
            );
        }

        @Override
        public void saveMedication(Medication medication) {
            // Mock implementation - in realtà salverebbe nel database
            System.out.println("Saving medication: " + medication.getDrugName());
        }
    }

    private static class MockMedicationLogService implements MedicationLogService {
        @Override
        public java.util.List<MedicationLog> getMedicationLogs() {
            return java.util.Arrays.asList(
                    new MedicationLog(1L, 1L, "Metformin", "500mg",
                            LocalDateTime.of(2023, 10, 27, 8, 30), "Taken"),
                    new MedicationLog(2L, 2L, "Gliclazide", "30mg",
                            LocalDateTime.of(2023, 10, 27, 9, 0), "Taken"),
                    new MedicationLog(3L, 1L, "Metformin", "500mg",
                            LocalDateTime.of(2023, 10, 26, 20, 0), "Missed"),
                    new MedicationLog(4L, 3L, "Insulin Glargine", "10 units",
                            LocalDateTime.of(2023, 10, 26, 22, 0), "Taken")
            );
        }

        @Override
        public void saveMedicationLog(MedicationLog log) {
            // Mock implementation - in realtà salverebbe nel database
            System.out.println("Saving medication log: " + log.getDrugName() + " - " + log.getStatus());
        }
    }
}