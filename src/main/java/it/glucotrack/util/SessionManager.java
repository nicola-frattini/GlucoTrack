package it.glucotrack.util;

import java.sql.SQLException;
import java.time.LocalDateTime;

import it.glucotrack.model.User;
import it.glucotrack.view.ViewNavigator;


/*
* SESSION MANAGER
* Singleton Class used to manage the session
*/

public class SessionManager {

    private static SessionManager instance;
    private static User currentUser; //
    private String currentUserType;
    private LocalDateTime loginTime;
    private UserDAO userDAO;


    // Private constructor for Singleton
    private SessionManager() {
        this.userDAO = new UserDAO();
    }


    // Singleton instance
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }


    // Authentication for a user
    public boolean login(String email, String password) {
        try {
            // Get user by email first
            User user = userDAO.getUserByEmail(email);

            if (user == null) {
                System.out.println("User not found for email: " + email);
                return false;
            }

            if (user.getPassword().equals(password)) {
                this.currentUser = user;
                this.currentUserType = determineUserType(user);
                this.loginTime = LocalDateTime.now();

                System.out.println("Session started for: " + user.getFullName() + " (" + currentUserType + ")");
                return true;
            } else {
                System.out.println("Password mismatch for email: " + email);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Database error during authentication: " + e.getMessage());
            return false;
        }
    }


    // End session
    public void logout() {
        if (currentUser != null) {
            System.out.println("Session ended for: " + currentUser.getFullName());
        }

        this.currentUser = null;
        this.currentUserType = null;
        this.loginTime = null;

        // Reindirizza al login
        ViewNavigator.getInstance().navigateTo(ViewNavigator.LOGIN_VIEW);
    }



    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserType() {
        return currentUserType;
    }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    public String getCurrentUserFullName() {
        return currentUser != null ? currentUser.getFullName() : "Unknown";
    }

    public String getCurrentUserEmail() {
        return currentUser != null ? currentUser.getEmail() : null;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public boolean hasRole(String role) {
        return role != null && role.equalsIgnoreCase(currentUserType);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isDoctor() {
        return hasRole("DOCTOR");
    }

    public boolean isPatient() {
        return hasRole("PATIENT");
    }

    // Refresh the currentUser
    public void refreshCurrentUser() {
        if (currentUser != null) {
            try {
                User updatedUser = userDAO.getUserById(currentUser.getId());
                if (updatedUser != null) {
                    this.currentUser = updatedUser;
                    System.out.println("User data refreshed for: " + updatedUser.getFullName());
                }
            } catch (SQLException e) {
                System.err.println("Error refreshing user data: " + e.getMessage());
            }
        }
    }


    public String getSessionInfo() {
        if (!isLoggedIn()) {
            return "No active session";
        }

        return String.format("Session Info:\n" +
                        "- User: %s (%s)\n" +
                        "- Type: %s\n" +
                        "- Login Time: %s\n" +
                        "- Session Duration: %s minutes",
                getCurrentUserFullName(),
                getCurrentUserEmail(),
                getCurrentUserType(),
                loginTime != null ? loginTime.toString() : "Unknown",
                loginTime != null ? java.time.Duration.between(loginTime, LocalDateTime.now()).toMinutes() : 0);
    }



    private String determineUserType(User user) throws SQLException {

        if (userDAO.getUsersByType("ADMIN").stream()
                .anyMatch(u -> u.getId() == user.getId())) {
            return "ADMIN";
        }

        if (userDAO.getUsersByType("DOCTOR").stream()
                .anyMatch(u -> u.getId() == user.getId())) {
            return "DOCTOR";
        }

        if (userDAO.getUsersByType("PATIENT").stream()
                .anyMatch(u -> u.getId() == user.getId())) {
            return "PATIENT";
        }

        return "PATIENT"; // Default fallback
    }

}