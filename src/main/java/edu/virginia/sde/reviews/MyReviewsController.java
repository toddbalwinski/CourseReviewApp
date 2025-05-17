package edu.virginia.sde.reviews;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class MyReviewsController implements Initializable {
    @FXML private Label headerLabel;
    @FXML private Label reviewCountLabel;
    @FXML private VBox reviewsContainer;
    @FXML private Label errorMessage;
    
    private final Database database;
    private List<Review> userReviews;
    private Map<Integer, Course> coursesMap;
    private String currentUsername;
    
    public MyReviewsController() {
        this.database = CourseReviewsApplication.getDatabase();
        this.userReviews = new ArrayList<>();
        this.coursesMap = new HashMap<>();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Get current user from UserSession
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            showError("Please log in first");
            handleBackButton();
            return;
        }
        
        this.currentUsername = currentUser.getUsername();
        loadReviewData();
        updateReviewsList();
        updateReviewCount();
    }
    
    private void loadReviewData() {
        try {
            // First, get all reviews by the current user
            userReviews = database.getReviewsByUser(new User(currentUsername, ""));
            
            // Then, get the course details for each review
            for (Review review : userReviews) {
                if (!coursesMap.containsKey(review.getCourseID())) {
                    Course course = getCourseById(review.getCourseID());
                    if (course != null) {
                        coursesMap.put(course.getCourseID(), course);
                    }
                }
            }
        } catch (SQLException e) {
            showError("Error loading reviews: " + e.getMessage());
        }
    }

    private Course getCourseById(int courseId) throws SQLException {
        List<Course> allCourses = database.getAllCourses();
        for (Course course : allCourses) {
            if (course.getCourseID() == courseId) {
                return course;
            }
        }
        return null;
    }
    
    private void updateReviewsList() {
        reviewsContainer.getChildren().clear();
        
        for (Review review : userReviews) {
            VBox reviewBox = createReviewBox(review);
            reviewsContainer.getChildren().add(reviewBox);
        }
    }
    
    private VBox createReviewBox(Review review) {
        VBox reviewBox = new VBox(5);
        reviewBox.setStyle(
            "-fx-border-color: lightgray; " +
            "-fx-padding: 10; " +
            "-fx-border-radius: 5; " +
            "-fx-cursor: hand;"
        );
        
        Course course = coursesMap.get(review.getCourseID());
        if (course == null) {
            return reviewBox; // Skip if course not found
        }
        
        Label courseLabel = new Label(
            String.format("%s %d: %s", 
                course.getMnemonic(), 
                course.getNumber(),
                course.getTitle())
        );
        courseLabel.setStyle("-fx-font-weight: bold;");
        
        HBox ratingBox = new HBox(5);
        Label ratingLabel = new Label(String.format("Rating: %.1f", review.getRating()));
        ratingBox.getChildren().add(ratingLabel);
        
        Label timestampLabel = new Label("Posted: " + review.getTimestamp());
        timestampLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
        ratingBox.getChildren().add(timestampLabel);
        
        reviewBox.getChildren().addAll(courseLabel, ratingBox);
        
        if (review.getComment() != null && !review.getComment().isEmpty()) {
            Label commentLabel = new Label(review.getComment());
            commentLabel.setWrapText(true);
            reviewBox.getChildren().add(commentLabel);
        }
        
        reviewBox.setOnMouseClicked(e -> handleReviewClick(course));
        
        return reviewBox;
    }
    
    private void updateReviewCount() {
        reviewCountLabel.setText(
            String.format("You have reviewed %d courses", userReviews.size())
        );
    }
    
    @FXML
    private void handleBackButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("course-search-screen.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            Stage stage = (Stage) reviewsContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Course Reviews - Search Courses");
            stage.show();
        } catch (IOException e) {
            showError("Error navigating back: " + e.getMessage());
        }
    }
    
    private void handleReviewClick(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("course-review-screen.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            
            CourseReviewsController controller = loader.getController();
            controller.initializeController(database);
            controller.setCourse(course);
            
            Stage stage = (Stage) reviewsContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Error opening course review: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
        }
    }
}