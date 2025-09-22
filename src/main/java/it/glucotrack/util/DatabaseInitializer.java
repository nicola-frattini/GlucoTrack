package it.glucotrack.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        try {

            // Create a  connection
            Connection conn = DatabaseInteraction.connect();

            if (!tablesExist(conn)) {
                //Execute the SQLSchema
                executeSchemaSQL(conn);

                // Populate with Mock Data
                DatabaseMockData.populateDatabase();
            }

            DatabaseMockData.printDatabaseContents();

        } catch (SQLException e) {
            System.err.println("Error during the initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeSchemaSQL(Connection conn) throws SQLException {
        try {

            InputStream inputStream = DatabaseInitializer.class
                .getClassLoader()
                .getResourceAsStream("database/Schema.sql");
            
            if (inputStream == null) {
                throw new RuntimeException("File Schema.sql not found in resources/database/");
            }
            
            StringBuilder sqlContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sqlContent.append(line).append("\n");
                }
            }

            // Execute the schema
            try (Statement stmt = conn.createStatement()) {
                String[] statements = sqlContent.toString().split(";");
                
                // Execute the CREATE TABLE
                for (String sql : statements) {
                    String cleanSql = cleanSqlStatement(sql);
                    if (!cleanSql.isEmpty() && cleanSql.toUpperCase().startsWith("CREATE TABLE")) {
                        try {
                            stmt.execute(cleanSql);
                            System.out.println("Created: " + extractTableName(cleanSql));
                        } catch (SQLException e) {
                            System.err.println("Error during the creation of " + cleanSql.substring(0, Math.min(100, cleanSql.length())));
                            throw e;
                        }
                    }
                }


                // Execute index and other statements
                for (String sql : statements) {
                    String cleanSql = cleanSqlStatement(sql);
                    if (!cleanSql.isEmpty() && !cleanSql.toUpperCase().startsWith("CREATE TABLE")) {
                        try {
                            stmt.execute(cleanSql);
                            if (cleanSql.toUpperCase().startsWith("CREATE INDEX")) {
                                System.out.println("Idex created: " + extractIndexName(cleanSql));
                            }
                        } catch (SQLException e) {
                            System.err.println("Error during the execution of " + cleanSql.substring(0, Math.min(100, cleanSql.length())));
                            throw e;
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            throw new SQLException("Error during Schema.sql reading", e);
        }
    }

    public static void resetDatabase() {
        try {

            Connection conn = DatabaseInteraction.connect();
            
            // Drop all the tables
            String[] tables = {
                "admin_managed_users", "risk_factors", "patient_symptoms", 
                "log_medications", "medications", "glucose_measurements", "users"
            };
            
            try (Statement stmt = conn.createStatement()) {
                for (String table : tables) {
                    try {
                        stmt.execute("DROP TABLE IF EXISTS " + table);
                    } catch (SQLException e) {
                        // Ignore errors if table doesn't exist
                    }
                }
            }
            
            // Recreate everything
            executeSchemaSQL(conn);
            DatabaseMockData.populateDatabase();
            
            System.out.println("Database recreated!");
            
        } catch (SQLException e) {
            System.err.println("Errore during the reset: " + e.getMessage());
        }
    }

    private static boolean tablesExist(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Little check
            stmt.executeQuery("SELECT COUNT(*) FROM users LIMIT 1");
            return true;
        } catch (SQLException e) {
                // Tables doesn't exist
            return false;
        }
    }

    //========================
    //==== HELPER METHODS ====
    //========================

    private static String cleanSqlStatement(String sql) {
        if (sql == null) return "";
        
        String[] lines = sql.split("\n");
        StringBuilder cleanSql = new StringBuilder();
        
        for (String line : lines) {
            String cleanLine = line.trim();
            if (!cleanLine.isEmpty() && !cleanLine.startsWith("--")) {

                int commentIndex = cleanLine.indexOf("--");
                if (commentIndex > 0) {
                    cleanLine = cleanLine.substring(0, commentIndex).trim();
                }
                if (!cleanLine.isEmpty()) {
                    cleanSql.append(cleanLine).append(" ");
                }
            }
        }
        
        return cleanSql.toString().trim();
    }
    
    private static String extractTableName(String createTableSql) {

        String[] parts = createTableSql.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equalsIgnoreCase("TABLE")) {
                return parts[i + 1];
            }
        }
        return "unknown";
    }
    
    private static String extractIndexName(String createIndexSql) {

        String[] parts = createIndexSql.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equalsIgnoreCase("INDEX")) {
                return parts[i + 1];
            }
        }
        return "unknown";
    }

    // Main for test
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("reset")) {
            resetDatabase();
        } else {
            initializeDatabase();
        }
    }
}