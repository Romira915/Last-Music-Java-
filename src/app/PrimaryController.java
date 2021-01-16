package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.File;

import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.Media;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import jdk.jfr.Frequency;

public class PrimaryController {
    @FXML
    private Button playButton;
    @FXML
    private Button pauseButton;
    @FXML
    private Slider seekBar;

    @FXML
    private Button volumeButton;
    @FXML
    private Button muteButton;
    @FXML
    private Slider volumeSlider;

    @FXML
    private Button allLoopButton;
    @FXML
    private Button singleLoopButton;
    @FXML
    private Button shuffleButton;

    @FXML
    public GridPane musicListPane;
    @FXML
    public GridPane playWaitingListPane;

    @FXML
    public Label musicPlayerTitleLabel;
    @FXML
    public Tab musicListTab;

    @FXML
    public ImageView albumArt;

    public MediaPlayerStatusPlayChangedOn mediaPlayerStatusPlayChangedOn = new MediaPlayerStatusPlayChangedOn();
    public MediaPlayerStatusStopChangedOn mediaPlayerStatusStopChangedOn = new MediaPlayerStatusStopChangedOn();
    public MediaPlayerStatusPauseChangedOn mediaPlayerStatusPauseChangedOn = new MediaPlayerStatusPauseChangedOn();

    private class MediaPlayerStatusPlayChangedOn implements Runnable {
        @Override
        public void run() {
            PrimaryController.this.playButton.setVisible(false);
            PrimaryController.this.pauseButton.setVisible(true);
        }
    }

    private class MediaPlayerStatusStopChangedOn implements Runnable {
        @Override
        public void run() {
            PrimaryController.this.playButton.setVisible(true);
            PrimaryController.this.pauseButton.setVisible(false);
        }
    }

    private class MediaPlayerStatusPauseChangedOn implements Runnable {
        @Override
        public void run() {
            PrimaryController.this.playButton.setVisible(true);
            PrimaryController.this.pauseButton.setVisible(false);
        }
    }

    private static void visualizeAll(Node nodes[]) {
        for (Node node : nodes) {
            if (node != null) {
                node.setVisible(true);
            }
        }
    }

    public void visualizeSpeakerButton(boolean isMute) {
        if (isMute) {
            this.volumeButton.setVisible(false);
            this.muteButton.setVisible(true);
        } else {
            this.volumeButton.setVisible(true);
            this.muteButton.setVisible(false);
        }
    }

    public void setValueSeekBar(double value) {
        this.seekBar.setValue(value);
    }

    public double getVolumeRate() {
        return this.volumeSlider.getValue();
    }

    private void activateAllLoop() {
        this.allLoopButton.setVisible(true);
        this.singleLoopButton.setVisible(false);
        this.allLoopButton.setOpacity(1);

        App.playMode = App.PlayMode.AllLoop;
    }

    private void activateSingleLoop() {
        this.allLoopButton.setVisible(false);
        this.singleLoopButton.setVisible(true);

        App.playMode = App.PlayMode.SingleLoop;
    }

    private void deactivateLoop() {
        this.allLoopButton.setVisible(true);
        this.singleLoopButton.setVisible(false);
        this.allLoopButton.setOpacity(0.5);

        App.playMode = App.PlayMode.NotLoop;
    }

    private void activateShuffle() {
        this.shuffleButton.setOpacity(1);

        App.isShuffleMode = true;
    }

    private void deactivateShuffle() {
        this.shuffleButton.setOpacity(0.5);

        App.isShuffleMode = false;
    }

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

    @FXML
    private void onPlayButtonPressed(ActionEvent event) {
        App.playMusic();
    }

    @FXML
    private void onPauseButtonPressed(ActionEvent event) {
        App.pauseMusic();
    }

    @FXML
    private void onStopButtonPressed() {
        App.stopMusic();
    }

    @FXML
    private void onMusicSeekBarChanged(MouseEvent event) {
        App.isSeekBarScrolling = true;

        Slider seekBar = (Slider) event.getSource();
        double rate = seekBar.getValue();

        App.setSeekInRate(rate);
    }

    @FXML
    private void onMusicSeekBarReleased() {
        App.isSeekBarScrolling = false;
    }

    @FXML
    private void onSpeakerButtonPressed(ActionEvent event) {
        App.toggleMute();
    }

    @FXML
    private void onVolumeBarChanged(MouseEvent event) {
        Slider volumeBar = (Slider) event.getSource();
        double rate = volumeBar.getValue();

        App.setVolume(rate);
    }

    @FXML
    private void onPressedObservationFolderMenu() {
        App.observationFolderStage.show();
    }

    @FXML
    private void onSearchFieldInput(ActionEvent event) {
        TextField target = (TextField) event.getTarget();
        App.searchWord = target.getText();
        // App.UpdateMusicList updateMusicList = new App.UpdateMusicList();
        // updateMusicList.run();

        Thread thread = new Thread(new App.UpdateMusicList());
        thread.start();
        App.threads.add(thread);
    }

    @FXML
    private void onPressedAboutMenu(ActionEvent event) {
        App.aboutStage.show();
    }

    @FXML
    private void onSkipNextButtonPressed(ActionEvent event) {
        boolean isPlaying = App.getMediaStatus() == Status.PLAYING;

        if (!App.toNextMedia()) {
            App.toFirstMedia();
        }

        if (isPlaying) {
            App.playMusic();
        }
    }

    @FXML
    private void onSkipPreviousButtonPressed(ActionEvent event) {
        boolean isPlaying = App.getMediaStatus() == Status.PLAYING;

        if (App.getMediaCurrentTime().toSeconds() < 2.0) {
            if (!App.toPreviousMedia()) {
                App.toLastMedia();
            }
        } else {
            App.setSeekInRate(0);
        }
        if (isPlaying) {
            App.playMusic();
        }
    }

    @FXML
    private void onAllLoopButtonPressed(ActionEvent event) {
        Button target = (Button) event.getTarget();

        if (target.getOpacity() <= 0.7) {
            this.activateAllLoop();
        } else {
            this.activateSingleLoop();
        }
    }

    @FXML
    private void onSingleLoopButtonPressed(ActionEvent event) {
        this.deactivateLoop();
    }

    @FXML
    private void onShuffleButtonPressed(ActionEvent event) {
        Button target = (Button) event.getTarget();

        if (target.getOpacity() <= 0.7) {
            this.activateShuffle();
        } else {
            this.deactivateShuffle();
        }
    }
}
