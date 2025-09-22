package it.glucotrack.controller;

import it.glucotrack.model.*;
import it.glucotrack.model.Alert;
import it.glucotrack.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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
    private ListView<String> alertsList;

    @FXML
    private ComboBox<String> severityFilterCombo;

    @FXML
    private ComboBox<String> patientFilterCombo;

    @FXML
    private Button newPrescriptionBtn;

    @FXML
    private VBox alertsContainer;

    private int doctorId;
    private GlucoseMeasurementDAO glucoseMeasurementDAO;

    private List<Alert> allAlerts;
    private Map<Integer, Patient> patientMap;

    @FXML
    public void initialize() throws SQLException {

        doctorId = SessionManager.getCurrentUser().getId();

        List<Patient> patients = new PatientDAO().getPatientsByDoctorId(doctorId);
        patientMap = patients.stream().collect(Collectors.toMap(Patient::getId, p -> p));

        patientFilterCombo.setItems(FXCollections.observableArrayList("All Patients"));
        patients.forEach(p -> patientFilterCombo.getItems().add(p.getName() + " " + p.getSurname()));
        patientFilterCombo.getSelectionModel().selectFirst();

        severityFilterCombo.setItems(FXCollections.observableArrayList("All Severities", "CRITICAL", "WARNING", "INFO"));
        severityFilterCombo.getSelectionModel().selectFirst();

        allAlerts = AlertManagement.generateDoctorAlerts(doctorId);

        applyFilters();

        patientFilterCombo.setOnAction(e -> applyFilters());
        severityFilterCombo.setOnAction(e -> applyFilters());
        loadAlerts();
    }

    private void applyFilters() {
        alertsContainer.getChildren().clear();

        String selectedSeverity = severityFilterCombo.getSelectionModel().getSelectedItem();
        String selectedPatient = patientFilterCombo.getSelectionModel().getSelectedItem();

        List<Alert> filtered = allAlerts.stream()
                .filter(a -> selectedSeverity.equals("All Severities") || a.getType().name().equals(selectedSeverity))
                .filter(a -> selectedPatient.equals("All Patients") ||
                        (a.getPatient() != null &&
                                (a.getPatient().getName() + " " + a.getPatient().getSurname()).equals(selectedPatient)))
                .sorted(Comparator.comparing(Alert::getType, Comparator.comparingInt(this::severityPriority))
                        .thenComparing(Alert::getDateAndTime, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        for (Alert alert : filtered) {
            alertsContainer.getChildren().add(createAlertBox(alert));
        }
    }

    private int severityPriority(AlertType type) {
        switch (type) {
            case CRITICAL: return 3;
            case WARNING: return 2;
            case INFO: return 1;
            default: return 0;
        }
    }


    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    private void loadAlerts() throws SQLException {
        alertsContainer.getChildren().clear();

        List<Alert> alerts = AlertManagement.generateDoctorAlerts(this.doctorId);


        alerts.sort((a1, a2) -> {
            int severity1 = getSeverity(a1.getType());
            int severity2 = getSeverity(a2.getType());
            if (severity1 != severity2) return severity2 - severity1;
            return a2.getDateAndTime().compareTo(a1.getDateAndTime());
        });


        for (Alert alert : alerts) {
            HBox alertBox = createAlertBox(alert);
            alertsContainer.getChildren().add(alertBox);
        }
    }

    private int getSeverity(AlertType type) {
        switch (type) {
            case CRITICAL: return 3;
            case WARNING: return 2;
            case INFO: return 1;
            default: return 0;
        }
    }


    private HBox createAlertBox(Alert alert) {
        HBox box = new HBox(10);
        box.setStyle("-fx-background-radius: 10; -fx-padding: 15; -fx-alignment: center-left;");
        box.setMaxWidth(Double.MAX_VALUE); // prendi tutta la larghezza disponibile


        switch (alert.getType()) {
            case INFO:
                box.setStyle(box.getStyle() + "-fx-background-color: #4caf50;");
                break;
            case WARNING:
                box.setStyle(box.getStyle() + "-fx-background-color: #ff9800;");
                break;
            case CRITICAL:
                box.setStyle(box.getStyle() + "-fx-background-color: #f44336;");
                break;
        }

        VBox content = new VBox(5);
        content.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(alert.getMessage());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        title.setWrapText(true); // Allows text to wrap if too long

        Label patientInfo = new Label();
        if (alert.getPatient() != null) {
            patientInfo.setText(alert.getPatient().getName() + " " + alert.getPatient().getSurname() + " - " +
                    alert.getDateAndTime().toLocalDate() + " " + alert.getDateAndTime().toLocalTime().withSecond(0).withNano(0));
            patientInfo.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 12px;");
        }

        content.getChildren().addAll(title, patientInfo);
        box.getChildren().add(content);

        return box;
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
        javafx.scene.Node current = alertsContainer.getScene().getRoot();
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
