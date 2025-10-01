package it.glucotrack.controller;
import it.glucotrack.model.Patient;
import it.glucotrack.util.PatientDAO;
import it.glucotrack.model.LogMedication;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Optional;
import it.glucotrack.util.MedicationDAO;
import it.glucotrack.util.LogMedicationDAO;
import it.glucotrack.util.SessionManager;
import it.glucotrack.model.Medication;

public class PatientDashboardMedicationsController implements Initializable {


    @FXML
    private TableView<Medication> prescribedMedicationsTable;

    @FXML
    private TableColumn<Medication, String> MedicationNameColumn;

    @FXML
    private TableColumn<Medication, String> dosageColumn;

    @FXML
    private TableColumn<Medication, String> frequencyColumn;

    @FXML
    private TableColumn<Medication, String> instructionsColumn;

    @FXML
    private TableView<LogMedication> intakeLogTable;

    @FXML
    private TableColumn<LogMedication, String> dateTimeColumn;

    @FXML
    private TableColumn<LogMedication, String> medicationColumn;

    @FXML
    private TableColumn<LogMedication, String> statusColumn;

    private ObservableList<Medication> prescribedMedications;
    private ObservableList<LogMedication> LogMedications;

    private Patient currentPatient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            this.currentPatient = PatientDAO.getPatientById(SessionManager.getCurrentUser().getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        setupPrescribedMedicationsTable();
        setupIntakeLogTable();

        setupEventHandlers();

        try {
            loadData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupPrescribedMedicationsTable() {
        MedicationNameColumn.setCellValueFactory(cell ->
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

    private void setupIntakeLogTable() {
        dateTimeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFormattedDateTime())
        );

        medicationColumn.setCellValueFactory(cell -> {
            try {
                return new SimpleStringProperty(
                        MedicationDAO.getMedicationById(cell.getValue().getMedication_id()).getName_medication()
                );
            } catch (SQLException e) {
                return new SimpleStringProperty("Error");
            }
        });

        statusColumn.setCellValueFactory(cell -> {
            String status = cell.getValue().isTaken() ? "Taken" : "Missed";
            return new SimpleStringProperty(status);
        });


        TableColumn<LogMedication, Boolean> takenCol = new TableColumn<>("Taken");
        takenCol.setCellValueFactory(cellData ->
                new ReadOnlyBooleanWrapper(cellData.getValue().isTaken())
        );


        // Custom cell factory per colorare lo status
        statusColumn.setCellFactory(new Callback<TableColumn<LogMedication, String>, TableCell<LogMedication, String>>() {
            @Override
            public TableCell<LogMedication, String> call(TableColumn<LogMedication, String> param) {
                return new TableCell<LogMedication, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText("● " + item);
                            switch (item.toLowerCase()) {
                                case "taken":
                                    setStyle("-fx-text-fill: #2ECC71;");
                                    break;
                                case "missed":
                                    setStyle("-fx-text-fill: #E74C3C;");
                                    break;
                                default:
                                    setStyle("-fx-text-fill: white;");
                                    break;
                            }
                        }
                    }
                };
            }
        });

        intakeLogTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white; " +
                               "-fx-selection-bar: #3498db; -fx-selection-bar-non-focused: #5dade2;");

        intakeLogTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        setupContextMenu();
    }
    
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem markTakenItem = new MenuItem("Mark as TAKEN");
        markTakenItem.setOnAction(e -> {
            LogMedication selectedLog = intakeLogTable.getSelectionModel().getSelectedItem();
            if (selectedLog != null) {
                updateLogMedicationStatus(selectedLog, true);
            }
        });
        
        MenuItem markMissedItem = new MenuItem("Mark as NOT TAKEN");
        markMissedItem.setOnAction(e -> {
            LogMedication selectedLog = intakeLogTable.getSelectionModel().getSelectedItem();
            if (selectedLog != null) {
                updateLogMedicationStatus(selectedLog, false);
            }
        });
        
        contextMenu.getItems().addAll(markTakenItem, markMissedItem);
        

        intakeLogTable.setContextMenu(contextMenu);
        

        intakeLogTable.setRowFactory(tv -> {
            TableRow<LogMedication> row = new TableRow<LogMedication>() {
                @Override
                protected void updateItem(LogMedication item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setStyle("");
                    } else {

                        if (!isSelected()) {
                            setStyle("");
                        }
                    }
                }
                
                @Override
                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    
                    if (getItem() != null) {
                        if (selected) {

                            setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                                   "-fx-border-color: #2980b9; -fx-border-width: 2px; " +
                                   "-fx-effect: dropshadow(gaussian, #2980b9, 5, 0, 0, 0);");
                        } else {

                            setStyle("");
                        }
                    }
                }
            };
            

            row.setOnMouseEntered(e -> {
                if (row.getItem() != null && !row.isSelected()) {
                    row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");
                }
            });
            
            row.setOnMouseExited(e -> {
                if (row.getItem() != null && !row.isSelected()) {
                    row.setStyle("");
                }
            });
            

            row.setOnContextMenuRequested(e -> {
                if (row.getItem() != null) {

                    intakeLogTable.getSelectionModel().select(row.getIndex());
                    

                    LogMedication log = row.getItem();
                    if (log.isTaken()) {
                        markTakenItem.setText("Already TAKEN");
                        markTakenItem.setDisable(true);
                        markMissedItem.setText("Mark as NOT TAKEN");
                        markMissedItem.setDisable(false);
                    } else {
                        markTakenItem.setText("Mark as TAKEN");
                        markTakenItem.setDisable(false);
                        markMissedItem.setText("NOT TAKEN");
                        markMissedItem.setDisable(true);
                    }
                    contextMenu.show(row, e.getScreenX(), e.getScreenY());
                }
                e.consume();
            });
            
            return row;
        });
    }

    private void updateLogMedicationStatus(LogMedication selectedLog, boolean b) {
        boolean newStatus = b ? true : false;
        if (newStatus != selectedLog.isTaken()) {
            selectedLog.setTaken(newStatus);
            try {
                LogMedicationDAO.updateLogMedication(selectedLog);
                intakeLogTable.refresh();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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


        MenuItem viewItem = new MenuItem("View Details");
        viewItem.setOnAction(e -> {
            Medication selected = prescribedMedicationsTable.getSelectionModel().getSelectedItem();
            if (selected != null) showMedicationDetailsPopup(selected);
        });
        ContextMenu menu = new ContextMenu(viewItem);
        prescribedMedicationsTable.setContextMenu(menu);
    }

    private void showMedicationDetailsPopup(Medication med) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController controller = loader.getController();
            controller.setTitle("Medication Details");
            controller.setSubtitle(med.getName_medication());
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();

            Label lblName = new Label("Name: " + med.getName_medication());
            lblName.setTextFill(Color.WHITE);
            Label lblDose = new Label("Dose: " + med.getDose());
            lblDose.setTextFill(Color.WHITE);
            Label lblFreq = new Label("Frequency: " + med.getFreq());
            lblFreq.setTextFill(Color.WHITE);
            Label lblInstruction= new Label("Instruction: " + med.getInstructions());
            lblInstruction.setTextFill(Color.WHITE);

            content.getChildren().addAll(lblName, lblDose, lblFreq, lblInstruction);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.setMinWidth(520);
            popupStage.setMinHeight(340);
            controller.setStage(popupStage);
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() throws SQLException {

        prescribedMedications = FXCollections.observableArrayList(
                currentPatient.getMedications()
        );
        prescribedMedicationsTable.setItems(prescribedMedications);

        ObservableList<LogMedication> logs = FXCollections.observableArrayList();
        for (Medication med : currentPatient.getMedications()) {
            System.out.println("Loading logs for medication: " + med.getName_medication());
            logs.addAll(LogMedicationDAO.getLogMedicationsByMedicationId(med.getId()));
        }
        intakeLogTable.setItems(logs);
        LogMedications = logs;

    }

    private void handleLogMedicationIntake() {


        Optional<LogMedication> result = showLogMedicationDialog();
        result.ifPresent(log -> {

            try {
                LogMedicationDAO.insertLogMedicationStatic(log);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }


            LogMedications.add(0, log);
            intakeLogTable.refresh();
        });
    }

    private Optional<LogMedication> showLogMedicationDialog() {
        Dialog<LogMedication> dialog = new Dialog<>();
        dialog.setTitle("Log Medication Intake");
        dialog.setHeaderText("Record medication intake");


        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Log Medication");
        alert.setHeaderText(null);
        alert.setContentText("Medication logging dialog will be implemented here.\nThis will allow selection of medication and status.");
        alert.showAndWait();

        return Optional.empty();
    }




}