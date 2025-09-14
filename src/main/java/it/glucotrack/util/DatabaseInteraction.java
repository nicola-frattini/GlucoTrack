package it.glucotrack.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseInteraction {

    private static final String DB_URL = "jdbc:sqlite:src/main/resources/database/glucotrack_db.sqlite";
    private static Connection connection = null;

    /**
     * Open a connection to the database (singleton pattern)
     */
    public static Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    /**
     * Close the database connection
     */
    public static void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Execute a SELECT query (returns java.sql.ResultSet, caller must close it)
     */
    public static java.sql.ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = connect();
        java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt.executeQuery();
    }

    /**
     * Execute an INSERT/UPDATE/DELETE (returns affected rows)
     */
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = connect();
        java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt.executeUpdate();
    }

    /**
     * Utility: set parameters for PreparedStatement
     */
    private static void setParameters(java.sql.PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}