package edu.virginia.sde.reviews;

public class Course {

    private final int courseID, number;
    private final String title, mnemonic;

    public Course(int courseID, String title, String mnemonic, int number) {
        this.courseID = courseID;
        this.title = title;
        this.mnemonic = mnemonic;
        this.number = number;
    }

    public int getCourseID() {
        return courseID;
    }

    public String getTitle() {
        return title;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public int getNumber() {
        return number;
    }
}