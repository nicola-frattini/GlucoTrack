package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;

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

        loadGlucoseReadingsFromDatabase();

        readingsTable.setItems(filteredData);
    }
    
    private void loadGlucoseReadingsFromDatabase() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                return;
            }
            
            int patientId = currentUser.getId();

            GlucoseMeasurementDAO glucoseDAO = new GlucoseMeasurementDAO();
            List<GlucoseMeasurement> measurements = glucoseDAO.getGlucoseMeasurementsByPatientId(patientId);
            

            for (GlucoseMeasurement measurement : measurements) {
                GlucoseReading reading = convertToGlucoseReading(measurement);
                readingsData.add(reading);
                filteredData.add(reading);
            }
            

        } catch (SQLException e) {
            System.err.println("Error from database: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Generic Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private GlucoseReading convertToGlucoseReading(GlucoseMeasurement measurement) {

        int value = Math.round(measurement.getGlucoseLevel());
        

        String status = measurement.getStatusString();
        

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

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);
            

            startDatePicker.setValue(startDate);
            endDatePicker.setValue(endDate);
            

            startDatePicker.setPromptText("");
            endDatePicker.setPromptText("");
            

            startDatePicker.setOnAction(e -> {
                System.out.println("Start date changed: " + startDatePicker.getValue());
                applyFilters();
            });
            endDatePicker.setOnAction(e -> {
                System.out.println("End date changed: " + endDatePicker.getValue());
                applyFilters();
            });
            
        } catch (Exception e) {
            System.err.println("Error during DatePicker configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupEventHandlers() {
        addReadingBtn.setOnAction(e -> handleAddNewReading());
    }

    private void showReadingDetailsPopup(GlucoseReading reading) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController controller = loader.getController();
            controller.setTitle("Measurament details");
            controller.setSubtitle("All the measurament details");
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();

            String note = "";
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    GlucoseMeasurementDAO glucoseDAO = new GlucoseMeasurementDAO();
                    java.util.List<GlucoseMeasurement> measurements = glucoseDAO.getGlucoseMeasurementsByPatientId(currentUser.getId());
                    for (GlucoseMeasurement m : measurements) {
                        if (m.getDateAndTime().equals(reading.getDateTime()) &&
                            m.getType().equals(reading.getType()) &&
                            Math.round(m.getGlucoseLevel()) == reading.getValue()) {
                            note = m.getNotes();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                note = "Error";
            }

            content.getChildren().addAll(
                new javafx.scene.control.Label("Date/Hour: " + reading.getFormattedTime()),
                new javafx.scene.control.Label("Type: " + reading.getType()),
                new javafx.scene.control.Label("Value: " + reading.getFormattedValue()),
                new javafx.scene.control.Label("Status: " + reading.getStatus()),
                new javafx.scene.control.Label("Notes: " + ((note == null || note.isEmpty()) ? "Nobody" : note))
            );
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setMinWidth(520);
            popupStage.setMinHeight(340);
            controller.setStage(popupStage);
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupContextMenu() {
        // Create context menu items
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        
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
        
        // Set up custom row factory with context menu, selection highlighting e doppio click
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

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.getClickCount() == 2) {
                    GlucoseReading reading = row.getItem();
                    showReadingDetailsPopup(reading);
                }
            });
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
            System.err.println("Error opening edit form: " + e.getMessage());
            showErrorAlert("Error", "Couldn't open edit form: " + e.getMessage());
        }
    }
    
    
    private void handleDeleteReading(GlucoseReading selectedReading) {
        // Show custom confirmation dialog
        boolean confirmed = showCustomConfirmationDialog(
            "Confirm Deletion",
            "Delete this reading?",
            String.format(
                "Do you really want delete the medication from %s at %s?\nValue: %s\nType: %s",
                selectedReading.getDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                selectedReading.getFormattedTime(),
                selectedReading.getFormattedValue(),
                selectedReading.getType()
            )
        );
        if (confirmed) {
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
                        
                        showSuccessAlert("Success", "Measurement deleted successfully.");
                    } else {
                        showErrorAlert("Error", "Couldn't delete the measurement.");
                    }
                } else {
                    showErrorAlert("Error", "No user in session.");
                }
                
            } catch (SQLException e) {
                showErrorAlert("Database error", "Error during measurement deletion: " + e.getMessage());
            }
        }
    }
    
    private void showErrorAlert(String title, String message) {
        showCustomPopup(title, message, "error");
    }
    
    private void showSuccessAlert(String title, String message) {
        showCustomPopup(title, message, "success");
    }

    // --- Custom Popup Helpers ---
    private void showCustomPopup(String title, String message, String type) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController controller = loader.getController();
            controller.setTitle(title);
            controller.setSubtitle(type.equals("error") ? "Error" : (type.equals("success") ? "Success" : "Info"));
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();
            javafx.scene.control.Label label = new javafx.scene.control.Label(message);
            label.setWrapText(true);
            content.getChildren().add(label);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setMinWidth(420);
            popupStage.setMinHeight(200);
            controller.setStage(popupStage);
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean showCustomConfirmationDialog(String title, String subtitle, String message) {
        final boolean[] result = {false};
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController controller = loader.getController();
            controller.setTitle(title);
            controller.setSubtitle(subtitle);
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();
            javafx.scene.control.Label label = new javafx.scene.control.Label(message);
            label.setWrapText(true);
            javafx.scene.control.Button yesBtn = new javafx.scene.control.Button("Sì");
            javafx.scene.control.Button noBtn = new javafx.scene.control.Button("No");
            yesBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 8;");
            noBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 8;");
            javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(16, yesBtn, noBtn);
            btnBox.setStyle("-fx-alignment: center; -fx-padding: 18 0 0 0;");
            content.getChildren().addAll(label, btnBox);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setMinWidth(420);
            popupStage.setMinHeight(220);
            controller.setStage(popupStage);
            yesBtn.setOnAction(ev -> { result[0] = true; popupStage.close(); });
            noBtn.setOnAction(ev -> { result[0] = false; popupStage.close(); });
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result[0];
    }



    private void applyFilters() {
        filteredData.clear();

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String selectedType = typeComboBox.getValue();

        for (GlucoseReading reading : readingsData) {
            LocalDate readingDate = reading.getDateTime().toLocalDate();
            
            // Veirfy the date range
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
    

    private void openGlucoseInsertForm() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardGlucoseInsert.fxml"));
            Parent glucoseInsertView = loader.load();
            
            PatientDashboardGlucoseInsertController insertController = loader.getController();
            

            insertController.setOnDataSaved(() -> {
                refreshData();

                returnToReadings();
            });
            
            // Set callback for cancel action
            insertController.setOnCancel(this::returnToReadings);
            
            loadContentInMainDashboard(glucoseInsertView);
            
        } catch (IOException e) {
            System.err.println("Error during form loading: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    private void loadContentInMainDashboard(Parent content) {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContentDirect(content);
            } else {
                System.err.println("Principal controller not available to load content.");
            }
        } catch (Exception e) {
            System.err.println("Error during dashboard loading: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Metodo per tornare alla sezione readings
    private void returnToReadings() {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContent("PatientDashboardReadings.fxml");
            } else {
                System.err.println("Principal controller not available to return to readings.");
            }
        } catch (Exception e) {
            System.err.println("Error returning to reading page: " + e.getMessage());
            e.printStackTrace();
        }
    }
}