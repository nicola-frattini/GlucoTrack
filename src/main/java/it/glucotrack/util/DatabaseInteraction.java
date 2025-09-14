package it.glucotrack.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseInteraction {
    // Database will be inn the resources/dabase folder
    private static final String DB_URL = "jdbc:sqlite:src/main/resources/database/glucotrack_db.sqlite";
    private static Connection connection = null;


}