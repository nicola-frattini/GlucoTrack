package it.glucotrack.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

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
    public void initialize() {
        // Inizializza la combo dei range temporali
        timeRangeCombo.getItems().addAll("Ultimi 7 giorni", "Ultimi 30 giorni", "Ultimo anno");
        timeRangeCombo.getSelectionModel().selectFirst();

        // Inizializza i dati della dashboard
        updateGlucoseData();
        updateChart();
    }

    private void updateGlucoseData() {
        // Esempio: recupera i dati dal modello e aggiorna le label
        currentGlucoseLabel.setText("120");
        trendLabel.setText("↓ 5%");
        statusLabel.setText("Normale");
    }

    private void updateChart() {
        // Esempio: aggiorna il grafico con dati fittizi
        glucoseChart.getData().clear();
        // ...aggiungi serie e dati al grafico...
    }

    @FXML
    private void onLogReadingClick(MouseEvent event) {
        // Logica per aggiungere una nuova misurazione
        // Ad esempio: apri una finestra/modal per inserire il valore
    }

    @FXML
    private void onRecordSymptomsClick(MouseEvent event) {
        // Logica per registrare sintomi
    }

    @FXML
    private void onAddMedicationClick(MouseEvent event) {
        // Logica per aggiungere farmaco
    }

    @FXML
    private void onViewHistoryClick(MouseEvent event) {
        // Logica per visualizzare la cronologia
    }

    // Puoi aggiungere altri metodi di supporto, come refresh, gestione alert, ecc.
}