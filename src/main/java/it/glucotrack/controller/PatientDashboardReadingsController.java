package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.sql.SQLException;
import java.util.List;
import it.glucotrack.util.SessionManager;
import it.glucotrack.util.GlucoseMeasurementDAO;
import it.glucotrack.model.User;
import it.glucotrack.model.GlucoseMeasurement;

public class PatientDashboardReadingsController implements Initializable {

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private Button addReadingBtn;

    @FXML
    private TableView<GlucoseReading> readingsTable;

    @FXML
    private TableColumn<GlucoseReading, String> timeColumn;

    @FXML
    private TableColumn<GlucoseReading, String> typeColumn;

    @FXML
    private TableColumn<GlucoseReading, String> valueColumn;

    @FXML
    private TableColumn<GlucoseReading, String> statusColumn;

    private ObservableList<GlucoseReading> readingsData;
    private ObservableList<GlucoseReading> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeData();
        setupTableColumns();
        setupComboBox();
        setupDatePicker();
        setupEventHandlers();
        applyFilters();
    }

    private void initializeData() {
        readingsData = FXCollections.observableArrayList();
        filteredData = FXCollections.observableArrayList();
        
        // Carica i dati reali dal database
        loadGlucoseReadingsFromDatabase();
        
        readingsTable.setItems(filteredData);
    }
    
    private void loadGlucoseReadingsFromDatabase() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                System.err.println("❌ Nessun utente in sessione per caricare le readings!");
                return;
            }
            
            int patientId = currentUser.getId();
            System.out.println("📊 Caricamento readings per paziente ID: " + patientId);
            
            GlucoseMeasurementDAO glucoseDAO = new GlucoseMeasurementDAO();
            List<GlucoseMeasurement> measurements = glucoseDAO.getGlucoseMeasurementsByPatientId(patientId);
            
            System.out.println("📊 Trovate " + measurements.size() + " misurazioni nel database");
            
            // Converti GlucoseMeasurement in GlucoseReading per la tabella
            for (GlucoseMeasurement measurement : measurements) {
                GlucoseReading reading = convertToGlucoseReading(measurement);
                readingsData.add(reading);
                filteredData.add(reading);
            }
            
            System.out.println("✅ Readings caricate con successo nella tabella");
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nel caricamento delle readings: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Errore generico nel caricamento delle readings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private GlucoseReading convertToGlucoseReading(GlucoseMeasurement measurement) {
        // Converti il valore dalla misurazione (da float a int per la tabella)
        int value = Math.round(measurement.getGlucoseLevel());
        
        // Usa lo status già calcolato dal modello
        String status = measurement.getStatusString();
        
        // Determina il tipo basato su beforeMeal
        String type = measurement.isBeforeMeal() ? "Before Meal" : "After Meal";
        
        return new GlucoseReading(measurement.getDateAndTime(), type, value, status);
    }

    private void setupTableColumns() {
        // Setup Time column
        timeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedTime()));

        // Setup Type column
        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getType()));

        // Setup Value column
        valueColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedValue()));

        // Setup Status column with colored indicators
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus()));

        // Custom cell factory for status column to add color indicators
        statusColumn.setCellFactory(new Callback<TableColumn<GlucoseReading, String>, TableCell<GlucoseReading, String>>() {
            @Override
            public TableCell<GlucoseReading, String> call(TableColumn<GlucoseReading, String> param) {
                return new TableCell<GlucoseReading, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            setStyle("");
                        } else {
                            setText(item);
                            // Apply colors based on status
                            switch (item.toLowerCase()) {
                                case "normal":
                                    setStyle("-fx-text-fill: #2ECC71;"); // Green
                                    break;
                                case "elevated":
                                    setStyle("-fx-text-fill: #F39C12;"); // Orange/Yellow
                                    break;
                                case "high":
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

        // Set table styling
        readingsTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white;");
    }

    private void setupComboBox() {
        ObservableList<String> readingTypes = FXCollections.observableArrayList(
                "All Types", "Pre-Meal", "Post-Meal", "Bedtime", "Fasting"
        );
        typeComboBox.setItems(readingTypes);
        typeComboBox.setValue("All Types");

        // Apply filters when selection changes
        typeComboBox.setOnAction(e -> applyFilters());
    }

    private void setupDatePicker() {
        try {
            // Imposta le date di default (ultima settimana) senza modificare prompt text
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);
            
            // Imposta valori di default in modo sicuro
            startDatePicker.setValue(startDate);
            endDatePicker.setValue(endDate);
            
            // Pulisce il prompt text per evitare conflitti
            startDatePicker.setPromptText("");
            endDatePicker.setPromptText("");
            
            // Listener per entrambi i date picker
            startDatePicker.setOnAction(e -> {
                System.out.println("Start date changed: " + startDatePicker.getValue());
                applyFilters();
            });
            endDatePicker.setOnAction(e -> {
                System.out.println("End date changed: " + endDatePicker.getValue());
                applyFilters();
            });
            
        } catch (Exception e) {
            System.err.println("Errore nella configurazione dei DatePicker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupEventHandlers() {
        addReadingBtn.setOnAction(e -> handleAddNewReading());
    }



    private void applyFilters() {
        filteredData.clear();

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String selectedType = typeComboBox.getValue();

        for (GlucoseReading reading : readingsData) {
            LocalDate readingDate = reading.getDateTime().toLocalDate();
            
            // Verifica se la data è nel range selezionato
            boolean matchesDate = true;
            if (startDate != null && endDate != null) {
                matchesDate = (readingDate.isEqual(startDate) || readingDate.isAfter(startDate)) &&
                             (readingDate.isEqual(endDate) || readingDate.isBefore(endDate));
            } else if (startDate != null) {
                matchesDate = readingDate.isEqual(startDate) || readingDate.isAfter(startDate);
            } else if (endDate != null) {
                matchesDate = readingDate.isEqual(endDate) || readingDate.isBefore(endDate);
            }
            
            boolean matchesType = selectedType == null || selectedType.equals("All Types") || reading.getType().equals(selectedType);

            if (matchesDate && matchesType) {
                filteredData.add(reading);
            }
        }
    }

    private void handleAddNewReading() {
        openGlucoseInsertForm();
    }

    // Method to add new reading programmatically
    public void addReading(GlucoseReading reading) {
        readingsData.add(reading);
        applyFilters();
    }

    // Method to refresh data (useful for external updates)
    public void refreshData() {
        // Ricarica i dati dal database
        readingsData.clear();
        filteredData.clear();
        loadGlucoseReadingsFromDatabase();
        applyFilters();
    }

    // Getter for readings data (useful for other controllers)
    public ObservableList<GlucoseReading> getReadingsData() {
        return readingsData;
    }

    // Inner class for GlucoseReading model
    public static class GlucoseReading {
        private LocalDateTime dateTime;
        private String type;
        private int value; // mg/dL
        private String status;

        public GlucoseReading(LocalDateTime dateTime, String type, int value, String status) {
            this.dateTime = dateTime;
            this.type = type;
            this.value = value;
            this.status = status;
        }

        // Getters
        public LocalDateTime getDateTime() { return dateTime; }
        public String getType() { return type; }
        public int getValue() { return value; }
        public String getStatus() { return status; }

        // Formatted getters for table display
        public String getFormattedTime() {
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        public String getFormattedValue() {
            return value + " mg/dL";
        }

        // Setters
        public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
        public void setType(String type) { this.type = type; }
        public void setValue(int value) { this.value = value; }
        public void setStatus(String status) { this.status = status; }

        @Override
        public String toString() {
            return String.format("GlucoseReading{time=%s, type=%s, value=%d, status=%s}",
                    getFormattedTime(), type, value, status);
        }
    }
    
    // Metodo per aprire il form di inserimento glicemia nel pannello centrale
    private void openGlucoseInsertForm() {
        try {
            // Carica il form nel pannello centrale del dashboard principale
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardGlucoseInsert.fxml"));
            Parent glucoseInsertView = loader.load();
            
            // Ottieni il controller del form
            PatientDashboardGlucoseInsertController insertController = loader.getController();
            
            // Imposta il callback per refresh dei dati quando si salva
            insertController.setOnDataSaved(() -> {
                refreshData();
                // Dopo il salvataggio, torna alla sezione readings
                returnToReadings();
            });
            
            // Imposta il callback per l'annullamento
            insertController.setOnCancel(this::returnToReadings);
            
            // Sostituisce il contenuto centrale con il form
            loadContentInMainDashboard(glucoseInsertView);
            
        } catch (IOException e) {
            System.err.println("Errore nell'apertura del form di inserimento glicemia: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Metodo per caricare contenuto nel pannello centrale del dashboard principale
    private void loadContentInMainDashboard(Parent content) {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContentDirect(content);
                System.out.println("✅ Contenuto caricato nel pannello centrale via controller principale");
            } else {
                System.err.println("❌ Controller principale non disponibile");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento del contenuto nel dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Metodo per tornare alla sezione readings
    private void returnToReadings() {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContent("PatientDashboardReadings.fxml");
                System.out.println("✅ Ritorno alla sezione readings completato");
            } else {
                System.err.println("❌ Controller principale non disponibile per il ritorno alla sezione readings");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel ritorno alla sezione readings: " + e.getMessage());
            e.printStackTrace();
        }
    }
}