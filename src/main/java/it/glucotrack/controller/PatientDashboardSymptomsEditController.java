package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import it.glucotrack.model.Symptom;
import it.glucotrack.util.SymptomDAO;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class PatientDashboardSymptomsEditController implements Initializable {

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
    private Button updateSymptomButton;

    private SymptomDAO symptomDAO;
    
    private Runnable onDataUpdated;
    
    private Runnable onCancel;
    
    private Symptom originalSymptom;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        symptomDAO = new SymptomDAO();
        setupValidation();
        setupComboBox();
    }

    private void setupComboBox() {

        severityComboBox.setItems(FXCollections.observableArrayList(
                "Mild",
                "Moderate",
                "Severe",
                "Very Severe"
        ));

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


        symptomDescriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 500) {
                symptomDescriptionArea.setText(oldValue);
            }
        });


        notesArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 1000) {
                notesArea.setText(oldValue);
            }
        });
    }
    
    public void setupForEdit(Symptom symptom) {
        this.originalSymptom = symptom;
        

        LocalDateTime dateTime = symptom.getDateAndTime();
        

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        dateField.setText(dateTime.format(dateFormatter));
        

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        timeField.setText(dateTime.format(timeFormatter));
        

        symptomDescriptionArea.setText(symptom.getSymptomName());
        severityComboBox.setValue(symptom.getGravity());
        

        if (symptom.getDuration() != null) {
            LocalTime duration = symptom.getDuration();
            hoursComboBox.setValue(duration.getHour());
            minutesComboBox.setValue(duration.getMinute());
        }
        
        if (symptom.getNotes() != null) {
            notesArea.setText(symptom.getNotes());
        }
    }

    @FXML
    private void handleUpdateSymptom() {
        if (validateInput()) {
            try {
                Symptom updatedSymptom = createSymptomFromInput();
                updatedSymptom.setId(originalSymptom.getId());
                updatedSymptom.setPatient_id(originalSymptom.getPatient_id());

                boolean success = symptomDAO.updateSymptom(updatedSymptom);

                if (success) {
                    showSuccessAlert();
                    
                    if (onDataUpdated != null) {
                        onDataUpdated.run();
                    }
                } else {
                    showErrorAlert("Error during update", "Can't update the symptom.");
                }

            } catch (SQLException e) {
                showErrorAlert("Errore Database", "Errore durante l'aggiornamento: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (dateField.getText().trim().isEmpty()) {
            errors.append("- Data is needed\n");
        } else {
            try {
                parseDate(dateField.getText());
            } catch (DateTimeParseException e) {
                errors.append("- Format not valid (use MM/dd/yyyy)\n");
            }
        }

        if (timeField.getText().trim().isEmpty()) {
            errors.append("- Hour is needed\n");
        } else {
            try {
                parseTime(timeField.getText());
            } catch (DateTimeParseException e) {
                errors.append("- Forma not valid (use hh:mm AM/PM)\n");
            }
        }

        if (symptomDescriptionArea.getText().trim().isEmpty()) {
            errors.append("- Symptom's name needed\n");
        }

        if (severityComboBox.getValue() == null || severityComboBox.getValue().trim().isEmpty()) {
            errors.append("- Severity needed\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Validation errors", errors.toString());
            return false;
        }

        return true;
    }

    private LocalDate parseDate(String dateText) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return LocalDate.parse(dateText, formatter);
    }

    private LocalTime parseTime(String timeText) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return LocalTime.parse(timeText, formatter);
    }



    private Symptom createSymptomFromInput() throws DateTimeParseException {
        LocalDate date = parseDate(dateField.getText());
        LocalTime time = parseTime(timeField.getText());
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        String symptomName = symptomDescriptionArea.getText().trim();
        String severity = severityComboBox.getValue();
        String notes = notesArea.getText().trim();
        Integer hours = hoursComboBox.getValue();
        Integer minutes = minutesComboBox.getValue();
        LocalTime duration = LocalTime.of(hours != null ? hours : 0, minutes != null ? minutes : 0);

        Symptom symptom = new Symptom();
        symptom.setSymptomName(symptomName);
        symptom.setGravity(severity);
        symptom.setDateAndTime(dateTime);
        symptom.setDuration(duration);
        symptom.setNotes(notes);

        return symptom;
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Symptom Updated");
        alert.setContentText("The symptom has been successfully updated.");

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