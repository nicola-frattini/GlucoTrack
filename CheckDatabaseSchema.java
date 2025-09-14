import java.sql.*;

public class CheckDatabaseSchema {
    public static void main(String[] args) {
        String dbPath = "C:\\Users\\nicol\\Documents\\github\\GlucoTrack\\glucotrack.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // Check if database file exists and is accessible
            System.out.println("✅ Database connection successful!");
            
            // Get table info for patient_symptoms
            String sql = "PRAGMA table_info(patient_symptoms)";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                System.out.println("\n📋 Colonne nella tabella patient_symptoms:");
                System.out.println("CID | Name | Type | NotNull | DefaultValue | PK");
                System.out.println("----+------+------+---------+--------------+---");
                
                while (rs.next()) {
                    int cid = rs.getInt("cid");
                    String name = rs.getString("name");
                    String type = rs.getString("type");
                    int notNull = rs.getInt("notnull");
                    String defaultValue = rs.getString("dflt_value");
                    int pk = rs.getInt("pk");
                    
                    System.out.printf("%3d | %-12s | %-12s | %7d | %-12s | %2d%n", 
                        cid, name, type, notNull, 
                        (defaultValue != null ? defaultValue : "NULL"), pk);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Errore database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}