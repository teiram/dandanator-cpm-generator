package com.grelobites.dandanator.cpm;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Preferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(Preferences.class);

    private static final String BOOTIMAGEPATH_PROPERTY = "bootImagePath";
    private static final String EMSBINARYPATH_PROPERTY = "emsBinaryPath";
    byte[] bootImage;
    byte[] emsBinary;

    private StringProperty bootImagePath;
    private StringProperty emsBinaryPath;

    private BooleanProperty validPreferences;

    private static Preferences INSTANCE;

    private Preferences() {
        this.bootImagePath = new SimpleStringProperty();
        this.emsBinaryPath = new SimpleStringProperty();
        this.validPreferences = new SimpleBooleanProperty(false);
        this.validPreferences.bind(emsBinaryPath.isNotEmpty());

        this.bootImagePath.addListener((observable, oldValue, newValue) -> {
            persistConfigurationValue(BOOTIMAGEPATH_PROPERTY, newValue);

        });
        this.emsBinaryPath.addListener((observable, oldValue, newValue) -> {
            persistConfigurationValue(EMSBINARYPATH_PROPERTY, newValue);
        });
    }

    public static Preferences getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    synchronized private static Preferences newInstance() {
        return setFromPreferences(new Preferences());
    }

    public byte[] getBootImage() throws IOException {
        if (bootImage == null) {
            if (getBootImagePath() != null) {
                bootImage = Files.readAllBytes(Paths.get(getBootImagePath()));
            } else {
                bootImage = Constants.getDefaultBootScreen();
            }
        }
        return bootImage;
    }

    public void setBootImage(byte[] bootImage) {
        this.bootImage = bootImage;
    }

    public byte[] getEmsBinary() throws IOException {
        if (emsBinary == null) {
            emsBinary = Files.readAllBytes(Paths.get(getEmsBinaryPath()));
        }
        return emsBinary;
    }

    public void setEmsBinary(byte[] emsBinary) {
        this.emsBinary = emsBinary;
    }

    public String getBootImagePath() {
        return bootImagePath.get();
    }

    public StringProperty bootImagePathProperty() {
        return bootImagePath;
    }

    public void setBootImagePath(String bootImagePath) {
        this.bootImagePath.set(bootImagePath);
        bootImage = null;
    }

    public String getEmsBinaryPath() {
        return emsBinaryPath.get();
    }

    public StringProperty emsBinaryPathProperty() {
        return emsBinaryPath;
    }

    public void setEmsBinaryPath(String emsBinaryPath) {
        this.emsBinaryPath.set(emsBinaryPath);
    }

    public boolean isValidPreferences() {
        return validPreferences.get();
    }

    public BooleanProperty validPreferencesProperty() {
        return validPreferences;
    }

    public static java.util.prefs.Preferences getApplicationPreferences() {
        return java.util.prefs.Preferences.userNodeForPackage(Preferences.class);
    }

    private static Preferences setFromPreferences(Preferences preferences) {
        java.util.prefs.Preferences p = getApplicationPreferences();
        preferences.setBootImagePath(p.get(BOOTIMAGEPATH_PROPERTY, null));
        preferences.setEmsBinaryPath(p.get(EMSBINARYPATH_PROPERTY, null));
        return preferences;
    }

    public static void persistConfigurationValue(String key, String value) {
        LOGGER.debug("persistConfigurationValue " + key + ", " + value);
        java.util.prefs.Preferences p = getApplicationPreferences();
        if (value != null) {
            p.put(key, value);
        } else {
            p.remove(key);
        }
    }

}
