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
    private TextField durationField;

    @FXML
    private TextArea notesArea;

    @FXML
    private Button logSymptomButton;

    private SymptomDAO symptomDAO;
    
    // Callback per quando i dati vengono salvati con successo
    private Runnable onDataSaved;
    
    // Callback per quando si preme cancel
    private Runnable onCancel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        symptomDAO = new SymptomDAO();

        // Inizializza i valori di default
        setupDefaultValues();
        setupValidation();
        setupComboBox();
    }

    private void setupDefaultValues() {
        // Imposta la data corrente in formato MM/dd/yyyy
        LocalDate now = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        dateField.setText(now.format(dateFormatter));

        // Imposta l'ora corrente in formato 12 ore
        LocalTime nowTime = LocalTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        timeField.setText(nowTime.format(timeFormatter));
    }

    private void setupComboBox() {
        // Popola il ComboBox con i livelli di severità
        severityComboBox.setItems(FXCollections.observableArrayList(
                "Mild",
                "Moderate",
                "Severe",
                "Very Severe"
        ));

        // Imposta il placeholder text
        severityComboBox.setPromptText("Select Severity");
    }

    private void setupValidation() {
        // Validazione per il campo durata (solo numeri e testo descrittivo)
        durationField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 50) {
                durationField.setText(oldValue);
            }
        });

        // Limita la lunghezza del nome del sintomo
        symptomDescriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 500) {
                symptomDescriptionArea.setText(oldValue);
            }
        });

        // Limita la lunghezza delle note
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
                // Salva il sintomo nel database
                boolean success = saveSymptom();

                if (success) {
                    showSuccessAlert();
                    clearForm();
                    
                    // Chiamare il callback se i dati sono stati salvati con successo
                    if (onDataSaved != null) {
                        onDataSaved.run();
                    }
                } else {
                    showErrorAlert("Errore durante il salvataggio", "Non è stato possibile salvare il sintomo.");
                }

            } catch (SQLException e) {
                showErrorAlert("Errore Database", "Errore durante il salvataggio: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                showErrorAlert("Errore", "Si è verificato un errore: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Validazione data
        if (dateField.getText().trim().isEmpty()) {
            errors.append("- La data è obbligatoria\n");
        } else {
            try {
                parseDate(dateField.getText());
            } catch (DateTimeParseException e) {
                errors.append("- Formato data non valido (usa MM/dd/yyyy)\n");
            }
        }

        // Validazione tempo
        if (timeField.getText().trim().isEmpty()) {
            errors.append("- L'ora è obbligatoria\n");
        } else {
            try {
                parseTime(timeField.getText());
            } catch (DateTimeParseException e) {
                errors.append("- Formato ora non valido (usa hh:mm AM/PM)\n");
            }
        }

        // Validazione nome sintomo
        if (symptomDescriptionArea.getText().trim().isEmpty()) {
            errors.append("- Il nome del sintomo è obbligatorio\n");
        }

        // Validazione severità
        if (severityComboBox.getValue() == null || severityComboBox.getValue().trim().isEmpty()) {
            errors.append("- Il livello di severità è obbligatorio\n");
        }

        // Durata è completamente opzionale

        if (errors.length() > 0) {
            showErrorAlert("Errori di Validazione", errors.toString());
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
        // Parsing della data e ora
        LocalDate date = parseDate(dateField.getText());
        LocalTime time = parseTime(timeField.getText());
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        String symptomDescription = symptomDescriptionArea.getText().trim();
        String severity = severityComboBox.getValue();
        String durationText = durationField.getText().trim();
        String notes = notesArea.getText().trim();

        // Crea un oggetto Symptom del modello
        it.glucotrack.model.Symptom symptom = new it.glucotrack.model.Symptom();
        symptom.setSymptomName(symptomDescription);
        symptom.setGravity(severity);
        symptom.setNotes(notes);
        symptom.setDateAndTime(dateTime);
        
        // Parsing della durata - può essere formato "HH:mm" o testo descrittivo
        LocalTime durationTime;
        if (durationText.isEmpty()) {
            durationTime = LocalTime.of(0, 0); // Default: 0 minuti
        } else {
            try {
                // Prova a parsare come formato HH:mm
                if (durationText.matches("\\d{1,2}:\\d{2}")) {
                    durationTime = LocalTime.parse(durationText);
                } else {
                    // Se è testo descrittivo, prova a convertire in tempo approssimativo
                    durationTime = parseDescriptiveDuration(durationText);
                }
            } catch (Exception e) {
                // Se fallisce, usa default di 30 minuti
                durationTime = LocalTime.of(0, 30);
            }
        }
        symptom.setDuration(durationTime);

        int patientId = SessionManager.getInstance().getCurrentUser().getId();
        
        // Usa il nuovo metodo che inserisce tutti i campi separati
        return symptomDAO.insertSymptom(patientId, symptom);
    }
    
    // Metodo helper per convertire durata descrittiva in LocalTime
    private LocalTime parseDescriptiveDuration(String description) {
        String lower = description.toLowerCase();
        
        // Cerca pattern comuni
        if (lower.contains("minuto") || lower.contains("minute")) {
            if (lower.contains("30")) return LocalTime.of(0, 30);
            if (lower.contains("15")) return LocalTime.of(0, 15);
            if (lower.contains("45")) return LocalTime.of(0, 45);
            return LocalTime.of(0, 30); // Default per minuti
        }
        
        if (lower.contains("ora") || lower.contains("hour")) {
            if (lower.contains("1")) return LocalTime.of(1, 0);
            if (lower.contains("2")) return LocalTime.of(2, 0);
            if (lower.contains("3")) return LocalTime.of(3, 0);
            return LocalTime.of(1, 0); // Default per ore
        }
        
        // Default fallback
        return LocalTime.of(0, 30);
    }

    private void clearForm() {
        // Reset della data e ora correnti
        setupDefaultValues();

        // Pulisci tutti i campi
        symptomDescriptionArea.clear();
        severityComboBox.setValue(null);
        severityComboBox.setPromptText("Select Severity");
        durationField.clear();
        notesArea.clear();
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successo");
        alert.setHeaderText("Sintomo Registrato");
        alert.setContentText("Il sintomo è stato registrato con successo nel tuo diario medico!");

        // Stile dell'alert per matchare il tema scuro
        alert.getDialogPane().setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Errore");
        alert.setContentText(message);

        // Stile dell'alert per matchare il tema scuro
        alert.getDialogPane().setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        alert.showAndWait();
    }

    // Metodo per chiudere la finestra (se necessario)
    public void closeWindow() {
        Stage stage = (Stage) logSymptomButton.getScene().getWindow();
        stage.close();
    }

    // Metodi di utilità per ottenere i valori correnti (utile per testing o integrazione)
    public String getCurrentSymptomName() {
        return symptomDescriptionArea.getText().trim();
    }

    public String getCurrentSeverity() {
        return severityComboBox.getValue();
    }

    public String getCurrentDuration() {
        return durationField.getText().trim();
    }

    public String getCurrentNotes() {
        return notesArea.getText().trim();
    }
    
    // Metodi per impostare i callback
    public void setOnDataSaved(Runnable onDataSaved) {
        this.onDataSaved = onDataSaved;
    }
    
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }
    
    // Metodo per gestire il cancel
    @FXML
    private void handleCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }
}