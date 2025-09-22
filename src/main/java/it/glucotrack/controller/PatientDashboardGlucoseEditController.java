package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import it.glucotrack.model.GlucoseMeasurement;
import it.glucotrack.model.User;
import it.glucotrack.util.GlucoseMeasurementDAO;
import it.glucotrack.util.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class PatientDashboardGlucoseEditController implements Initializable {


    private Runnable onDataUpdated;
    

    private Runnable onCancel;
    

    private PatientDashboardReadingsController.GlucoseReading originalReading;
    private GlucoseMeasurement originalMeasurement;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField timeField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TextField valueField;

    @FXML
    private TextArea notesArea;

    @FXML
    private Button updateButton;

    private GlucoseMeasurementDAO glucoseMeasurementDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        glucoseMeasurementDAO = new GlucoseMeasurementDAO();
        setupValidation();
        setupComboBox();
    }

    private void setupComboBox() {

        typeComboBox.setItems(FXCollections.observableArrayList(
                "Before Breakfast",
                "After Breakfast",
                "Before Lunch",
                "After Lunch",
                "Before Dinner",
                "After Dinner",
                "Before Sleep",
                "Fasting",
                "Random"
        ));
    }

    private void setupValidation() {

        valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                valueField.setText(oldValue);
            }
        });

        timeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("([01]?\\d|2[0-3]):?[0-5]?\\d?")) {
                timeField.setText(oldValue);
            }
        });
    }
    
    public void setupForEdit(PatientDashboardReadingsController.GlucoseReading reading) {
        this.originalReading = reading;
        
        datePicker.setValue(reading.getDateTime().toLocalDate());
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        timeField.setText(reading.getDateTime().format(timeFormatter));
        
        typeComboBox.setValue(reading.getType());
        valueField.setText(String.valueOf(reading.getValue()));
        

        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                originalMeasurement = glucoseMeasurementDAO.findGlucoseMeasurement(
                    currentUser.getId(), 
                    reading.getDateTime(), 
                    (float) reading.getValue()
                );
                
                if (originalMeasurement != null && originalMeasurement.getNotes() != null) {
                    notesArea.setText(originalMeasurement.getNotes());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during notes loading: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateEntry() {
        if (validateInput()) {
            try {

                GlucoseMeasurement updatedMeasurement = createMeasurementFromInput();
                
                if (originalMeasurement != null) {
                    updatedMeasurement.setId(originalMeasurement.getId());
                }


                boolean success = glucoseMeasurementDAO.updateGlucoseMeasurement(updatedMeasurement);

                if (success) {
                    showSuccessAlert();
                    

                    if (onDataUpdated != null) {
                        onDataUpdated.run();
                    }
                } else {
                    showErrorAlert("Error during updating measuration", "Couldn't update the Measurement.");
                }

            } catch (SQLException e) {
                showErrorAlert("Database Error", "Error during the update: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Date validation
        if (datePicker.getValue() == null) {
            errors.append("- Date is required\n");
        }

        // Time validation
        if (timeField.getText().trim().isEmpty()) {
            errors.append("- Time is required\n");
        } else {
            try {
                parseTime(timeField.getText());
            } catch (DateTimeParseException e) {
                errors.append("- Invalid time format (use HH:mm)\n");
            }
        }

        // Value validation
        if (valueField.getText().trim().isEmpty()) {
            errors.append("- Blood glucose value is required\n");
        } else {
            try {
                float value = Float.parseFloat(valueField.getText());
                if (value <= 0 || value > 1000) {
                    errors.append("- Value must be between 1 and 1000 mg/dL\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- Invalid blood glucose value\n");
            }
        }

        // Type validation
        if (typeComboBox.getValue() == null || typeComboBox.getValue().trim().isEmpty()) {
            errors.append("- Measurement type is required\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Validation Errors", errors.toString());
            return false;
        }

        return true;
    }


    private LocalTime parseTime(String timeText) throws DateTimeParseException {

        if (timeText.matches("\\d{1,2}\\d{2}")) {
            timeText = timeText.substring(0, timeText.length()-2) + ":" + timeText.substring(timeText.length()-2);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
        return LocalTime.parse(timeText, formatter);
    }

    private GlucoseMeasurement createMeasurementFromInput() throws DateTimeParseException {
        LocalDate date = datePicker.getValue();
        LocalTime time = parseTime(timeField.getText());
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        float value = Float.parseFloat(valueField.getText());
        String type = typeComboBox.getValue();
        String notes = notesArea.getText().trim();

        if (notes.isEmpty()) {
            notes = type;
        }

        int patientId = SessionManager.getInstance().getCurrentUserId();

        return new GlucoseMeasurement(patientId, dateTime, value, type, notes);
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText("Measurement Updated");
        alert.setContentText("Measurement updated with success!");
        
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
        alert.setContentText(message);

        
        alert.showAndWait();
    }


    public void setOnDataUpdated(Runnable onDataUpdated) {
        this.onDataUpdated = onDataUpdated;
    }
    

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }
    
    @FXML
    private void handleCancel() {

        if (onCancel != null) {
            onCancel.run();
        }
    }
}