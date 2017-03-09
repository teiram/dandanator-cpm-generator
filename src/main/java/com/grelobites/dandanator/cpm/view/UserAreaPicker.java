package com.grelobites.dandanator.cpm.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class UserAreaPicker {
    private static final int MIN_USER_AREA = 0;
    private static final int MAX_USER_AREA = 15;
    private final IntegerProperty userArea;

    private final BooleanProperty disable;

    private final Button decreaseButton;
    private final Button increaseButton;
    private final Label userAreaLabel;

    public UserAreaPicker(Button decreaseButton, Button increaseButton, Label userAreaLabel) {
        this.userArea = new SimpleIntegerProperty(0);
        this.disable = new SimpleBooleanProperty(false);

        this.decreaseButton = decreaseButton;
        this.increaseButton = increaseButton;
        this.userAreaLabel = userAreaLabel;

        this.decreaseButton.disableProperty().bind(disable.or(userArea.isEqualTo(MIN_USER_AREA)));
        this.increaseButton.disableProperty().bind(disable.or(userArea.isEqualTo(MAX_USER_AREA)));
        this.userAreaLabel.disableProperty().bind(disable);

        this.increaseButton.setOnAction(e -> {
            if (userArea.get() < MAX_USER_AREA) {
                userArea.set(userArea.get() + 1);
            }
        });
        this.decreaseButton.setOnAction(e -> {
            if (userArea.get() > MIN_USER_AREA) {
                userArea.set(userArea.get() + 1);
            }
        });
        this.userArea.addListener((observable, oldValue, newValue) -> {
            this.userAreaLabel.setText(String.format("%02d", userArea.get()));
        });
    }

    public boolean isDisable() {
        return disable.get();
    }

    public BooleanProperty disableProperty() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable.set(disable);
    }

    public int getUserArea() {
        return userArea.get();
    }

    public IntegerProperty userAreaProperty() {
        return userArea;
    }

    public void setUserArea(int userArea) {
        this.userArea.set(userArea);
    }
}
