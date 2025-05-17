package edu.virginia.sde.reviews;

import java.io.FileInputStream;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class CourseReviewsApplication extends Application {

    private static Database database;
    private static String databaseError = null;

    public static Database getDatabase() {
        return database;
    }

    public static String getDatabaseError() {
        return databaseError;
    }

    public static void main(String[] args) {
        try {
            database = new Database("course_reviews.db");
            database.connect();
            database.createTables();
        } 
        catch (Exception e) {
            databaseError = "Failed to initialize the database: " + e.getMessage();
            e.printStackTrace();
        }

        launch(args);
    }

    public void start(Stage stage) throws Exception {
        //loading comfortaa font
        Font.loadFont(getClass().getResourceAsStream("/edu/virginia/sde/reviews/fonts/Comfortaa-VariableFont_wght.ttf"), 14);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login-screen.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);

        scene.getStylesheets().add(getClass().getResource("styles/login-screen.css").toExternalForm());

        stage.setTitle("Course Reviews - Login");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (database != null) {
            database.disconnect();
        }
    }
}