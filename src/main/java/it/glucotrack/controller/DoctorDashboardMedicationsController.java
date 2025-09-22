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
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;


import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
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
        System.out.println("Iniziamo con il dashboard");
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
            System.err.println("Errore nell'apertura del form di inserimento terapia: " + e.getMessage());
            showErrorAlert("Errore", "Impossibile aprire il form di inserimento.");
        }

    }



    private void setupPrescribedMedicationsTable() {
        System.out.println("setupPrescribedMedicationsTable");
        patientFullNameColumn.setCellValueFactory( cell ->
                new SimpleStringProperty(cell.getValue().getName_medication())
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

                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    if (selected && getItem() != null) {
                        setStyle("-fx-background-color: #1ABC9C; -fx-text-fill: white;");
                    } else if (getItem() != null) {
                        setStyle("");
                    }
                }
            };
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
            System.err.println("Errore nell'apertura del form di modifica sintomo: " + e.getMessage());
            showErrorAlert("Errore", "Impossibile aprire il form di modifica.");
        }
    }



    private void loadContentInMainDashboard(Parent content) {
        try {
            // CORREZIONE: Usa DoctorDashboardController invece di PatientDashboardController
            DoctorDashboardController mainController = DoctorDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContentDirect(content);
                System.out.println("✅ Contenuto caricato nel pannello centrale via controller principale");
            } else {
                System.err.println("❌ Controller principale non disponibile - tentativo alternativo");
                // Metodo alternativo se il singleton fallisce
                loadContentAlternativeMethod(content);
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento del contenuto nel dashboard: " + e.getMessage());
            e.printStackTrace();
            // Prova metodo alternativo
            loadContentAlternativeMethod(content);
        }
    }

    private void loadContentAlternativeMethod(Parent content) {
        try {
            // Trova il StackPane contentPane tramite la gerarchia dei nodi
            javafx.scene.layout.StackPane contentPane = findContentPaneInScene();

            if (contentPane != null) {
                javafx.application.Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(content);
                });
                System.out.println("✅ Contenuto caricato con metodo alternativo");
            } else {
                System.err.println("❌ Impossibile trovare il contentPane");
                showErrorAlert("Errore", "Impossibile caricare la vista di modifica. Riprova.");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel metodo alternativo: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Errore", "Errore nel caricamento della vista: " + e.getMessage());
        }
    }

    private javafx.scene.layout.StackPane findContentPaneInScene() {
        try {
            javafx.scene.Node currentNode = prescribedMedicationsTable;

            // Risali la gerarchia fino alla scena root
            while (currentNode.getParent() != null) {
                currentNode = currentNode.getParent();

                // Cerca ricorsivamente il StackPane con id "contentPane"
                if (currentNode instanceof javafx.scene.Parent) {
                    javafx.scene.layout.StackPane contentPane = findStackPaneRecursively(currentNode);
                    if (contentPane != null) {
                        return contentPane;
                    }
                }
            }

            // Se non trova contentPane, cerca qualsiasi StackPane che possa essere il contenitore principale
            return findAnyStackPane(prescribedMedicationsTable.getScene().getRoot());

        } catch (Exception e) {
            System.err.println("Errore nella ricerca del contentPane: " + e.getMessage());
            return null;
        }
    }


    private javafx.scene.layout.StackPane findStackPaneRecursively(javafx.scene.Node node) {
        if (node instanceof javafx.scene.layout.StackPane) {
            javafx.scene.layout.StackPane stackPane = (javafx.scene.layout.StackPane) node;

            // Cerca prima per ID "contentPane"
            if ("contentPane".equals(stackPane.getId())) {
                return stackPane;
            }

            // Se non ha l'ID giusto, continua la ricerca nei figli
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

    private javafx.scene.layout.StackPane findAnyStackPane(javafx.scene.Node node) {
        if (node instanceof javafx.scene.layout.StackPane) {
            return (javafx.scene.layout.StackPane) node;
        }

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
                System.out.println("✅ Ritorno alla vista medications completato");
            } else {
                System.err.println("❌ Controller principale non disponibile per il ritorno");
                // Metodo alternativo - ricarica la vista corrente
                refreshCurrentView();
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel ritorno alla view delle terapie: " + e.getMessage());
            e.printStackTrace();
            refreshCurrentView();
        }
    }

    private void refreshCurrentView() {
        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    refreshData(); // Ricarica i dati
                    System.out.println("✅ Dati ricaricati con successo");
                } catch (Exception e) {
                    System.err.println("❌ Errore nel refresh dei dati: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Errore nel refresh della vista: " + e.getMessage());
        }
    }

    private void refreshData() {
        try{
            loadData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private void handleDeleteMedication(Medication selectedMedication) throws SQLException {
        // Show custom confirmation dialog
        boolean confirmed = showCustomConfirmationDialog(
                "Conferma Cancellazione",
                "Eliminare questa terapia?",
                String.format(
                        "Vuoi davvero eliminare la terapia?:\n\n" +
                                "Paziente: %s\n" +
                                "Medicina: %s\n" +
                                "Frequenza: %s\n" +
                        PatientDAO.getPatientById(selectedMedication.getPatient_id()).getFullName()
                )
        );
        if (confirmed) {
            // Delete from database
            try {
                MedicationDAO medicationDAO = new MedicationDAO();
                boolean deleted = MedicationDAO.deleteMedication(selectedMedication.getId());
                boolean deletedLog = LogMedicationDAO.deleteLogsByMedicationId(selectedMedication.getId());

                if (deleted && deletedLog) {
                    // Remove from table data
                    prescribedMedications.remove(selectedMedication);

                    showSuccessAlert("Successo", "Terapia eliminata con successo.");
                    System.out.println("✅ Terapia eliminata con successo");
                } else {
                    showErrorAlert("Errore", "Impossibile eliminare la terapia dal database.");
                }

            } catch (SQLException e) {
                System.err.println("Errore nell'eliminazione della terapia: " + e.getMessage());
                showErrorAlert("Errore Database", "Errore nell'eliminazione del sintomo: " + e.getMessage());
            }
        }
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


    void loadData() throws SQLException {
        // Carica farmaci prescritti dal dottore
        System.out.println("Parto con il load");
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


}
