package it.glucotrack.controller;

import it.glucotrack.model.*;
import it.glucotrack.util.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PatientDashboardHomeController {

    @FXML
    private ComboBox<String> timeRangeCombo;

    @FXML
    private Label currentGlucoseLabel;

    @FXML
    private Label trendLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private LineChart<String, Number> glucoseChart;

    @FXML
    private VBox alertsContainer;

    private GlucoseMeasurementDAO glucoseMeasurementDAO;

    private Patient patient;

    @FXML
    public void initialize() throws SQLException {

        this.patient = PatientDAO.getPatientById(SessionManager.getInstance().getCurrentUser().getId());

        loadAlerts();


        // Initialize DAO
        glucoseMeasurementDAO = new GlucoseMeasurementDAO();

        timeRangeCombo.getItems().addAll("Last 7 days", "Last 30 days", "Last year");

        timeRangeCombo.setOnAction(e -> {
            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            System.out.println("Change period: " + selectedPeriod);

            javafx.application.Platform.runLater(() -> {
                try {
                    updateGlucoseData();
                    updateChart();
                } catch (Exception ex) {
                    System.err.println("Error during change period: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        });


        timeRangeCombo.getSelectionModel().select("Last 7 days");

        updateGlucoseData();
        updateChart();
    }

    private void updateGlucoseData() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                List<GlucoseMeasurement> allMeasurements = glucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(currentUser.getId());

                if (!allMeasurements.isEmpty()) {

                    String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
                    int daysBack = getDaysFromPeriod(selectedPeriod);
                    List<GlucoseMeasurement> filteredMeasurements = filterMeasurementsByPeriod(allMeasurements, daysBack);

                    if (!filteredMeasurements.isEmpty()) {

                        calculateAndDisplayStatistics(filteredMeasurements);
                    } else {

                        GlucoseMeasurement latest = allMeasurements.get(0);
                        currentGlucoseLabel.setText(String.format("%.0f", latest.getGlucoseLevel()));
                        setStatusWithColor(latest.getGlucoseLevel());
                        trendLabel.setText("N/A (no data in period)");
                    }
                } else {
                    currentGlucoseLabel.setText("N/A");
                    trendLabel.setText("N/A");
                    statusLabel.setText("No data");
                }
            }
        } catch (SQLException e) {
            currentGlucoseLabel.setText("Error");
            trendLabel.setText("N/A");
            statusLabel.setText("Error");
        }
    }

    private void loadAlerts() throws SQLException {

        alertsContainer.getChildren().clear();

        List<Alert> alerts = AlertManagement.generatePatientAlerts(PatientDAO.getPatientById(
                SessionManager.getInstance().getCurrentUser().getId()));

        for (Alert alert : alerts) {
            HBox alertBox = createAlertBox(alert);
            alertsContainer.getChildren().add(alertBox);
        }
    }

    private HBox createAlertBox(Alert alert) {
        HBox box = new HBox(10);
        box.setStyle("-fx-background-radius: 10; -fx-padding: 15; -fx-pref-height: 80; -fx-alignment: center-left;");

        switch (alert.getType()) {
            case INFO:
                box.setStyle(box.getStyle() + "-fx-background-color: #4caf50;");
                break;
            case WARNING:
                box.setStyle(box.getStyle() + "-fx-background-color: #ff9800;");
                break;
            case CRITICAL:
                box.setStyle(box.getStyle() + "-fx-background-color: #f44336;");
                break;
        }

        VBox content = new VBox(5);
        Label title = new Label(alert.getMessage());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        title.setWrapText(true);

        content.getChildren().addAll(title);

        box.getChildren().addAll(content);
        return box;
    }

    private void updateChart() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) return;

            glucoseChart.getData().clear();
            glucoseChart.getXAxis().setAnimated(false);
            glucoseChart.getYAxis().setAnimated(false);
            glucoseChart.setAnimated(false);


            List<GlucoseMeasurement> measurements = glucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(currentUser.getId());

            if (measurements.isEmpty()) {
                System.err.println("No glucose measurements available for chart.");
                return;
            }


            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            int daysBack = getDaysFromPeriod(selectedPeriod);


            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(daysBack);
            List<GlucoseMeasurement> filteredMeasurements = measurements.stream()
                .filter(m -> m.getDateAndTime().isAfter(cutoffDate))
                .sorted((a, b) -> a.getDateAndTime().compareTo(b.getDateAndTime()))
                .collect(Collectors.toList());


            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Glicemia (mg/dL)");


            int maxPoints = getMaxPointsForPeriod(selectedPeriod);

            if (filteredMeasurements.size() > maxPoints) {

                double step = (double) filteredMeasurements.size() / maxPoints;
                for (int i = 0; i < maxPoints; i++) {
                    int index = (int) Math.round(i * step);
                    if (index >= filteredMeasurements.size()) {
                        index = filteredMeasurements.size() - 1;
                    }
                    GlucoseMeasurement measurement = filteredMeasurements.get(index);
                    String dateStr = formatDateForChart(measurement.getDateAndTime(), selectedPeriod);
                    series.getData().add(new XYChart.Data<>(dateStr, measurement.getGlucoseLevel()));
                }
            } else {

                for (int i = 0; i < filteredMeasurements.size(); i++) {
                    GlucoseMeasurement measurement = filteredMeasurements.get(i);
                    String dateStr = formatDateForChart(measurement.getDateAndTime(), selectedPeriod);
                    series.getData().add(new XYChart.Data<>(dateStr, measurement.getGlucoseLevel()));
                }
            }

            glucoseChart.getData().add(series);



            javafx.application.Platform.runLater(() -> {
                glucoseChart.requestLayout();
                glucoseChart.autosize();
            });

        } catch (SQLException e) {
            System.err.println("Error during chart update: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Generic error during chart update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getMaxPointsForPeriod(String period) {
        switch (period) {
            case "Last 7 days": return 15;
            case "Last 30 days": return 20;
            case "Last year": return 25;
            default: return 15;
        }
    }

    private String formatDateForChart(java.time.LocalDateTime dateTime, String period) {
        try {
            switch (period) {
                case "Last 7 days":

                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                case "Last 30 days":
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                case "Last year":
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
                default:
                    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
            }
        } catch (Exception e) {
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
        }
    }

    private int getDaysFromPeriod(String period) {
        switch (period) {
            case "Last 7 days": return 7;
            case "Last 30 days": return 30;
            case "Last anno": return 365;
            default: return 7;
        }
    }

    @FXML
    private void onLogReadingClick(ActionEvent event) {
        openGlucoseInsertForm();
    }

    @FXML
    private void onContactClick(ActionEvent event)throws Exception {
        Doctor doctor = DoctorDAO.getDoctorById(patient.getDoctorId());
        MailHelper.openMailClient(doctor.getEmail());
    }

    @FXML
    private void onRecordSymptomsClick(ActionEvent event) {
        openSymptomInsertForm();
    }

    private List<GlucoseMeasurement> filterMeasurementsByPeriod(List<GlucoseMeasurement> measurements, int daysBack) {
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(daysBack);
        return measurements.stream()
                .filter(m -> m.getDateAndTime().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }
    
    private void calculateAndDisplayStatistics(List<GlucoseMeasurement> measurements) {
        if (measurements.isEmpty()) return;
        
        measurements.sort((a, b) -> b.getDateAndTime().compareTo(a.getDateAndTime()));
        
        GlucoseMeasurement latest = measurements.get(0);
        currentGlucoseLabel.setText(String.format("%.0f", latest.getGlucoseLevel()));
        
        setStatusWithColor(latest.getGlucoseLevel());
        
        if (measurements.size() > 1) {
            GlucoseMeasurement oldest = measurements.get(measurements.size() - 1);
            double change = ((double)(latest.getGlucoseLevel() - oldest.getGlucoseLevel()) / oldest.getGlucoseLevel()) * 100;
            
            String trendText;
            String trendColor;
            if (Math.abs(change) < 1.0) {
                trendText = "Stable";
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
    

    
    private void setStatusWithColor(float glucose) {
        String statusText;
        String colorStyle;
        
        if (glucose < 70) {
            statusText = "Low";
            colorStyle = "-fx-text-fill: #f44336;";
        } else if (glucose <= 140) {
            statusText = "Normal";
            colorStyle = "-fx-text-fill: #4caf50;";
        } else if (glucose <= 180) {
            statusText = "Elevated";
            colorStyle = "-fx-text-fill: #ff9800;";
        } else {
            statusText = "High";
            colorStyle = "-fx-text-fill: #f44336;";
        }
        
        statusLabel.setText(statusText);
        statusLabel.setStyle(colorStyle);
    }
    
    private void openGlucoseInsertForm() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardGlucoseInsert.fxml"));
            Parent glucoseInsertView = loader.load();

            // Get the controller of the form
            PatientDashboardGlucoseInsertController insertController = loader.getController();

            insertController.setOnDataSaved(() -> {
                updateGlucoseData();

                returnToHome();
            });
            

            insertController.setOnCancel(this::returnToHome);


            loadContentInMainDashboard(glucoseInsertView);
            
        } catch (IOException e) {
            System.err.println("Error during the insert measurement form loading: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openSymptomInsertForm() {
        try {


            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardSymptomInsert.fxml"));
            Parent symptomInsertView = loader.load();


            PatientDashboardSymptomsInsertController insertController = loader.getController();

            insertController.setOnDataSaved(() -> {
                updateGlucoseData();
                returnToHome();
            });
            

            insertController.setOnCancel(this::returnToHome);


            loadContentInMainDashboard(symptomInsertView);
            
        } catch (IOException e) {
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
            System.err.println("Error during dashboard loading: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Metodo per tornare alla home dashboard
    private void returnToHome() {
        try {
            PatientDashboardController mainController = PatientDashboardController.getInstance();
            if (mainController != null) {
                mainController.loadCenterContent("PatientDashboardHome.fxml");
                System.out.println("✅ Ritorno alla home completato");
            } else {
                System.err.println("❌ Controller principale non disponibile per il ritorno alla home");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel ritorno alla home: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

