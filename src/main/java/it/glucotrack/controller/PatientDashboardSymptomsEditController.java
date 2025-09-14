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
    
    // Callback per quando i dati vengono aggiornati con successo
    private Runnable onDataUpdated;
    
    // Callback per quando si preme cancel
    private Runnable onCancel;
    
    // Il sintomo originale che stiamo modificando
    private Symptom originalSymptom;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        symptomDAO = new SymptomDAO();
        setupValidation();
        setupComboBox();
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
        // Setup per i ComboBox di ore e minuti
        for (int i = 0; i <= 23; i++) {
            hoursComboBox.getItems().add(i);
        }
        for (int i = 0; i <= 59; i++) {
            minutesComboBox.getItems().add(i);
        }
        hoursComboBox.setValue(0);
        minutesComboBox.setValue(0);

        // Limita la lunghezza del nome del sintomo
        symptomDescriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 500) {
                symptomDescriptionArea.setText(oldValue);
            }
        });

        // Limita la lunghezza delle note
        notesArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 1000) {
                notesArea.setText(oldValue);
            }
        });
    }
    
    public void setupForEdit(Symptom symptom) {
        this.originalSymptom = symptom;
        
        // Carica i dati del sintomo nei campi del form
        LocalDateTime dateTime = symptom.getDateAndTime();
        
        // Imposta la data in formato MM/dd/yyyy
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        dateField.setText(dateTime.format(dateFormatter));
        
        // Imposta l'ora in formato 12 ore
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        timeField.setText(dateTime.format(timeFormatter));
        
        // Carica gli altri campi
        symptomDescriptionArea.setText(symptom.getSymptomName());
        severityComboBox.setValue(symptom.getGravity());
        
        // Imposta la durata nei ComboBox
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
                // Crea il sintomo aggiornato
                Symptom updatedSymptom = createSymptomFromInput();
                updatedSymptom.setId(originalSymptom.getId());
                updatedSymptom.setPatient_id(originalSymptom.getPatient_id());

                // Aggiorna nel database
                boolean success = symptomDAO.updateSymptom(updatedSymptom);

                if (success) {
                    showSuccessAlert();
                    
                    // Notifica il refresh dei dati al controller padre
                    if (onDataUpdated != null) {
                        onDataUpdated.run();
                    }
                } else {
                    showErrorAlert("Errore durante l'aggiornamento", "Non è stato possibile aggiornare il sintomo.");
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
        alert.setTitle("Successo");
        alert.setHeaderText("Sintomo Aggiornato");
        alert.setContentText("Il sintomo è stato aggiornato con successo!");

        // Applica lo stile dark
        try {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/assets/css/dashboard-styles.css").toExternalForm());
            dialogPane.getStyleClass().add("alert");
        } catch (Exception e) {
            System.err.println("Impossibile applicare lo stile al dialog: " + e.getMessage());
        }
        
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Errore");
        alert.setContentText(message);

        // Applica lo stile dark
        try {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/assets/css/dashboard-styles.css").toExternalForm());
            dialogPane.getStyleClass().add("alert");
        } catch (Exception e) {
            System.err.println("Impossibile applicare lo stile al dialog: " + e.getMessage());
        }
        
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