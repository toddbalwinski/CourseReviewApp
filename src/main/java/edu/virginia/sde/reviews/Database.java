package edu.virginia.sde.reviews;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private final String sqliteFilename;
    private Connection connection;

    public Database(String sqliteFilename) {
        this.sqliteFilename = sqliteFilename;
    }

    /**
     * Connect to the SQLite Database. Enables foreign key enforcement and disables auto-commit.
     *
     * @throws SQLException
     */
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            throw new IllegalStateException("The connection is already opened");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFilename);
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        connection.setAutoCommit(false);
    }

    /**
     * Commit all changes since the connection was opened or since the last commit/rollback.
     *
     * @throws SQLException
     */
    public void commit() throws SQLException {
        connection.commit();
    }

    /**
     * Rollback to the last commit or when the connection was opened.
     *
     * @throws SQLException
     */
    public void rollback() throws SQLException {
        connection.rollback();
    }

    /**
     * Ends the connection to the database.
     *
     * @throws SQLException
     */
    public void disconnect() throws SQLException {
        connection.close();
    }

    /**
     * Creates the three database tables: Users, Courses, and Reviews, with the appropriate constraints
     * including foreign keys, if they do not exist already.
     *
     * @throws SQLException
     */
    public void createTables() throws SQLException {
        if (connection.isClosed())
            throw new IllegalStateException("Connection is already closed");

        String createUsersTable = "CREATE TABLE IF NOT EXISTS Users (" +
                "username TEXT PRIMARY KEY," +
                "password TEXT NOT NULL" +
                ") STRICT;";

        String createCoursesTable = "CREATE TABLE IF NOT EXISTS Courses (" +
                "courseID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "courseMnemonic TEXT NOT NULL," +
                "courseNumber INTEGER NOT NULL" +
                ") STRICT;";

                String createReviewsTable = "CREATE TABLE IF NOT EXISTS Reviews (" +
    "reviewID INTEGER PRIMARY KEY AUTOINCREMENT," +
    "courseID INTEGER," +
    "authorUsername TEXT," +
    "rating REAL NOT NULL CHECK (rating BETWEEN 1 AND 5)," +
    "comment TEXT," +
    "timestamp TEXT NOT NULL," +
    "FOREIGN KEY(courseID) REFERENCES Courses(courseID) ON DELETE CASCADE," +
    "FOREIGN KEY(authorUsername) REFERENCES Users(username) ON DELETE CASCADE," +
    "UNIQUE(courseID, authorUsername)" +
    ") STRICT;";

        connection.prepareStatement(createUsersTable).execute();
        connection.prepareStatement(createCoursesTable).execute();
        connection.prepareStatement(createReviewsTable).execute();
    }

    /**
     * removes data from the tables and leaves the tables empty
     */
    public void clearTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM Reviews");
            statement.executeUpdate("DELETE FROM Courses");
            statement.executeUpdate("DELETE FROM Users");
        } catch (SQLException e) {
            rollback();
            throw e;
        }
    }

    //initialize database
    /* MOVED OUT OF Database.java to solve concurrency issue
    public void initializeDatabase() throws SQLException {
        connect();
        createTables();
    }
    */

    /*
     * following methods are for login screen
     */

    //validates users upon login
    public boolean validateUser(String username, String password) throws SQLException {
        String query = "SELECT 1 FROM Users WHERE username = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } 
        catch (SQLIntegrityConstraintViolationException e) {
            //if username already exists
            return false;
        } 
        catch (SQLException e) {
            rollback();
            throw e;
        }
    }    

    //creates users
    public boolean createUser(String username, String password) throws SQLException {
        String query = "INSERT INTO Users(username, password) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
            commit();
            return true;
        } 
        catch (SQLIntegrityConstraintViolationException e) {
            //if username already exists
            return false;
        }
    }
    
    /*
     * following methods are for course search screen
     */

     //searches for courses dynamically (even if some fields are blank)
    public List<Course> searchCourses(String subject, String number, String title) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT * FROM Courses WHERE 1=1");
        List<Object> params = new ArrayList<>();
    
        if (!subject.isEmpty()) {
            query.append(" AND LOWER(courseMnemonic) = LOWER(?)");
            params.add(subject);
        }
        if (!number.isEmpty()) {
            query.append(" AND courseNumber = ?");
            params.add(Integer.parseInt(number));
        }
        if (!title.isEmpty()) {
            query.append(" AND LOWER(title) LIKE LOWER(?)");
            params.add("%" + title + "%");
        }
    
        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
    
            ResultSet rs = stmt.executeQuery();
            List<Course> courses = new ArrayList<>();
            while (rs.next()) {
                courses.add(new Course(
                    rs.getInt("courseID"),
                    rs.getString("title"),
                    rs.getString("courseMnemonic"),
                    rs.getInt("courseNumber")
                ));
            }
            return courses;
        }
    }
    
    

    //add new course
    public boolean addCourse(String subject, int number, String title) throws SQLException {
        String checkQuery = "SELECT 1 FROM Courses WHERE courseMnemonic = ? AND courseNumber = ? AND title = ?";
        String insertQuery = "INSERT INTO Courses (courseMnemonic, courseNumber, title) VALUES (?, ?, ?)";
        try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
             PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
            checkStatement.setString(1, subject);
            checkStatement.setInt(2, number);
            checkStatement.setString(3, title);
            ResultSet rs = checkStatement.executeQuery();
            if (!rs.next()) {
                insertStatement.setString(1, subject);
                insertStatement.setInt(2, number);
                insertStatement.setString(3, title);
                insertStatement.executeUpdate();
                commit();
                return true;
            } else {
                return false; 
            } 
        } catch (SQLException e) {
            rollback();
            throw e;
        }
    }

    //retrievs a list of all courses
    public List<Course> getAllCourses() throws SQLException {
        String query = "SELECT courseID, title, courseMnemonic, courseNumber FROM Courses";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet rs = statement.executeQuery();
            List<Course> courses = new ArrayList<>();
            while (rs.next()) {
                courses.add(new Course(
                    rs.getInt("courseID"),
                    rs.getString("title"),
                    rs.getString("courseMnemonic"),
                    rs.getInt("courseNumber")
                ));
            }
            return courses;
        }
    }    
    
    //METHODS UNDER FOR REVIEWS PORTION OF PROJECT
    
    private String getCurrentESTTime() {
    return LocalDateTime.now(ZoneId.of("America/New_York"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
}

    public void addReviews(List<Review> reviews) throws SQLException {
        String upsertReview = "REPLACE INTO Reviews(courseID, authorUsername, rating, comment, timestamp) " +
                             "VALUES (?, ?, ?, ?, ?)";
    
        try (PreparedStatement statement = connection.prepareStatement(upsertReview)) {
            String currentESTTime = getCurrentESTTime();
            for (Review review : reviews) {
                statement.setInt(1, review.getCourseID());
                statement.setString(2, review.getAuthorUsername());
                statement.setDouble(3, review.getRating());
                statement.setString(4, review.getComment());
                statement.setString(5, currentESTTime);
                statement.executeUpdate();
            }
            commit();
        } catch (SQLException e) {
            System.err.println("Error adding reviews: " + e.getMessage());
            e.printStackTrace();
            rollback();
            throw e;
        }
    }

    public void deleteReview(int courseID, String authorUsername) throws SQLException {
        String deleteSQL = "DELETE FROM Reviews WHERE courseID = ? AND authorUsername = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(deleteSQL)) {
            statement.setInt(1, courseID);
            statement.setString(2, authorUsername);
            statement.executeUpdate();
            commit();
        } catch (SQLException e) {
            rollback();
            throw e;
        }
    }

    public List<Review> getReviewsByCourse(Course course) throws SQLException {
        String query = "SELECT * FROM Reviews WHERE courseID = ?";  
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, course.getCourseID());
            ResultSet rs = statement.executeQuery();
    
            List<Review> reviews = new ArrayList<>();
            while (rs.next()) {
                Review review = new Review(
                    rs.getInt("reviewID"),
                    rs.getInt("courseID"),
                    rs.getString("authorUsername"),
                    rs.getDouble("rating"),
                    rs.getString("comment"),
                    rs.getString("timestamp")
                );
                reviews.add(review);
            }
            return reviews;
        }
    }

    public List<Review> getReviewsByUser(User user) throws SQLException {
        String query = "SELECT reviewID, courseID, authorUsername, rating, comment, timestamp " +
                "FROM Reviews WHERE authorUsername = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, user.getUsername());
            ResultSet rs = statement.executeQuery();

            List<Review> reviews = new ArrayList<>();
            while (rs.next()) {
                int reviewID = rs.getInt("reviewID");
                int courseID = rs.getInt("courseID");
                String authorUsername = rs.getString("authorUsername");
                double rating = rs.getDouble("rating");
                String comment = rs.getString("comment");
                String timestamp = rs.getString("timestamp");

                reviews.add(new Review(reviewID, courseID, authorUsername, rating, comment, timestamp));
            }
            return reviews;
        }
    }

}