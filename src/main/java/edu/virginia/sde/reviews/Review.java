package edu.virginia.sde.reviews;

public class Review {

    private final int reviewID, courseID;
    private final double rating;
    private final String authorUsername, comment, timestamp;

    public Review(int reviewID, int courseID, String authorUsername, double rating, String comment) {
        this.reviewID = reviewID;
        this.courseID = courseID;
        this.authorUsername = authorUsername;
        this.rating = rating;
        this.comment = comment;
        timestamp = "";
    }

    public Review(int reviewID, int courseID, String authorUsername, double rating, String comment, String timestamp) {
        this.reviewID = reviewID;
        this.courseID = courseID;
        this.authorUsername = authorUsername;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public int getReviewID() {
        return reviewID;
    }

    public int getCourseID() {
        return courseID;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public double getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getTimestamp() {
        return timestamp;
    }
}