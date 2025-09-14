package it.glucotrack.controller;

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
import it.glucotrack.model.GlucoseMeasurement;
import it.glucotrack.model.User;
import it.glucotrack.util.GlucoseMeasurementDAO;
import it.glucotrack.util.SessionManager;

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
    
    private GlucoseMeasurementDAO glucoseMeasurementDAO;

    @FXML
    public void initialize() {
        System.out.println("🏠 PatientDashboardHomeController inizializzato!");
        
        // Initialize DAO
        glucoseMeasurementDAO = new GlucoseMeasurementDAO();
        
        // Inizializza la combo dei range temporali
        timeRangeCombo.getItems().addAll("Ultimi 7 giorni", "Ultimi 30 giorni", "Ultimo anno");
        
        // Listener per il cambio di periodo temporale
        timeRangeCombo.setOnAction(e -> {
            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            System.out.println("🔄 Cambio periodo: " + selectedPeriod);
            
            // Pausa breve per evitare conflitti nel refresh del grafico
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

        // Inizializza i dati della dashboard
        updateGlucoseData();
        updateChart();
    }

    private void updateGlucoseData() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                List<GlucoseMeasurement> allMeasurements = glucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(currentUser.getId());
                
                if (!allMeasurements.isEmpty()) {
                    // Filtra i dati in base al periodo selezionato
                    String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
                    int daysBack = getDaysFromPeriod(selectedPeriod);
                    List<GlucoseMeasurement> filteredMeasurements = filterMeasurementsByPeriod(allMeasurements, daysBack);
                    
                    if (!filteredMeasurements.isEmpty()) {
                        // Calcola statistiche sui dati filtrati
                        calculateAndDisplayStatistics(filteredMeasurements);
                    } else {
                        // Nessun dato nel periodo selezionato, usa l'ultima misurazione disponibile
                        GlucoseMeasurement latest = allMeasurements.get(0);
                        currentGlucoseLabel.setText(String.format("%.0f", latest.getGlucoseLevel()));
                        setStatusWithColor(latest.getGlucoseLevel());
                        trendLabel.setText("N/A (nessun dato nel periodo)");
                    }
                } else {
                    currentGlucoseLabel.setText("N/A");
                    trendLabel.setText("N/A");
                    statusLabel.setText("Nessun dato");
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero dati glucosio: " + e.getMessage());
            currentGlucoseLabel.setText("Errore");
            trendLabel.setText("N/A");
            statusLabel.setText("Errore");
        }
    }

    private void updateChart() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) return;
            
            // Pulizia completa del grafico
            glucoseChart.getData().clear();
            glucoseChart.getXAxis().setAnimated(false);
            glucoseChart.getYAxis().setAnimated(false);
            glucoseChart.setAnimated(false);
            
            // Ottieni i dati dal database
            List<GlucoseMeasurement> measurements = glucoseMeasurementDAO.getGlucoseMeasurementsByPatientId(currentUser.getId());
            
            if (measurements.isEmpty()) {
                System.out.println("⚠️ Nessuna misurazione trovata per il grafico");
                return;
            }
            
            // Filtra i dati in base al periodo selezionato
            String selectedPeriod = timeRangeCombo.getSelectionModel().getSelectedItem();
            int daysBack = getDaysFromPeriod(selectedPeriod);
            
            // Filtra e ordina i dati per il periodo
            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(daysBack);
            List<GlucoseMeasurement> filteredMeasurements = measurements.stream()
                .filter(m -> m.getDateAndTime().isAfter(cutoffDate))
                .sorted((a, b) -> a.getDateAndTime().compareTo(b.getDateAndTime()))
                .collect(Collectors.toList());
            
            // Crea serie dati per il grafico
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Glicemia (mg/dL)");
            
            // Limita il numero di punti visualizzati per evitare sovrapposizioni delle date
            int maxPoints = getMaxPointsForPeriod(selectedPeriod);
            
            if (filteredMeasurements.size() > maxPoints) {
                // Campionamento uniforme per distribuire i punti nel tempo
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
                // Se ci sono pochi punti, mostra tutti ma con spaziatura minima
                for (int i = 0; i < filteredMeasurements.size(); i++) {
                    GlucoseMeasurement measurement = filteredMeasurements.get(i);
                    String dateStr = formatDateForChart(measurement.getDateAndTime(), selectedPeriod);
                    series.getData().add(new XYChart.Data<>(dateStr, measurement.getGlucoseLevel()));
                }
            }
            
            glucoseChart.getData().add(series);
            
            System.out.println("📊 Grafico aggiornato - Periodo: " + selectedPeriod + 
                             ", Punti visualizzati: " + series.getData().size() + 
                             "/" + filteredMeasurements.size());
            
            // Forza il refresh completo del grafico
            javafx.application.Platform.runLater(() -> {
                glucoseChart.requestLayout();
                glucoseChart.autosize();
            });
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nell'aggiornamento del grafico: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Errore generico nel grafico: " + e.getMessage());
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
    
    private int getDaysFromPeriod(String period) {
        switch (period) {
            case "Ultimi 7 giorni": return 7;
            case "Ultimi 30 giorni": return 30;
            case "Ultimo anno": return 365;
            default: return 7;
        }
    }

    @FXML
    private void onLogReadingClick(javafx.event.ActionEvent event) {
        System.out.println("=============================================");
        System.out.println("🖱️ PULSANTE LOG READING CLICCATO!");
        System.out.println("=============================================");
        openGlucoseInsertForm();
    }

    @FXML
    private void onRecordSymptomsClick(ActionEvent event) {
        System.out.println("🩺 Pulsante Record Symptoms cliccato!");
        openSymptomInsertForm();
    }

    @FXML
    private void onAddMedicationClick(ActionEvent event) {
        // Logica per aggiungere farmaco
    }

    @FXML
    private void onViewHistoryClick(ActionEvent event) {
        // Logica per visualizzare la cronologia
    }

    private List<GlucoseMeasurement> filterMeasurementsByPeriod(List<GlucoseMeasurement> measurements, int daysBack) {
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(daysBack);
        return measurements.stream()
                .filter(m -> m.getDateAndTime().isAfter(cutoffDate))
                .collect(Collectors.toList());
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
    
    // Metodo per aprire il form di inserimento glicemia nel pannello centrale
    private void openGlucoseInsertForm() {
        try {
            System.out.println("🔄 Apertura form inserimento glicemia...");
            
            // Carica il form nel pannello centrale del dashboard principale
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardGlucoseInsert.fxml"));
            Parent glucoseInsertView = loader.load();
            System.out.println("✅ FXML caricato con successo");
            
            // Ottieni il controller del form
            PatientDashboardGlucoseInsertController insertController = loader.getController();
            System.out.println("✅ Controller ottenuto: " + (insertController != null ? "OK" : "NULL"));
            
            // Imposta il callback per refresh dei dati quando si salva
            insertController.setOnDataSaved(() -> {
                updateGlucoseData();
                // Dopo il salvataggio, torna alla home
                returnToHome();
            });
            
            // Imposta il callback per l'annullamento
            insertController.setOnCancel(this::returnToHome);
            System.out.println("✅ Callback impostati");
            
            // Sostituisce il contenuto centrale con il form
            loadContentInMainDashboard(glucoseInsertView);
            
        } catch (IOException e) {
            System.err.println("❌ Errore nell'apertura del form di inserimento glicemia: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Metodo per aprire il form di inserimento sintomi nel pannello centrale
    private void openSymptomInsertForm() {
        try {
            System.out.println("🔄 Apertura form inserimento sintomi...");
            
            // Carica il form nel pannello centrale del dashboard principale
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/PatientDashboardSymptomInsert.fxml"));
            Parent symptomInsertView = loader.load();
            System.out.println("✅ FXML sintomi caricato con successo");
            
            // Ottieni il controller del form
            PatientDashboardSymptomsInsertController insertController = loader.getController();
            System.out.println("✅ Controller sintomi ottenuto: " + (insertController != null ? "OK" : "NULL"));
            
            // Imposta il callback per refresh dei dati quando si salva
            insertController.setOnDataSaved(() -> {
                updateGlucoseData(); // Potremmo aggiungere un refresh dei sintomi se necessario
                // Dopo il salvataggio, torna alla home
                returnToHome();
            });
            
            // Imposta il callback per l'annullamento
            insertController.setOnCancel(this::returnToHome);
            System.out.println("✅ Callback sintomi impostati");
            
            // Sostituisce il contenuto centrale con il form
            loadContentInMainDashboard(symptomInsertView);
            
        } catch (IOException e) {
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