import java.sql.*;
import java.util.Scanner;

public class ParkingManagementSystem{
    static final String URL = "jdbc:mysql://localhost:3306/ParkingManagement";
    static final String USER = "root";
    static final String PASSWORD = "YOUR_PASSWORD"; 

    public static void main(String[] args) {
        try (
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();
            Scanner sc = new Scanner(System.in)
        ) {
            createTables(stmt);

            int choice;
            do {
                System.out.println("\n--- Parking Management System ---");
                System.out.println("1. Add User");
                System.out.println("2. Add Parking Slot");
                System.out.println("3. Book a Slot");
                System.out.println("4. View All Users");
                System.out.println("5. View All Slots");
                System.out.println("6. View All Bookings");
                System.out.println("7. Exit");
                System.out.print("Enter your choice: ");

                while (!sc.hasNextInt()) {
                    System.out.print("Invalid input. Please enter a number: ");
                    sc.next(); // discard invalid input
                }
                choice = sc.nextInt();
                sc.nextLine(); // consume newline

                switch (choice) {
                    case 1 -> addUser(conn, sc);
                    case 2 -> addSlot(conn, sc);
                    case 3 -> bookSlot(conn, sc);
                    case 4 -> viewUsers(stmt);
                    case 5 -> viewSlots(stmt);
                    case 6 -> viewBookings(stmt);
                    case 7 -> System.out.println("Exiting...");
                    default -> System.out.println("Invalid choice.");
                }

            } while (choice != 7);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void createTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Users (" +
                "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) UNIQUE NOT NULL, " +
                "vehicle_number VARCHAR(20) UNIQUE)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ParkingSlots (" +
                "slot_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "slot_number VARCHAR(10) UNIQUE NOT NULL, " +
                "is_available BOOLEAN DEFAULT TRUE)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Bookings (" +
                "booking_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT, " +
                "slot_id INT, " +
                "booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES Users(user_id), " +
                "FOREIGN KEY (slot_id) REFERENCES ParkingSlots(slot_id))");
    }

    static void addUser(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter name: ");
        String name = sc.nextLine();
        System.out.print("Enter email: ");
        String email = sc.nextLine();
        System.out.print("Enter vehicle number: ");
        String vehicle = sc.nextLine();

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Users (name, email, vehicle_number) VALUES (?, ?, ?)");
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setString(3, vehicle);

        try {
            ps.executeUpdate();
            System.out.println("User added.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Email or vehicle number already exists.");
        }
    }

    static void addSlot(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter slot number (e.g., A1): ");
        String slot = sc.nextLine();

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ParkingSlots (slot_number) VALUES (?)");
        ps.setString(1, slot);

        try {
            ps.executeUpdate();
            System.out.println("Slot added.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Slot already exists.");
        }
    }

    static void bookSlot(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter user ID: ");
        while (!sc.hasNextInt()) {
            System.out.print("Please enter a valid user ID: ");
            sc.next(); // discard invalid input
        }
        int userId = sc.nextInt();

        System.out.print("Enter slot ID: ");
        while (!sc.hasNextInt()) {
            System.out.print("Please enter a valid slot ID: ");
            sc.next(); // discard invalid input
        }
        int slotId = sc.nextInt();
        sc.nextLine(); // consume newline

        // Check availability
        PreparedStatement checkSlot = conn.prepareStatement(
                "SELECT is_available FROM ParkingSlots WHERE slot_id = ?");
        checkSlot.setInt(1, slotId);
        ResultSet rs = checkSlot.executeQuery();

        if (rs.next() && rs.getBoolean("is_available")) {
            // Book the slot
            PreparedStatement book = conn.prepareStatement(
                    "INSERT INTO Bookings (user_id, slot_id) VALUES (?, ?)");
            book.setInt(1, userId);
            book.setInt(2, slotId);
            book.executeUpdate();

            // Update slot availability
            PreparedStatement updateSlot = conn.prepareStatement(
                    "UPDATE ParkingSlots SET is_available = FALSE WHERE slot_id = ?");
            updateSlot.setInt(1, slotId);
            updateSlot.executeUpdate();

            System.out.println("Slot booked successfully.");
        } else {
            System.out.println("Slot not available or doesn't exist.");
        }
    }

    static void viewUsers(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT * FROM Users");
        System.out.println("\n-- Users --");
        while (rs.next()) {
            System.out.println(rs.getInt("user_id") + ": " +
                    rs.getString("name") + ", " +
                    rs.getString("email") + ", " +
                    rs.getString("vehicle_number"));
        }
    }

    static void viewSlots(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT * FROM ParkingSlots");
        System.out.println("\n-- Parking Slots --");
        while (rs.next()) {
            System.out.println(rs.getInt("slot_id") + ": Slot " +
                    rs.getString("slot_number") +
                    " - Available: " + rs.getBoolean("is_available"));
        }
    }

    static void viewBookings(Statement stmt) throws SQLException {
        String query = "SELECT b.booking_id, u.name, p.slot_number, b.booking_time " +
                       "FROM Bookings b " +
                       "JOIN Users u ON b.user_id = u.user_id " +
                       "JOIN ParkingSlots p ON b.slot_id = p.slot_id";

        ResultSet rs = stmt.executeQuery(query);
        System.out.println("\n-- Bookings --");
        while (rs.next()) {
            System.out.println("Booking ID " + rs.getInt("booking_id") + ": " +
                    rs.getString("name") + " -> Slot " +
                    rs.getString("slot_number") + " at " +
                    rs.getTimestamp("booking_time"));
        }
    }
}
