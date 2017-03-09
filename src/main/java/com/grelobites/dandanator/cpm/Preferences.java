package com.grelobites.dandanator.cpm;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Preferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(Preferences.class);

    byte[] bootImage;
    byte[] emsBinary;


    private StringProperty bootImagePath;
    private StringProperty emsBinaryPath;

    private static Preferences INSTANCE;

    private Preferences() {
        this.bootImagePath = new SimpleStringProperty();
        this.emsBinaryPath = new SimpleStringProperty();
    }

    public static Preferences getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    synchronized private static Preferences newInstance() {
        return new Preferences();
    }

    public byte[] getBootImage() throws IOException {
        if (bootImage == null) {
            bootImage = Constants.getDefaultBootScreen();
        }
        return bootImage;
    }

    public void setBootImage(byte[] bootImage) {
        this.bootImage = bootImage;
    }

    public byte[] getEmsBinary() throws IOException {
        if (emsBinary == null) {
            emsBinary = Constants.getDefaultEmsBinary();
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

}
