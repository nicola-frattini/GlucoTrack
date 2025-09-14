package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class DoctorDashboardHomeController {
    @FXML
    private TableView<String[]> patientTable;

    @FXML
    private ListView<String> alertsList;

    @FXML
    public void initialize() {
        setupPatientTable();
        loadFakeData();
    }

    private void setupPatientTable() {
        // Colonne già definite in FXML
        TableColumn<String[], String> nameCol = (TableColumn<String[], String>) patientTable.getColumns().get(0);
        TableColumn<String[], String> readingCol = (TableColumn<String[], String>) patientTable.getColumns().get(1);
        TableColumn<String[], String> adherenceCol = (TableColumn<String[], String>) patientTable.getColumns().get(2);
        TableColumn<String[], String> statusCol = (TableColumn<String[], String>) patientTable.getColumns().get(3);
        TableColumn<String[], String> actionCol = (TableColumn<String[], String>) patientTable.getColumns().get(4);

        nameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[0]));
        readingCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[1]));
        adherenceCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[2]));
        statusCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[3]));
        actionCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue()[4]));
    }

    private void loadFakeData() {
        ObservableList<String[]> patients = FXCollections.observableArrayList(
                new String[]{"Sophia Clark", "120 mg/dL", "85%", "Stable", "View Details"},
                new String[]{"Ethan Harris", "250 mg/dL", "60%", "High Risk", "View Details"},
                new String[]{"Olivia Turner", "180 mg/dL", "75%", "Monitor", "View Details"},
                new String[]{"Liam Foster", "110 mg/dL", "90%", "Stable", "View Details"},
                new String[]{"Ava Bennett", "300 mg/dL", "50%", "Critical", "View Details"}
        );

        patientTable.setItems(patients);

        // Alerts fake
        alertsList.setItems(FXCollections.observableArrayList(
                "⚠ Ethan Harris - High Glucose (250 mg/dL)",
                "⛔ Ava Bennett - Missed Medication: Insulin"
        ));
    }
}
