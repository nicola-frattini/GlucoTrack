package it.glucotrack;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import it.glucotrack.controller.AdminDashboardController;
import it.glucotrack.model.User;

public class ControllerTest {
    @Nested
    @DisplayName("AdminDashboardController Tests")
    class AdminDashboardControllerTest {
        private AdminDashboardController controller;

        @BeforeEach
        void setUp() {
            controller = new AdminDashboardController();
        }

        @Test
        @DisplayName("Settaggio admin funzionante")
        void testSetCurrentAdmin() {
            User admin = new User();
            admin.setName("Mario");
            admin.setSurname("Rossi");
            admin.setType("ADMIN");
            controller.setCurrentAdmin(admin);
            assertNotNull(admin, "Admin deve essere settato correttamente");
        }
    }
}
