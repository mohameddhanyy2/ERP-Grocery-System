package com.groceryerp.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

    private static final String URL = "jdbc:sqlite:grocery_erp.db";

    private static Database instance;

    private Connection connection;

    private Database() {
        try {
            connection = DriverManager.getConnection(URL);
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            System.out.println("Failed to close database: " + e.getMessage());
        }
    }
}
