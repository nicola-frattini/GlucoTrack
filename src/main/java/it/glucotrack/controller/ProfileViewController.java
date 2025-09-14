package it.glucotrack.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ProfileViewController implements Initializable {
    
    // Header elements
    @FXML private Circle profileImageCircle;
    @FXML private ImageView profileImageView;
    @FXML private Label patientNameLabel;
    @FXML private Label patientIdLabel;
    @FXML private Label lastVisitLabel;
    
    // Tab buttons
    @FXML private Button overviewTab;
    @FXML private Button trendsTab;
    @FXML private Button medicationTab;
    @FXML private Button notesTab;
    
    // Content areas
    @FXML private VBox overviewContent;
    @FXML private VBox trendsContent;
    @FXML private VBox medicationContent;
    @FXML private VBox notesContent;
    
    // Overview content elements
    @FXML private Label averageGlucoseLabel;
    @FXML private Label averageGlucoseChangeLabel;
    @FXML private LineChart<String, Number> glucoseTrendsChart;
    @FXML private Label metforminPercentageLabel;
    @FXML private ProgressBar metforminProgressBar;
    @FXML private Label insulinPercentageLabel;
    @FXML private ProgressBar insulinProgressBar;
    @FXML private VBox symptomsContainer;
    @FXML private TableView<TherapyModification> therapyModificationsTable;
    @FXML private TableColumn<TherapyModification, String> dateColumn;
    @FXML private TableColumn<TherapyModification, String> modificationColumn;
    @FXML private TableColumn<TherapyModification, String> modifiedByColumn;
    
    // Action buttons
    @FXML private Button modifyTherapyBtn;
    @FXML private Button updatePatientInfoBtn;
    
    // Data models
    private Patient currentPatient;
    private boolean isDoctorView = false;
    private ObservableList<TherapyModification> therapyModifications;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTabs();
        setupTable();
        setupCharts();
        loadSampleData();
        setupButtons();
    }
    
    private void setupTabs() {
        // Set up tab switching
        overviewTab.setOnAction(e -> switchToTab("overview"));
        trendsTab.setOnAction(e -> switchToTab("trends"));
        medicationTab.setOnAction(e -> switchToTab("medication"));
        notesTab.setOnAction(e -> switchToTab("notes"));
    }
    
    private void switchToTab(String tabName) {
        // Hide all content
        overviewContent.setVisible(false);
        trendsContent.setVisible(false);
        medicationContent.setVisible(false);
        notesContent.setVisible(false);
        
        // Reset tab styles
        resetTabStyles();
        
        // Show selected content and update tab style
        switch (tabName) {
            case "overview":
                overviewContent.setVisible(true);
                setActiveTabStyle(overviewTab);
                break;
            case "trends":
                trendsContent.setVisible(true);
                setActiveTabStyle(trendsTab);
                loadTrendsContent();
                break;
            case "medication":
                medicationContent.setVisible(true);
                setActiveTabStyle(medicationTab);
                loadMedicationContent();
                break;
            case "notes":
                notesContent.setVisible(true);
                setActiveTabStyle(notesTab);
                loadNotesContent();
                break;
        }
    }
    
    private void resetTabStyles() {
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-border-width: 0 0 1 0; -fx-border-color: #2C3E50;";
        overviewTab.setStyle(inactiveStyle);
        trendsTab.setStyle(inactiveStyle);
        medicationTab.setStyle(inactiveStyle);
        notesTab.setStyle(inactiveStyle);
    }
    
    private void setActiveTabStyle(Button activeTab) {
        String activeStyle = "-fx-background-color: #3498DB; -fx-text-fill: white; -fx-border-width: 0 0 3 0; -fx-border-color: #3498DB;";
        activeTab.setStyle(activeStyle);
    }
    
    private void setupTable() {
        therapyModifications = FXCollections.observableArrayList();
        
        dateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        modificationColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getModification()));
        modifiedByColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getModifiedBy()));
        
        therapyModificationsTable.setItems(therapyModifications);
        
        // Custom table styling
        therapyModificationsTable.setRowFactory(tv -> {
            TableRow<TherapyModification> row = new TableRow<>();
            row.setStyle("-fx-background-color: #34495E; -fx-text-fill: white;");
            return row;
        });
    }
    
    private void setupCharts() {
        // Configure glucose trends chart
        glucoseTrendsChart.setAnimated(false);
        glucoseTrendsChart.getXAxis().setLabel("Days");
        glucoseTrendsChart.getYAxis().setLabel("mg/dL");
        
        // Add sample data to chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Glucose Levels");
        
        series.getData().add(new XYChart.Data<>("Day 1", 120));
        series.getData().add(new XYChart.Data<>("Day 7", 135));
        series.getData().add(new XYChart.Data<>("Day 14", 142));
        series.getData().add(new XYChart.Data<>("Day 21", 138));
        series.getData().add(new XYChart.Data<>("Day 30", 145));
        
        glucoseTrendsChart.getData().add(series);
    }
    
    private void setupButtons() {
        modifyTherapyBtn.setOnAction(e -> handleModifyTherapy());
        updatePatientInfoBtn.setOnAction(e -> handleUpdatePatientInfo());
    }
    
    private void loadSampleData() {
        // Load sample patient data
        currentPatient = new Patient("Sophia Bennett", "123456");
        updatePatientInfo();
        
        // Load sample symptoms
        loadSymptoms();
        
        // Load sample therapy modifications
        loadTherapyModifications();
    }
    
    private void updatePatientInfo() {
        if (currentPatient != null) {
            patientNameLabel.setText(currentPatient.getName());
            patientIdLabel.setText("Patient ID: " + currentPatient.getId());
            lastVisitLabel.setText("Last visit: " + formatLastVisit(currentPatient.getLastVisit()));
        }
    }
    
    private String formatLastVisit(LocalDateTime lastVisit) {
        if (lastVisit == null) return "Never";
        
        long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(lastVisit.toLocalDate(), LocalDate.now());
        if (daysAgo == 0) return "Today";
        if (daysAgo == 1) return "Yesterday";
        if (daysAgo < 7) return daysAgo + " days ago";
        if (daysAgo < 14) return "1 week ago";
        if (daysAgo < 30) return (daysAgo / 7) + " weeks ago";
        return (daysAgo / 30) + " months ago";
    }
    
    private void loadSymptoms() {
        symptomsContainer.getChildren().clear();
        
        String[] symptoms = {"Fatigue", "Frequent urination", "Increased thirst"};
        String[] severities = {"Moderate", "Mild", "Severe"};
        String[] colors = {"#F39C12", "#2ECC71", "#E74C3C"};
        
        for (int i = 0; i < symptoms.length; i++) {
            HBox symptomBox = new HBox(10);
            symptomBox.setPadding(new Insets(8, 12, 8, 12));
            symptomBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4;");
            
            Label symptomLabel = new Label(symptoms[i]);
            symptomLabel.setTextFill(Color.WHITE);
            symptomLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
            
            Label severityLabel = new Label(severities[i]);
            severityLabel.setTextFill(Color.web(colors[i]));
            severityLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            severityLabel.setStyle("-fx-background-color: " + colors[i] + "33; -fx-background-radius: 3; -fx-padding: 2 6 2 6;");
            
            symptomBox.getChildren().addAll(symptomLabel, severityLabel);
            symptomsContainer.getChildren().add(symptomBox);
        }
    }
    
    private void loadTherapyModifications() {
        therapyModifications.clear();
        
        therapyModifications.add(new TherapyModification(
            LocalDate.now().minusDays(5),
            "Increased Metformin to 1000mg twice daily",
            "Dr. Smith"
        ));
        
        therapyModifications.add(new TherapyModification(
            LocalDate.now().minusDays(12),
            "Added evening insulin dose (10 units)",
            "Dr. Johnson"
        ));
        
        therapyModifications.add(new TherapyModification(
            LocalDate.now().minusDays(25),
            "Started blood glucose monitoring 3x daily",
            "Nurse Williams"
        ));
    }
    
    private void loadTrendsContent() {
        if (trendsContent.getChildren().isEmpty()) {
            Label placeholder = new Label("Trends analysis will be displayed here");
            placeholder.setTextFill(Color.web("#BDC3C7"));
            placeholder.setFont(Font.font(16));
            trendsContent.getChildren().add(placeholder);
        }
    }
    
    private void loadMedicationContent() {
        if (medicationContent.getChildren().isEmpty()) {
            Label placeholder = new Label("Detailed medication information will be displayed here");
            placeholder.setTextFill(Color.web("#BDC3C7"));
            placeholder.setFont(Font.font(16));
            medicationContent.getChildren().add(placeholder);
        }
    }
    
    private void loadNotesContent() {
        if (notesContent.getChildren().isEmpty()) {
            VBox notesBox = new VBox(10);
            
            Label notesTitle = new Label("Clinical Notes");
            notesTitle.setTextFill(Color.WHITE);
            notesTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
            
            TextArea notesArea = new TextArea();
            notesArea.setPrefRowCount(10);
            notesArea.setStyle("-fx-control-inner-background: #34495E; -fx-text-fill: white;");
            notesArea.setPromptText("Enter clinical notes here...");
            
            if (isDoctorView) {
                Button saveNotesBtn = new Button("Save Notes");
                saveNotesBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 5;");
                saveNotesBtn.setOnAction(e -> handleSaveNotes(notesArea.getText()));
                notesBox.getChildren().addAll(notesTitle, notesArea, saveNotesBtn);
            } else {
                notesArea.setEditable(false);
                notesBox.getChildren().addAll(notesTitle, notesArea);
            }
            
            notesContent.getChildren().add(notesBox);
        }
    }
    
    private void handleModifyTherapy() {
        if (isDoctorView) {
            // Open therapy modification dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Modify Therapy");
            alert.setHeaderText("Therapy Modification");
            alert.setContentText("Therapy modification dialog would open here.");
            alert.showAndWait();
        } else {
            // Patient view - show information only
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Therapy Information");
            alert.setHeaderText("Current Therapy");
            alert.setContentText("Contact your doctor to discuss therapy modifications.");
            alert.showAndWait();
        }
    }
    
    private void handleUpdatePatientInfo() {
        if (isDoctorView) {
            // Open patient info editing dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Update Patient Info");
            alert.setHeaderText("Edit Patient Information");
            alert.setContentText("Patient information editing dialog would open here.");
            alert.showAndWait();
        } else {
            // Patient view - show read-only info
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Patient Information");
            alert.setHeaderText("Your Information");
            alert.setContentText("Contact your healthcare provider to update your information.");
            alert.showAndWait();
        }
    }
    
    private void handleSaveNotes(String notes) {
        // Save notes logic
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notes Saved");
        alert.setHeaderText("Success");
        alert.setContentText("Clinical notes have been saved successfully.");
        alert.showAndWait();
    }
    
    // Public methods for setting up the view
    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        updatePatientInfo();
    }
    
    public void setDoctorView(boolean isDoctorView) {
        this.isDoctorView = isDoctorView;
        updateViewPermissions();
    }
    
    private void updateViewPermissions() {
        if (!isDoctorView) {
            // Hide/disable doctor-only features
            modifyTherapyBtn.setDisable(true);
            modifyTherapyBtn.setText("View Therapy");
            updatePatientInfoBtn.setDisable(true);
            updatePatientInfoBtn.setText("View Info");
        }
    }
    
    // Inner classes for data models
    public static class Patient {
        private String name;
        private String id;
        private LocalDateTime lastVisit;
        
        public Patient(String name, String id) {
            this.name = name;
            this.id = id;
            this.lastVisit = LocalDateTime.now().minusDays(14); // Default 2 weeks ago
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public LocalDateTime getLastVisit() { return lastVisit; }
        public void setLastVisit(LocalDateTime lastVisit) { this.lastVisit = lastVisit; }
    }
    
    public static class TherapyModification {
        private LocalDate date;
        private String modification;
        private String modifiedBy;
        
        public TherapyModification(LocalDate date, String modification, String modifiedBy) {
            this.date = date;
            this.modification = modification;
            this.modifiedBy = modifiedBy;
        }
        
        // Getters and setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getModification() { return modification; }
        public void setModification(String modification) { this.modification = modification; }
        public String getModifiedBy() { return modifiedBy; }
        public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
    }
}