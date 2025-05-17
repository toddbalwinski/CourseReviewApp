package edu.virginia.sde.reviews;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;

public class CourseReviewScene extends VBox {
    private final String currentUserId = "user123"; 
    private List<Review> reviews;
    private Review currentUserReview;
    private final Course course;
    
    private VBox reviewsContainer;
    private VBox addReviewContainer;
    private Label averageRatingLabel;
    private ToggleGroup ratingGroup;
    private TextArea commentArea;
    
    public CourseReviewScene(Course course) {
        this.course = course;
        this.reviews = new ArrayList<>();
        
        loadDummyData();
        
        setPadding(new Insets(20));
        setSpacing(20);
        
        setupHeader();
        setupReviewsList();
        setupAddReviewSection();
        setupBackButton();
        
        updateAverageRating();
        checkAndShowUserReview();
    }
    
    private void setupHeader() {
        Label courseTitle = new Label(
            String.format("%s %s: %s", 
                course.getMnemonic(), 
                course.getNumber(), 
                course.getTitle()
            )
        );
        courseTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        averageRatingLabel = new Label();
        averageRatingLabel.setStyle("-fx-font-size: 16px;");
        
        getChildren().addAll(courseTitle, averageRatingLabel);
    }
    
    private void setupReviewsList() {
        Label reviewsHeader = new Label("Course Reviews");
        reviewsHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        reviewsContainer = new VBox(10);
        ScrollPane scrollPane = new ScrollPane(reviewsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        
        getChildren().addAll(reviewsHeader, scrollPane);
        updateReviewsList();
    }
    
    private void setupAddReviewSection() {
        addReviewContainer = new VBox(10);
        addReviewContainer.setPadding(new Insets(10));
        addReviewContainer.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5;");
        
        Label ratingLabel = new Label("Rating:");
        ratingGroup = new ToggleGroup();
        HBox ratingButtons = new HBox(10);
        
        for (int i = 1; i <= 5; i++) {
            RadioButton rb = new RadioButton(String.valueOf(i));
            rb.setToggleGroup(ratingGroup);
            rb.setUserData(i);
            ratingButtons.getChildren().add(rb);
        }
        
        Label commentLabel = new Label("Comment (optional):");
        commentArea = new TextArea();
        commentArea.setPrefRowCount(3);
        
        Button submitButton = new Button("Submit Review");
        submitButton.setOnAction(e -> handleReviewSubmission());
        
        addReviewContainer.getChildren().addAll(
            ratingLabel, 
            ratingButtons, 
            commentLabel, 
            commentArea, 
            submitButton
        );
        
        getChildren().add(addReviewContainer);
    }
    
    private void setupBackButton() {
        Button backButton = new Button("Back to Course Search");
        backButton.setOnAction(e -> handleBackButton());
        getChildren().add(backButton);
    }
    
    private void handleReviewSubmission() {
        Toggle selectedRating = ratingGroup.getSelectedToggle();
        if (selectedRating == null) {
            showAlert("Please select a rating");
            return;
        }
        
        int rating = (Integer) selectedRating.getUserData();
        String comment = commentArea.getText().trim();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        
        if (currentUserReview != null) {
            currentUserReview.setRating(rating);
            currentUserReview.setComment(comment);
            currentUserReview.setTimestamp(timestamp);
        } else {
            Review newReview = new Review(
                currentUserId,
                rating,
                comment,
                timestamp
            );
            reviews.add(newReview);
            currentUserReview = newReview;
        }
        
        updateReviewsList();
        updateAverageRating();
        checkAndShowUserReview();
    }
    
    private void handleDeleteReview() {
        if (currentUserReview != null) {
            reviews.remove(currentUserReview);
            currentUserReview = null;
            updateReviewsList();
            updateAverageRating();
            checkAndShowUserReview();
        }
    }
    
    private void updateReviewsList() {
        reviewsContainer.getChildren().clear();
        
        for (Review review : reviews) {
            VBox reviewBox = new VBox(5);
            reviewBox.setStyle("-fx-border-color: lightgray; -fx-padding: 10; -fx-border-radius: 5;");
            
            Label ratingLabel = new Label("Rating: " + review.getRating());
            Label timestampLabel = new Label("Posted: " + review.getTimestamp().toString());
            
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
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);
            
        DecimalFormat df = new DecimalFormat("0.00");
        averageRatingLabel.setText("Average Rating: " + df.format(average));
    }
    
    private void checkAndShowUserReview() {
        currentUserReview = reviews.stream()
            .filter(r -> r.getUserId().equals(currentUserId))
            .findFirst()
            .orElse(null);
            
        if (currentUserReview != null) {
            addReviewContainer.getChildren().clear();
            
            RadioButton selectedRating = (RadioButton) ratingGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(currentUserReview.getRating()))
                .findFirst()
                .orElse(null);
            if (selectedRating != null) {
                selectedRating.setSelected(true);
            }
            
            commentArea.setText(currentUserReview.getComment());
            
            Button updateButton = new Button("Update Review");
            updateButton.setOnAction(e -> handleReviewSubmission());
            
            Button deleteButton = new Button("Delete Review");
            deleteButton.setOnAction(e -> handleDeleteReview());
            
            HBox buttonBox = new HBox(10, updateButton, deleteButton);
            
            addReviewContainer.getChildren().addAll(
                new Label("Edit Your Review:"),
                new HBox(10, ratingGroup.getToggles().stream()
                    .map(t -> (RadioButton) t)
                    .toArray(RadioButton[]::new)),
                new Label("Comment (optional):"),
                commentArea,
                buttonBox
            );
        }
    }
    
    private void handleBackButton() {
        // TO DO: implement functionality for navigation
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void loadDummyData() {
        reviews.add(new Review(
            "user456",
            4,
            "Great course! Learned a lot about software development.",
            Timestamp.valueOf("2024-03-01 10:30:00")
        ));
        reviews.add(new Review(
            "user789",
            5,
            "Excellent professor and material.",
            Timestamp.valueOf("2024-02-28 15:45:00")
        ));
    }
    
    private static class Course {
        private final String mnemonic;
        private final String number;
        private final String title;
        
        public Course(String mnemonic, String number, String title) {
            this.mnemonic = mnemonic;
            this.number = number;
            this.title = title;
        }
        
        public String getMnemonic() { return mnemonic; }
        public String getNumber() { return number; }
        public String getTitle() { return title; }
    }
    
    private static class Review {
        private final String userId;
        private int rating;
        private String comment;
        private Timestamp timestamp;
        
        public Review(String userId, int rating, String comment, Timestamp timestamp) {
            this.userId = userId;
            this.rating = rating;
            this.comment = comment;
            this.timestamp = timestamp;
        }
        
        public String getUserId() { return userId; }
        public int getRating() { return rating; }
        public String getComment() { return comment; }
        public Timestamp getTimestamp() { return timestamp; }
        
        public void setRating(int rating) { this.rating = rating; }
        public void setComment(String comment) { this.comment = comment; }
        public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    }
}