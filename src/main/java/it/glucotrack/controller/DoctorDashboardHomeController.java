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
