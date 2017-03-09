package com.grelobites.dandanator.cpm;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Preferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(Preferences.class);

    byte[] bootImage;
    byte[] emsImage;


    private StringProperty bootImagePath;
    private StringProperty emsImagePath;

    private static Preferences INSTANCE;

    private Preferences() {
        this.bootImagePath = new SimpleStringProperty();
        this.emsImagePath = new SimpleStringProperty();
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

    public byte[] getEmsImage() {
        return emsImage;
    }

    public void setEmsImage(byte[] emsImage) {
        this.emsImage = emsImage;
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

    public String getEmsImagePath() {
        return emsImagePath.get();
    }

    public StringProperty emsImagePathProperty() {
        return emsImagePath;
    }

    public void setEmsImagePath(String emsImagePath) {
        this.emsImagePath.set(emsImagePath);
    }

}
