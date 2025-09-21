package it.glucotrack.controller;

import it.glucotrack.util.SessionManager;
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

public class DoctorDashboardMedicationsEditController {

    @FXML private ComboBox<Patient> patientComboBox;
    @FXML private TextField medicationNameField;
    @FXML private TextField dosageField;
    @FXML private ComboBox<Frequency> frequencyComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea notesTextArea;
    @FXML private Button updateButton;
    @FXML private Button cancelButton;
    @FXML private Button backButton;

    private MedicationDAO medicationDAO;
    private PatientDAO patientDAO;
    private LogMedicationDAO logMedicationDAO;
    private Medication currentMedication; // The medication being edited
    private boolean isDataLoaded = false;

    private Runnable onCancel;
    private Runnable onDataUpdated;


    @FXML
    public void initialize() {
        medicationDAO = new MedicationDAO();
        patientDAO = new PatientDAO();
        logMedicationDAO = new LogMedicationDAO();

        setupComboBoxes();
        setupValidation();
    }

    /**
     * Set the medication to be edited
     * This method should be called after the controller is loaded
     */
    public void setMedicationToEdit(Medication medication) {
        this.currentMedication = medication;
        loadMedicationData();
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
                showError("No Patients Found", "No patients are registered in the system.");
                return;
            }

            patientComboBox.setItems(FXCollections.observableArrayList(patients));
            System.out.println("Patients loaded successfully into ComboBox");

            // If we're editing and have a current medication, select the correct patient
            if (currentMedication != null && !isDataLoaded) {
                selectPatientById(currentMedication.getPatient_id());
            }

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

    private void selectPatientById(int patientId) {
        for (Patient patient : patientComboBox.getItems()) {
            if (patient.getId() == patientId) {
                patientComboBox.setValue(patient);
                break;
            }
        }
    }

    private void loadMedicationData() {
        if (currentMedication == null) {
            showError("Error", "No medication selected for editing.");
            return;
        }

        try {
            // Populate form fields with current medication data
            medicationNameField.setText(currentMedication.getName_medication());
            dosageField.setText(currentMedication.getDose());
            frequencyComboBox.setValue(currentMedication.getFreq());
            startDatePicker.setValue(currentMedication.getStart_date());
            endDatePicker.setValue(currentMedication.getEnd_date());

            if (currentMedication.getInstructions() != null && !currentMedication.getInstructions().trim().isEmpty()) {
                notesTextArea.setText(currentMedication.getInstructions());
            }

            // Select the correct patient if patients are already loaded
            if (!patientComboBox.getItems().isEmpty()) {
                selectPatientById(currentMedication.getPatient_id());
            }

            isDataLoaded = true;
            System.out.println("✅ Medication data loaded successfully for editing");

        } catch (Exception e) {
            System.err.println("Error loading medication data: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "Failed to load medication data for editing.");
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

    @FXML
    private void handleUpdate() {
        if (!validateForm()) {
            return;
        }

        // Check if any changes were made
        if (!hasChanges()) {
            showInfo("No Changes", "No changes were detected. The medication remains unchanged.");
            return;
        }

        // Confirm update with user
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Update Medication");
        confirmAlert.setHeaderText("Are you sure you want to update this medication?");
        confirmAlert.setContentText("This action will update the medication details and may affect scheduled medication logs.");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // Store original values for comparison
            LocalDate originalStartDate = currentMedication.getStart_date();
            LocalDate originalEndDate = currentMedication.getEnd_date();
            Frequency originalFrequency = currentMedication.getFreq();

            // Update medication object with new values
            Patient selectedPatient = patientComboBox.getValue();
            currentMedication.setPatient_id(selectedPatient.getId());
            currentMedication.setName_medication(medicationNameField.getText().trim());
            currentMedication.setDose(dosageField.getText().trim());
            currentMedication.setFreq(frequencyComboBox.getValue());
            currentMedication.setStart_date(startDatePicker.getValue());
            currentMedication.setEnd_date(endDatePicker.getValue());
            currentMedication.setInstructions(notesTextArea.getText().trim().isEmpty() ? null : notesTextArea.getText().trim());

            // Update in database
            boolean success = medicationDAO.updateMedication(currentMedication, currentMedication.getPatient_id());

            if (success) {
                // Check if we need to recreate medication logs
                boolean needsLogUpdate = !originalStartDate.equals(currentMedication.getStart_date()) ||
                        !originalEndDate.equals(currentMedication.getEnd_date()) ||
                        !originalFrequency.equals(currentMedication.getFreq());

                if (needsLogUpdate) {
                    updateMedicationLogs(originalStartDate, originalEndDate, originalFrequency);
                }

                showSuccess("Medication updated successfully!" +
                        (needsLogUpdate ? "\nMedication schedule has been updated accordingly." : ""));
                navigateBackToMedicationsList();
            } else {
                showError("Error", "Failed to update medication in database.");
            }

        } catch (Exception e) {
            System.err.println("Error updating medication: " + e.getMessage());
            e.printStackTrace();
            showError("Error", "An unexpected error occurred while updating the medication.");
        }
    }

    private boolean hasChanges() {
        if (currentMedication == null) return false;

        Patient selectedPatient = patientComboBox.getValue();
        if (selectedPatient == null || selectedPatient.getId() != currentMedication.getPatient_id()) {
            return true;
        }

        String currentName = currentMedication.getName_medication() != null ? currentMedication.getName_medication() : "";
        String currentDosage = currentMedication.getDose() != null ? currentMedication.getDose() : "";
        String currentNotes = currentMedication.getInstructions() != null ? currentMedication.getInstructions() : "";

        String newName = medicationNameField.getText().trim();
        String newDosage = dosageField.getText().trim();
        String newNotes = notesTextArea.getText().trim();

        return !currentName.equals(newName) ||
                !currentDosage.equals(newDosage) ||
                !currentMedication.getFreq().equals(frequencyComboBox.getValue()) ||
                !currentMedication.getStart_date().equals(startDatePicker.getValue()) ||
                !currentMedication.getEnd_date().equals(endDatePicker.getValue()) ||
                !currentNotes.equals(newNotes);
    }

    private void updateMedicationLogs(LocalDate originalStartDate, LocalDate originalEndDate, Frequency originalFrequency) {
        try {
            // Delete existing future medication logs (from today onwards)
            LocalDate today = LocalDate.now();
            logMedicationDAO.deleteFutureLogMedications(currentMedication.getId(), today);

            // Create new medication logs based on updated schedule
            createLogMedications(currentMedication.getId(), currentMedication, today);

            System.out.println("✅ Medication logs updated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error updating medication logs: " + e.getMessage());
            showError("Warning", "Medication updated but there was an issue updating the medication schedule. Please check the logs manually.");
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

    private void createLogMedications(int medicationId, Medication medication, LocalDate startFrom) {
        try {
            LocalDate startDate = startFrom.isAfter(medication.getStart_date()) ? startFrom : medication.getStart_date();
            LocalDate endDate = medication.getEnd_date();
            String frequency = medication.getFreq().toString();

            LocalDate currentDate = startDate;

            while (!currentDate.isAfter(endDate)) {
                switch (frequency.toLowerCase()) {
                    case "once a day":
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        currentDate = currentDate.plusDays(1);
                        break;

                    case "twice a day":
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        createLogMedicationEntry(medicationId, currentDate, "21:00");
                        currentDate = currentDate.plusDays(1);
                        break;

                    case "three times a day":
                        createLogMedicationEntry(medicationId, currentDate, "08:00");
                        createLogMedicationEntry(medicationId, currentDate, "14:00");
                        createLogMedicationEntry(medicationId, currentDate, "20:00");
                        currentDate = currentDate.plusDays(1);
                        break;

                    case "every 6 hours":
                        createLogMedicationEntry(medicationId, currentDate, "06:00");
                        createLogMedicationEntry(medicationId, currentDate, "12:00");
                        createLogMedicationEntry(medicationId, currentDate, "18:00");
                        createLogMedicationEntry(medicationId, currentDate, "00:00");
                        currentDate = currentDate.plusDays(1);
                        break;

                    case "every 8 hours":
                        createLogMedicationEntry(medicationId, currentDate, "06:00");
                        createLogMedicationEntry(medicationId, currentDate, "14:00");
                        createLogMedicationEntry(medicationId, currentDate, "22:00");
                        currentDate = currentDate.plusDays(1);
                        break;

                    case "once a week":
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        currentDate = currentDate.plusWeeks(1);
                        break;

                    case "every other day":
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        currentDate = currentDate.plusDays(2);
                        break;

                    default:
                        createLogMedicationEntry(medicationId, currentDate, "09:00");
                        currentDate = currentDate.plusDays(1);
                        break;
                }
            }

            System.out.println("✅ Updated medication logs from " + startDate + " to " + endDate + " with frequency: " + frequency);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create medication schedule: " + e.getMessage());
        }
    }

    private void createLogMedicationEntry(int medicationId, LocalDate date, String time) {
        try {
            LogMedication logMedication = new LogMedication();
            logMedication.setMedication_id(medicationId);
            logMedication.setDateAndTime(LocalDateTime.parse(date + "T" + time + ":00"));
            logMedication.setTaken(false);

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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleCancel() {
        // Check if there are unsaved changes
        if (hasChanges()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Discard Changes");
            confirmAlert.setHeaderText("You have unsaved changes.");
            confirmAlert.setContentText("Are you sure you want to discard your changes and go back?");

            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        navigateBackToMedicationsList();
    }

    @FXML
    private void handleBackToList() {
        handleCancel(); // Same logic as cancel - check for changes first
    }

    public void setOnDataUpdated(Runnable onDataUpdated) {
        this.onDataUpdated = onDataUpdated;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }


}