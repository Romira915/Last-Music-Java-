package app;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import java.io.File;

public class SecondaryController {

    @FXML
    public VBox observationFolderListVBox;

    @FXML
    private void onFolderAddButtonPressed() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("ライブラリに追加するフォルダ");
        File selectedDirectory = directoryChooser.showDialog(App.observationFolderStage);

        if (selectedDirectory != null) {
            App.addLibraryFolderlist(selectedDirectory.toString());
            App.observationFolderStage.sizeToScene();
        }
    }

    @FXML
    private void onOKButtonClicked() {
        App.observationFolderStage.close();
        App.onSecondaryStageClose();
    }

}