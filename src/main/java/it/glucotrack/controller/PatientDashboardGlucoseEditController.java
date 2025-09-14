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

    // Callback per notificare il refresh dei dati
    private Runnable onDataUpdated;
    
    // Callback per gestire l'annullamento
    private Runnable onCancel;
    
    // La misurazione originale che stiamo modificando
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
        // Popola il ComboBox con le opzioni di tipo misurazione
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
        // Validazione in tempo reale per il campo valore
        valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                valueField.setText(oldValue);
            }
        });

        // Validazione per il campo tempo
        timeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("([01]?\\d|2[0-3]):?[0-5]?\\d?")) {
                timeField.setText(oldValue);
            }
        });
    }
    
    public void setupForEdit(PatientDashboardReadingsController.GlucoseReading reading) {
        this.originalReading = reading;
        
        // Carica i dati della misurazione nei campi del form
        datePicker.setValue(reading.getDateTime().toLocalDate());
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        timeField.setText(reading.getDateTime().format(timeFormatter));
        
        typeComboBox.setValue(reading.getType());
        valueField.setText(String.valueOf(reading.getValue()));
        
        // Carica la misurazione completa dal database per ottenere le note
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
            System.err.println("Errore nel caricamento delle note: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateEntry() {
        if (validateInput()) {
            try {
                // Crea la misurazione aggiornata
                GlucoseMeasurement updatedMeasurement = createMeasurementFromInput();
                
                if (originalMeasurement != null) {
                    updatedMeasurement.setId(originalMeasurement.getId());
                }

                // Aggiorna nel database
                boolean success = glucoseMeasurementDAO.updateGlucoseMeasurement(updatedMeasurement);

                if (success) {
                    showSuccessAlert();
                    
                    // Notifica il refresh dei dati al controller padre
                    if (onDataUpdated != null) {
                        onDataUpdated.run();
                    }
                } else {
                    showErrorAlert("Errore durante l'aggiornamento", "Non è stato possibile aggiornare la misurazione.");
                }

            } catch (SQLException e) {
                showErrorAlert("Errore Database", "Errore durante l'aggiornamento: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Validazione data
        if (datePicker.getValue() == null) {
            errors.append("- La data è obbligatoria\n");
        }

        // Validazione tempo
        if (timeField.getText().trim().isEmpty()) {
            errors.append("- L'ora è obbligatoria\n");
        } else {
            try {
                parseTime(timeField.getText());
            } catch (DateTimeParseException e) {
                errors.append("- Formato ora non valido (usa HH:mm)\n");
            }
        }

        // Validazione valore
        if (valueField.getText().trim().isEmpty()) {
            errors.append("- Il valore glicemico è obbligatorio\n");
        } else {
            try {
                float value = Float.parseFloat(valueField.getText());
                if (value <= 0 || value > 1000) {
                    errors.append("- Il valore deve essere tra 1 e 1000 mg/dL\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- Valore glicemico non valido\n");
            }
        }

        // Validazione tipo
        if (typeComboBox.getValue() == null || typeComboBox.getValue().trim().isEmpty()) {
            errors.append("- Il tipo di misurazione è obbligatorio\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Errori di Validazione", errors.toString());
            return false;
        }

        return true;
    }

    private LocalTime parseTime(String timeText) throws DateTimeParseException {
        // Aggiungi i : se mancano
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

        // Se le note sono vuote, usa il tipo come nota
        if (notes.isEmpty()) {
            notes = type;
        }

        int patientId = SessionManager.getInstance().getCurrentUserId();

        return new GlucoseMeasurement(patientId, dateTime, value, type, notes);
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successo");
        alert.setHeaderText("Misurazione Aggiornata");
        alert.setContentText("La misurazione glicemica è stata aggiornata con successo!");

        // Applica lo stile dark
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/assets/css/dashboard-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("alert");
        
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Errore");
        alert.setContentText(message);

        // Applica lo stile dark
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/assets/css/dashboard-styles.css").toExternalForm());
        dialogPane.getStyleClass().add("alert");
        
        alert.showAndWait();
    }

    // Setter per il callback di refresh dati
    public void setOnDataUpdated(Runnable onDataUpdated) {
        this.onDataUpdated = onDataUpdated;
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