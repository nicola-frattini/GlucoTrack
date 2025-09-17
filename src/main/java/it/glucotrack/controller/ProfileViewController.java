package it.glucotrack.controller;

import it.glucotrack.model.*;
import it.glucotrack.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProfileViewController implements Initializable {

    // Header elements
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
    @FXML private LineChart<String, Number> glucoseChart;
    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private Label currentGlucoseLabel;
    @FXML private Label trendLabel;
    @FXML private Label statusLabel;
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
    private StackPane parentContentPane;

    // DAOs
    private GlucoseMeasurementDAO glucoseMeasurementDAO;
    private SymptomDAO symptomDAO;
    private MedicationDAO medicationDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize DAOs
        this.glucoseMeasurementDAO = new GlucoseMeasurementDAO();
        this.symptomDAO = new SymptomDAO();
        this.medicationDAO = new MedicationDAO();

        timeRangeCombo.getItems().addAll("Ultimi 7 giorni", "Ultimi 30 giorni", "Ultimo anno");
        timeRangeCombo.setOnAction(e -> {
            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            System.out.println("🔄 Cambio periodo: " + selectedPeriod);// Pausa breve per evitare conflitti nel refresh del grafico
            javafx.application.Platform.runLater(() -> {
                try {
                    // Aggiorna sia i dati numerici che il grafico quando cambia il periodo
                    updateGlucoseData();
                    updateChart();
                    System.out.println("✅ Aggiornamento completato per periodo: " + selectedPeriod);
                } catch (Exception ex) {
                    System.err.println("❌ Errore durante il cambio periodo: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        });

        // Seleziona il default (7 giorni) e trigger del listener
        timeRangeCombo.getSelectionModel().select("Ultimi 7 giorni");




        setupTabs();
        setupTable();
        setupCharts();
        setupButtons();
    }

    private void setupTabs() {
        overviewTab.setOnAction(e -> switchToTab("overview"));
        trendsTab.setOnAction(e -> switchToTab("trends"));
        medicationTab.setOnAction(e -> switchToTab("medication"));
        notesTab.setOnAction(e -> switchToTab("notes"));
        switchToTab("overview");
    }

    private void switchToTab(String tabName) {
        overviewContent.setVisible(false);
        trendsContent.setVisible(false);
        medicationContent.setVisible(false);
        notesContent.setVisible(false);

        resetTabStyles();

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

        therapyModificationsTable.setRowFactory(tv -> {
            TableRow<TherapyModification> row = new TableRow<>();
            row.setStyle("-fx-background-color: #34495E; -fx-text-fill: white;");
            return row;
        });
    }

    private void setupCharts() {
        glucoseChart.setAnimated(false);
        glucoseChart.getXAxis().setLabel("Date");
        glucoseChart.getYAxis().setLabel("mg/dL");
        glucoseChart.setLegendVisible(false);
    }



    private void setupButtons() {
        modifyTherapyBtn.setOnAction(e -> handleModifyTherapy());
        updatePatientInfoBtn.setOnAction(e -> handleUpdatePatientInfo());
    }

    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        if (patient != null) {
            try {
                // Fetch all data for the patient from the database
                List<GlucoseMeasurement> measurements = glucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(patient.getId());
                patient.setGlucoseReadings(measurements);

                List<Symptom> symptoms = symptomDAO.getSymptomsForTable(patient.getId());
                // The Patient model stores symptoms as a List<String>
                patient.setSymptoms(symptoms.stream().map(Symptom::getSymptomName).collect(Collectors.toList()));

                List<Medication> medications = medicationDAO.getMedicationsByPatientId(patient.getId());
                patient.setMedications(medications);

                // Update the UI with the fetched data
                updatePatientInfo();
                updatePatientSpecificData();
                loadTherapyModifications();

            } catch (SQLException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText("Failed to load patient data");
                alert.setContentText("There was an error accessing the database: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    public void setDoctorView(boolean isDoctorView) {
        this.isDoctorView = isDoctorView;
        updateViewPermissions();
    }

    private void updatePatientInfo() {
        if (currentPatient != null) {
            patientNameLabel.setText(currentPatient.getFullName());
            patientIdLabel.setText("Patient ID: " + currentPatient.getId());

            LocalDateTime lastVisit = null;
            if (!currentPatient.getGlucoseReadings().isEmpty()) {
                lastVisit = currentPatient.getGlucoseReadings().stream()
                        .max((r1, r2) -> r1.getDateAndTime().compareTo(r2.getDateAndTime()))
                        .map(GlucoseMeasurement::getDateAndTime)
                        .orElse(null);
            }
            lastVisitLabel.setText("Last visit: " + formatLastVisit(lastVisit));
        }
    }

    private void updatePatientSpecificData() {
        if (currentPatient != null) {
            loadRealSymptoms();
            updateGlucoseData();
            updateMedicationProgress();
        }
    }

    private void loadRealSymptoms() {
        symptomsContainer.getChildren().clear();

        List<String> symptoms = currentPatient.getSymptoms();
        if (symptoms == null || symptoms.isEmpty()) {
            Label noSymptomsLabel = new Label("No symptoms reported");
            noSymptomsLabel.setTextFill(Color.web("#BDC3C7"));
            noSymptomsLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
            noSymptomsLabel.setPadding(new Insets(10));
            symptomsContainer.getChildren().add(noSymptomsLabel);
            return;
        }

        for (String symptom : symptoms) {
            HBox symptomBox = new HBox(10);
            symptomBox.setPadding(new Insets(8, 12, 8, 12));
            symptomBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 4;");

            Label symptomLabel = new Label(symptom);
            symptomLabel.setTextFill(Color.WHITE);
            symptomLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

            String severity = determineSeverity(symptom);
            String severityColor = getSeverityColor(severity);

            Label severityLabel = new Label(severity);
            severityLabel.setTextFill(Color.web(severityColor));
            severityLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            severityLabel.setStyle("-fx-background-color: " + severityColor + "33; -fx-background-radius: 3; -fx-padding: 2 6 2 6;");

            symptomBox.getChildren().addAll(symptomLabel, severityLabel);
            symptomsContainer.getChildren().add(symptomBox);
        }
    }

    private String determineSeverity(String symptom) {
        String lowerSymptom = symptom.toLowerCase();
        if (lowerSymptom.contains("severe") || lowerSymptom.contains("excessive") ||
                lowerSymptom.contains("frequent") || lowerSymptom.contains("extreme")) {
            return "High";
        } else if (lowerSymptom.contains("moderate") || lowerSymptom.contains("occasional") ||
                lowerSymptom.contains("mild")) {
            return "Moderate";
        } else {
            return "Normal";
        }
    }

    private String getSeverityColor(String severity) {
        switch (severity) {
            case "High": return "#E74C3C";
            case "Moderate": return "#F39C12";
            case "Normal": return "#2ECC71";
            default: return "#3498DB";
        }
    }

    private List<GlucoseMeasurement> filterMeasurementsByPeriod(List<GlucoseMeasurement> measurements, int daysBack) {
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(daysBack);
        return measurements.stream()
                .filter(m -> m.getDateAndTime().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }

    private void setStatusWithColor(float glucose) {
        String statusText;
        String colorStyle;

        if (glucose < 70) {
            statusText = "Low";
            colorStyle = "-fx-text-fill: #f44336;"; // Rosso per valori bassi (stesso del High)
        } else if (glucose <= 140) {
            statusText = "Normal";
            colorStyle = "-fx-text-fill: #4caf50;"; // Verde per valori normali (70-140)
        } else if (glucose <= 180) {
            statusText = "Elevated";
            colorStyle = "-fx-text-fill: #ff9800;"; // Arancione per valori elevati (140-180)
        } else {
            statusText = "High";
            colorStyle = "-fx-text-fill: #f44336;"; // Rosso per valori alti (>180)
        }

        statusLabel.setText(statusText);
        statusLabel.setStyle(colorStyle);
    }

    private void updateGlucoseData() {
        if (currentPatient == null) return;

        try {
            List<GlucoseMeasurement> allMeasurements =
                    glucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(currentPatient.getId());

            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            int daysBack = getDaysFromPeriod(selectedPeriod);
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);

            List<GlucoseMeasurement> filteredMeasurements = allMeasurements.stream()
                    .filter(m -> !m.getDateAndTime().isBefore(cutoffDate))
                    .sorted((a, b) -> a.getDateAndTime().compareTo(b.getDateAndTime()))
                    .collect(Collectors.toList());

            if (filteredMeasurements.isEmpty()) {
                currentGlucoseLabel.setText("N/A");
                trendLabel.setText("N/A");
                statusLabel.setText("");
                return;
            }

            // L’ultima misurazione è la più recente
            GlucoseMeasurement latest = filteredMeasurements.get(filteredMeasurements.size() - 1);
            currentGlucoseLabel.setText(String.format("%.0f", latest.getGlucoseLevel()));
            setStatusWithColor(latest.getGlucoseLevel());

            // Trend: confronto primo e ultimo valore del periodo
            if (filteredMeasurements.size() > 1) {
                GlucoseMeasurement first = filteredMeasurements.get(0);
                double change = ((latest.getGlucoseLevel() - first.getGlucoseLevel()) / first.getGlucoseLevel()) * 100;
                if (Math.abs(change) < 1) {
                    trendLabel.setText("Stabile");
                    trendLabel.setStyle("-fx-text-fill: #8892b0;");
                } else if (change > 0) {
                    trendLabel.setText(String.format("↑ %.1f%%", change));
                    trendLabel.setStyle("-fx-text-fill: #f44336;");
                } else {
                    trendLabel.setText(String.format("↓ %.1f%%", Math.abs(change)));
                    trendLabel.setStyle("-fx-text-fill: #4caf50;");
                }
            } else {
                trendLabel.setText("N/A");
                trendLabel.setStyle("-fx-text-fill: #8892b0;");
            }

            // Aggiorna grafico
            updateChart();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private void calculateAndDisplayStatistics(List<GlucoseMeasurement> measurements) {
        if (measurements.isEmpty()) return;

        // Ordina per data (più recente per primo)
        measurements.sort((a, b) -> b.getDateAndTime().compareTo(a.getDateAndTime()));

        // Valore corrente (più recente nel periodo)
        GlucoseMeasurement latest = measurements.get(0);
        currentGlucoseLabel.setText(String.format("%.0f", latest.getGlucoseLevel()));

        // Status basato sul valore più recente con colore
        setStatusWithColor(latest.getGlucoseLevel());

        // Calcola trend confrontando prima e ultima misurazione del periodo
        if (measurements.size() > 1) {
            GlucoseMeasurement oldest = measurements.get(measurements.size() - 1);
            double change = ((double)(latest.getGlucoseLevel() - oldest.getGlucoseLevel()) / oldest.getGlucoseLevel()) * 100;

            String trendText;
            String trendColor;
            if (Math.abs(change) < 1.0) {
                trendText = "Stabile";
                trendColor = "-fx-text-fill: #8892b0;"; // Grigio per stabile
            } else if (change > 0) {
                trendText = String.format("↑ %.1f%%", change);
                trendColor = "-fx-text-fill: #f44336;"; // Rosso per trend positivo (peggioramento)
            } else {
                trendText = String.format("↓ %.1f%%", Math.abs(change));
                trendColor = "-fx-text-fill: #4caf50;"; // Verde per trend negativo (miglioramento)
            }
            trendLabel.setText(trendText);
            trendLabel.setStyle(trendColor);
        } else {
            trendLabel.setText("N/A");
            trendLabel.setStyle("-fx-text-fill: #8892b0;");
        }
    }

    private void updateChart() {
        try {
            if (currentPatient == null) return;

            glucoseChart.getData().clear();
            glucoseChart.setAnimated(false);

            List<GlucoseMeasurement> measurements = glucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(currentPatient.getId());
            if (measurements.isEmpty()) return;

            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            int daysBack = getDaysFromPeriod(selectedPeriod);
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);

            List<GlucoseMeasurement> filteredMeasurements = measurements.stream()
                    .filter(m -> !m.getDateAndTime().isBefore(cutoffDate))
                    .sorted((a, b) -> a.getDateAndTime().compareTo(b.getDateAndTime()))
                    .collect(Collectors.toList());

            if (filteredMeasurements.isEmpty()) return;

            XYChart.Series<String, Number> series = new XYChart.Series<>();

            for (GlucoseMeasurement m : filteredMeasurements) {
                String dateStr = formatDateForChart(m.getDateAndTime(), selectedPeriod);
                series.getData().add(new XYChart.Data<>(dateStr, m.getGlucoseLevel()));
            }

            glucoseChart.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private int getMaxPointsForPeriod(String period) {
        switch (period) {
            case "Ultimi 7 giorni": return 15;
            case "Ultimi 30 giorni": return 20;
            case "Ultimo anno": return 25;
            default: return 15;
        }
    }


    private String formatDateForChart(java.time.LocalDateTime dateTime, String period) {
        try {
            switch (period) {
                case "Ultimi 7 giorni":
                    // Formato compatto per 7 giorni
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                case "Ultimi 30 giorni":
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                case "Ultimo anno":
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
                default:
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel formato data: " + e.getMessage());
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
        }
    }

    private double calculateMedicationAdherence(int patientId, MedicationDAO medicationDAO, LogMedicationDAO logMedicationDAO) {
        try {
            java.util.List<Medication> meds = medicationDAO.getActiveMedicationsByPatientId(patientId);
            if (meds.isEmpty()) return 100.0;
            double total = 0, count = 0;
            for (Medication med : meds) {
                java.util.List<it.glucotrack.model.LogMedication> logs = logMedicationDAO.getLogMedicationsByMedicationIdUpToNow(med.getId());
                if (logs.isEmpty()) continue;
                long taken = logs.stream().filter(it.glucotrack.model.LogMedication::isTaken).count();
                total += (double)taken / logs.size();
                count++;
            }
            return count > 0 ? (total / count) * 100.0 : 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void updateMedicationProgress() {
        if (currentPatient != null) {
            try {
                LogMedicationDAO logMedicationDAO = new LogMedicationDAO();
                List<Medication> meds = currentPatient.getMedications();

                System.out.println("🔹 Paziente ID: " + currentPatient.getId());
                System.out.println("🔹 Numero di terapie attive: " + meds.size());

                if (meds.isEmpty()) {
                    System.out.println("⚠️ Nessuna terapia attiva trovata.");
                    metforminProgressBar.setProgress(0);
                    insulinProgressBar.setProgress(0);
                    metforminPercentageLabel.setText("0%");
                    insulinPercentageLabel.setText("0%");
                    return;
                }

                double totalAdherence = 0.0;
                int countedMeds = 0;

                for (Medication med : meds) {
                    List<it.glucotrack.model.LogMedication> logs = logMedicationDAO.getLogMedicationsByMedicationIdUpToNow(med.getId());
                    System.out.println("🔹 Terapia: " + med.getName_medication() + " | Log trovati: " + logs.size());

                    if (logs.isEmpty()) continue;

                    long taken = logs.stream().filter(it.glucotrack.model.LogMedication::isTaken).count();
                    double adherence = (double) taken / logs.size();
                    System.out.println("   - Somministrazioni prese: " + taken + "/" + logs.size() + " -> Adherence: " + String.format("%.2f", adherence * 100) + "%");

                    totalAdherence += adherence;
                    countedMeds++;
                }

                double adherencePercentage = countedMeds > 0 ? (totalAdherence / countedMeds) * 100.0 : 0.0;
                double progress = adherencePercentage / 100.0;

                System.out.println("🔹 Adherence totale calcolata: " + String.format("%.2f", adherencePercentage) + "%");

                metforminProgressBar.setProgress(progress);
                insulinProgressBar.setProgress(progress);
                metforminPercentageLabel.setText(String.format("%.0f%%", adherencePercentage));
                insulinPercentageLabel.setText(String.format("%.0f%%", adherencePercentage));

            } catch (Exception e) {
                e.printStackTrace();
                metforminProgressBar.setProgress(0);
                insulinProgressBar.setProgress(0);
                metforminPercentageLabel.setText("0%");
                insulinPercentageLabel.setText("0%");
            }
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

    private void loadTherapyModifications() {
        therapyModifications.clear();
        if (currentPatient != null) {
            therapyModifications.add(new TherapyModification(
                    LocalDate.now().minusDays(5),
                    "Updated glucose monitoring frequency",
                    "Dr. Smith"
            ));
            if (!currentPatient.getGlucoseReadings().isEmpty()) {
                double avgGlucose = currentPatient.getGlucoseReadings().stream()
                        .mapToDouble(GlucoseMeasurement::getGlucoseLevel)
                        .average()
                        .orElse(0);
                if (avgGlucose > 180) {
                    therapyModifications.add(new TherapyModification(
                            LocalDate.now().minusDays(12),
                            "Increased insulin dosage due to high glucose levels",
                            "Dr. Johnson"
                    ));
                } else if (avgGlucose > 140) {
                    therapyModifications.add(new TherapyModification(
                            LocalDate.now().minusDays(12),
                            "Adjusted Metformin dosage",
                            "Dr. Johnson"
                    ));
                }
            }
            therapyModifications.add(new TherapyModification(
                    LocalDate.now().minusDays(25),
                    "Initial diabetes management plan established",
                    "Dr. Smith"
            ));
        }
    }

    private void loadTrendsContent() {
        if (!trendsContent.getChildren().isEmpty()) return;
        VBox trendsBox = new VBox(15);
        trendsBox.setPadding(new Insets(20));

        Label trendsTitle = new Label("Glucose Trends Analysis");
        trendsTitle.setTextFill(Color.WHITE);
        trendsTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

        if (currentPatient != null && !currentPatient.getGlucoseReadings().isEmpty()) {
            Label trendAnalysis = createTrendAnalysis();
            trendsBox.getChildren().addAll(trendsTitle, trendAnalysis);
        } else {
            Label noDataLabel = new Label("No glucose data available for trend analysis");
            noDataLabel.setTextFill(Color.web("#BDC3C7"));
            noDataLabel.setFont(Font.font(16));
            trendsBox.getChildren().addAll(trendsTitle, noDataLabel);
        }

        trendsContent.getChildren().add(trendsBox);
    }

    private int getDaysFromPeriod(String period) {
        switch (period) {
            case "Ultimi 7 giorni": return 7;
            case "Ultimi 30 giorni": return 30;
            case "Ultimo anno": return 365;
            default: return 7;
        }
    }


    private Label createTrendAnalysis() {
        StringBuilder analysis = new StringBuilder();

        List<GlucoseMeasurement> readings = currentPatient.getGlucoseReadings();
        double average = readings.stream().mapToDouble(GlucoseMeasurement::getGlucoseLevel).average().orElse(0);
        long highReadings = readings.stream().filter(r -> r.getGlucoseLevel() > 180).count();
        long normalReadings = readings.stream().filter(r -> r.getGlucoseLevel() >= 80 && r.getGlucoseLevel() <= 140).count();

        analysis.append(String.format("Average glucose level: %.1f mg/dL\n", average));
        analysis.append(String.format("Total readings: %d\n", readings.size()));
        if (readings.size() > 0) {
            analysis.append(String.format("High readings (>180): %d (%.1f%%)\n",
                    highReadings, (double)highReadings / readings.size() * 100));
            analysis.append(String.format("Normal readings (80-140): %d (%.1f%%)\n",
                    normalReadings, (double)normalReadings / readings.size() * 100));
        } else {
            analysis.append("High readings (>180): 0 (0.0%)\n");
            analysis.append("Normal readings (80-140): 0 (0.0%)\n");
        }


        if (average > 180) {
            analysis.append("\nRecommendation: Glucose levels are consistently high. Consider medication adjustment.");
        } else if (average > 140) {
            analysis.append("\nRecommendation: Glucose levels are moderately elevated. Continue monitoring closely.");
        } else {
            analysis.append("\nRecommendation: Glucose levels are well controlled. Maintain current regimen.");
        }

        Label analysisLabel = new Label(analysis.toString());
        analysisLabel.setTextFill(Color.WHITE);
        analysisLabel.setFont(Font.font(14));
        analysisLabel.setWrapText(true);

        return analysisLabel;
    }

    private void loadMedicationContent() {
        if (!medicationContent.getChildren().isEmpty()) {
            return;
        }

        VBox medicationBox = new VBox(15);
        medicationBox.setPadding(new Insets(20));

        Label medicationTitle = new Label("Current Medications");
        medicationTitle.setTextFill(Color.WHITE);
        medicationTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

        VBox medicationsList = new VBox(10);
        if (currentPatient != null && !currentPatient.getMedications().isEmpty()) {
            for (Medication medication : currentPatient.getMedications()) {
                HBox medBox = createMedicationBox(
                        medication.getName_medication(),
                        medication.getDose(),
                        medication.getFreq().getDisplayName(),
                        medication.getInstructions()
                );
                medicationsList.getChildren().add(medBox);
            }
        } else {
            Label noMedsLabel = new Label("No medications currently prescribed.");
            noMedsLabel.setTextFill(Color.web("#BDC3C7"));
            noMedsLabel.setFont(Font.font(14));
            medicationsList.getChildren().add(noMedsLabel);
        }

        medicationBox.getChildren().addAll(medicationTitle, medicationsList);
        medicationContent.getChildren().add(medicationBox);
    }

    private HBox createMedicationBox(String name, String dosage, String frequency, String instructions) {
        HBox medBox = new HBox(15);
        medBox.setPadding(new Insets(10));
        medBox.setStyle("-fx-background-color: #34495E; -fx-background-radius: 5;");
        VBox medInfo = new VBox(5);
        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        Label dosageLabel = new Label("Dosage: " + dosage);
        dosageLabel.setTextFill(Color.web("#BDC3C7"));
        dosageLabel.setFont(Font.font(12));
        Label frequencyLabel = new Label("Frequency: " + frequency);
        frequencyLabel.setTextFill(Color.web("#BDC3C7"));
        frequencyLabel.setFont(Font.font(12));
        Label instructionsLabel = new Label("Instructions: " + instructions);
        instructionsLabel.setTextFill(Color.web("#BDC3C7"));
        instructionsLabel.setFont(Font.font(12));
        medInfo.getChildren().addAll(nameLabel, dosageLabel, frequencyLabel, instructionsLabel);
        medBox.getChildren().add(medInfo);
        return medBox;
    }

    private void loadNotesContent() {
        if (!notesContent.getChildren().isEmpty()) return;

        VBox notesBox = new VBox(10);
        notesBox.setPadding(new Insets(20));
        Label notesTitle = new Label("Clinical Notes");
        notesTitle.setTextFill(Color.WHITE);
        notesTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(10);
        notesArea.setStyle("-fx-control-inner-background: #34495E; -fx-text-fill: white;");
        notesArea.setPromptText("Enter clinical notes here...");

        if (currentPatient != null) {
            StringBuilder notes = new StringBuilder();
            notes.append("Patient: ").append(currentPatient.getFullName()).append("\n");
            notes.append("Date: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");

            if (!currentPatient.getGlucoseReadings().isEmpty()) {
                double avgGlucose = currentPatient.getGlucoseReadings().stream()
                        .mapToDouble(GlucoseMeasurement::getGlucoseLevel)
                        .average().orElse(0);
                notes.append("Average glucose level: ").append(String.format("%.1f mg/dL", avgGlucose)).append("\n");
            }
            if (!currentPatient.getSymptoms().isEmpty()) {
                notes.append("Reported symptoms: ").append(String.join(", ", currentPatient.getSymptoms())).append("\n");
            }
            notes.append("\nClinical observations:\n");
            notes.append("- Patient compliance appears good\n");
            notes.append("- Continue current medication regimen\n");
            notes.append("- Schedule follow-up in 2 weeks\n");
            notesArea.setText(notes.toString());
        }

        if (isDoctorView) {
            Button saveNotesBtn = new Button("Save Notes");
            saveNotesBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 10 20;");
            saveNotesBtn.setOnAction(e -> handleSaveNotes(notesArea.getText()));
            notesBox.getChildren().addAll(notesTitle, notesArea, saveNotesBtn);
        } else {
            notesArea.setEditable(false);
            notesBox.getChildren().addAll(notesTitle, notesArea);
        }

        notesContent.getChildren().add(notesBox);
    }

    private void updateViewPermissions() {
        if (!isDoctorView) {
            modifyTherapyBtn.setDisable(true);
            modifyTherapyBtn.setText("View Therapy");
            updatePatientInfoBtn.setDisable(true);
            updatePatientInfoBtn.setText("View Info");
        } else {
            modifyTherapyBtn.setDisable(false);
            modifyTherapyBtn.setText("Modify Therapy");
            updatePatientInfoBtn.setDisable(false);
            updatePatientInfoBtn.setText("Update Info");
        }
    }

    private void handleModifyTherapy() {
        if (isDoctorView) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Modify Therapy");
            alert.setHeaderText("Therapy Modification");
            alert.setContentText("Therapy modification dialog would open here for " +
                    (currentPatient != null ? currentPatient.getFullName() : "patient"));
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Therapy Information");
            alert.setHeaderText("Current Therapy");
            alert.setContentText("Contact your doctor to discuss therapy modifications.");
            alert.showAndWait();
        }
    }

    private void handleUpdatePatientInfo() {
        if (isDoctorView) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Update Patient Info");
            alert.setHeaderText("Edit Patient Information");
            alert.setContentText("Patient information editing dialog would open here for " +
                    (currentPatient != null ? currentPatient.getFullName() : "patient"));
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Patient Information");
            alert.setHeaderText("Your Information");
            alert.setContentText("Contact your healthcare provider to update your information.");
            alert.showAndWait();
        }
    }

    private void handleSaveNotes(String notes) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notes Saved");
        alert.setHeaderText("Success");
        alert.setContentText("Clinical notes have been saved successfully for " +
                (currentPatient != null ? currentPatient.getFullName() : "patient"));
        alert.showAndWait();
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
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getModification() { return modification; }
        public void setModification(String modification) { this.modification = modification; }
        public String getModifiedBy() { return modifiedBy; }
        public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
    }

    private void handleBackToPatientsList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/DoctorDashboardPatients.fxml"));
            Parent patientsView = loader.load();
            if (parentContentPane != null) {
                parentContentPane.getChildren().clear();
                parentContentPane.getChildren().add(patientsView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Navigation Error");
            alert.setContentText("Could not return to patients list: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void setParentContentPane(StackPane contentPane) {
        this.parentContentPane = contentPane;
    }
}