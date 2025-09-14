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
import it.glucotrack.util.LogMedicationDAO;
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
        medicationLogService = new DatabaseMedicationLogService(); // Usa il servizio database
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

        intakeLogTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white; " +
                               "-fx-selection-bar: #3498db; -fx-selection-bar-non-focused: #5dade2;");
        
        // Configura la selezione della tabella
        intakeLogTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // Aggiungi context menu con tasto destro
        setupContextMenu();
    }
    
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem markTakenItem = new MenuItem("✓ Segna come Presa");
        markTakenItem.setOnAction(e -> {
            MedicationLog selectedLog = intakeLogTable.getSelectionModel().getSelectedItem();
            if (selectedLog != null) {
                updateMedicationLogStatus(selectedLog, true);
            }
        });
        
        MenuItem markMissedItem = new MenuItem("✗ Segna come Non Presa");
        markMissedItem.setOnAction(e -> {
            MedicationLog selectedLog = intakeLogTable.getSelectionModel().getSelectedItem();
            if (selectedLog != null) {
                updateMedicationLogStatus(selectedLog, false);
            }
        });
        
        contextMenu.getItems().addAll(markTakenItem, markMissedItem);
        
        // Aggiungi il context menu alla tabella
        intakeLogTable.setContextMenu(contextMenu);
        
        // Configura il context menu e l'evidenziazione delle righe
        intakeLogTable.setRowFactory(tv -> {
            TableRow<MedicationLog> row = new TableRow<MedicationLog>() {
                @Override
                protected void updateItem(MedicationLog item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // Mantieni lo stile normale di default (trasparente)
                        if (!isSelected()) {
                            setStyle("");
                        }
                    }
                }
                
                @Override
                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    
                    if (getItem() != null) {
                        if (selected) {
                            // Stile evidenziato per la riga selezionata
                            setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                                   "-fx-border-color: #2980b9; -fx-border-width: 2px; " +
                                   "-fx-effect: dropshadow(gaussian, #2980b9, 5, 0, 0, 0);");
                        } else {
                            // Ritorna allo stile normale (trasparente, usa lo stile della tabella)
                            setStyle("");
                        }
                    }
                }
            };
            
            // Aggiungi hover effect leggero per migliorare l'usabilità
            row.setOnMouseEntered(e -> {
                if (row.getItem() != null && !row.isSelected()) {
                    row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");
                }
            });
            
            row.setOnMouseExited(e -> {
                if (row.getItem() != null && !row.isSelected()) {
                    row.setStyle("");
                }
            });
            
            // Context menu con selezione automatica della riga
            row.setOnContextMenuRequested(e -> {
                if (row.getItem() != null) {
                    // Seleziona automaticamente la riga quando si clicca tasto destro
                    intakeLogTable.getSelectionModel().select(row.getIndex());
                    
                    // Aggiorna il testo degli item in base allo stato attuale
                    MedicationLog log = row.getItem();
                    if ("Taken".equals(log.getStatus())) {
                        markTakenItem.setText("✓ Già Presa");
                        markTakenItem.setDisable(true);
                        markMissedItem.setText("✗ Segna come Non Presa");
                        markMissedItem.setDisable(false);
                    } else {
                        markTakenItem.setText("✓ Segna come Presa");
                        markTakenItem.setDisable(false);
                        markMissedItem.setText("✗ Già Non Presa");
                        markMissedItem.setDisable(true);
                    }
                    contextMenu.show(row, e.getScreenX(), e.getScreenY());
                }
                e.consume();
            });
            
            return row;
        });
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
        boolean updateMedicationLogStatus(Long logId, boolean taken);
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

    // Implementazione database per MedicationLogService
    private static class DatabaseMedicationLogService implements MedicationLogService {
        private LogMedicationDAO logMedicationDAO;
        private MedicationDAO medicationDAO;
        
        public DatabaseMedicationLogService() {
            this.logMedicationDAO = new LogMedicationDAO();
            this.medicationDAO = new MedicationDAO();
        }
        
        @Override
        public java.util.List<MedicationLog> getMedicationLogs() {
            java.util.List<MedicationLog> medicationLogs = new java.util.ArrayList<>();
            
            try {
                // Ottengo l'ID del paziente corrente (assumendo che sia nel SessionManager)
                int currentPatientId = SessionManager.getInstance().getCurrentUserId();
                
                // Prima ottengo tutti i farmaci del paziente (usando il tipo completo del modello)
                java.util.List<it.glucotrack.model.Medication> medications = medicationDAO.getMedicationsByPatientId(currentPatientId);
                
                // Per ogni farmaco, ottengo i log corrispondenti solo fino ad oggi
                for (it.glucotrack.model.Medication medication : medications) {
                    java.util.List<it.glucotrack.model.LogMedication> logMedications = 
                        logMedicationDAO.getLogMedicationsByMedicationIdUpToNow(medication.getId());
                    
                    // Converto ogni LogMedication in MedicationLog per la UI
                    for (it.glucotrack.model.LogMedication logMedication : logMedications) {
                        MedicationLog uiLog = new MedicationLog(
                            (long) logMedication.getId(),
                            (long) logMedication.getMedication_id(),
                            medication.getName_medication(),
                            medication.getDose(),
                            logMedication.getDateAndTime(),
                            logMedication.isTaken() ? "Taken" : "Missed"
                        );
                        medicationLogs.add(uiLog);
                    }
                }
                
                // Ordino per data/ora decrescente (più recenti prima)
                medicationLogs.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
                
            } catch (Exception e) {
                System.err.println("Errore nel caricamento dei log dei farmaci: " + e.getMessage());
                e.printStackTrace();
            }
            
            return medicationLogs;
        }
        
        @Override
        public void saveMedicationLog(MedicationLog log) {
            try {
                // Converto MedicationLog della UI in LogMedication del modello
                it.glucotrack.model.LogMedication modelLog = new it.glucotrack.model.LogMedication(
                    log.getId().intValue(),
                    log.getMedicationId().intValue(),
                    log.getDateTime(),
                    "Taken".equals(log.getStatus())
                );
                
                if (log.getId() == null || log.getId() <= 0) {
                    // Nuovo log - inserisco
                    logMedicationDAO.insertLogMedication(modelLog);
                } else {
                    // Log esistente - aggiorno
                    logMedicationDAO.updateLogMedication(modelLog);
                }
                
                System.out.println("Log farmaco salvato: " + log.getDrugName() + " - " + log.getStatus());
                
            } catch (Exception e) {
                System.err.println("Errore nel salvamento del log farmaco: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        @Override
        public boolean updateMedicationLogStatus(Long logId, boolean taken) {
            try {
                return logMedicationDAO.updateLogMedicationStatus(logId.intValue(), taken);
            } catch (SQLException e) {
                System.err.println("Errore nell'aggiornamento dello stato del log farmaco: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
    
    // Metodo di utilità per aggiornare lo stato di un log
    private void updateMedicationLogStatus(MedicationLog log, boolean taken) {
        try {
            boolean success = medicationLogService.updateMedicationLogStatus(log.getId(), taken);
            
            if (success) {
                // Aggiorna lo stato locale
                log.setStatus(taken ? "Taken" : "Missed");
                
                // Refresh della tabella per mostrare il cambiamento
                intakeLogTable.refresh();
                
                // Messaggio di conferma
                String statusText = taken ? "presa" : "non presa";
                System.out.println("✅ Medicina " + log.getDrugName() + " segnata come " + statusText);
                
                // Opzionale: mostra un alert di conferma
                showStatusUpdateAlert(log.getDrugName(), taken);
                
            } else {
                showErrorAlert("Errore", "Impossibile aggiornare lo stato della medicina");
            }
            
        } catch (Exception e) {
            System.err.println("Errore nell'aggiornamento dello stato: " + e.getMessage());
            showErrorAlert("Errore", "Si è verificato un errore durante l'aggiornamento: " + e.getMessage());
        }
    }
    
    private void showStatusUpdateAlert(String drugName, boolean taken) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Stato Aggiornato");
        alert.setHeaderText(null);
        
        String statusText = taken ? "presa" : "non presa";
        alert.setContentText("Medicina " + drugName + " segnata come " + statusText);
        
        alert.getDialogPane().setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        alert.showAndWait();
    }
    
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Errore");
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        alert.showAndWait();
    }
}