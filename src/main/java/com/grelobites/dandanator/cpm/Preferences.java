package com.grelobites.dandanator.cpm;

import com.grelobites.dandanator.cpm.model.HandlerType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    private static final String HANDLERTYPE_PROPERTY = "handlerType";
    byte[] bootImage;

    final private StringProperty bootImagePath;

    final private BooleanProperty validPreferences;
    final private ObjectProperty<HandlerType> handlerType;

    private static Preferences INSTANCE;

    private Preferences() {
        this.bootImagePath = new SimpleStringProperty();
        this.validPreferences = new SimpleBooleanProperty(true);
        this.handlerType = new SimpleObjectProperty<>(HandlerType.SPECTRUM);
        this.bootImagePath.addListener((observable, oldValue, newValue) ->
                persistConfigurationValue(BOOTIMAGEPATH_PROPERTY, newValue));
        this.handlerType.addListener((observable, oldValue, newValue) ->
            persistConfigurationValue(HANDLERTYPE_PROPERTY, newValue.name()));
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

    public HandlerType getHandlerType() {
        return handlerType.get();
    }

    public ObjectProperty<HandlerType> handlerTypeProperty() {
        return handlerType;
    }

    public void setHandlerType(HandlerType handlerType) {
        this.handlerType.set(handlerType);
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
        preferences.setHandlerType(HandlerType.valueOf(p.get(HANDLERTYPE_PROPERTY, HandlerType.SPECTRUM.name())));
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
