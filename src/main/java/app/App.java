package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.util.ArrayList;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static ArrayList<Media> playWaitingList = new ArrayList<>();
    private static MediaPlayer mediaPlayer;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setScene(scene);

        App.playWaitingList.add(new Media(
                new File("f:/Users/Yudai/Music/My music/Aqours/New Romantic Sailors/01 New Romantic Sailors.mp3")
                        .toURI().toString()));

        App.mediaPlayer = new MediaPlayer(App.playWaitingList.get(0));

        App.mediaPlayer.setVolume(1);

        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    static void playMusic() throws IOException {
        App.mediaPlayer.play();
    }

    static void stopMusic() throws IOException {
        App.mediaPlayer.stop();
    }

    public static void main(String[] args) {
        launch();
    }

}