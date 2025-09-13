package it.glucotrack;


//<a target="_blank" href="https://icons8.com/icon/80576/blood-sample">Campione di sangue</a> icona di <a target="_blank" href="https://icons8.com">Icons8</a>

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {

            // primaryStage.initStyle(StageStyle.TRANSPARENT); Aspetto di avere la navbar

            // Load and set the application icon
            Image icon = new Image(getClass().getResourceAsStream("/assets/icons/Logo.png"));
            primaryStage.getIcons().add(icon);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/LoginView.fxml"));            Parent root = loader.load();
            Scene scene = new Scene(root);

            primaryStage.setTitle("ScoreShare");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}