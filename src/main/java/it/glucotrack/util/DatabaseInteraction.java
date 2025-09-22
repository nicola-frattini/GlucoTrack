package it.glucotrack.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/*
* DATABASE INTERACTION
*/

public class DatabaseInteraction {

    private static String dbUrl = "jdbc:sqlite:src/main/resources/database/glucotrack_db.sqlite";
    private static Connection connection = null;



    // Change DB for Tests
    public static void setDatabasePath(String path) {
        dbUrl = "jdbc:sqlite:" + path;
        disconnect(); // Ensure new connection uses the new path
    }


    // Open a connection to the DB (singleton pattern)
    public static Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
        }
        return connection;
    }


    // Close DB connection
    public static void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    //Execute a SELECT query (returns java.sql.ResultSet, caller must close it)
    public static java.sql.ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = connect();
        java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt.executeQuery();
    }


    //Execute an INSERT/UPDATE/DELETE (returns affected rows)
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = connect();
        java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt.executeUpdate();
    }


    //Utility: set parameters for PreparedStatement
    private static void setParameters(java.sql.PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}