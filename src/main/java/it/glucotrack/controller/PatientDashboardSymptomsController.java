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
import it.glucotrack.util.SymptomDAO;
import it.glucotrack.util.SessionManager;

public class PatientDashboardSymptomsController implements Initializable {

    @FXML
    private Button addNewSymptomBtn;

    @FXML
    private TableView<Symptom> symptomsTable;

    @FXML
    private TableColumn<Symptom, String> dateRecordedColumn;

    @FXML
    private TableColumn<Symptom, String> symptomColumn;

    @FXML
    private TableColumn<Symptom, String> severityColumn;

    @FXML
    private TableColumn<Symptom, String> durationColumn;

    private ObservableList<Symptom> symptoms;

    // Simulazione servizio database - da sostituire con dependency injection
    private SymptomService symptomService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializza servizi fittizi
        initializeServices();

        // Setup tabella
        setupSymptomsTable();

        // Setup eventi
        setupEventHandlers();

        // Carica dati
        loadData();
    }

    private void initializeServices() {
        // Initialize DAO service
        symptomService = new DatabaseSymptomService();
    }

    private void setupSymptomsTable() {
        dateRecordedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedDateTime()));

        symptomColumn.setCellValueFactory(new PropertyValueFactory<>("symptomName"));

        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));

        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));

        // Custom cell factory per colorare la severity
        severityColumn.setCellFactory(new Callback<TableColumn<Symptom, String>, TableCell<Symptom, String>>() {
            @Override
            public TableCell<Symptom, String> call(TableColumn<Symptom, String> param) {
                return new TableCell<Symptom, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            switch (item.toLowerCase()) {
                                case "mild":
                                    setStyle("-fx-text-fill: #F39C12;"); // Orange
                                    break;
                                case "moderate":
                                    setStyle("-fx-text-fill: #E67E22;"); // Darker Orange
                                    break;
                                case "severe":
                                    setStyle("-fx-text-fill: #E74C3C;"); // Red
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

        symptomsTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white;");
    }

    private void setupEventHandlers() {
        addNewSymptomBtn.setOnAction(e -> handleAddNewSymptom());
    }

    private void loadData() {
        symptoms = FXCollections.observableArrayList(
                symptomService.getAllSymptoms()
        );
        symptomsTable.setItems(symptoms);
    }

    private void handleAddNewSymptom() {
        Optional<Symptom> result = showAddSymptomDialog();
        result.ifPresent(symptom -> {
            // Salva nel "database"
            symptomService.saveSymptom(symptom);

            // Aggiorna UI
            symptoms.add(0, symptom); // Aggiungi in cima (più recente)
            symptomsTable.refresh();
        });
    }

    private Optional<Symptom> showAddSymptomDialog() {
        // Placeholder per dialog - implementazione completa da fare
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Add New Symptom");
        alert.setHeaderText(null);
        alert.setContentText("Add symptom dialog will be implemented here.\nThis will allow entry of symptom details, severity, and duration.");
        alert.showAndWait();

        return Optional.empty(); // Temporary
    }

    // Metodi per future integrazioni
    public void refreshData() {
        loadData();
    }

    public ObservableList<Symptom> getSymptoms() {
        return symptoms;
    }

    public void addSymptom(Symptom symptom) {
        symptomService.saveSymptom(symptom);
        symptoms.add(0, symptom);
        symptomsTable.refresh();
    }

    // ========== CLASSE MODELLO ==========

    public static class Symptom {
        private Long id;
        private String symptomName;
        private String severity; // "Mild", "Moderate", "Severe"
        private String duration;
        private LocalDateTime dateRecorded;
        private String notes;

        public Symptom(Long id, String symptomName, String severity, String duration,
                       LocalDateTime dateRecorded, String notes) {
            this.id = id;
            this.symptomName = symptomName;
            this.severity = severity;
            this.duration = duration;
            this.dateRecorded = dateRecorded;
            this.notes = notes;
        }

        // Constructor senza ID per nuovi sintomi
        public Symptom(String symptomName, String severity, String duration,
                       LocalDateTime dateRecorded, String notes) {
            this(null, symptomName, severity, duration, dateRecorded, notes);
        }

        // Getters e Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getSymptomName() { return symptomName; }
        public void setSymptomName(String symptomName) { this.symptomName = symptomName; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public LocalDateTime getDateRecorded() { return dateRecorded; }
        public void setDateRecorded(LocalDateTime dateRecorded) { this.dateRecorded = dateRecorded; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        // Metodo per display
        public String getFormattedDateTime() {
            return dateRecorded.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a"));
        }

        @Override
        public String toString() {
            return String.format("Symptom{id=%d, name=%s, severity=%s, duration=%s, date=%s}",
                    id, symptomName, severity, duration, getFormattedDateTime());
        }
    }

    // ========== SERVIZIO INTERFACE ==========

    public interface SymptomService {
        java.util.List<Symptom> getAllSymptoms();
        java.util.List<Symptom> getSymptomsByDateRange(LocalDateTime start, LocalDateTime end);
        java.util.List<Symptom> getSymptomsBySeverity(String severity);
        void saveSymptom(Symptom symptom);
        void deleteSymptom(Long id);
        Optional<Symptom> getSymptomById(Long id);
    }

    // ========== SERVIZI DATABASE E MOCK ==========

    // Implementazione database
    private static class DatabaseSymptomService implements SymptomService {
        private SymptomDAO symptomDAO;
        
        public DatabaseSymptomService() {
            this.symptomDAO = new SymptomDAO();
        }
        
        @Override
        public java.util.List<Symptom> getAllSymptoms() {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    java.util.List<String> dbSymptoms = 
                        symptomDAO.getSymptomsByPatientId(currentUser.getId());
                    
                    // Convert from simple strings to controller Symptom objects
                    java.util.List<Symptom> controllerSymptoms = new java.util.ArrayList<>();
                    int id = 1;
                    for (String symptomName : dbSymptoms) {
                        Symptom controllerSymptom = new Symptom(
                            (long) id++,
                            symptomName,
                            "N/A", // severity not available from current DAO
                            "N/A", // duration not available from current DAO
                            LocalDateTime.now(), // date not available from current DAO
                            "Imported from database"
                        );
                        controllerSymptoms.add(controllerSymptom);
                    }
                    return controllerSymptoms;
                }
                return new java.util.ArrayList<>();
            } catch (SQLException e) {
                System.err.println("Errore nel recupero symptoms: " + e.getMessage());
                return new java.util.ArrayList<>();
            }
        }
        
        @Override
        public java.util.List<Symptom> getSymptomsByDateRange(LocalDateTime start, LocalDateTime end) {
            // Implementation for date range filtering would go here
            return getAllSymptoms(); // For now, return all
        }
        
        @Override
        public java.util.List<Symptom> getSymptomsBySeverity(String severity) {
            // Implementation for severity filtering would go here
            return getAllSymptoms(); // For now, return all
        }
        
        @Override
        public void saveSymptom(Symptom symptom) {
            try {
                // Convert from controller Symptom to model Symptom would be needed here
                System.out.println("Save symptom called: " + symptom.getSymptomName());
            } catch (Exception e) {
                System.err.println("Errore nel salvataggio symptom: " + e.getMessage());
            }
        }
        
        @Override
        public void deleteSymptom(Long id) {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    // For now, we can't delete by id since DAO only deletes by patient_id + symptom name
                    System.out.println("Delete symptom called for id: " + id);
                }
            } catch (Exception e) {
                System.err.println("Errore nella cancellazione symptom: " + e.getMessage());
            }
        }
        
        @Override
        public Optional<Symptom> getSymptomById(Long id) {
            // Implementation would go here
            return Optional.empty();
        }
    }

    private static class MockSymptomService implements SymptomService {

        @Override
        public java.util.List<Symptom> getAllSymptoms() {
            return java.util.Arrays.asList(
                    new Symptom(1L, "Increased thirst", "Moderate", "Approx. 2 hours",
                            LocalDateTime.of(2023, 10, 28, 10, 15), "Persistent thirst despite drinking water"),

                    new Symptom(2L, "Blurred vision", "Mild", "15 minutes",
                            LocalDateTime.of(2023, 10, 27, 19, 0), "Temporary blurred vision episode"),

                    new Symptom(3L, "Fatigue", "Severe", "All morning",
                            LocalDateTime.of(2023, 10, 27, 9, 30), "Extreme tiredness affecting daily activities"),

                    new Symptom(4L, "Frequent urination", "Moderate", "Throughout the night",
                            LocalDateTime.of(2023, 10, 26, 23, 0), "Woke up multiple times during night"),

                    new Symptom(5L, "Headache", "Mild", "30 minutes",
                            LocalDateTime.of(2023, 10, 26, 14, 45), "Mild headache after lunch")
            );
        }

        @Override
        public java.util.List<Symptom> getSymptomsByDateRange(LocalDateTime start, LocalDateTime end) {
            return getAllSymptoms().stream()
                    .filter(s -> s.getDateRecorded().isAfter(start) && s.getDateRecorded().isBefore(end))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public java.util.List<Symptom> getSymptomsBySeverity(String severity) {
            return getAllSymptoms().stream()
                    .filter(s -> s.getSeverity().equalsIgnoreCase(severity))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public void saveSymptom(Symptom symptom) {
            // Mock implementation - in realtà salverebbe nel database
            System.out.println("Saving symptom: " + symptom.getSymptomName() +
                    " with severity: " + symptom.getSeverity());
        }

        @Override
        public void deleteSymptom(Long id) {
            // Mock implementation
            System.out.println("Deleting symptom with ID: " + id);
        }

        @Override
        public Optional<Symptom> getSymptomById(Long id) {
            return getAllSymptoms().stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst();
        }
    }
}