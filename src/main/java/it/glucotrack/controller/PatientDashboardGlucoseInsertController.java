package it.glucotrack.controller;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

import it.glucotrack.model.GlucoseMeasurement;
import it.glucotrack.util.GlucoseMeasurementDAO;
import it.glucotrack.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PatientDashboardGlucoseInsertController implements Initializable {


    private Runnable onDataSaved;
    

    private Runnable onCancel;

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
    private Button saveButton;

    private GlucoseMeasurementDAO glucoseMeasurementDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        glucoseMeasurementDAO = new GlucoseMeasurementDAO();

        setupDefaultValues();
        setupValidation();
        setupComboBox();
    }

    private void setupDefaultValues() {

        datePicker.setValue(LocalDate.now());


        LocalTime now = LocalTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        timeField.setText(now.format(timeFormatter));
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
                "Fasting"
            
        ));


        typeComboBox.setValue("Before Breakfast");
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

    @FXML
    private void handleSaveEntry() {
        if (validateInput()) {
            try {

                GlucoseMeasurement measurement = createMeasurementFromInput();

                boolean success = glucoseMeasurementDAO.insertGlucoseMeasurement(measurement);

                if (success) {
                    showSuccessAlert();
                    clearForm();
                    
                    if (onDataSaved != null) {
                        onDataSaved.run();
                    }
                } else {
                    showErrorAlert("Error during save data", "Couldn't save the measurement.");
                }

            } catch (SQLException e) {
                showErrorAlert("Database error", "Error during the save: " + e.getMessage());
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

    private void clearForm() {
        datePicker.setValue(LocalDate.now());

        LocalTime now = LocalTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        timeField.setText(now.format(timeFormatter));

        typeComboBox.setValue("Before Breakfast");
        valueField.clear();
        notesArea.clear();
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Saved measuration");
        alert.setContentText("Measuration saved with success!");

        alert.showAndWait();

    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
        alert.setContentText(message);

        alert.showAndWait();
    }



    // Metodo per chiudere la finestra (se necessario)
    public void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
    
    // Setter per il callback di refresh dati
    public void setOnDataSaved(Runnable onDataSaved) {
        this.onDataSaved = onDataSaved;
    }
    
    // Setter per il callback di annullamento
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }
    
    @FXML
    private void handleCancel() {
        // Esegui il callback di annullamento se presente
        if (onCancel != null) {
            onCancel.run();
        }
    }
}