package it.glucotrack;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import it.glucotrack.view.ViewNavigator;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {

            // Load and set the application icon
            Image icon = new Image(getClass().getResourceAsStream("/assets/icons/Logo.png"));
            primaryStage.getIcons().add(icon);


            ViewNavigator navigator = ViewNavigator.getInstance();
            navigator.setPrimaryStage(primaryStage);

            primaryStage.setTitle("GlucoTrack");
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);

            navigator.navigateTo(ViewNavigator.LOGIN_VIEW);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}