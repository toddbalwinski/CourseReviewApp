package edu.virginia.sde.reviews;

import java.io.IOException;
import java.sql.SQLException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginScreenController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorMessage;

    @FXML
    private Button logoutButton;

    private final Database db;

    public LoginScreenController() {
        this.db = CourseReviewsApplication.getDatabase();
    }

    @FXML
    public void initialize() {
        String databaseError = CourseReviewsApplication.getDatabaseError();
        if (databaseError != null) {
            errorMessage.setText(databaseError);
        }
    }

    @FXML
    public void login() {
        if (db == null) {
            errorMessage.setText("Database is not available. Please contact support.");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorMessage.setText("Both fields are required.");
            return;
        }

        try {
            if (db.validateUser(username, password)) {
                // Create User object and set in UserSession
                User user = new User(username, password);
                UserSession.getInstance().setUser(user);
                
                errorMessage.setText("Welcome back, " + username + "!");
                navigateToCourseSearch();
            } else {
                errorMessage.setText("Username and password combination is incorrect.");
            }
        } catch (SQLException e) {
            errorMessage.setText("An error occurred. Please try again.");
        }
    }

    @FXML
    public void createAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorMessage.setText("Username and password fields cannot be empty.");
            return;
        }

        if (password.length() < 8) {
            errorMessage.setText("Password must be at least 8 characters long.");
            return;
        }

        try {
            if (db.createUser(username, password)) {
                // Create User object and set in UserSession
                User user = new User(username, password);
                UserSession.getInstance().setUser(user);
                errorMessage.setText("Account created successfully!");
            } else {
                errorMessage.setText("Account already exists.");
            }
        } catch (SQLException e) {
            errorMessage.setText("Error creating account.");
        }
    }

    @FXML
    public void handleClose() {
        System.exit(0);
    }

    public void navigateToCourseSearch() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("course-search-screen.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1280, 720);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Course Reviews - Search Courses");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            errorMessage.setText("Failed to load course search screen. Please try again.");
        }
    }
}