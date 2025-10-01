package it.glucotrack.controller;

import it.glucotrack.model.Patient;
import it.glucotrack.util.PatientDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.scene.paint.Color;


import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ResourceBundle;
import it.glucotrack.model.Symptom;
import it.glucotrack.util.SymptomDAO;
import it.glucotrack.util.SessionManager;

public class PatientDashboardSymptomsController implements Initializable {

    @FXML
    private Button addNewSymptomBtn;

    @FXML
    private TableView<Symptom> symptomsTable;

    @FXML
    private TableColumn<Symptom, String> dateRecordedColumn;

    @FXML
    private TableColumn<Symptom, String> symptomColumn;

    @FXML
    private TableColumn<Symptom, String> severityColumn;

    @FXML
    private TableColumn<Symptom, String> durationColumn;

    private ObservableList<Symptom> symptoms;

    private SymptomDAO symptomDAO;

    private Patient currentPatient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            this.currentPatient = PatientDAO.getPatientById(SessionManager.getCurrentUser().getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.symptomDAO = new SymptomDAO();

        setupSymptomsTable();

        setupEventHandlers();

        try {
            loadData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupSymptomsTable() {
        dateRecordedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateAndTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))));

        symptomColumn.setCellValueFactory(new PropertyValueFactory<>("symptomName"));

        severityColumn.setCellValueFactory(new PropertyValueFactory<>("gravity"));

        durationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDuration().toString()));

        // Custom cell factory per colorare la severity
        severityColumn.setCellFactory(new Callback<TableColumn<Symptom, String>, TableCell<Symptom, String>>() {
            @Override
            public TableCell<Symptom, String> call(TableColumn<Symptom, String> param) {
                return new TableCell<Symptom, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            switch (item.toLowerCase()) {
                                case "mild":
                                    setStyle("-fx-text-fill: #F39C12;"); // Orange
                                    break;
                                case "moderate":
                                    setStyle("-fx-text-fill: #E67E22;"); // Darker Orange
                                    break;
                                case "severe":
                                    setStyle("-fx-text-fill: #E74C3C;"); // Red
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

        symptomsTable.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white;");
        
        // Setup context menu for the table
        setupContextMenu();
    }

    private void setupEventHandlers() {
        addNewSymptomBtn.setOnAction(e -> handleAddNewSymptom());
    }

    private void showSymptomDetailsPopup(Symptom sym) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController controller = loader.getController();
            controller.setTitle("Symptom details");
            controller.setSubtitle(sym.getSymptomName());
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();

            Label lblSeverity = new Label("Severity: " + sym.getGravity());
            lblSeverity.setTextFill(Color.WHITE);
            Label lblDuration = new Label("Duration: " + sym.getDuration());
            lblDuration.setTextFill(Color.WHITE);
            Label lblDateTime = new Label("Date/Time: " + sym.getDateAndTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            lblDateTime.setTextFill(Color.WHITE);
            Label lblNotes = new Label("Notes: " + ((sym.getNotes() == null || sym.getNotes().isEmpty()) ? "Nessuna" : sym.getNotes()));
            lblNotes.setTextFill(Color.WHITE);

            content.getChildren().addAll(lblSeverity, lblDuration, lblDateTime, lblNotes);


            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(StageStyle.UNDECORATED);
            popupStage.setScene(scene);
            popupStage.setMinWidth(520);
            popupStage.setMinHeight(340);
            controller.setStage(popupStage);
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupContextMenu() {
        // Create context menu items
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        
        // Set up actions
        editItem.setOnAction(e -> {
            Symptom selectedSymptom = symptomsTable.getSelectionModel().getSelectedItem();
            if (selectedSymptom != null) {
                handleEditSymptom(selectedSymptom);
            }
        });
        
        deleteItem.setOnAction(e -> {
            Symptom selectedSymptom = symptomsTable.getSelectionModel().getSelectedItem();
            if (selectedSymptom != null) {
                handleDeleteSymptom(selectedSymptom);
            }
        });
        
        // Create context menu
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(editItem, deleteItem);
        
        // Set up custom row factory with context menu, selection highlighting e doppio click
        symptomsTable.setRowFactory(tv -> {
            TableRow<Symptom> row = new TableRow<Symptom>() {
                @Override
                protected void updateItem(Symptom item, boolean empty) {
                    super.updateItem(item, empty);
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
            // Double click to show details
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Symptom sym = row.getItem();
                    showSymptomDetailsPopup(sym);
                }
            });

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
            // Only show context menu when row has data
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(contextMenu)
            );
            return row;
        });
    }
    
    private void handleEditSymptom(Symptom selectedSymptom) {
        try {
            // Load the edit form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardSymptomEdit.fxml"));
            Parent editView = loader.load();
            
            // Get the controller and set up for editing
            PatientDashboardSymptomsEditController editController = loader.getController();
            editController.setupForEdit(selectedSymptom);
            System.out.println("Setting up edit for symptom: " + selectedSymptom);
            // Set callbacks
            editController.setOnDataUpdated(() -> {
                try {
                    refreshData();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                returnToSymptoms();
            });
            
            editController.setOnCancel(this::returnToSymptoms);
            
            // Load in main dashboard
            loadContentInMainDashboard(editView);
            
        } catch (Exception e) {
            System.err.println("Error during symptom edit: " + e.getMessage());
            showErrorAlert("Error", "Couldn't open edit form.");
        }
    }
    
    private void handleDeleteSymptom(Symptom selectedSymptom) {
        // Show custom confirmation dialog
        boolean confirmed = showConfirmationDialog(
            "Do you want to delete this symptom?",
            String.format(
                "Do you really wan to delete the symptom:\n\n" +
                "Name: %s\n" +
                "Severity: %s\n" +
                "Date: %s\n" +
                "Note: %s",
                selectedSymptom.getSymptomName(),
                selectedSymptom.getGravity(),
                selectedSymptom.getDateAndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                selectedSymptom.getNotes().isEmpty() ? "No notes" : selectedSymptom.getNotes()
            )
        );
        if (confirmed) {
            // Delete from database
            try {
                SymptomDAO symptomDAO = new SymptomDAO();
                boolean deleted = symptomDAO.deleteSymptomById(selectedSymptom.getId());
                
                if (deleted) {
                    // Remove from table data
                    symptoms.remove(selectedSymptom);
                    
                    showSuccessAlert("Success", "Symptom successfully deleted.");
                } else {
                    showErrorAlert("Error", "Can't delete the symptom from the database.");
                }
                
            } catch (SQLException e) {
                System.err.println("Errord symptom deletion: " + e.getMessage());
                showErrorAlert("Errord Database", " error during symptom deletion: " + e.getMessage());
            }
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(yes, no);

        return alert.showAndWait().orElse(no) == yes;
    }

    private void loadData() throws SQLException {
        System.out.println("Loading symptoms: " + SymptomDAO.getSymptomsByPatientId(currentPatient.getId()));
        symptoms = FXCollections.observableArrayList(
                SymptomDAO.getSymptomsForTable(currentPatient.getId())
        );
        System.out.println("Loaded symptoms: " + symptoms);
        symptomsTable.setItems(symptoms);
    }

    private void handleAddNewSymptom() {
        openSymptomInsertForm();
    }



    public void refreshData() throws SQLException {
        loadData();
    }

    public ObservableList<Symptom> getSymptoms() {
        return symptoms;
    }

    

    private void openSymptomInsertForm() {
        try {


            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardSymptomInsert.fxml"));
            Parent symptomInsertView = loader.load();


            // Get form controller
            PatientDashboardSymptomsInsertController insertController = loader.getController();

            insertController.setOnDataSaved(() -> {
                try {
                    refreshData();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                returnToSymptoms();
            });
            
            insertController.setOnCancel(this::returnToSymptoms);

            loadContentInMainDashboard(symptomInsertView);
            
        } catch (Exception e) {
            System.err.println("Error during insert form loading: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void loadContentInMainDashboard(Parent content) {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContentDirect(content);
            } else {
                System.err.println("Principal controller not available to load content.");
            }
        } catch (Exception e) {
            System.err.println("Error during content loading: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    private void returnToSymptoms() {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContent("PatientDashboardSymptoms.fxml");
            } else {
                System.err.println("Principal controller not available to return to symptoms.");
            }
        } catch (Exception e) {
            System.err.println("Error during the return: " + e.getMessage());
            e.printStackTrace();
        }
    }
}