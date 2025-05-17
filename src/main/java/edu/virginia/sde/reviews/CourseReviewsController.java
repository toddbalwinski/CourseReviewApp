package edu.virginia.sde.reviews;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CourseReviewsController implements Initializable {
    @FXML private Label courseTitleLabel;
    @FXML private Label averageRatingLabel;
    @FXML private VBox reviewsContainer;
    @FXML private VBox addReviewContainer;
    @FXML private HBox ratingBox;
    @FXML private TextArea commentArea;
    @FXML private Button deleteButton;
    @FXML private Button submitButton;
    @FXML private Button backButton;

    private String currentUsername; 
    private List<Review> reviews;
    private Review currentUserReview;
    private ToggleGroup ratingGroup;
    private Course course;
    private Database database;
    private User currentUser;

    public CourseReviewsController() {
        this.reviews = new ArrayList<>();
        this.currentUsername = null;
        this.database = null;
    }

    public void initializeController(Database database) {
        this.database = database;
        this.currentUsername = UserSession.getInstance().getUser().getUsername();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            showError("Authentication Error", new Exception("Please log in first"));
            handleBackButton();
            return;
        }

        reviews = new ArrayList<>();
        ratingGroup = new ToggleGroup();
        setupRatingButtons();
    }

    public void setCourse(Course course) {
        this.course = course;
        setupCourseInfo();
        loadReviews();
        checkAndShowUserReview();
    }

    private void setupCourseInfo() {
        courseTitleLabel.setText(
            String.format("%s %d: %s", 
                course.getMnemonic(), 
                course.getNumber(), 
                course.getTitle())
        );
    }

    private void setupRatingButtons() {
        for (int i = 1; i <= 5; i += 1) {
            RadioButton rb = new RadioButton(String.valueOf(i));
            rb.setToggleGroup(ratingGroup);
            rb.setUserData(i);
            ratingBox.getChildren().add(rb);
        }
    }

    private void loadReviews() {
        try {
            reviews = database.getReviewsByCourse(course);
            updateReviewsList();
            updateAverageRating();
        } catch (SQLException e) {
            showError("Error loading reviews", e);
        }
    }

    @FXML
    private void handleReviewSubmission() {
        Toggle selectedRating = ratingGroup.getSelectedToggle();
        if (selectedRating == null) {
            showAlert("Please select a rating");
            return;
        }

        int rating = (Integer) selectedRating.getUserData();
        String comment = commentArea.getText().trim();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Review newReview = new Review(
            currentUserReview != null ? currentUserReview.getReviewID() : 0, // Database will assign actual ID
            course.getCourseID(),
            currentUsername,
            rating,
            comment,
            timestamp
        );

        try {
            List<Review> reviewsToAdd = new ArrayList<>();
            reviewsToAdd.add(newReview);
            database.addReviews(reviewsToAdd);
            database.commit();
            
            // Reload reviews to get the updated list with database-assigned IDs
            loadReviews();
            checkAndShowUserReview();
        } catch (SQLException e) {
            try {
                database.rollback();
            } catch (SQLException rollbackEx) {
                showError("Error rolling back transaction", rollbackEx);
            }
            showError("Error submitting review", e);
        }
    }

    @FXML
    private void handleDeleteReview() {
        try {
            if (currentUserReview != null) {
                database.deleteReview(course.getCourseID(), currentUsername);
                
                ratingGroup.selectToggle(null);
                commentArea.clear();
                
                loadReviews();
                checkAndShowUserReview();
            }
        } catch (SQLException e) {
            showError("Error deleting review", e);
        }
    }

    @FXML
    private void handleBackButton() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("course-search-screen.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1280, 720);

            Stage stage = (Stage) ratingBox.getScene().getWindow();
            stage.setTitle("Course Reviews - Search Courses");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
        }
    }

    private void updateReviewsList() {
        reviewsContainer.getChildren().clear();
        
        for (Review review : reviews) {
            VBox reviewBox = new VBox(5);
            reviewBox.setStyle("-fx-border-color: lightgray; -fx-padding: 10; -fx-border-radius: 5;");
            
            Label ratingLabel = new Label(String.format("Rating: %.1f", review.getRating()));
            Label timestampLabel = new Label("Posted: " + review.getTimestamp());
            
            reviewBox.getChildren().addAll(ratingLabel, timestampLabel);
            
            if (!review.getComment().isEmpty()) {
                Label commentLabel = new Label("Comment: " + review.getComment());
                commentLabel.setWrapText(true);
                reviewBox.getChildren().add(commentLabel);
            }
            
            reviewsContainer.getChildren().add(reviewBox);
        }
    }

    private void updateAverageRating() {
        if (reviews.isEmpty()) {
            averageRatingLabel.setText("No reviews yet");
            return;
        }

        double average = reviews.stream()
            .mapToDouble(Review::getRating)
            .average()
            .orElse(0.0);

        averageRatingLabel.setText(String.format("Average Rating: %.2f", average));
    }

    private void checkAndShowUserReview() {
        currentUserReview = reviews.stream()
            .filter(r -> r.getAuthorUsername().equals(currentUsername))
            .findFirst()
            .orElse(null);

        deleteButton.setVisible(currentUserReview != null);

        if (currentUserReview != null) {
            ratingGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(currentUserReview.getRating()))
                .findFirst()
                .ifPresent(t -> t.setSelected(true));
            
            commentArea.setText(currentUserReview.getComment());
            submitButton.setText("Update Review");
        } else {
            ratingGroup.selectToggle(null);
            commentArea.clear();
            submitButton.setText("Submit Review");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}