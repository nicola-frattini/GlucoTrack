package it.glucotrack.controller;
import it.glucotrack.model.Patient;
import it.glucotrack.util.PatientDAO;
import it.glucotrack.model.LogMedication;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.input.MouseButton;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;
import it.glucotrack.model.User;
import it.glucotrack.util.MedicationDAO;
import it.glucotrack.util.LogMedicationDAO;
import it.glucotrack.util.SessionManager;
import it.glucotrack.model.Medication;

public class PatientDashboardMedicationsController implements Initializable {

    @FXML
    private Button logMedicationBtn;

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

    // Simulazione servizi database - da sostituire con dependency injection
    private Patient currentPatient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializza servizi fittizi
        try {
            this.currentPatient = PatientDAO.getPatientById(SessionManager.getCurrentUser().getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Setup tabelle
        setupPrescribedMedicationsTable();
        setupIntakeLogTable();

        // Setup eventi
        setupEventHandlers();

        // Carica dati
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
                return new SimpleStringProperty("Errore");
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
        
        // Configura la selezione della tabella
        intakeLogTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // Aggiungi context menu con tasto destro
        setupContextMenu();
    }
    
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem markTakenItem = new MenuItem("✓ Segna come Presa");
        markTakenItem.setOnAction(e -> {
            LogMedication selectedLog = intakeLogTable.getSelectionModel().getSelectedItem();
            if (selectedLog != null) {
                updateLogMedicationStatus(selectedLog, true);
            }
        });
        
        MenuItem markMissedItem = new MenuItem("✗ Segna come Non Presa");
        markMissedItem.setOnAction(e -> {
            LogMedication selectedLog = intakeLogTable.getSelectionModel().getSelectedItem();
            if (selectedLog != null) {
                updateLogMedicationStatus(selectedLog, false);
            }
        });
        
        contextMenu.getItems().addAll(markTakenItem, markMissedItem);
        
        // Aggiungi il context menu alla tabella
        intakeLogTable.setContextMenu(contextMenu);
        
        // Configura il context menu e l'evidenziazione delle righe
        intakeLogTable.setRowFactory(tv -> {
            TableRow<LogMedication> row = new TableRow<LogMedication>() {
                @Override
                protected void updateItem(LogMedication item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // Mantieni lo stile normale di default (trasparente)
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
                            // Stile evidenziato per la riga selezionata
                            setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                                   "-fx-border-color: #2980b9; -fx-border-width: 2px; " +
                                   "-fx-effect: dropshadow(gaussian, #2980b9, 5, 0, 0, 0);");
                        } else {
                            // Ritorna allo stile normale (trasparente, usa lo stile della tabella)
                            setStyle("");
                        }
                    }
                }
            };
            
            // Aggiungi hover effect leggero per migliorare l'usabilità
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
            
            // Context menu con selezione automatica della riga
            row.setOnContextMenuRequested(e -> {
                if (row.getItem() != null) {
                    // Seleziona automaticamente la riga quando si clicca tasto destro
                    intakeLogTable.getSelectionModel().select(row.getIndex());
                    
                    // Aggiorna il testo degli item in base allo stato attuale
                    LogMedication log = row.getItem();
                    if (log.isTaken()) {
                        markTakenItem.setText("✓ Già Presa");
                        markTakenItem.setDisable(true);
                        markMissedItem.setText("✗ Segna come Non Presa");
                        markMissedItem.setDisable(false);
                    } else {
                        markTakenItem.setText("✓ Segna come Presa");
                        markTakenItem.setDisable(false);
                        markMissedItem.setText("✗ Già Non Presa");
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
        logMedicationBtn.setOnAction(e -> handleLogMedicationIntake());

        // Doppio click su riga della tabella farmaci
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

        // Context menu "View" su farmaco
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
            controller.setTitle("Dettagli Farmaco");
            controller.setSubtitle(med.getName_medication());
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();
            content.getChildren().addAll(
                new javafx.scene.control.Label("Dosaggio: " + med.getName_medication()),
                new javafx.scene.control.Label("Frequenza: " + med.getName_medication()),
                new javafx.scene.control.Label("Istruzioni: " + med.getInstructions())
            );
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setMinWidth(520);
            popupStage.setMinHeight(340);
            controller.setStage(popupStage);
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() throws SQLException {
        // Carica farmaci prescritti
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
        // Crea dialog per logging
        // Serve a
        Optional<LogMedication> result = showLogMedicationDialog();
        result.ifPresent(log -> {
            // Salva il log che è stato
            try {
                LogMedicationDAO.insertLogMedicationStatic(log);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            // Aggiorna UI
            LogMedications.add(0, log);
            intakeLogTable.refresh();
        });
    }

    private Optional<LogMedication> showLogMedicationDialog() {
        Dialog<LogMedication> dialog = new Dialog<>();
        dialog.setTitle("Log Medication Intake");
        dialog.setHeaderText("Record medication intake");

        // Placeholder per dialog - implementazione completa da fare
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Log Medication");
        alert.setHeaderText(null);
        alert.setContentText("Medication logging dialog will be implemented here.\nThis will allow selection of medication and status.");
        alert.showAndWait();

        return Optional.empty(); // Temporary
    }




}