/*

MIT License

Copyright (c) 2021 Tanaka Yudai

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.event.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.media.MediaException;
import javafx.scene.Node;

/**
 * JavaFX App
 */
public class App extends Application {
    public static boolean isSeekBarScrolling = false;
    public static Stage observationFolderStage = new Stage();
    public static Stage aboutStage = new Stage();
    public static String searchWord = "";

    private static final String DATABASE_PATH = "jdbc:sqlite:";
    private static final String LIBRARY_FOLDER_LIST_PATH = "appdata/libraryFolderlist";
    private static final String EXTENSIONS[] = { ".mp3", ".mp4", ".m4a", ".wav", ".aif", ".aiff" };

    private static Scene scene;
    private static ArrayList<String> playWaitingList = new ArrayList<>();
    private static String musicSetInNowPlayer;
    private static MediaPlayer mediaPlayer;
    private static LinkedHashSet<String> libraryFolderlist = new LinkedHashSet<>();
    private static LinkedHashSet<String> musicPathListWithinLibrary = new LinkedHashSet<>();
    private static PrimaryController primaryController;
    private static SecondaryController secondaryController;
    private static AboutController aboutController;
    private static MusicSelectEventHandler musicSelectEventHandler;
    private static OnEndOfMediaEventHandler onEndOfMediaEventHandler = new OnEndOfMediaEventHandler();
    private static ArrayList<Thread> threads = new ArrayList<>();
    private static boolean endThread = false;
    private static ContextMenu musicSelectContextMenu = new ContextMenu();
    private static String musicSelectedBySecondaryMouseClick = "";
    private static MusicButtonContextMenuEventHandler musicButtonContextMenuEventHandler = new MusicButtonContextMenuEventHandler();

    private static class LibraryFolderScanThread implements Runnable {
        public void run() {
            App.musicPathListWithinLibrary.clear();
            for (String libraryfolderString : App.libraryFolderlist) {
                App.fileSearch(libraryfolderString, App.EXTENSIONS);

                if (endThread) {
                    return;
                }
            }
            Platform.runLater(new UpdateMusicList());
        }
    }

    public static class UpdateMusicList implements Runnable {
        private static final String USE_STYLE = "-fx-background-color: rgb(100, 100, 100);\n-fx-text-fill: white;\n";

        public void run() {
            var musicListPaneList = App.primaryController.musicListPane;
            musicListPaneList.getChildren().clear();
            for (String musicPath : App.musicPathListWithinLibrary) {
                try {
                    File musicFile = new File(musicPath);
                    String fileName = musicFile.getName();
                    String fileNameNoExtension = App.filenameNoExtension(fileName);
                    String fileExtension = App.fileExtension(fileName);

                    if (App.searchWord != null && App.searchWord != ""
                            && !fileNameNoExtension.contains(App.searchWord)) {
                        continue;
                    }

                    Button musicButton = App.createMusicButton(musicPath);

                    Label fileExtensionLabel = new Label(fileExtension);
                    fileExtensionLabel.setStyle(UpdateMusicList.USE_STYLE);

                    musicListPaneList.addColumn(0, musicButton);
                    musicListPaneList.addColumn(1, fileExtensionLabel);
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
        }
    }

    private class MusicSelectEventHandler implements EventHandler<MouseEvent> {
        public void handle(MouseEvent event) {
            Button target = (Button) event.getSource();

            if (event.getEventType().equals(MouseEvent.MOUSE_CLICKED) && event.getButton().equals(MouseButton.PRIMARY)
                    && event.getClickCount() == 2) {

                String musicPath = target.getId();
                if (App.primaryController.musicListTab.isSelected()) {
                    App.clearPlayWaitingList();
                }
                App.addPlayWaitingList(musicPath);

                App.setMediaPlayer(musicPath);
                App.playMusic();
            }

            // if (event.getEventType().equals(MouseEvent.MOUSE_CLICKED)
            // && event.getButton().equals(MouseButton.SECONDARY)) {
            // App.musicSelectContextMenu.show(target, event.getScreenX(),
            // event.getScreenY());
            // App.musicSelectedBySecondaryMouseClick = target.getId();
            // }
        }
    }

    private static class MusicButtonContextMenuEventHandler implements EventHandler<ContextMenuEvent> {
        @Override
        public void handle(ContextMenuEvent event) {
            if (event.getEventType().equals(ContextMenuEvent.CONTEXT_MENU_REQUESTED)) {
                Button target = (Button) event.getSource();
                App.musicSelectContextMenu.show(target, event.getScreenX(), event.getScreenY());
                App.musicSelectedBySecondaryMouseClick = target.getId();
            }
        }
    }

    private static class OnEndOfMediaEventHandler implements Runnable {
        @Override
        public void run() {
            int nextIndex = App.playWaitingList.lastIndexOf(App.musicSetInNowPlayer) + 1;

            if (nextIndex < App.playWaitingList.size()) {
                App.setMediaPlayer(App.playWaitingList.get(nextIndex));
                App.playMusic();
            } else {
                // Thread thread = new
                // Thread(App.primaryController.mediaPlayerStatusStopChangedOn);
                // thread.start();

                App.stopMusic();
                App.setSeekInRate(0.0);
            }
        }
    }

    private static class SynchronizeSeekBarThread implements Runnable {
        @Override
        public void run() {
            while (!App.endThread) {
                if (!App.isSeekBarScrolling) {
                    if (App.primaryController != null) {
                        App.primaryController.setValueSeekBar(App.getSeekInRate());
                    }
                }

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }
            }
        }
    }

    private static class SecondaryStageCloseRequestedOnEventHandler implements EventHandler<WindowEvent> {
        @Override
        public void handle(WindowEvent event) {
            App.onSecondaryStageClose();
        }
    }

    private static enum Controller {
        Primary, Secondary, About
    }

    @Override
    public void start(Stage stage) throws IOException {
        App.musicSelectEventHandler = new App.MusicSelectEventHandler();

        scene = new Scene(loadFXML("primary", Controller.Primary));
        stage.setScene(scene);
        stage.setTitle("Last Music - ミュージックプレイヤー");
        // stage.setWidth(1280);
        stage.setHeight(720);
        App.observationFolderStage.setTitle("ライブラリ フォルダ");
        App.observationFolderStage.setScene(new Scene(loadFXML("secondary", Controller.Secondary)));
        // App.observationFolderStage.setOnCloseRequest(e -> e.consume());
        App.observationFolderStage.initOwner(stage);
        App.observationFolderStage.initModality(Modality.WINDOW_MODAL);
        App.observationFolderStage.setResizable(false);
        App.observationFolderStage.setOnCloseRequest(new App.SecondaryStageCloseRequestedOnEventHandler());
        App.aboutStage.setTitle("Last Musicについて");
        App.aboutStage.setScene(new Scene(loadFXML("about", Controller.About)));
        App.aboutStage.initOwner(stage);
        App.aboutStage.setResizable(false);

        File libraryFolderlistFile = new File(App.LIBRARY_FOLDER_LIST_PATH);
        if (libraryFolderlistFile.exists()) {
            App.joinFolderToLibraryfromAppDataFile(libraryFolderlistFile);
        } else {
            String specialDirectory = App.getUserMusicDirectory();
            if (specialDirectory.length() != 0) {
                App.libraryFolderlist.add(specialDirectory);
                App.joinLibraryFolderToFile(specialDirectory, libraryFolderlistFile);
            }
        }

        MenuItem addPlayWaitingListMenuItem = new MenuItem("再生待ちリストに追加");
        addPlayWaitingListMenuItem.addEventHandler(ActionEvent.ACTION,
                e -> App.addPlayWaitingList(App.musicSelectedBySecondaryMouseClick));
        App.musicSelectContextMenu.getItems().add(addPlayWaitingListMenuItem);

        Thread lFolderScanThread = new Thread(new App.LibraryFolderScanThread());
        lFolderScanThread.start();

        Thread synchronizeSeekBarThread = new Thread(new App.SynchronizeSeekBarThread());
        synchronizeSeekBarThread.start();

        App.threads.add(lFolderScanThread);
        App.threads.add(synchronizeSeekBarThread);

        App.updateObservationFolderUI();

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        App.endThread = true;

        for (Thread thread : App.threads) {
            thread.join(1000);
        }
    }

    private static String filenameNoExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private static String fileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public static void fileSearch(String path, String extensions[]) {
        try {
            File dir = new File(path);
            File files[] = dir.listFiles();
            for (File file : files) {
                String fileName = file.getName();
                if (file.isDirectory()) {
                    fileSearch(path + "/" + fileName, extensions);
                } else {
                    for (String extension : extensions) {
                        if (fileName.endsWith(extension)) {
                            App.musicPathListWithinLibrary.add(path + "/" + fileName);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private static String getUserMusicDirectory() {
        String ret = "";

        try {
            String cmdline = "reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders\" /v \"My Music\"";

            String line = "";

            Process pc = Runtime.getRuntime().exec(cmdline);
            BufferedReader br = new BufferedReader(new InputStreamReader(pc.getInputStream()));

            while ((line = br.readLine()) != null) {
                if (line.indexOf("My Music") != -1) {
                    ret = line.substring(line.indexOf(":") - 1, line.length());
                    // ret = ret.replace("\\", "/");
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static void joinFolderToLibraryfromAppDataFile(File file) {
        try {
            if (file.isFile() && file.canRead()) {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    App.libraryFolderlist.add(line);
                }

                bufferedReader.close();
            }
        } catch (IOException e) {
            return;
        }
    }

    public static boolean joinLibraryFolderToFile(String libraryPath, File file) {
        try {
            if (file.createNewFile()) {
                FileWriter fileWriter = new FileWriter(file, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(libraryPath);
                bufferedWriter.newLine();

                bufferedWriter.close();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            // e.printStackTrace();
            return false;
        }
    }

    public static boolean exportLibraryFolderToFile(File file) {
        try {
            if (file.canWrite()) {
                FileWriter fileWriter = new FileWriter(file, false);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                for (String folderPath : App.libraryFolderlist) {
                    bufferedWriter.write(folderPath);
                    bufferedWriter.newLine();
                }

                bufferedWriter.close();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            // e.printStackTrace();
            return false;
        }
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml, Controller.Primary));
    }

    private static Parent loadFXML(String fxml, Controller controller) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Parent parent = fxmlLoader.load();

        switch (controller) {
            case Primary:
                App.primaryController = fxmlLoader.getController();
                break;
            case Secondary:
                App.secondaryController = fxmlLoader.getController();
                break;
            case About:
                App.aboutController = fxmlLoader.getController();
                break;
            default:
                break;
        }
        return parent;
    }

    private static Button createMusicButton(String musicPath) {
        File file = new File(musicPath);
        Button musicButton = new Button(App.filenameNoExtension(file.getName()));
        musicButton.setId(musicPath);
        musicButton.setStyle(UpdateMusicList.USE_STYLE);
        musicButton.addEventHandler(MouseEvent.ANY, (EventHandler) App.musicSelectEventHandler);
        musicButton.addEventHandler(ContextMenuEvent.ANY, App.musicButtonContextMenuEventHandler);

        return musicButton;
    }

    public static void playMusic() {
        if (App.mediaPlayer != null) {
            App.mediaPlayer.play();
        }
    }

    public static void pauseMusic() {
        if (App.mediaPlayer != null) {
            App.mediaPlayer.pause();
        }
    }

    public static void stopMusic() {
        if (App.mediaPlayer != null) {
            App.mediaPlayer.stop();
        }
    }

    public static void setSeekInRate(double rate) {
        if (App.mediaPlayer != null) {
            Duration mediaLength = App.mediaPlayer.getMedia().getDuration();
            App.mediaPlayer.seek(new Duration(rate * mediaLength.toMillis()));
        }
    }

    public static double getSeekInRate() {
        if (App.mediaPlayer != null) {
            return App.mediaPlayer.getCurrentTime().toMillis() / App.mediaPlayer.getMedia().getDuration().toMillis();
        }

        return 0.0;
    }

    public static void toggleMute() {
        if (App.mediaPlayer != null) {
            App.mediaPlayer.setMute(!App.mediaPlayer.isMute());
            App.primaryController.visualizeSpeakerButton(App.mediaPlayer.isMute());
        }
    }

    public static boolean isMute() {
        if (App.mediaPlayer != null) {
            return App.mediaPlayer.isMute();
        }

        return false;
    }

    public static void setVolume(double rate) {
        if (App.mediaPlayer != null) {
            App.mediaPlayer.setVolume(rate);
        }
    }

    public static boolean setMediaPlayer(String path) {
        File file = new File(path);

        if (file.exists() && file.isFile() && file.canRead()) {
            try {
                if (App.mediaPlayer != null) {
                    App.mediaPlayer.dispose();
                }
                App.mediaPlayer = new MediaPlayer(new Media(file.toURI().toString()));
                App.mediaPlayer.setOnPlaying(App.primaryController.mediaPlayerStatusPlayChangedOn);
                App.mediaPlayer.setOnStopped(App.primaryController.mediaPlayerStatusStopChangedOn);
                App.mediaPlayer.setOnPaused(App.primaryController.mediaPlayerStatusPauseChangedOn);
                App.mediaPlayer.setOnEndOfMedia(App.onEndOfMediaEventHandler);
                App.mediaPlayer.setVolume(App.primaryController.getVolumeRate());
                App.primaryController.musicPlayerTitleLabel.setText(App.filenameNoExtension(file.getName()));
                App.primaryController.visualizeSpeakerButton(App.mediaPlayer.isMute());
                App.musicSetInNowPlayer = path;

                return true;
            } catch (UnsupportedOperationException e) {
                App.primaryController.musicPlayerTitleLabel.setText("このファイルはサポートされていません");
            } catch (MediaException e) {
                App.primaryController.musicPlayerTitleLabel.setText("このファイルはサポートされていません");
            } catch (Exception e) {
                App.primaryController.musicPlayerTitleLabel.setText(e.getMessage());
            }
        } else {
            App.primaryController.musicPlayerTitleLabel.setText("ファイルが存在しないか読み取ることができません");
        }

        return false;
    }

    private static void addPlayWaitingList(String musicPath) {
        if (!App.playWaitingList.contains(musicPath)) {
            App.playWaitingList.add(musicPath);

            Button musicButton = App.createMusicButton(musicPath);
            Label musicLabel = new Label(App.fileExtension(musicPath));
            musicLabel.setStyle(UpdateMusicList.USE_STYLE);

            App.primaryController.playWaitingListPane.addColumn(0, musicButton);
            App.primaryController.playWaitingListPane.addColumn(1, musicLabel);

            if (App.playWaitingList.size() == 1) {
                App.setMediaPlayer(musicPath);
            }
        }
    }

    private static void clearPlayWaitingList() {
        App.playWaitingList.clear();
        App.primaryController.playWaitingListPane.getChildren().clear();
    }

    private static void updateObservationFolderUI() {
        var folderListVBox = App.secondaryController.observationFolderListVBox.getChildren();
        folderListVBox.clear();

        for (String path : App.libraryFolderlist) {
            CheckBox folderCheckBox = new CheckBox(path);
            folderCheckBox.setId("darkmode");
            folderCheckBox.setSelected(true);

            folderListVBox.add(folderCheckBox);
        }
    }

    public static void addLibraryFolderlist(String path) {
        App.libraryFolderlist.add(path);
        App.updateObservationFolderUI();
    }

    public static void onSecondaryStageClose() {
        ObservableList<Node> UILibraryFolderList = App.secondaryController.observationFolderListVBox.getChildren();

        for (Node node : UILibraryFolderList) {
            CheckBox folderCheckBox = (CheckBox) node;
            if (folderCheckBox.isSelected()) {
                App.libraryFolderlist.add(folderCheckBox.getText());
            } else {
                App.libraryFolderlist.remove(folderCheckBox.getText());
            }
        }

        App.updateObservationFolderUI();
        Thread thread = new Thread(new App.LibraryFolderScanThread());
        thread.start();
        App.threads.add(thread);
        App.exportLibraryFolderToFile(new File(App.LIBRARY_FOLDER_LIST_PATH));
    }

    public static void main(String[] args) {
        launch();
    }
}