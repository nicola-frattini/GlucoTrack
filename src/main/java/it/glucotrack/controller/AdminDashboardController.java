package it.glucotrack.controller;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.ResourceBundle;

import it.glucotrack.model.Doctor;
import it.glucotrack.model.Gender;
import it.glucotrack.model.Patient;
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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class AdminDashboardController implements Initializable {

    // Search and filter elements
    @FXML private TextField searchField;
    @FXML private Button filterBtn;
    @FXML private Button addUserBtn;

    // Table elements
    @FXML private TableView<UserTableData> usersTable;
    @FXML private TableColumn<UserTableData, Integer> idColumn;
    @FXML private TableColumn<UserTableData, String> nameColumn;
    @FXML private TableColumn<UserTableData, String> emailColumn;
    @FXML private TableColumn<UserTableData, String> typeColumn;

    // Context menu
    @FXML private ContextMenu tableContextMenu;
    @FXML private MenuItem viewUserMenuItem;
    @FXML private MenuItem editUserMenuItem;
    @FXML private MenuItem deleteUserMenuItem;

    // Status elements
    @FXML private Label statusLabel;
    @FXML private Label totalUserLabel;

    // Data and DAOs
    private ObservableList<UserTableData> UserTableData;
    private FilteredList<UserTableData> filteredusers;
    private UserTableData selectedUser;
    private UserDAO userDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.userDAO = new UserDAO();
        setupTable();
        setupSearch();
        setupButtons();
        setupContextMenu();
        loadUsersData();
        updateStatusBar();
    }

    // Set the doctorId from the login screen
    public void setDoctorId(int doctorId) {
        refreshUsersList();
    }

    private void setupTable() {
        UserTableData = FXCollections.observableArrayList();
        filteredusers = new FilteredList<>(UserTableData);
        usersTable.setItems(filteredusers);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        usersTable.setRowFactory(tv -> {
            TableRow<UserTableData> row = new TableRow<UserTableData>() {
                @Override
                protected void updateItem(UserTableData userData, boolean empty) {
                    super.updateItem(userData, empty);
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    viewUserProfile(getSelectedUser());
                }
            });

            return row;
        });

        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> selectedUser = newSelection);

        usersTable.getStyleClass().add("users-table");
    }



    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue);
        });

        searchField.getStyleClass().add("search-field");
    }

    private void setupButtons() {
        addUserBtn.setOnAction(e -> handleAddUser());
        filterBtn.setOnAction(e -> handleFilter());

        addUserBtn.getStyleClass().addAll("btn", "btn-primary");
        filterBtn.getStyleClass().addAll("btn", "btn-secondary");
    }

    private void setupContextMenu() {
        viewUserMenuItem.setOnAction(e -> {
            if (selectedUser != null) {
                viewUserProfile(selectedUser);
            }
        });

        editUserMenuItem.setOnAction(e -> {
            if (selectedUser != null) {
                editUser(selectedUser);
            }
        });


        deleteUserMenuItem.setOnAction(e -> {
            if (selectedUser != null) {
                deleteUser(selectedUser);
            }
        });

        usersTable.setContextMenu(tableContextMenu);
        tableContextMenu.getStyleClass().add("context-menu");
    }

    private void loadUsersData() {
        UserTableData.clear();
        try {
            // Fetch users from the database
            List<User> users = userDAO.getAllUsers();
            if (users != null) {
                for (User user : users) {
                    UserTableData tableData = new UserTableData(user);
                    UserTableData.add(tableData);
                }
            }
            statusLabel.setText("users loaded successfully from the database");
        } catch (SQLException e) {
            statusLabel.setText("Error loading users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterUsers(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredusers.setPredicate(null);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredusers.setPredicate(patientData ->
                    patientData.getIdString().toLowerCase().contains(lowerCaseFilter) ||
                    patientData.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                    patientData.getEmail().toLowerCase().contains(lowerCaseFilter) ||
                    patientData.getType().toLowerCase().contains(lowerCaseFilter)
            );
        }
        updateStatusBar();
    }

    private void updateStatusBar() {
        int totalUsers = UserTableData.size();
        int filteredCount = filteredusers.size();

        if (filteredCount == totalUsers) {
            totalUserLabel.setText("Total: " + totalUsers + " users");
            statusLabel.setText("All users displayed");
        } else {
            totalUserLabel.setText("Showing: " + filteredCount + " of " + totalUsers + " users");
            statusLabel.setText("Search results filtered");
        }
    }

    // Action handlers - just make buttons clickable without implementing full functionality
    private void handleAddUser() {
        System.out.println("Add User button clicked - functionality not implemented yet");
        statusLabel.setText("Add User functionality coming soon...");
    }

    private void handleFilter() {
        System.out.println("Filter button clicked - functionality not implemented yet");
        statusLabel.setText("Filter functionality coming soon...");
    }

    private void viewUserProfile(UserTableData userData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/fxml/ProfileView.fxml"));
            Parent profileRoot = loader.load();

            ProfileViewController profileController = loader.getController();
            //profileController.setUser(userData.getUser());
            //profileController.setAdminView(true);

            Scene scene = addUserBtn.getScene();
            BorderPane rootPane = (BorderPane) scene.getRoot();
            StackPane contentPane = (StackPane) rootPane.getCenter();

            profileController.setParentContentPane(contentPane);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(profileRoot);

            statusLabel.setText("Opened profile for " + userData.getFullName());
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Errore nel caricamento del profilo: " + e.getMessage());
        }
    }

    private void editUser(UserTableData userData) {
        System.out.println("Edit patient: " + userData.getFullName());
        statusLabel.setText("Edit functionality for " + userData.getFullName() + " coming soon...");
    }



    private void deleteUser(UserTableData userData) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete " + userData.getFullName() + "?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Confirm Deletion");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    boolean success = userDAO.deleteUser(userData.getId());
                    if (success) {
                        refreshUsersList();
                        statusLabel.setText(userData.getFullName() + " deleted successfully.");
                    } else {
                        statusLabel.setText("Failed to delete " + userData.getFullName() + ".");
                    }
                } catch (SQLException e) {
                    statusLabel.setText("Error deleting user: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // Public methods for external use
    public void refreshUsersList() {
        loadUsersData();
        updateStatusBar();
        statusLabel.setText("users list refreshed");
    }

    public void setSelectedPatient(UserTableData userData) {
        usersTable.getSelectionModel().select(userData);
        selectedUser = userData;
    }

    public void clearSelection() {
        usersTable.getSelectionModel().clearSelection();
        selectedUser = null;
    }

    public UserTableData getSelectedUser() {
        return selectedUser;
    }

    public ObservableList<UserTableData> getusersList() {
        return UserTableData;
    }

    public void setContentPane(StackPane parentContentPane) {
        // It is used to navigate back to this view from ProfileView
        BorderPane rootPane = (BorderPane) addUserBtn.getScene().getRoot();
        StackPane contentPane = (StackPane) rootPane.getCenter();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(parentContentPane);

    }

    // Wrapper class for table display
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
        public User getUser() { return this.user; }


        public SimpleIntegerProperty idProperty() { return id; }
        public SimpleStringProperty fullNameProperty() { return fullName; }
        public SimpleStringProperty emailProperty() { return email; }
        public SimpleStringProperty typeProperty() { return type; }

        public String getIdString() {
            //Return the id formatted as a String
            return Integer.toString(id.get());
        }
    }
}