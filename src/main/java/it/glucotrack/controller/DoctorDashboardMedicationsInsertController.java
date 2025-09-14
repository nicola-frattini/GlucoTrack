package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import it.glucotrack.model.Frequency;
import it.glucotrack.model.Medication;
import it.glucotrack.model.Patient;
import it.glucotrack.util.MedicationDAO;
import it.glucotrack.util.PatientDAO;
import it.glucotrack.util.LogMedicationDAO;
import it.glucotrack.model.LogMedication;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DoctorDashboardMedicationsInsertController {

    @FXML private ComboBox<Patient> patientComboBox;
    @FXML private TextField medicationNameField;
    @FXML private TextField dosageField;
    @FXML private ComboBox<Frequency> frequencyComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea notesTextArea;
    @FXML private Button prescribeButton;
    @FXML private Button clearButton;
    @FXML private Button backButton;

    private MedicationDAO medicationDAO;
    private PatientDAO patientDAO;
    private LogMedicationDAO logMedicationDAO;

    @FXML
    public void initialize() {
        medicationDAO = new MedicationDAO();
        patientDAO = new PatientDAO();
        logMedicationDAO = new LogMedicationDAO();
        
        setupComboBoxes();
        setupValidation();
        setupDefaultValues();
    }

    private void setupComboBoxes() {
        // Setup frequency ComboBox
        frequencyComboBox.setItems(FXCollections.observableArrayList(Frequency.values()));
        frequencyComboBox.setCellFactory(param -> new ListCell<Frequency>() {
            @Override
            protected void updateItem(Frequency item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        frequencyComboBox.setButtonCell(new ListCell<Frequency>() {
            @Override
            protected void updateItem(Frequency item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });

        // Setup patient ComboBox
        loadPatients();
        patientComboBox.setCellFactory(param -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " " + item.getSurname() + " (ID: " + item.getId() + ")");
                }
            }
        });
        patientComboBox.setButtonCell(new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " " + item.getSurname());
                }
            }
        });
    }

    private void loadPatients() {
        try {
            System.out.println("Loading patients from database...");
            List<Patient> patients = patientDAO.getAllPatients();
            System.out.println("Found " + patients.size() + " patients");
            
            if (patients.isEmpty()) {
                System.out.println("No patients found in database");
                showError("No Patients Found", "No patients are registered in the system. Please add patients first.");
                return;
            }
            
            patientComboBox.setItems(FXCollections.observableArrayList(patients));
            System.out.println("Patients loaded successfully into ComboBox");
            
        } catch (SQLException e) {
            System.err.println("Error loading patients: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Could not load patient list from database: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error loading patients: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "An unexpected error occurred while loading patients: " + e.getMessage());
        }
    }

    private void setupValidation() {
        // Add validation listeners
        medicationNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 100) {
                medicationNameField.setText(oldValue);
            }
        });

        dosageField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 50) {
                dosageField.setText(oldValue);
            }
        });

        notesTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 500) {
                notesTextArea.setText(oldValue);
            }
        });
    }

    private void setupDefaultValues() {
        // Set default start date to today
        startDatePicker.setValue(LocalDate.now());
        
        // Set default frequency
        frequencyComboBox.setValue(Frequency.ONCE_A_DAY);
    }

    @FXML
    private void handlePrescribe() {
        if (!validateForm()) {
            return;
        }

        try {
            Patient selectedPatient = patientComboBox.getValue();
            String medicationName = medicationNameField.getText().trim();
            String dosage = dosageField.getText().trim();
            Frequency frequency = frequencyComboBox.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            String notes = notesTextArea.getText().trim();

            // Create medication object
            Medication medication = new Medication(
                    selectedPatient.getId(),
                    medicationName,
                    dosage,
                    frequency,
                    startDate,
                    endDate,
                    notes.isEmpty() ? null : notes
            );

            // Save to database and get the ID
            int medicationId = medicationDAO.insertMedicationAndGetId(medication);
            medication.setId(medicationId); // Set the ID in our object
            
            if (medicationId > 0) {
                // Create log medications based on frequency and date range
                createLogMedications(medicationId, medication);
                
                showSuccess("Medication prescribed successfully!\nSchedule logs have been created for the patient.");
                clearForm();
                // Navigate back to medications list
                navigateBackToMedicationsList();
            } else {
                showError("Error", "Failed to save medication to database.");
            }

        } catch (Exception e) {
            System.err.println("Error prescribing medication: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "An unexpected error occurred while prescribing the medication.");
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (patientComboBox.getValue() == null) {
            errors.append("• Please select a patient\n");
        }

        if (medicationNameField.getText().trim().isEmpty()) {
            errors.append("• Please enter medication name\n");
        }

        if (dosageField.getText().trim().isEmpty()) {
            errors.append("• Please enter dosage\n");
        }

        if (frequencyComboBox.getValue() == null) {
            errors.append("• Please select frequency\n");
        }

        if (startDatePicker.getValue() == null) {
            errors.append("• Please select start date\n");
        }

        if (endDatePicker.getValue() != null && startDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                errors.append("• End date cannot be before start date\n");
            }
        }

        if (errors.length() > 0) {
            showError("Validation Error", "Please fix the following errors:\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    private void clearForm() {
        patientComboBox.setValue(null);
        medicationNameField.clear();
        dosageField.clear();
        frequencyComboBox.setValue(Frequency.ONCE_A_DAY);
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(null);
        notesTextArea.clear();
    }

    private void createLogMedications(int medicationId, Medication medication) {
        try {
            LocalDate startDate = medication.getStart_date();
            LocalDate endDate = medication.getEnd_date();
            String frequency = medication.getFreq().toString();
            
            LocalDate currentDate = startDate;
            
            while (!currentDate.isAfter(endDate)) {
                switch (frequency.toLowerCase()) {
                    case "once a day":
                        // Create one log per day
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        currentDate = currentDate.plusDays(1);
                        break;
                        
                    case "twice a day":
                        // Create two logs per day
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        createLogMedicationEntry(medicationId, currentDate, "21:00");
                        currentDate = currentDate.plusDays(1);
                        break;
                        
                    case "three times a day":
                        // Create three logs per day
                        createLogMedicationEntry(medicationId, currentDate, "08:00");
                        createLogMedicationEntry(medicationId, currentDate, "14:00");
                        createLogMedicationEntry(medicationId, currentDate, "20:00");
                        currentDate = currentDate.plusDays(1);
                        break;
                        
                    case "once a week":
                        // Create one log per week (same day of week as start date)
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        currentDate = currentDate.plusWeeks(1);
                        break;
                        
                    case "every other day":
                        // Create one log every two days
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        currentDate = currentDate.plusDays(2);
                        break;
                        
                    default:
                        // Default to daily if frequency not recognized
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        currentDate = currentDate.plusDays(1);
                        break;
                }
            }
            
            System.out.println("✅ Created medication logs from " + startDate + " to " + endDate + " with frequency: " + frequency);
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to create medication schedule: " + e.getMessage());
        }
    }
    
    private void createLogMedicationEntry(int medicationId, LocalDate date, String time) {
        try {
            LogMedication logMedication = new LogMedication();
            logMedication.setMedication_id(medicationId);
            logMedication.setDateAndTime(LocalDateTime.parse(date + "T" + time + ":00")); // Format: YYYY-MM-DDTHH:MM:SS
            logMedication.setTaken(false); // Default to not taken
            
            boolean success = logMedicationDAO.insertLogMedication(logMedication);
            if (!success) {
                System.err.println("❌ Failed to insert log medication for date: " + date + " time: " + time);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error creating log medication entry: " + e.getMessage());
        }
    }

    private void navigateBackToMedicationsList() {
        try {
            StackPane contentPane = findContentPane();
            if (contentPane != null) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/DoctorDashboardMedications.fxml"));
                Node medicationsView = loader.load();
                contentPane.getChildren().clear();
                contentPane.getChildren().add(medicationsView);
                System.out.println("✅ Successfully navigated back to medications list");
            } else {
                System.err.println("❌ Could not find contentPane for navigation");
                showError("Navigation Error", "Could not navigate back to medications list. Please use the sidebar navigation.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error navigating back to medications list: " + e.getMessage());
            e.printStackTrace();
            showError("Navigation Error", "Could not navigate back to medications list: " + e.getMessage());
        }
    }

    private StackPane findContentPane() {
        Node current = medicationNameField.getScene().getRoot();
        return findStackPaneRecursively(current);
    }

    private StackPane findStackPaneRecursively(Node node) {
        if (node instanceof StackPane && 
            ((StackPane) node).getId() != null && 
            ((StackPane) node).getId().equals("contentPane")) {
            return (StackPane) node;
        }
        
        if (node instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                StackPane result = findStackPaneRecursively(child);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void handleClear() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Clear Form");
        confirmAlert.setHeaderText("Are you sure you want to clear all fields?");
        confirmAlert.setContentText("All entered data will be lost.");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            clearForm();
        }
    }
    
    @FXML
    private void handleBackToList() {
        navigateBackToMedicationsList();
    }
}
