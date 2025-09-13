package it.glucotrack.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ViewNavigator {

    private static ViewNavigator instance;
    private Stage primaryStage;
    private Map<String, String> viewPaths;

    // Costanti per le viste
    public static final String LOGIN_VIEW = "LOGIN";
    public static final String REGISTER_VIEW = "REGISTER";
    public static final String PATIENT_DASHBOARD = "PATIENT_DASHBOARD";
    public static final String DOCTOR_DASHBOARD = "DOCTOR_DASHBOARD";
    public static final String ADMIN_DASHBOARD = "ADMIN_DASHBOARD";
    public static final String MAIN_DASHBOARD = "MAIN_DASHBOARD";


    private ViewNavigator() {
        initializeViewPaths();
    }
    public static ViewNavigator getInstance() {
        if (instance == null) {
            instance = new ViewNavigator();
        }
        return instance;
    }

    private void initializeViewPaths() {
        viewPaths = new HashMap<>();
        viewPaths.put(LOGIN_VIEW, "/assets/fxml/LoginView.fxml");
        viewPaths.put(REGISTER_VIEW, "/assets/fxml/RegisterView.fxml");
        viewPaths.put(PATIENT_DASHBOARD, "/assets/fxml/PatientDashboard.fxml");
        viewPaths.put(DOCTOR_DASHBOARD, "/assets/fxml/DoctorDashboard.fxml");
        viewPaths.put(ADMIN_DASHBOARD, "/assets/fxml/AdminDashboard.fxml");
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void navigateTo(String viewName) {
        navigateTo(viewName, null);
    }

    public void navigateTo(String viewName, String title) {
        try {
            String fxmlPath = viewPaths.get(viewName);
            if (fxmlPath == null) {
                throw new IllegalArgumentException("View not found: " + viewName);
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Salva le dimensioni correnti
            double width = primaryStage.getWidth();
            double height = primaryStage.getHeight();
            boolean wasMaximized = primaryStage.isMaximized();

            Scene newScene = new Scene(root, width, height);
            primaryStage.setScene(newScene);

            // Imposta il titolo se fornito
            if (title != null) {
                primaryStage.setTitle(title);
            } else {
                primaryStage.setTitle(getDefaultTitle(viewName));
            }

            // Ripristina lo stato della finestra
            if (wasMaximized) {
                primaryStage.setMaximized(true);
            }

        } catch (IOException e) {
            System.err.println("Navigation Error: Could not load " + viewName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getDefaultTitle(String viewName) {
        switch (viewName) {
            case LOGIN_VIEW: return "GlucoTrack - Login";
            case REGISTER_VIEW: return "GlucoTrack - Register";
            case PATIENT_DASHBOARD: return "GlucoTrack - Patient Dashboard";
            case DOCTOR_DASHBOARD: return "GlucoTrack - Doctor Dashboard";
            case ADMIN_DASHBOARD: return "GlucoTrack - Admin Dashboard";
            default: return "GlucoTrack";
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
