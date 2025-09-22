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


            ViewNavigator navigator = ViewNavigator.getInstance();
            navigator.setPrimaryStage(primaryStage);

            primaryStage.setTitle("GlucoTrack");
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);

            navigator.navigateTo(ViewNavigator.LOGIN_VIEW);

            primaryStage.setMaximized(true);

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