package it.glucotrack.controller;

import it.glucotrack.model.User;
import it.glucotrack.util.UserDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardHomeController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button addUserBtn;

    @FXML private TableView<UserTableData> usersTable;
    @FXML private TableColumn<UserTableData, Integer> idColumn;
    @FXML private TableColumn<UserTableData, String> nameColumn;
    @FXML private TableColumn<UserTableData, String> emailColumn;
    @FXML private TableColumn<UserTableData, String> typeColumn;

    @FXML private ContextMenu tableContextMenu;
    @FXML private MenuItem viewUserMenuItem;
    @FXML private MenuItem editUserMenuItem;
    @FXML private MenuItem deleteUserMenuItem;

    @FXML private Label statusLabel;
    @FXML private Label totalUserLabel;

    private ObservableList<UserTableData> userTableData;
    private FilteredList<UserTableData> filteredUsers;
    private UserTableData selectedUser;
    private UserDAO userDAO;
    private User currentAdmin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();
        setupTable();
        setupSearch();
        setupButtons();
        setupContextMenu();
        loadUsersData();
        updateStatusBar();
    }

    public void setCurrentAdmin(User admin) {
        this.currentAdmin = admin;
    }

    private void setupTable() {
        userTableData = FXCollections.observableArrayList();
        filteredUsers = new FilteredList<>(userTableData);
        usersTable.setItems(filteredUsers);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> selectedUser = newSel
        );

        usersTable.setRowFactory(tv -> {
            TableRow<UserTableData> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    viewUserProfile(selectedUser);
                }
            });
            return row;
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));
    }

    private void setupButtons() {
        addUserBtn.setOnAction(e -> handleAddUser());
    }

    private void setupContextMenu() {
        viewUserMenuItem.setOnAction(e -> { if (selectedUser != null) viewUserProfile(selectedUser); });
        editUserMenuItem.setOnAction(e -> { if (selectedUser != null) editUser(selectedUser); });
        deleteUserMenuItem.setOnAction(e -> { if (selectedUser != null) deleteUser(selectedUser); });
        usersTable.setContextMenu(tableContextMenu);
    }

    private void loadUsersData() {
        userTableData.clear();
        try {
            List<User> users = userDAO.getAllUsers();
            if (users != null) {
                for (User user : users) {
                    userTableData.add(new UserTableData(user));
                }
            }
            statusLabel.setText("Users loaded successfully from database");
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading users: " + e.getMessage());
        }
    }

    private void filterUsers(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredUsers.setPredicate(null);
        } else {
            String lc = searchText.toLowerCase();
            filteredUsers.setPredicate(u -> u.getIdString().contains(lc)
                    || u.getFullName().toLowerCase().contains(lc)
                    || u.getEmail().toLowerCase().contains(lc)
                    || u.getType().toLowerCase().contains(lc));
        }
        updateStatusBar();
    }

    private void updateStatusBar() {
        int total = userTableData.size();
        int filtered = filteredUsers.size();
        if (filtered == total) {
            totalUserLabel.setText("Total: " + total + " users");
            statusLabel.setText("All users displayed");
        } else {
            totalUserLabel.setText("Showing: " + filtered + " of " + total + " users");
            statusLabel.setText("Search results filtered");
        }
    }

    private void handleAddUser() {
        statusLabel.setText("Add User functionality coming soon...");
    }

    private void viewUserProfile(UserTableData userData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/ProfileView.fxml"));
            Parent profileRoot = loader.load();
            ProfileViewController profileController = loader.getController();

             ProfileViewController.UserRole role = null;
             User userToView;
             if (currentAdmin != null && userData.getId() == currentAdmin.getId()) {

                 role = ProfileViewController.UserRole.ADMIN_OWN_PROFILE;
                 userToView = currentAdmin;
             } else {

                 if ("Patient".equalsIgnoreCase(userData.getType()) || "Doctor".equalsIgnoreCase(userData.getType())) {
                     role = ProfileViewController.UserRole.ADMIN_VIEWING_USER;
                 }
                 userToView = userData.getUser();
             }
             profileController.setUserRole(role, userToView);
             Scene scene = addUserBtn.getScene();
             BorderPane rootPane = (BorderPane) scene.getRoot();
             StackPane contentPane = (StackPane) rootPane.getCenter();
             profileController.setParentContentPane(contentPane);
             contentPane.getChildren().clear();
             contentPane.getChildren().add(profileRoot);
             statusLabel.setText("Opened profile for " + userData.getFullName());
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error loading profile: " + e.getMessage());
        }
    }

    private void editUser(UserTableData userData) {
        statusLabel.setText("Edit functionality for " + userData.getFullName() + " coming soon...");
    }

    private void deleteUser(UserTableData userData) {
        if (currentAdmin != null && userData.getId() == currentAdmin.getId()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "You cannot delete your own account!", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + userData.getFullName() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    if (userDAO.deleteUser(userData.getId())) {
                        loadUsersData();
                        statusLabel.setText(userData.getFullName() + " deleted successfully.");
                    } else {
                        statusLabel.setText("Failed to delete " + userData.getFullName());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    statusLabel.setText("Error deleting user: " + e.getMessage());
                }
            }
        });
    }

    // Wrapper class
    public static class UserTableData {
        private final User user;
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty email;
        private final SimpleStringProperty type;

        public UserTableData(User user) {
            this.user = user;
            this.id = new SimpleIntegerProperty(user.getId());
            this.fullName = new SimpleStringProperty(user.getName() + " " + user.getSurname());
            this.email = new SimpleStringProperty(user.getEmail());
            this.type = new SimpleStringProperty(user.getType());
        }

        public int getId() { return id.get(); }
        public String getFullName() { return fullName.get(); }
        public String getEmail() { return email.get(); }
        public String getType() { return type.get(); }
        public User getUser() { return user; }
        public String getIdString() { return String.valueOf(id.get()); }
    }
}
