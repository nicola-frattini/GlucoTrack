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
import java.util.Optional;

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
        
        // Usa il tipo direttamente dal modello
        String type = measurement.getType();
        
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
        
        // Setup context menu for the table
        setupContextMenu();
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
    
    private void setupContextMenu() {
        // Create context menu items
        MenuItem editItem = new MenuItem("Modifica");
        MenuItem deleteItem = new MenuItem("Cancella");
        
        // Set up actions
        editItem.setOnAction(e -> {
            GlucoseReading selectedReading = readingsTable.getSelectionModel().getSelectedItem();
            if (selectedReading != null) {
                handleEditReading(selectedReading);
            }
        });
        
        deleteItem.setOnAction(e -> {
            GlucoseReading selectedReading = readingsTable.getSelectionModel().getSelectedItem();
            if (selectedReading != null) {
                handleDeleteReading(selectedReading);
            }
        });
        
        // Create context menu
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(editItem, deleteItem);
        
        // Set up custom row factory with context menu and selection highlighting
        readingsTable.setRowFactory(tv -> {
            TableRow<GlucoseReading> row = new TableRow<GlucoseReading>() {
                @Override
                protected void updateItem(GlucoseReading item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    }
                }
                
                @Override
                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    if (selected && getItem() != null) {
                        setStyle("-fx-background-color: #0078d4; -fx-text-fill: white;");
                    } else if (getItem() != null) {
                        setStyle("");
                    }
                }
            };
            
            // Add hover effect
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
            
            // Only show context menu when row has data
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(contextMenu)
            );
            
            return row;
        });
    }
    
    private void handleEditReading(GlucoseReading selectedReading) {
        try {
            // Load the edit form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardGlucoseEdit.fxml"));
            Parent editView = loader.load();
            
            // Get the controller and set up for editing
            PatientDashboardGlucoseEditController editController = loader.getController();
            editController.setupForEdit(selectedReading);
            
            // Set callbacks
            editController.setOnDataUpdated(() -> {
                refreshData();
                returnToReadings();
            });
            
            editController.setOnCancel(this::returnToReadings);
            
            // Load in main dashboard
            loadContentInMainDashboard(editView);
            
        } catch (IOException e) {
            System.err.println("Errore nell'apertura del form di modifica: " + e.getMessage());
            showErrorAlert("Errore", "Impossibile aprire il form di modifica.");
        }
    }
    
    private void handleDeleteReading(GlucoseReading selectedReading) {
        // Show confirmation dialog
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Conferma Cancellazione");
        confirmationAlert.setHeaderText("Eliminare questa misurazione?");
        confirmationAlert.setContentText(String.format(
            "Vuoi davvero eliminare la misurazione del %s alle %s?\nValore: %s\nTipo: %s",
            selectedReading.getDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            selectedReading.getFormattedTime(),
            selectedReading.getFormattedValue(),
            selectedReading.getType()
        ));
        
        // Apply dark theme to the alert
        DialogPane dialogPane = confirmationAlert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/assets/css/dashboard-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("alert");
        
        Optional<ButtonType> result = confirmationAlert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Delete from database
            try {
                GlucoseMeasurementDAO glucoseDAO = new GlucoseMeasurementDAO();
                
                // Find the corresponding GlucoseMeasurement in database by matching datetime and value
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    boolean deleted = glucoseDAO.deleteGlucoseMeasurement(
                        currentUser.getId(), 
                        selectedReading.getDateTime(), 
                        (float) selectedReading.getValue()
                    );
                    
                    if (deleted) {
                        // Remove from table data
                        readingsData.remove(selectedReading);
                        filteredData.remove(selectedReading);
                        
                        showSuccessAlert("Successo", "Misurazione eliminata con successo.");
                        System.out.println("✅ Misurazione eliminata con successo");
                    } else {
                        showErrorAlert("Errore", "Impossibile eliminare la misurazione dal database.");
                    }
                } else {
                    showErrorAlert("Errore", "Nessun utente in sessione.");
                }
                
            } catch (SQLException e) {
                System.err.println("Errore nell'eliminazione della misurazione: " + e.getMessage());
                showErrorAlert("Errore Database", "Errore nell'eliminazione della misurazione: " + e.getMessage());
            }
        }
    }
    
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Apply dark theme
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/assets/css/dashboard-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("alert");
        
        alert.showAndWait();
    }
    
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Apply dark theme
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/assets/css/dashboard-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("alert");
        
        alert.showAndWait();
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