package it.glucotrack.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ResourceBundle;
import java.util.Optional;
import it.glucotrack.model.User;
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

    // Simulazione servizio database - da sostituire con dependency injection
    private SymptomService symptomService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializza servizi fittizi
        initializeServices();

        // Setup tabella
        setupSymptomsTable();

        // Setup eventi
        setupEventHandlers();

        // Carica dati
        loadData();
    }

    private void initializeServices() {
        // Initialize DAO service
        symptomService = new DatabaseSymptomService();
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
            controller.setTitle("Dettagli Sintomo");
            controller.setSubtitle(sym.getSymptomName());
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();
            content.getChildren().addAll(
                new javafx.scene.control.Label("Gravità: " + sym.getGravity()),
                new javafx.scene.control.Label("Durata: " + sym.getDuration()),
                new javafx.scene.control.Label("Data/Ora: " + sym.getDateAndTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                new javafx.scene.control.Label("Note: " + ((sym.getNotes() == null || sym.getNotes().isEmpty()) ? "Nessuna" : sym.getNotes()))
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
    
    private void setupContextMenu() {
        // Create context menu items
        MenuItem editItem = new MenuItem("Modifica");
        MenuItem deleteItem = new MenuItem("Cancella");
        
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
            // Doppio click per mostrare i dettagli
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
            
            // Set callbacks
            editController.setOnDataUpdated(() -> {
                refreshData();
                returnToSymptoms();
            });
            
            editController.setOnCancel(this::returnToSymptoms);
            
            // Load in main dashboard
            loadContentInMainDashboard(editView);
            
        } catch (Exception e) {
            System.err.println("Errore nell'apertura del form di modifica sintomo: " + e.getMessage());
            showErrorAlert("Errore", "Impossibile aprire il form di modifica.");
        }
    }
    
    private void handleDeleteSymptom(Symptom selectedSymptom) {
        // Show custom confirmation dialog
        boolean confirmed = showCustomConfirmationDialog(
            "Conferma Cancellazione",
            "Eliminare questo sintomo?",
            String.format(
                "Vuoi davvero eliminare il sintomo:\n\n" +
                "Nome: %s\n" +
                "Gravità: %s\n" +
                "Data: %s\n" +
                "Note: %s",
                selectedSymptom.getSymptomName(),
                selectedSymptom.getGravity(),
                selectedSymptom.getDateAndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                selectedSymptom.getNotes().isEmpty() ? "Nessuna nota" : selectedSymptom.getNotes()
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
                    
                    showSuccessAlert("Successo", "Sintomo eliminato con successo.");
                    System.out.println("✅ Sintomo eliminato con successo");
                } else {
                    showErrorAlert("Errore", "Impossibile eliminare il sintomo dal database.");
                }
                
            } catch (SQLException e) {
                System.err.println("Errore nell'eliminazione del sintomo: " + e.getMessage());
                showErrorAlert("Errore Database", "Errore nell'eliminazione del sintomo: " + e.getMessage());
            }
        }
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
            controller.setSubtitle(type.equals("error") ? "Errore" : (type.equals("success") ? "Successo" : "Info"));
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
            // Disabilita lo spostamento: rimuovi i listener drag dal title bar custom
            controller.setStage(popupStage, false); // popup: drag disabilitato
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean showCustomConfirmationDialog(String title, String subtitle, String message) {
        final boolean[] result = {false};
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/assets/fxml/CustomPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            it.glucotrack.component.CustomPopupController controller = loader.getController();
            controller.setTitle(title);
            controller.setSubtitle(subtitle);
            javafx.scene.layout.VBox content = controller.getPopupContent();
            content.getChildren().clear();
            javafx.scene.control.Label label = new javafx.scene.control.Label(message);
            label.setWrapText(true);
            javafx.scene.control.Button yesBtn = new javafx.scene.control.Button("Sì");
            javafx.scene.control.Button noBtn = new javafx.scene.control.Button("No");
            yesBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 8;");
            noBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 8;");
            javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(16, yesBtn, noBtn);
            btnBox.setStyle("-fx-alignment: center; -fx-padding: 18 0 0 0;");
            content.getChildren().addAll(label, btnBox);
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.setMinWidth(420);
            popupStage.setMinHeight(220);
            popupStage.setResizable(false);
            // Disabilita lo spostamento: rimuovi i listener drag dal title bar custom
            // (Assicurati che CustomTitleBarController non implementi drag per questi popup)
            controller.setStage(popupStage, false); // popup: drag disabilitato
            yesBtn.setOnAction(ev -> { result[0] = true; popupStage.close(); });
            noBtn.setOnAction(ev -> { result[0] = false; popupStage.close(); });
            popupStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result[0];
    }

    private void loadData() {
        symptoms = FXCollections.observableArrayList(
                symptomService.getAllSymptoms()
        );
        symptomsTable.setItems(symptoms);
    }

    private void handleAddNewSymptom() {
        System.out.println("➕ Pulsante Add New Symptom cliccato!");
        openSymptomInsertForm();
    }



    // Metodi per future integrazioni
    public void refreshData() {
        loadData();
    }

    public ObservableList<Symptom> getSymptoms() {
        return symptoms;
    }

    public void addSymptom(Symptom symptom) {
        symptomService.saveSymptom(symptom);
        symptoms.add(0, symptom);
        symptomsTable.refresh();
    }
    
    // Metodo per aprire il form di inserimento sintomi nel pannello centrale
    private void openSymptomInsertForm() {
        try {
            System.out.println("🔄 Apertura form inserimento sintomi da sezione sintomi...");
            
            // Carica il form nel pannello centrale del dashboard principale
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardSymptomInsert.fxml"));
            Parent symptomInsertView = loader.load();
            System.out.println("✅ FXML sintomi caricato con successo");
            
            // Ottieni il controller del form
            PatientDashboardSymptomsInsertController insertController = loader.getController();
            System.out.println("✅ Controller sintomi ottenuto: " + (insertController != null ? "OK" : "NULL"));
            
            // Imposta il callback per refresh dei dati quando si salva
            insertController.setOnDataSaved(() -> {
                refreshData();
                // Dopo il salvataggio, torna alla sezione sintomi
                returnToSymptoms();
            });
            
            // Imposta il callback per l'annullamento
            insertController.setOnCancel(this::returnToSymptoms);
            System.out.println("✅ Callback sintomi impostati");
            
            // Sostituisce il contenuto centrale con il form
            loadContentInMainDashboard(symptomInsertView);
            
        } catch (Exception e) {
            System.err.println("❌ Errore nell'apertura del form di inserimento sintomi: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Metodo per caricare contenuto nel pannello centrale del dashboard principale
    private void loadContentInMainDashboard(Parent content) {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContentDirect(content);
                System.out.println("✅ Contenuto caricato nel pannello centrale via controller principale");
            } else {
                System.err.println("❌ Controller principale non disponibile");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento del contenuto nel dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Metodo per tornare alla sezione sintomi
    private void returnToSymptoms() {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContent("PatientDashboardSymptoms.fxml");
                System.out.println("✅ Ritorno alla sezione sintomi completato");
            } else {
                System.err.println("❌ Controller principale non disponibile per il ritorno alla sezione sintomi");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel ritorno alla sezione sintomi: " + e.getMessage());
            e.printStackTrace();
        }
    }



    // ========== SERVIZIO INTERFACE ==========

    public interface SymptomService {
        java.util.List<Symptom> getAllSymptoms();
        java.util.List<Symptom> getSymptomsByDateRange(LocalDateTime start, LocalDateTime end);
        java.util.List<Symptom> getSymptomsBySeverity(String severity);
        void saveSymptom(Symptom symptom);
        void deleteSymptom(Long id);
        Optional<Symptom> getSymptomById(Long id);
    }

    // ========== SERVIZI DATABASE E MOCK ==========

    // Implementazione database
    private static class DatabaseSymptomService implements SymptomService {
        private SymptomDAO symptomDAO;
        
        public DatabaseSymptomService() {
            this.symptomDAO = new SymptomDAO();
        }
        
        @Override
        public java.util.List<Symptom> getAllSymptoms() {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    // Usa il nuovo metodo che restituisce oggetti Symptom completi
                    return symptomDAO.getSymptomsForTable(currentUser.getId());
                }
                return new java.util.ArrayList<>();
            } catch (SQLException e) {
                System.err.println("Errore nel recupero symptoms: " + e.getMessage());
                return new java.util.ArrayList<>();
            }
        }
        
        @Override
        public java.util.List<Symptom> getSymptomsByDateRange(LocalDateTime start, LocalDateTime end) {
            // Implementation for date range filtering would go here
            return getAllSymptoms(); // For now, return all
        }
        
        @Override
        public java.util.List<Symptom> getSymptomsBySeverity(String severity) {
            // Implementation for severity filtering would go here
            return getAllSymptoms(); // For now, return all
        }
        
        @Override
        public void saveSymptom(Symptom symptom) {
            try {
                // Convert from controller Symptom to model Symptom would be needed here
                System.out.println("Save symptom called: " + symptom.getSymptomName());
            } catch (Exception e) {
                System.err.println("Errore nel salvataggio symptom: " + e.getMessage());
            }
        }
        
        @Override
        public void deleteSymptom(Long id) {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    // For now, we can't delete by id since DAO only deletes by patient_id + symptom name
                    System.out.println("Delete symptom called for id: " + id);
                }
            } catch (Exception e) {
                System.err.println("Errore nella cancellazione symptom: " + e.getMessage());
            }
        }
        
        @Override
        public Optional<Symptom> getSymptomById(Long id) {
            // Implementation would go here
            return Optional.empty();
        }
    }


}