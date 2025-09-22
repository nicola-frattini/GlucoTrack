package it.glucotrack.controller;

import it.glucotrack.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;

import it.glucotrack.model.User;

public class AdminDashboardController {

    @FXML private StackPane contentPane;
    @FXML private Button profileBtn;
    @FXML private Button dashboardBtn;
    @FXML private Button logoutBtn;

    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Label statusLabel;
    @FXML private Label totalUserLabel;

    private User currentAdmin;

    @FXML
    public void initialize() {
        loadCenterContent("/assets/fxml/AdminDashboardHome.fxml");
    }

    public void setCurrentAdmin(User admin) {
        this.currentAdmin = admin;
        if (adminNameLabel != null)
            adminNameLabel.setText(admin.getName() + " " + admin.getSurname());

        if(adminRoleLabel != null)
            adminRoleLabel.setText("Role: " + admin.getType());
    }

    private void loadCenterContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            contentPane.getChildren().clear();
            contentPane.getChildren().add(content);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error loading content: " + e.getMessage());
        }
    }

    @FXML
    private void onDashboardClick() {
        loadCenterContent("/assets/fxml/AdminDashboardHome.fxml");
    }

    @FXML
    private void onProfileClick() {
        if (currentAdmin != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/ProfileView.fxml"));
                Parent profileRoot = loader.load();
                ProfileViewController profileController = loader.getController();
                profileController.setUserRole(ProfileViewController.UserRole.ADMIN_OWN_PROFILE, currentAdmin);
                Scene scene = profileBtn.getScene();
                BorderPane rootPane = (BorderPane) scene.getRoot();
                StackPane contentPane = (StackPane) rootPane.getCenter();
                profileController.setParentContentPane(contentPane); contentPane.getChildren().clear(); contentPane.getChildren().add(profileRoot);
                statusLabel.setText("Opened your profile");
            } catch (Exception e) {
                e.printStackTrace();
                statusLabel.setText("Error loading your profile: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onLogoutClick() {
        try {
            SessionManager.getInstance().logout();
            System.out.println("👋 Logout eseguito con successo");
        } catch (Exception e) {
            System.err.println("❌ Errore durante il logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setContentPane(StackPane parentContentPane) {
        BorderPane rootPane = (BorderPane) profileBtn.getScene().getRoot();
        StackPane contentPane = (StackPane) rootPane.getCenter();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(parentContentPane);
    }
}
