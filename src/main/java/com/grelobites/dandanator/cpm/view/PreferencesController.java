package com.grelobites.dandanator.cpm.view;

import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.Preferences;
import com.grelobites.dandanator.cpm.util.ImageUtil;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.view.util.DialogUtil;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class PreferencesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesController.class);

    private WritableImage bootImage;

    @FXML
    private ImageView bootImageView;

    @FXML
    private Button changeBootImageButton;

    @FXML
    private Label bootImagePath;

    @FXML
    private Button resetBootImageButton;

    @FXML
    private Button changeEmsBinaryPathButton;

    @FXML
    private Label emsBinaryPath;

    @FXML
    private Button resetEmsBinaryPathButton;


    private void initializeImages() throws IOException {
        bootImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(),
                new ByteArrayInputStream(Preferences.getInstance().getBootImage()));
    }


    private void recreateBootImage() throws IOException {
        LOGGER.debug("RecreateBootImage");
        ImageUtil.scrLoader(bootImage,
                new ByteArrayInputStream(Preferences.getInstance().getBootImage()));
    }

    private void updateBootImage(File bootImageFile) throws IOException {
        if (isReadableFile(bootImageFile) && bootImageFile.length() == Constants.SPECTRUM_FULLSCREEN_SIZE) {
            Preferences.getInstance().setBootImagePath(bootImageFile.getAbsolutePath());
            recreateBootImage();
        } else {
            throw new IllegalArgumentException("No valid boot image file provided");
        }
    }

    private boolean isEmsBinaryPathValid(File emsFile) {
        try {
            return isReadableFile(emsFile) && emsFile.length() <= Constants.MAX_EMS_FILE_SIZE;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateEmsBinaryPath(File emsFile) throws IOException {
        if (isEmsBinaryPathValid(emsFile)) {
            Preferences.getInstance().setEmsBinaryPath(emsFile.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("No valid EMS file provided");
        }
    }

    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private static void showGenericFileErrorAlert() {
        DialogUtil.buildErrorAlert(LocaleUtil.i18n("fileImportError"),
                LocaleUtil.i18n("fileImportErrorHeader"),
                LocaleUtil.i18n("fileImportErrorContent"))
                .showAndWait();
    }

    private void backgroundImageSetup() {
        bootImageView.setImage(bootImage);
        changeBootImageButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectNewBootImage"));
            final File bootImageFile = chooser.showOpenDialog(changeBootImageButton
                    .getScene().getWindow());
            if (bootImageFile != null) {
                try {
                    updateBootImage(bootImageFile);
                } catch (Exception e) {
                    LOGGER.error("Updating boot image from " + bootImageFile, e);
                    showGenericFileErrorAlert();
                }
            }
        });

        bootImagePath.textProperty().bind(Bindings.createStringBinding(() -> {
            Preferences preferences = Preferences.getInstance();
            if (preferences.getBootImagePath() == null) {
                return LocaleUtil.i18n("builtInValue");
            } else {
                return preferences.getBootImagePath();
            }
        }, Preferences.getInstance().bootImagePathProperty()));

        resetBootImageButton.setOnAction(event -> {
            try {
                Preferences.getInstance().setBootImagePath(null);
                recreateBootImage();
            } catch (Exception e) {
                LOGGER.error("Resetting boot Image", e);
            }
        });
        Preferences.getInstance().bootImagePathProperty().addListener(
                (observable, oldValue, newValue) -> {
                    try {
                        recreateBootImage();
                    } catch (IOException ioe) {
                        LOGGER.error("Updating boot image", ioe);
                    }
                });
    }

    private void emsBinarySetup() {
        Preferences preferences = Preferences.getInstance();

        if (preferences.getEmsBinaryPath() != null &&
                !isEmsBinaryPathValid(new File(preferences.getEmsBinaryPath()))) {
            DialogUtil.buildErrorAlert(LocaleUtil.i18n("preferencesError"),
                    LocaleUtil.i18n("emsFileProblem"),
                    String.format(LocaleUtil.i18n("emsFileNoLongerAvailable"),
                            preferences.getEmsBinaryPath()))
                    .showAndWait();
            preferences.setEmsBinaryPath(null);
        }

        changeEmsBinaryPathButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectEmsMessage"));
            final File emsFile = chooser.showOpenDialog(changeEmsBinaryPathButton.getScene().getWindow());
            if (emsFile != null) {
                try {
                    updateEmsBinaryPath(emsFile);
                } catch (Exception e) {
                    LOGGER.error("Updating EMS from " + emsFile, e);
                    showGenericFileErrorAlert();
                }
            }
        });
        resetEmsBinaryPathButton.setOnAction(event -> {
            try {
                preferences.setEmsBinaryPath(null);
            } catch (Exception e) {
                LOGGER.error("Resetting EMS Path", e);
            }
        });

        emsBinaryPath.textProperty().bind(Bindings.createStringBinding(() -> {
            if (preferences.getEmsBinaryPath() == null) {
                return LocaleUtil.i18n("notDefinedValue");
            } else {
                return preferences.getEmsBinaryPath();
            }
        }, Preferences.getInstance().emsBinaryPathProperty()));

    }

    @FXML
    private void initialize() throws IOException {
        initializeImages();

        backgroundImageSetup();

        emsBinarySetup();

    }
}
