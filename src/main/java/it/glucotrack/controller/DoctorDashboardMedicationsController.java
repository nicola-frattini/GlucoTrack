package it.glucotrack.controller;

import it.glucotrack.model.*;
import it.glucotrack.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;



import java.awt.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class DoctorDashboardMedicationsController implements Initializable {

    @FXML
    private Button logMedicationBtn;

    @FXML
    private TableView<Medication> prescribedMedicationsTable;

    @FXML
    private TableColumn<Medication, String> patientFullNameColumn;

    @FXML
    private TableColumn<Medication, String> medicationNameColumn;

    @FXML
    private TableColumn<Medication, String> dosageColumn;

    @FXML
    private TableColumn<Medication, String> frequencyColumn;

    @FXML
    private TableColumn<Medication, String> instructionsColumn;

    private ObservableList<Medication> prescribedMedications;

    private Doctor currentDoctor;


    public void initialize(URL location, ResourceBundle resources) {

        try {
            this.currentDoctor = DoctorDAO.getDoctorById(SessionManager.getCurrentUser().getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        setupPrescribedMedicationsTable();
        try {
            loadData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        setupEventHandlers();

    }

    @FXML
    private void onInsertMedication() {
        try{
            // Load the insert form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/DoctorDashboardMedicationsInsert.fxml"));
            Parent insertView = loader.load();

            // Get the controller and set up for insertion
            DoctorDashboardMedicationsInsertController insertController = loader.getController();

            insertController.setOnCancel(this::returnToMedications);

            // Load in main dashboard
            loadContentInMainDashboard(insertView);

        } catch (Exception e) {
            showErrorAlert("Error", "Couldn't open the insert form.");
        }

    }



    private void setupPrescribedMedicationsTable() {
        System.out.println("setupPrescribedMedicationsTable");
        patientFullNameColumn.setCellValueFactory( cell ->
                {
                    try {
                        return new SimpleStringProperty(PatientDAO.getPatientById(cell.getValue().getPatient_id()).getFullName());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
         );
        medicationNameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getName_medication())
        );
        dosageColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDose())
        );
        frequencyColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFreq().toString())
        );
        instructionsColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getInstructions())
        );


        prescribedMedicationsTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white;");
    }


    private void setupEventHandlers() {

        prescribedMedicationsTable.setRowFactory(tv -> {
            TableRow<Medication> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Medication med = row.getItem();
                    showMedicationDetailsPopup(med);
                }
            });
            return row;
        });

        setupContextMenu();
    }

    private void setupContextMenu() {

        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");

        //Set up actions
        editItem.setOnAction(e -> {
            Medication selectedMed = prescribedMedicationsTable.getSelectionModel().getSelectedItem();
            if (selectedMed != null) {
                handleEditMedication(selectedMed);
            }
        });

        deleteItem.setOnAction(e -> {
            Medication selectedMed = prescribedMedicationsTable.getSelectionModel().getSelectedItem();
            if(selectedMed != null) {
                try {
                    handleDeleteMedication(selectedMed);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(editItem, deleteItem);
        prescribedMedicationsTable.setRowFactory(tv -> {
            TableRow<Medication> row = new TableRow<Medication>() {

                @Override
                protected void updateItem(Medication item, boolean empty) {

                    super.updateItem(item,empty);
                    if (empty || item == null) {
                        setStyle("");
                    }
                }

                @Override
                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    if (selected && getItem() != null) {
                        setStyle("-fx-background-color: #0078d4; -fx-text-fill: white;");
                    } else if (getItem() != null) {
                        setStyle("");
                    }
                }
            };

            // Add hover effect
            row.setOnMouseEntered(e -> {
                if (row.getItem() != null && !row.isSelected()) {
                    row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");
                }
            });
            // Remove hover effect
            row.setOnMouseExited(e -> {
                if (row.getItem() != null && !row.isSelected()) {
                    row.setStyle("");
                }
            });

            row.setOnMouseClicked(event -> {
                if(!row.isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Medication med = row.getItem();
                    showMedicationDetailsPopup(med);
                }
            });
            row.setOnMouseEntered(event -> {
                if (row.getItem() != null && !row.isSelected()) {
                    row.setStyle("-fx-background-color: #34495E; -fx-text-fill: white;");
                }
            });
            row.setOnMouseExited( e -> {
                if (row.getItem() != null && !row.isSelected()) {
                    row.setStyle("");
                }
            });
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;

        });
    }

    private void handleEditMedication(Medication selectedMedication) {
        try {
            // Load the edit form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/DoctorDashboardMedicationsEdit.fxml"));
            Parent editView = loader.load();

            // Get the controller and set up for editing
            DoctorDashboardMedicationsEditController editController = loader.getController();
            editController.setMedicationToEdit(selectedMedication);

            // Set callbacks
            editController.setOnDataUpdated(() -> {
                refreshData();
                returnToMedications();
            });

            editController.setOnCancel(this::returnToMedications);

            // Load in main dashboard
            loadContentInMainDashboard(editView);

        } catch (Exception e) {
            showErrorAlert("Error", "Impossible open the edit form");
        }
    }



    private void loadContentInMainDashboard(Parent content) {
        try {
            DoctorDashboardController mainController = DoctorDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContentDirect(content);
            } else {
                System.err.println("Principal controller not available, trying alternative method");
                loadContentAlternativeMethod(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            loadContentAlternativeMethod(content);
        }
    }

    private void loadContentAlternativeMethod(Parent content) {
        try {
            javafx.scene.layout.StackPane contentPane = findContentPaneInScene();

            if (contentPane != null) {
                javafx.application.Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(content);
                });
            } else {
                showErrorAlert("Error", "Couldn't find the main content pane.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Error during loading view: " + e.getMessage());
        }
    }

    private javafx.scene.layout.StackPane findContentPaneInScene() {
        try {
            javafx.scene.Node currentNode = prescribedMedicationsTable;

            while (currentNode.getParent() != null) {
                currentNode = currentNode.getParent();

                if (currentNode instanceof javafx.scene.Parent) {
                    javafx.scene.layout.StackPane contentPane = findStackPaneRecursively(currentNode);
                    if (contentPane != null) {
                        return contentPane;
                    }
                }
            }

            return findAnyStackPane(prescribedMedicationsTable.getScene().getRoot());

        } catch (Exception e) {
            System.err.println("Error during content Pane search: " + e.getMessage());
            return null;
        }
    }


    private javafx.scene.layout.StackPane findStackPaneRecursively(javafx.scene.Node node) {
        if (node instanceof javafx.scene.layout.StackPane) {
            javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) node;

            // Check for specific ID if needed

            if ("contentPane".equals(stackPane.getId())) {
                return stackPane;
            }
            // If no specific ID, return the first StackPane found

        }

        // Recurse into children if it's a Parent
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

    private javafx.scene.layout.StackPane findAnyStackPane(javafx.scene.Node node) {
        if (node instanceof javafx.scene.layout.StackPane) {
            return (javafx.scene.layout.StackPane) node;
        }
        // Recurse into children if it's a Parent

        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                javafx.scene.layout.StackPane result = findAnyStackPane(child);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private void returnToMedications() {
        try {
            DoctorDashboardController mainController = DoctorDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContent("DoctorDashboardMedications.fxml");
            } else {
                refreshCurrentView();
            }
        } catch (Exception e) {
            e.printStackTrace();
            refreshCurrentView();
        }
    }

    private void refreshCurrentView() {
        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    refreshData(); // Ricarica i dati
                } catch (Exception e) {
                    System.err.println("Error during data refresh: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error during data refresh: " + e.getMessage());
        }
    }

    private void refreshData() {
        try{
            extracted();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void extracted() throws SQLException {
        loadData();
    }


    private void handleDeleteMedication(Medication selectedMedication) throws SQLException {
        // Use standard JavaFX confirmation dialog
    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirm Deletion");
    alert.setHeaderText("Delete this therapy?");
    alert.setContentText(String.format(
        "Do you really want to delete the therapy?:\n\n" +
            "Patient: %s\n" +
            "Medication: %s\n" +
            "Frequency: %s\n",
        PatientDAO.getPatientById(selectedMedication.getPatient_id()).getFullName(),
        selectedMedication.getName_medication(),
        selectedMedication.getFreq().name()
    ));
    java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            // Delete from database
            try {
                boolean deleted = MedicationDAO.deleteMedication(selectedMedication.getId());
                boolean deletedLog = LogMedicationDAO.deleteLogsByMedicationId(selectedMedication.getId());

                if (deleted) {
                    loadData();
                    javafx.scene.control.Alert successAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Therapy successfully eliminated.");
                    successAlert.showAndWait();
                } else {
                    javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Couldn't delete the therapy.");
                    errorAlert.showAndWait();
                }

            } catch (SQLException e) {
                javafx.scene.control.Alert dbErrorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                dbErrorAlert.setTitle("Database Error");
                dbErrorAlert.setHeaderText(null);
                dbErrorAlert.setContentText("Error during therapy deletion: " + e.getMessage());
                dbErrorAlert.showAndWait();
            }
        }
    }



    private void showMedicationDetailsPopup(Medication med) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController controller = loader.getController();
            controller.setTitle("Therapy Details");
            controller.setSubtitle(med.getName_medication());
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();

            Label lblPatient = new Label("Patient: " + PatientDAO.getPatientById(med.getPatient_id()).getFullName());
            lblPatient.setTextFill(Color.WHITE);
            Label lblMedication = new Label("Medication: " + med.getName_medication());
            lblMedication.setTextFill(Color.WHITE);
            Label lblDose = new Label("Dose: " + med.getDose());
            lblDose.setTextFill(Color.WHITE);
            Label lblFrequency = new Label("Frequency: " + med.getFreq());
            lblFrequency.setTextFill(Color.WHITE);
            Label lblInstruction = new Label("Instruction: " + med.getInstructions());
            lblInstruction.setTextFill(Color.WHITE);

            content.getChildren().addAll(lblPatient, lblMedication, lblDose, lblFrequency, lblInstruction);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.setScene(scene);
            popupStage.setMinWidth(520);
            popupStage.setMinHeight(340);
            controller.setStage(popupStage);
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void loadData() throws SQLException {

        List<Patient> patients = PatientDAO.getPatientsByDoctorId(currentDoctor.getId());
        List<Medication> meds = new ArrayList<>();
        for(Patient patient : patients){
            meds.addAll(patient.getMedications());
        }
        System.out.println(meds);
        prescribedMedications = FXCollections.observableArrayList(
                meds
        );
        prescribedMedicationsTable.setItems(prescribedMedications);

    }

    private void showErrorAlert(String title, String message) {
        showCustomPopup(title, message, "error");
    }

    private void showSuccessAlert(String title, String message) {
        showCustomPopup(title, message, "success");
    }

    // --- Custom Popup Helpers ---
    private void showCustomPopup(String title, String message, String type) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController controller = loader.getController();
            controller.setTitle(title);
            controller.setSubtitle(type.equals("error") ? "Error" : (type.equals("success") ? "Success" : "Info"));
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();
            javafx.scene.control.Label label = new javafx.scene.control.Label(message);
            label.setWrapText(true);
            content.getChildren().add(label);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setMinWidth(420);
            popupStage.setMinHeight(200);
            popupStage.setResizable(false);

            controller.setStage(popupStage, false);
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
