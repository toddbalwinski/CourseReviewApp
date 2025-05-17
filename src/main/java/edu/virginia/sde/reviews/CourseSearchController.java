package edu.virginia.sde.reviews;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CourseSearchController {

    @FXML private TextField subjectSearchField;
    @FXML private TextField numberSearchField;
    @FXML private TextField titleSearchField;
    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> subjectColumn;
    @FXML private TableColumn<Course, Integer> numberColumn;
    @FXML private TableColumn<Course, String> titleColumn;
    @FXML private TableColumn<Course, String> ratingColumn;
    @FXML private TableColumn<Course, Void> actionColumn;
    @FXML private TextField addSubjectField;
    @FXML private TextField addNumberField;
    @FXML private TextField addTitleField;
    @FXML private Label errorMessage;

    private final Database db;
    private User currentUser;

    public CourseSearchController() {
        this.db = CourseReviewsApplication.getDatabase();
    }

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            errorMessage.setText("Please log in first");
            logOut();
            return;
        }

        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("mnemonic"));
        numberColumn.setCellValueFactory(new PropertyValueFactory<>("number"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        
        // Set up rating column with calculated average
        ratingColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue();
            try {
                List<Review> reviews = db.getReviewsByCourse(course);
                if (reviews.isEmpty()) {
                    return new SimpleStringProperty("No reviews");
                }
                
                double averageRating = reviews.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);
                
                return new SimpleStringProperty(String.format("%.1f", averageRating));
            } catch (SQLException e) {
                e.printStackTrace();
                return new SimpleStringProperty("Error");
            }
        });

        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button reviewButton = new Button("View Reviews");

            {
                reviewButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    try {
                        navigateToCourseReview(course);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(reviewButton);
                }
            }
        });

        loadCourses();
    }

    @FXML
    private void navigateToCourseReview(Course course) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("course-review-screen.fxml"));
        Parent root = loader.load();
        
        CourseReviewsController reviewController = loader.getController();
        reviewController.initializeController(db);
        reviewController.setCourse(course);
        
        Scene scene = new Scene(root, 1280, 720);
        Stage stage = (Stage) courseTable.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void search() {
        String subject = subjectSearchField.getText().trim();
        String number = numberSearchField.getText().trim();
        String title = titleSearchField.getText().trim();

        try {
            List<Course> courses = db.searchCourses(subject, number, title);
            courseTable.getItems().setAll(courses);
            if (courses.isEmpty()) {
                errorMessage.setText("No courses found.");
            } else {
                errorMessage.setText("");
            }
        } catch (SQLException e) {
            errorMessage.setText("Course search error.");
            System.out.println("Error searching for courses: " + e.getMessage());
        }
    }

    @FXML
    public void handleAddCourse() {
        String subject = addSubjectField.getText().trim().toUpperCase();
        String number = addNumberField.getText().trim();
        String title = addTitleField.getText().trim();

        if (!subject.matches("[A-Z]{2,4}")) {
            errorMessage.setText("Subject must be 2-4 letters.");
            return;
        }
        if (!number.matches("\\d{4}")) {
            errorMessage.setText("Number must be exactly 4 digits.");
            return;
        }
        if (title.isEmpty() || title.length() > 50) {
            errorMessage.setText("Title must be 1-50 characters.");
            return;
        }

        try {
            if(db.addCourse(subject, Integer.parseInt(number), title)) {
                loadCourses();
                errorMessage.setText("");
            } else {
                errorMessage.setText("Course already exists.");
            }
        } catch (SQLException e) {
            errorMessage.setText("Error adding course.");
        }
    }

    @FXML
    public void handleMyReviews() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("my-reviews-screen.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);

            Stage stage = (Stage) courseTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("My Reviews");
            stage.show();
        } catch (IOException e) {
            errorMessage.setText("Error navigating to My Reviews screen.");
        }
    }

    @FXML
    public void logOut() {
        UserSession.getInstance().clearSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login-screen.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);

            Stage stage = (Stage) courseTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login Screen");
            stage.show();
        } catch (IOException e) {
            errorMessage.setText("Error navigating to login screen.");
        }
    }

    private void loadCourses() {
        try {
            List<Course> courses = db.getAllCourses();
            courseTable.getItems().setAll(courses);
        } catch (SQLException e) {
            errorMessage.setText("Error loading courses.");
        }
    }
}