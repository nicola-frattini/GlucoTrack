package it.glucotrack.controller;

import it.glucotrack.model.Alert;
import it.glucotrack.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import it.glucotrack.model.User;
import it.glucotrack.model.Patient;
import it.glucotrack.model.GlucoseMeasurement;
import it.glucotrack.model.Medication;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class DoctorDashboardHomeController {
    @FXML
    private TableView<String[]> patientTable;

    @FXML
    private ListView<String> alertsList;
    
    @FXML
    private Button managePatientsBtn;
    
    @FXML
    private Button manageMedicationsBtn;
    
    @FXML
    private Button viewReportsBtn;
    
    @FXML
    private Button newPrescriptionBtn;

    @FXML
    private VBox alertsContainer;

    private int doctorId;
    private GlucoseMeasurementDAO glucoseMeasurementDAO;



    @FXML
    public void initialize() throws SQLException {
        setupPatientTable();
        loadSummaryWithLatestPatients();
        doctorId = 1;
        loadAlerts();
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    private void loadAlerts() throws SQLException {

        alertsContainer.getChildren().clear();

        List<it.glucotrack.model.Alert> alerts = AlertManagement.generateDoctorAlerts(this.doctorId);


        for (it.glucotrack.model.Alert alert : alerts) {
            HBox alertBox = createAlertBox(alert);
            alertsContainer.getChildren().add(alertBox);
        }
    }

    private HBox createAlertBox(Alert alert) {
        HBox box = new HBox(10);
        box.setStyle("-fx-background-radius: 10; -fx-padding: 15;");

        // Colore di sfondo in base al tipo di alert
        switch (alert.getType()) {
            case INFO:
                box.setStyle(box.getStyle() + "-fx-background-color: #0f1c35;");
            case WARNING:
                box.setStyle(box.getStyle() + "-fx-background-color: #2d1b1b; -fx-border-color: #ff9800; -fx-border-width: 1;");
            case CRITICAL:
                box.setStyle(box.getStyle() + "-fx-background-color: #2d1b1b; -fx-border-color: #f44336; -fx-border-width: 1;");
        }
        Label icon = new Label();
        switch(alert.getType()) {
            case INFO : icon.setText("ℹ️");
            case WARNING : icon.setText("⚠️");
            case CRITICAL : icon.setText("❗");
        }
        icon.setStyle("-fx-font-size: 16px;");

        VBox content = new VBox(5);
        Label title = new Label(alert.getMessage());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label patientName = new Label(alert.getPatient().getNameAndSurname());
        patientName.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        Label date = new Label(alert.getDate().toLocalDate().toString());
        date.setStyle("-fx-text-fill: #8892b0; -fx-font-size: 12px;");

        content.getChildren().addAll(title,patientName, date);

        box.getChildren().addAll(icon, content);
        return box;
    }



    @SuppressWarnings("unchecked")
    private void setupPatientTable() {
    // Colonne già definite in FXML
    TableColumn<String[], String> nameCol = (TableColumn<String[], String>) patientTable.getColumns().get(0);
    TableColumn<String[], String> readingCol = (TableColumn<String[], String>) patientTable.getColumns().get(1);
    TableColumn<String[], String> adherenceCol = (TableColumn<String[], String>) patientTable.getColumns().get(2);
    TableColumn<String[], String> statusCol = (TableColumn<String[], String>) patientTable.getColumns().get(3);

    nameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[0]));
    readingCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[1]));
    adherenceCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[2]));
    statusCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[3]));
    }

    private void loadSummaryWithLatestPatients() {
        try {
            int doctorId = SessionManager.getInstance().getCurrentUserId();
            PatientDAO patientDAO = new PatientDAO();
            GlucoseMeasurementDAO glucoseDAO = new GlucoseMeasurementDAO();
            MedicationDAO medicationDAO = new MedicationDAO();
            LogMedicationDAO logMedicationDAO = new LogMedicationDAO();

            // Prendi solo i pazienti associati a questo dottore
            List<Patient> doctorPatients = patientDAO.getPatientsByDoctorId(this.doctorId);


            System.out.println("Trovati " + doctorPatients.size() + " pazienti per il dottore con ID " + doctorId);


            // Per ogni paziente, prendi l'ultima misurazione e la inserisce in un hashmap

            // HashMap dove la chiave è l'ID del paziente e il valore è l'ultima misurazione
            Map<Integer, GlucoseMeasurement> latestMeasurementsMap = new HashMap<>();

            if (doctorPatients != null) {
                for (Patient patient : doctorPatients) {
                    // Prendi l'ultima misurazione del paziente
                    GlucoseMeasurement lastMeasurement = glucoseMeasurementDAO.getLatestMeasurementByPatientId(patient.getId());
                    if (lastMeasurement != null) {
                        latestMeasurementsMap.put(patient.getId(), lastMeasurement);
                    }
                }
            }

            // Ordina per data ultima misurazione (decrescente) e prendi i primi 4
            List<GlucoseMeasurement> top4Measurements = latestMeasurementsMap.values().stream()
                    .sorted((m1, m2) -> m2.getDateAndTime().compareTo(m1.getDateAndTime())) // decrescente
                    .limit(4)
                    .collect(Collectors.toList());

            // Crea la lista di 4 pazienti da inserire nella tabella patient name = name surname, last reading = date time - value, adherence = % calcolata, status = from glucose value
            ObservableList<String[]> tableData = FXCollections.observableArrayList();
            for (GlucoseMeasurement measurement : top4Measurements) {
                Patient patient = doctorPatients.stream()
                        .filter(p -> p.getId() == measurement.getPatientId())
                        .findFirst()
                        .orElse(null);
                if (patient != null) {
                    String name = patient.getName() + " " + patient.getSurname();
                    String lastReading = measurement.getDateAndTime().toLocalDate().toString() + " " +
                            measurement.getDateAndTime().toLocalTime().toString() + " - " +
                            measurement.getGlucoseLevel() + " mg/dL";
                    double adherencePercent = calculateMedicationAdherence(patient.getId(), medicationDAO, logMedicationDAO);
                    String adherence = String.format("%.1f%%", adherencePercent);
                    String status = getStatusFromGlucose(measurement.getGlucoseLevel());
                    tableData.add(new PatientSummaryRow(name, lastReading, adherence, status).toArray());
                }


                patientTable.getItems().addAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private double calculateMedicationAdherence(int patientId, MedicationDAO medicationDAO, LogMedicationDAO logMedicationDAO) {
        try {
            java.util.List<Medication> meds = medicationDAO.getActiveMedicationsByPatientId(patientId);
            if (meds.isEmpty()) return 100.0;
            double total = 0, count = 0;
            for (Medication med : meds) {
                java.util.List<it.glucotrack.model.LogMedication> logs = logMedicationDAO.getLogMedicationsByMedicationIdUpToNow(med.getId());
                if (logs.isEmpty()) continue;
                long taken = logs.stream().filter(it.glucotrack.model.LogMedication::isTaken).count();
                total += (double)taken / logs.size();
                count++;
            }
            return count > 0 ? (total / count) * 100.0 : 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String getStatusFromGlucose(float value) {
        if (value < 70) return "Low";
        if (value > 180) return "High";
        return "Normal";
    }

    private static class PatientSummaryRow {
        String name, lastReading, adherence, status;
        PatientSummaryRow(String n, String l, String a, String s) { name=n; lastReading=l; adherence=a; status=s; }
        String[] toArray() { return new String[]{name, lastReading, adherence, status}; }
    }
    
    @FXML
    private void onManagePatientsClick() {
        // TODO: Navigate to patients management page
        System.out.println("Navigate to Patients Management");
    }
    
    @FXML
    private void onManageMedicationsClick() {
        try {
            // Find the parent StackPane and load the medications view
            javafx.scene.layout.StackPane contentPane = findContentPane();
            if (contentPane != null) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/DoctorDashboardMedications.fxml"));
                javafx.scene.Node medicationsView = loader.load();
                contentPane.getChildren().clear();
                contentPane.getChildren().add(medicationsView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error navigating to medications page: " + e.getMessage());
        }
    }
    
    @FXML
    private void onViewReportsClick() {
        // TODO: Navigate to reports page
        System.out.println("Navigate to Reports");
    }
    
    @FXML
    private void onNewPrescriptionClick() {
        try {
            // Find the parent StackPane and load the new prescription view
            javafx.scene.layout.StackPane contentPane = findContentPane();
            if (contentPane != null) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/DoctorDashboardMedicationsInsert.fxml"));
                javafx.scene.Node prescriptionView = loader.load();
                contentPane.getChildren().clear();
                contentPane.getChildren().add(prescriptionView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error navigating to new prescription page: " + e.getMessage());
        }
    }
    
    private javafx.scene.layout.StackPane findContentPane() {
        // Find the StackPane in the parent hierarchy
        javafx.scene.Node current = patientTable.getScene().getRoot();
        return findStackPaneRecursively(current);
    }
    
    private javafx.scene.layout.StackPane findStackPaneRecursively(javafx.scene.Node node) {
        if (node instanceof javafx.scene.layout.StackPane && 
            ((javafx.scene.layout.StackPane) node).getId() != null && 
            ((javafx.scene.layout.StackPane) node).getId().equals("contentPane")) {
            return (javafx.scene.layout.StackPane) node;
        }
        
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                javafx.scene.layout.StackPane result = findStackPaneRecursively(child);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
}
