package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import it.glucotrack.model.GlucoseMeasurement;
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

public class PatientDashboardGlucoseInsertController implements Initializable {

    // Callback per notificare il refresh dei dati
    private Runnable onDataSaved;
    
    // Callback per gestire l'annullamento
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

        // Inizializza i valori di default
        setupDefaultValues();
        setupValidation();
        setupComboBox();
    }

    private void setupDefaultValues() {
        // Imposta la data corrente
        datePicker.setValue(LocalDate.now());

        // Imposta l'ora corrente
        LocalTime now = LocalTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        timeField.setText(now.format(timeFormatter));
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

        // Imposta il valore di default
        typeComboBox.setValue("Before Breakfast");
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

    @FXML
    private void handleSaveEntry() {
        if (validateInput()) {
            try {
                // Crea la misurazione
                GlucoseMeasurement measurement = createMeasurementFromInput();

                // Salva nel database
                boolean success = glucoseMeasurementDAO.insertGlucoseMeasurement(measurement);

                if (success) {
                    showSuccessAlert();
                    clearForm();
                    
                    // Notifica il refresh dei dati al controller padre
                    if (onDataSaved != null) {
                        onDataSaved.run();
                    }
                } else {
                    showErrorAlert("Errore durante il salvataggio", "Non è stato possibile salvare la misurazione.");
                }

            } catch (SQLException e) {
                showErrorAlert("Errore Database", "Errore durante il salvataggio: " + e.getMessage());
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

        // Determina se è prima del pasto basandosi sul tipo
        boolean beforeMeal = type.toLowerCase().contains("before") || type.equals("Fasting");

        // Se le note sono vuote, usa il tipo come nota
        if (notes.isEmpty()) {
            notes = type;
        }

        int patientId = SessionManager.getInstance().getCurrentUserId();

        return new GlucoseMeasurement(patientId, dateTime, value, beforeMeal, notes);
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
        alert.setTitle("Successo");
        alert.setHeaderText("Misurazione Salvata");
        alert.setContentText("La misurazione glicemica è stata salvata con successo!");

        // Stile dell'alert
        alert.getDialogPane().setStyle("-fx-background-color: #34495e;");
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Errore");
        alert.setContentText(message);

        // Stile dell'alert
        alert.getDialogPane().setStyle("-fx-background-color: #34495e;");
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