package com.example;

import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        if (isDevMode(args)) {
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");

        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
             Scanner scanner = new Scanner(System.in)) {

            boolean loggedIn = false;
            while (!loggedIn) {
                System.out.print("Username: ");
                String username = scanner.nextLine();

                System.out.print("Password: ");
                String password = scanner.nextLine();

                if (validateLogin(connection, username, password)) {
                    loggedIn = true;
                } else {
                    System.out.println("Invalid username or password");
                    System.out.print("Enter 0 to exit or any key to try again: ");
                    String input = scanner.nextLine();
                    if ("0".equals(input.trim())) {
                        return;
                    }
                }
            }

            showMainMenu(connection, scanner);

        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed: " + e.getMessage(), e);
        }
    }

    private boolean validateLogin(Connection connection, String username, String password) {
        String sql = "SELECT user_id FROM account WHERE name = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private void showMainMenu(Connection connection, Scanner scanner) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1) List moon missions");
            System.out.println("2) Get a moon mission by mission_id");
            System.out.println("3) Count missions for a given year");
            System.out.println("4) Create an account");
            System.out.println("5) Update an account password");
            System.out.println("6) Delete an account");
            System.out.println("0) Exit");
            System.out.print("Choose option: ");

            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    listMoonMissions(connection);
                    break;
                case "2":
                    getMissionById(connection, scanner);
                    break;
                case "3":
                    countMissionsByYear(connection, scanner);
                    break;
                case "4":
                    createAccount(connection, scanner);
                    break;
                case "5":
                    updateAccountPassword(connection, scanner);
                    break;
                case "6":
                    deleteAccount(connection, scanner);
                    break;
                case "0":
                    exit = true;
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void listMoonMissions(Connection connection) {
        String sql = "SELECT spacecraft FROM moon_mission ORDER BY launch_date";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n=== Moon Missions ===");
            while (rs.next()) {
                String name = rs.getString("spacecraft");
                System.out.println(name);
            }

        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void getMissionById(Connection connection, Scanner scanner) {
        System.out.print("Enter mission ID: ");
        String missionIdStr = scanner.nextLine();

        try {
            long missionId = Long.parseLong(missionIdStr);
            String sql = "SELECT * FROM moon_mission WHERE mission_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, missionId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    System.out.println("\nMission ID: " + rs.getLong("mission_id"));
                    System.out.println("Spacecraft: " + rs.getString("spacecraft"));
                    System.out.println("Launch date: " + rs.getDate("launch_date"));
                    System.out.println("Operator: " + rs.getString("operator"));
                    System.out.println("Mission type: " + rs.getString("mission_type"));
                    System.out.println("Outcome: " + rs.getString("outcome"));
                } else {
                    System.out.println("No mission found with ID: " + missionId);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid mission ID format");
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private void countMissionsByYear(Connection connection, Scanner scanner) {
        System.out.print("Enter year: ");
        String yearStr = scanner.nextLine();

        try {
            int year = Integer.parseInt(yearStr);
            String sql = "SELECT COUNT(*) as count FROM moon_mission WHERE YEAR(launch_date) = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, year);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("Number of missions in " + year + ": " + count);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid year format");
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private void createAccount(Connection connection, Scanner scanner) {
        System.out.print("Enter first name: ");
        String firstName = scanner.nextLine();

        System.out.print("Enter last name: ");
        String lastName = scanner.nextLine();

        System.out.print("Enter SSN: ");
        String ssn = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String username = firstName.substring(0, Math.min(3, firstName.length())) +
                lastName.substring(0, Math.min(3, lastName.length()));

        String sql = "INSERT INTO account (first_name, last_name, ssn, name, password) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, ssn);
            stmt.setString(4, username);
            stmt.setString(5, password);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Account created successfully");
            } else {
                System.out.println("Failed to create account");
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private void updateAccountPassword(Connection connection, Scanner scanner) {
        System.out.print("Enter user ID: ");
        String userIdStr = scanner.nextLine();

        System.out.print("Enter new password: ");
        String newPassword = scanner.nextLine();

        try {
            long userId = Long.parseLong(userIdStr);
            String sql = "UPDATE account SET password = ? WHERE user_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, newPassword);
                stmt.setLong(2, userId);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Password updated successfully");
                } else {
                    System.out.println("No account found with ID: " + userId);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid user ID format");
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private void deleteAccount(Connection connection, Scanner scanner) {
        System.out.print("Enter user ID: ");
        String userIdStr = scanner.nextLine();

        try {
            long userId = Long.parseLong(userIdStr);
            String sql = "DELETE FROM account WHERE user_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, userId);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Account deleted successfully");
                } else {
                    System.out.println("No account found with ID: " + userId);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid user ID format");
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))
            return true;
        return Arrays.asList(args).contains("--dev");
    }

    private static String resolveConfig(String propertyKey, String envKey) {
        String v = System.getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}