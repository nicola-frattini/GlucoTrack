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
            System.out.println("🔧 Inizializzazione database...");
            
            // 1. Crea connessione (questo creerà il file .sqlite se non esiste)
            Connection conn = DatabaseInteraction.connect();
            System.out.println("✅ File database creato/connesso");
            
            // 2. Controlla se le tabelle esistono già
            if (!tablesExist(conn)) {
                // 3. Esegui lo schema SQL
                executeSchemaSQL(conn);
                System.out.println("✅ Tabelle create");
                
                // 4. Popola con dati mock
                DatabaseMockData.populateDatabase();
                System.out.println("✅ Dati mock inseriti");
            } else {
                System.out.println("✅ Database già inizializzato");
            }
            
            // 5. Stampa contenuto database per debug
            DatabaseMockData.printDatabaseContents();
            
            System.out.println("🎉 Database pronto!");
            
        } catch (SQLException e) {
            System.err.println("❌ Errore durante l'inizializzazione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeSchemaSQL(Connection conn) throws SQLException {
        try {
            // Leggi il file Schema.sql dalle risorse
            InputStream inputStream = DatabaseInitializer.class
                .getClassLoader()
                .getResourceAsStream("database/Schema.sql");
            
            if (inputStream == null) {
                throw new RuntimeException("File Schema.sql non trovato in resources/database/");
            }
            
            StringBuilder sqlContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sqlContent.append(line).append("\n");
                }
            }
            
            // Esegui le istruzioni SQL in due fasi: prima le tabelle, poi gli indici
            try (Statement stmt = conn.createStatement()) {
                String[] statements = sqlContent.toString().split(";");
                
                // Fase 1: Esegui solo i CREATE TABLE
                System.out.println("  📋 Creazione tabelle...");
                for (String sql : statements) {
                    String cleanSql = cleanSqlStatement(sql);
                    if (!cleanSql.isEmpty() && cleanSql.toUpperCase().startsWith("CREATE TABLE")) {
                        try {
                            stmt.execute(cleanSql);
                            System.out.println("  ✅ Tabella creata: " + extractTableName(cleanSql));
                        } catch (SQLException e) {
                            System.err.println("❌ Errore creazione tabella: " + cleanSql.substring(0, Math.min(100, cleanSql.length())));
                            throw e;
                        }
                    }
                }
                
                // Fase 2: Esegui gli indici e altri statement
                System.out.println("  🔍 Creazione indici...");
                for (String sql : statements) {
                    String cleanSql = cleanSqlStatement(sql);
                    if (!cleanSql.isEmpty() && !cleanSql.toUpperCase().startsWith("CREATE TABLE")) {
                        try {
                            stmt.execute(cleanSql);
                            if (cleanSql.toUpperCase().startsWith("CREATE INDEX")) {
                                System.out.println("  ✅ Indice creato: " + extractIndexName(cleanSql));
                            }
                        } catch (SQLException e) {
                            System.err.println("❌ Errore durante l'esecuzione di: " + cleanSql.substring(0, Math.min(100, cleanSql.length())));
                            throw e;
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            throw new SQLException("Errore durante la lettura del file Schema.sql", e);
        }
    }

    // Metodo per reset completo del database
    public static void resetDatabase() {
        try {
            System.out.println("🔄 Reset database...");
            
            Connection conn = DatabaseInteraction.connect();
            
            // Drop tutte le tabelle
            String[] tables = {
                "admin_managed_users", "risk_factors", "patient_symptoms", 
                "log_medications", "medications", "glucose_measurements", "users"
            };
            
            try (Statement stmt = conn.createStatement()) {
                for (String table : tables) {
                    try {
                        stmt.execute("DROP TABLE IF EXISTS " + table);
                    } catch (SQLException e) {
                        // Ignora errori se la tabella non esiste
                    }
                }
            }
            
            // Ricrea tutto
            executeSchemaSQL(conn);
            DatabaseMockData.populateDatabase();
            
            System.out.println("✅ Database resettato e ripopolato!");
            
        } catch (SQLException e) {
            System.err.println("❌ Errore durante il reset: " + e.getMessage());
        }
    }

    // Controlla se le tabelle principali esistono
    private static boolean tablesExist(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Prova a fare una query su una tabella chiave
            stmt.executeQuery("SELECT COUNT(*) FROM users LIMIT 1");
            return true;
        } catch (SQLException e) {
            // Se la query fallisce, le tabelle non esistono
            return false;
        }
    }

    // Metodi helper per la pulizia degli statement SQL
    private static String cleanSqlStatement(String sql) {
        if (sql == null) return "";
        
        // Rimuovi commenti e pulisci
        String[] lines = sql.split("\n");
        StringBuilder cleanSql = new StringBuilder();
        
        for (String line : lines) {
            String cleanLine = line.trim();
            if (!cleanLine.isEmpty() && !cleanLine.startsWith("--")) {
                // Rimuovi commenti inline
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
        // Estrae il nome della tabella da "CREATE TABLE table_name ..."
        String[] parts = createTableSql.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equalsIgnoreCase("TABLE")) {
                return parts[i + 1];
            }
        }
        return "unknown";
    }
    
    private static String extractIndexName(String createIndexSql) {
        // Estrae il nome dell'indice da "CREATE INDEX index_name ..."
        String[] parts = createIndexSql.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equalsIgnoreCase("INDEX")) {
                return parts[i + 1];
            }
        }
        return "unknown";
    }

    // Main per test
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("reset")) {
            resetDatabase();
        } else {
            initializeDatabase();
        }
    }
}