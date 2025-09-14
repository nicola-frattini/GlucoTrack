package it.glucotrack;

import it.glucotrack.view.ViewNavigator;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("[Main] Inizio avvio applicazione");
            it.glucotrack.util.DatabaseInitializer.initializeDatabase();
            System.out.println("[Main] Database inizializzato");

            Image icon = new Image(getClass().getResourceAsStream("/assets/icons/Logo.png"));
            primaryStage.getIcons().add(icon);
            System.out.println("[Main] Icona caricata");


            ViewNavigator navigator = ViewNavigator.getInstance();
            navigator.setPrimaryStage(primaryStage);
            System.out.println("[Main] ViewNavigator pronto");

            primaryStage.setTitle("GlucoTrack");
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);

            System.out.println("[Main] Navigo a LOGIN_VIEW");
            navigator.navigateTo(ViewNavigator.LOGIN_VIEW);
            System.out.println("[Main] Navigazione completata");

            primaryStage.setMaximized(true);
            System.out.println("[Main] Massimizzato all'avvio");

            primaryStage.show();
            System.out.println("[Main] Stage mostrato");
        } catch (Exception e) {
            System.err.println("[Main] Errore in fase di avvio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}