package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import it.glucotrack.util.SymptomDAO;
import it.glucotrack.util.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class PatientDashboardSymptomsInsertController implements Initializable {

    @FXML
    private TextField dateField;

    @FXML
    private TextField timeField;

    @FXML
    private TextArea symptomDescriptionArea;

    @FXML
    private ComboBox<String> severityComboBox;

    @FXML
    private ComboBox<Integer> hoursComboBox;

    @FXML
    private ComboBox<Integer> minutesComboBox;

    @FXML
    private TextArea notesArea;

    @FXML
    private Button logSymptomButton;

    private SymptomDAO symptomDAO;
    
    // Callback for succesfull data save
    private Runnable onDataSaved;
    
    // Callback for cancle click
    private Runnable onCancel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        symptomDAO = new SymptomDAO();

        setupDefaultValues();
        setupValidation();
        setupComboBox();
    }

    private void setupDefaultValues() {

        LocalDate now = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        dateField.setText(now.format(dateFormatter));


        LocalTime nowTime = LocalTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        timeField.setText(nowTime.format(timeFormatter));
    }

    private void setupComboBox() {

        severityComboBox.setItems(FXCollections.observableArrayList(
                "Mild",
                "Moderate",
                "Severe",
                "Very Severe"
        ));

        // Placeholder text
        severityComboBox.setPromptText("Select Severity");
    }

    private void setupValidation() {

        for (int i = 0; i <= 23; i++) {
            hoursComboBox.getItems().add(i);
        }
        for (int i = 0; i <= 59; i++) {
            minutesComboBox.getItems().add(i);
        }
        hoursComboBox.setValue(0);
        minutesComboBox.setValue(0);

        // Limit length
        symptomDescriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 500) {
                symptomDescriptionArea.setText(oldValue);
            }
        });

        // Limit length
        notesArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 300) {
                notesArea.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleLogSymptom() {
        if (validateInput()) {
            try {

                boolean success = saveSymptom();

                if (success) {
                    showSuccessAlert();
                    clearForm();
                    
                    if (onDataSaved != null) {
                        onDataSaved.run();
                    }
                } else {
                    showErrorAlert("Error during the saveo", "Can't save symptom.");
                }

            } catch (SQLException e) {
                showErrorAlert("Error Database", "Error during the saveo: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                showErrorAlert("Error", "Error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Date validation
        if (dateField.getText().trim().isEmpty()) {
            errors.append("- Date is required\n");
        } else {
            try {
                parseDate(dateField.getText());
            } catch (DateTimeParseException e) {
                errors.append("- Invalid date format (use MM/dd/yyyy)\n");
            }
        }

        // Time validation
        if (timeField.getText().trim().isEmpty()) {
            errors.append("- Time is required\n");
        } else {
            try {
                parseTime(timeField.getText());
            } catch (DateTimeParseException e) {
                errors.append("- Invalid time format (use hh:mm AM/PM)\n");
            }
        }

        // Symptom name validation
        if (symptomDescriptionArea.getText().trim().isEmpty()) {
            errors.append("- Symptom description is required\n");
        }

        // Severity validation
        if (severityComboBox.getValue() == null || severityComboBox.getValue().trim().isEmpty()) {
            errors.append("- Severity level is required\n");
        }

        // Duration is fully optional

        if (errors.length() > 0) {
            showErrorAlert("Validation Errors", errors.toString());
            return false;
        }

        return true;
    }


    private LocalDate parseDate(String dateText) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return LocalDate.parse(dateText, formatter);
    }

    private LocalTime parseTime(String timeText) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return LocalTime.parse(timeText, formatter);
    }

    private boolean saveSymptom() throws SQLException {
        // Parsing date ad hour
        LocalDate date = parseDate(dateField.getText());
        LocalTime time = parseTime(timeField.getText());
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        String symptomDescription = symptomDescriptionArea.getText().trim();
        String severity = severityComboBox.getValue();
        Integer hours = hoursComboBox.getValue();
        Integer minutes = minutesComboBox.getValue();
        String notes = notesArea.getText().trim();

        // Create Symptom object
        it.glucotrack.model.Symptom symptom = new it.glucotrack.model.Symptom();
        symptom.setSymptomName(symptomDescription);
        symptom.setGravity(severity);
        symptom.setNotes(notes);
        symptom.setDateAndTime(dateTime);
        
        // Create duration time
        LocalTime durationTime = LocalTime.of(hours != null ? hours : 0, minutes != null ? minutes : 0);
        symptom.setDuration(durationTime);

        int patientId = SessionManager.getInstance().getCurrentUser().getId();
        
        return symptomDAO.insertSymptom(patientId, symptom);
    }
    


    private void clearForm() {

        // Reset date and time to current
        setupDefaultValues();

        // Clean other fields
        symptomDescriptionArea.clear();
        severityComboBox.setValue(null);
        severityComboBox.setPromptText("Select Severity");
        hoursComboBox.setValue(0);
        minutesComboBox.setValue(0);
        notesArea.clear();
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Symptom Recorded");
        alert.setContentText("The symptom has been successfully recorded in your medical diary!");

        // Style the alert to match the dark theme
        alert.getDialogPane().setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        alert.showAndWait();
    }


    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
        alert.setContentText(message);

        // Style the alert
        alert.getDialogPane().setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        alert.showAndWait();
    }


    //=== Callback setters ===

    public void setOnDataSaved(Runnable onDataSaved) {
        this.onDataSaved = onDataSaved;
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