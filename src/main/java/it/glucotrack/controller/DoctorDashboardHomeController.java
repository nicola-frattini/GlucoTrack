package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import it.glucotrack.util.SessionManager;
import it.glucotrack.util.UserDAO;
import it.glucotrack.util.GlucoseMeasurementDAO;
import it.glucotrack.util.MedicationDAO;
import it.glucotrack.util.LogMedicationDAO;
import it.glucotrack.model.User;
import it.glucotrack.model.GlucoseMeasurement;
import it.glucotrack.model.Medication;
import java.util.Comparator;
import java.util.stream.Collectors;

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
    public void initialize() {
        setupPatientTable();
        loadSummaryWithLatestPatients();
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
            UserDAO userDAO = new UserDAO();
            GlucoseMeasurementDAO glucoseDAO = new GlucoseMeasurementDAO();
            MedicationDAO medicationDAO = new MedicationDAO();
            LogMedicationDAO logMedicationDAO = new LogMedicationDAO();

            // Prendi solo i pazienti associati a questo dottore
            java.util.List<User> doctorPatients = userDAO.getPatientsByDoctorId(doctorId);

            // Per ogni paziente, prendi l'ultima misurazione
            java.util.List<PatientSummaryRow> summaryRows = new java.util.ArrayList<>();
            for (User p : doctorPatients) {
                GlucoseMeasurement last = glucoseDAO.getLatestGlucoseMeasurement(p.getId());
                if (last != null) {
                    double adherence = calculateMedicationAdherence(p.getId(), medicationDAO, logMedicationDAO);
                    summaryRows.add(new PatientSummaryRow(
                        p.getName() + " " + p.getSurname(),
                        last.getDateAndTime().toString() + " - " + (int)last.getGlucoseLevel() + " mg/dL",
                        String.format("%.0f%%", adherence),
                        getStatusFromGlucose(last.getGlucoseLevel())
                    ));
                }
            }
            // Ordina per data ultima misurazione (decrescente)
            summaryRows.sort(Comparator.comparing((PatientSummaryRow r) -> r.lastReading).reversed());
            // Prendi i primi 4
            ObservableList<String[]> patients = FXCollections.observableArrayList(
                summaryRows.stream().limit(4).map(PatientSummaryRow::toArray).collect(Collectors.toList())
            );
            patientTable.setItems(patients);
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
