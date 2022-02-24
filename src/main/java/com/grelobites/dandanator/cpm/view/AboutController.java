package com.grelobites.dandanator.cpm.view;

import com.grelobites.dandanator.cpm.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AboutController {

    @FXML
    private ImageView logo;

    @FXML
    private Label versionLabel;

    @FXML
    private void initialize()  {
        logo.setImage(new Image(AboutController.class.getResourceAsStream("/app-icon.png")));
        versionLabel.setText(String.format("Version %s", Constants.currentVersion()));
    }
}
