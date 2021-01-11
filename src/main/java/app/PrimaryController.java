package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javafx.fxml.FXML;

public class PrimaryController {
    private String getUserMusicDirectory() {
        String ret = "";

        try {
            String cmdline = "reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders\" /v \"My Music\"";

            String line = "";

            Process pc = Runtime.getRuntime().exec(cmdline);
            BufferedReader br = new BufferedReader(new InputStreamReader(pc.getInputStream()));

            while ((line = br.readLine()) != null) {
                if (line.indexOf("My Music") != -1) {
                    ret = line.substring(line.indexOf(":") - 1, line.length());
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

    @FXML
    private void onPlayMusic() throws IOException {
        App.playMusic();
    }

    @FXML
    private void onStopMusic() throws IOException {
        App.stopMusic();
    }
}
