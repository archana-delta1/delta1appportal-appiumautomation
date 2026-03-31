package com.deltaone.utils;

import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseUtils {

    private static Properties props = new Properties();

    // Load configuration once when the class is loaded
    static {
        // Dynamically build the exact path from the project root
        String projectPath = System.getProperty("user.dir");
        String propertyFilePath = projectPath + "/src/test/resources/config.properties";

        try (FileInputStream fis = new FileInputStream(propertyFilePath)) {
            props.load(fis);
            System.out.println("Successfully loaded config.properties from: " + propertyFilePath);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Could not load config.properties at " + propertyFilePath);
            e.printStackTrace();
            throw new RuntimeException("Sorry, unable to find config.properties at " + propertyFilePath);
        }
    }

    private static String getJdbcUrl() {
        return String.format("jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true;",
                props.getProperty("db.host"),
                props.getProperty("db.port"),
                props.getProperty("db.name"));
    }

    public static Connection getConnection() throws SQLException {
        String url = getJdbcUrl();
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, pass);
    }

    public static void verifyDatabaseConnection() {
        String url = getJdbcUrl();
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.password");

        System.out.println("Connecting to: " + url);

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            
            // 1. Validate connection
            try (Statement stmt = conn.createStatement()) {
                stmt.executeQuery("SELECT 1");
                System.out.println("Connection validated.");
            }

            // 2. Fetch SQL Version
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT @@VERSION AS SqlVersion")) {
                if (rs.next()) {
                    System.out.println("SQL Server Version: " + rs.getString("SqlVersion"));
                }
            }

            // 3. Fetch Application Settings
            String query = "SELECT [AppKey], [AppValue] FROM [DeltaOne].[dbo].[ApplicationSettings]";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                System.out.println("Data from ApplicationSettings:");
                while (rs.next()) {
                    System.out.println("SettingId: " + rs.getString("AppKey") + 
                                       ", SettingName: " + rs.getString("AppValue"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
}