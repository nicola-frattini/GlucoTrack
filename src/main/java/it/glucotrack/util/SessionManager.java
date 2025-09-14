package it.glucotrack.util;

import java.sql.SQLException;
import java.time.LocalDateTime;

import it.glucotrack.model.User;
import it.glucotrack.view.ViewNavigator;

public class SessionManager {
    
    private static SessionManager instance;
    private User currentUser;
    private String currentUserType;
    private LocalDateTime loginTime;
    private UserDAO userDAO;
    
    // Private constructor per Singleton
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
    
    /**
     * Autentica un utente e avvia la sessione
     */
    public boolean login(String email, String password) {
        try {
            User user = userDAO.authenticateUser(email, password);
            
            if (user != null) {
                this.currentUser = user;
                this.currentUserType = determineUserType(user);
                this.loginTime = LocalDateTime.now();
                
                System.out.println("✅ Session started for: " + user.getFullName() + " (" + currentUserType + ")");
                return true;
            } else {
                System.out.println("❌ Authentication failed for email: " + email);
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Database error during authentication: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Termina la sessione corrente
     */
    public void logout() {
        if (currentUser != null) {
            System.out.println("🔐 Session ended for: " + currentUser.getFullName());
        }
        
        this.currentUser = null;
        this.currentUserType = null;
        this.loginTime = null;
        
        // Reindirizza al login
        ViewNavigator.getInstance().navigateTo(ViewNavigator.LOGIN_VIEW);
    }
    
    /**
     * Controlla se c'è una sessione attiva
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Ottiene l'utente corrente
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Ottiene il tipo dell'utente corrente
     */
    public String getCurrentUserType() {
        return currentUserType;
    }
    
    /**
     * Ottiene l'ID dell'utente corrente
     */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
    
    /**
     * Ottiene il nome completo dell'utente corrente
     */
    public String getCurrentUserFullName() {
        return currentUser != null ? currentUser.getFullName() : "Unknown";
    }
    
    /**
     * Ottiene l'email dell'utente corrente
     */
    public String getCurrentUserEmail() {
        return currentUser != null ? currentUser.getEmail() : null;
    }
    
    /**
     * Ottiene il tempo di login
     */
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    /**
     * Controlla se l'utente ha un ruolo specifico
     */
    public boolean hasRole(String role) {
        return role != null && role.equalsIgnoreCase(currentUserType);
    }
    
    /**
     * Controlla se l'utente è un admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    /**
     * Controlla se l'utente è un dottore
     */
    public boolean isDoctor() {
        return hasRole("DOCTOR");
    }
    
    /**
     * Controlla se l'utente è un paziente
     */
    public boolean isPatient() {
        return hasRole("PATIENT");
    }
    
    /**
     * Aggiorna i dati dell'utente corrente (dopo modifiche al profilo)
     */
    public void refreshCurrentUser() {
        if (currentUser != null) {
            try {
                User updatedUser = userDAO.getUserById(currentUser.getId());
                if (updatedUser != null) {
                    this.currentUser = updatedUser;
                    System.out.println("🔄 User data refreshed for: " + updatedUser.getFullName());
                }
            } catch (SQLException e) {
                System.err.println("❌ Error refreshing user data: " + e.getMessage());
            }
        }
    }
    
    /**
     * Ottiene informazioni sulla sessione per debug
     */
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
    
    /**
     * Determina il tipo di utente dal database
     */
    private String determineUserType(User user) throws SQLException {
        // Controlla il tipo direttamente dal database
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
    
    /**
     * Verifica se la sessione è ancora valida (per future implementazioni di timeout)
     */
    public boolean isSessionValid() {
        if (!isLoggedIn()) {
            return false;
        }
        
        // Qui potresti aggiungere logica per timeout sessione
        // Per ora, ritorna sempre true se l'utente è loggato
        return true;
    }
    
    /**
     * Forza il logout se la sessione non è valida
     */
    public void validateSession() {
        if (!isSessionValid()) {
            logout();
        }
    }
}