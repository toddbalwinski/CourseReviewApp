package edu.virginia.sde.reviews;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.sql.Timestamp;
import java.util.*;

public class MyReviewsScene extends VBox {
    private final String currentUserId = "user123";
    private List<Review> userReviews;
    private final Map<String, Course> coursesMap;  // To store course info
    
    public MyReviewsScene() {
        this.userReviews = new ArrayList<>();
        this.coursesMap = new HashMap<>();
        
        loadDummyData();
        
        setPadding(new Insets(20));
        setSpacing(20);
        
        setupHeader();
        setupReviewsList();
        setupBackButton();
    }
    
    private void setupHeader() {
        Label headerLabel = new Label("My Course Reviews");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label reviewCountLabel = new Label(
            String.format("You have reviewed %d courses", userReviews.size())
        );
        reviewCountLabel.setStyle("-fx-font-size: 16px;");
        
        getChildren().addAll(headerLabel, reviewCountLabel);
    }
    
    private void setupReviewsList() {
        VBox reviewsContainer = new VBox(10);
        ScrollPane scrollPane = new ScrollPane(reviewsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        
        for (Review review : userReviews) {
            VBox reviewBox = createReviewBox(review);
            reviewsContainer.getChildren().add(reviewBox);
        }
        
        getChildren().add(scrollPane);
    }
    
    private VBox createReviewBox(Review review) {
        VBox reviewBox = new VBox(5);
        reviewBox.setStyle(
            "-fx-border-color: lightgray; " +
            "-fx-padding: 10; " +
            "-fx-border-radius: 5; " +
            "-fx-cursor: hand;"
        );
        
        Course course = coursesMap.get(review.getCourseId());
        
        Label courseLabel = new Label(
            String.format("%s %s", course.getMnemonic(), course.getNumber())
        );
        courseLabel.setStyle("-fx-font-weight: bold;");
        
        HBox ratingBox = new HBox(5);
        Label ratingLabel = new Label("Rating: " + review.getRating());
        ratingBox.getChildren().add(ratingLabel);
        
        reviewBox.getChildren().addAll(courseLabel, ratingBox);
        
        if (!review.getComment().isEmpty()) {
            Label commentLabel = new Label(review.getComment());
            commentLabel.setWrapText(true);
            reviewBox.getChildren().add(commentLabel);
        }
        
        // Add click handler to navigate to course review scene
        reviewBox.setOnMouseClicked(e -> handleReviewClick(course));
        
        return reviewBox;
    }
    
    private void setupBackButton() {
        Button backButton = new Button("Back to Course Search");
        backButton.setOnAction(e -> handleBackButton());
        getChildren().add(backButton);
    }
    
    private void handleReviewClick(Course course) {
        // TO DO: Navigate to CourseReviewScene for the selected course
        System.out.println("Navigating to review for: " + 
            course.getMnemonic() + " " + course.getNumber());
    }
    
    private void handleBackButton() {
        // TO DO: Navigate back to CourseSearchScene
        System.out.println("Navigating back to course search");
    }
    
    private void loadDummyData() {
        // Create some dummy courses
        Course course1 = new Course("CS", "3140", "Software Development Essentials");
        Course course2 = new Course("CS", "2100", "Data Structures and Algorithms");
        Course course3 = new Course("CS", "4640", "Programming Languages");
        
        coursesMap.put("CS3140", course1);
        coursesMap.put("CS2100", course2);
        coursesMap.put("CS4640", course3);
        
        // Create dummy reviews
        userReviews.add(new Review(
            currentUserId,
            "CS3140",
            4,
            "Great course! Learned a lot about software development.",
            Timestamp.valueOf("2024-03-01 10:30:00")
        ));
        
        userReviews.add(new Review(
            currentUserId,
            "CS2100",
            5,
            "Essential course for any CS student. Challenging but rewarding.",
            Timestamp.valueOf("2024-02-15 14:20:00")
        ));
        
        userReviews.add(new Review(
            currentUserId,
            "CS4640",
            3,
            "",  // Empty comment example
            Timestamp.valueOf("2024-01-20 09:15:00")
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
        private final String courseId;
        private final int rating;
        private final String comment;
        private final Timestamp timestamp;
        
        public Review(String userId, String courseId, int rating, 
                     String comment, Timestamp timestamp) {
            this.userId = userId;
            this.courseId = courseId;
            this.rating = rating;
            this.comment = comment;
            this.timestamp = timestamp;
        }
        
        public String getUserId() { return userId; }
        public String getCourseId() { return courseId; }
        public int getRating() { return rating; }
        public String getComment() { return comment; }
        public Timestamp getTimestamp() { return timestamp; }
    }
}