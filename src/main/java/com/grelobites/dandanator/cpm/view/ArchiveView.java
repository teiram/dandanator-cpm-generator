package com.grelobites.dandanator.cpm.view;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.util.ArchiveFlags;
import com.grelobites.dandanator.cpm.util.ArchiveUtil;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveView {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveView.class);
    private ApplicationContext applicationContext;
    private TextField name;
    private TextField extension;
    private Label size;
    private UserAreaPicker userArea;
    private CheckBox readOnlyAttribute;
    private CheckBox systemAttribute;
    private CheckBox archivedAttribute;

    private Archive currentArchive;

    private TextFormatter<String> getCpmTextFormatter(final int maxLength) {
        return new TextFormatter<>(c -> {
            if (c.isContentChange()) {
                LOGGER.debug("Change was " + c);
                String filteredName = ArchiveUtil.toCpmValidName(c.getControlNewText(),
                        maxLength);
                int oldLength = c.getControlText().length();
                int newLength = c.getControlNewText().length();
                int correctedLength = filteredName.length();
                c.setText(filteredName);
                c.setRange(0, oldLength);
                c.setCaretPosition(Math.max(0, c.getCaretPosition() - (newLength - correctedLength)));

                c.setAnchor(Math.max(0, c.getAnchor() - (newLength - correctedLength)));
                LOGGER.debug("Change updated to " + c);
            }
            return c;
        });

    }

    private boolean isNameAlreadyInUse(long id, String name, String extension, int userArea) {
        return applicationContext.getArchiveList().filtered(a ->
                a.getId() != id &&
                a.getUserArea() == userArea &&
                a.getName().equals(name) &&
                a.getExtension().equals(extension)).size() > 0;
    }

    public ArchiveView(ApplicationContext applicationContext,
                       TextField name, TextField extension, Label size,
                       UserAreaPicker userArea, CheckBox readOnlyAttribute,
                       CheckBox systemAttribute, CheckBox archivedAttribute) {
        this.applicationContext = applicationContext;
        this.name = name;
        this.extension = extension;
        this.size = size;
        this.userArea = userArea;
        this.readOnlyAttribute = readOnlyAttribute;
        this.systemAttribute = systemAttribute;
        this.archivedAttribute = archivedAttribute;

        ChangeListener<String> nameChangeListener = (observable, oldValue, newValue) -> {
            if (currentArchive != null) {
                if (isNameAlreadyInUse(currentArchive.getId(), newValue, currentArchive.getExtension(),
                        currentArchive.getUserArea())) {
                    name.getStyleClass().add(Constants.TEXT_ERROR_STYLE);
                } else {
                    name.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
                    currentArchive.setName(newValue);
                }
            }
        };
        this.name.textProperty().addListener(nameChangeListener);
        ChangeListener<String> extensionChangeListener = (observable, oldValue, newValue) -> {
            if (currentArchive != null) {
                if (isNameAlreadyInUse(currentArchive.getId(), currentArchive.getName(),
                        newValue, currentArchive.getUserArea())) {
                    extension.getStyleClass().add(Constants.TEXT_ERROR_STYLE);
                } else {
                    extension.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
                    currentArchive.setExtension(newValue);
                }
            }
        };
        this.extension.textProperty().addListener(extensionChangeListener);
        ChangeListener<Number> userAreaChangeListener = (observable, oldValue, newValue) -> {
            if (currentArchive != null) {
                if (isNameAlreadyInUse(currentArchive.getId(), currentArchive.getName(),
                        currentArchive.getExtension(), newValue.intValue())) {
                    userArea.getStyleClass().add(Constants.TEXT_ERROR_STYLE);
                } else {
                    userArea.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
                    currentArchive.setUserArea(newValue.intValue());
                }
            }
        };
        this.userArea.userAreaProperty().addListener(userAreaChangeListener);
        ChangeListener<Boolean> readOnlyAttributeChangeListener = (observable, oldValue, newValue) ->
                updateAttribute(ArchiveFlags.READ_ONLY, newValue);
        this.readOnlyAttribute.selectedProperty().addListener(readOnlyAttributeChangeListener);
        ChangeListener<Boolean> systemAttributeChangeListener = (observable, oldValue, newValue) ->
                updateAttribute(ArchiveFlags.SYSTEM, newValue);
        this.systemAttribute.selectedProperty().addListener(systemAttributeChangeListener);
        ChangeListener<Boolean> archivedAttributeChangeListener = (observable, oldValue, newValue) ->
                updateAttribute(ArchiveFlags.ARCHIVED, newValue);
        this.archivedAttribute.selectedProperty().addListener(archivedAttributeChangeListener);

        this.name.setTextFormatter(getCpmTextFormatter(Constants.CPM_FILENAME_MAXLENGTH));
        this.extension.setTextFormatter(getCpmTextFormatter(Constants.CPM_FILEEXTENSION_MAXLENGTH));
    }

    private void updateAttribute(ArchiveFlags attribute, boolean value) {
        if (currentArchive != null) {
            if (value) {
                currentArchive.getFlags().add(attribute);
            } else {
                currentArchive.getFlags().remove(attribute);
            }
        }
        LOGGER.debug("Updated flags " + attribute + " for archive " + currentArchive);
    }

    public void bindToArchive(Archive archive) {
        unbindFromCurrentArchive();
        if (archive != null) {
            this.name.setText(archive.getName());
            this.extension.setText(archive.getExtension());
            this.userArea.setUserArea(archive.getUserArea());
            size.setText(String.format("%d", archive.getSize()));
            readOnlyAttribute.setSelected(archive.getFlags().contains(ArchiveFlags.READ_ONLY));
            systemAttribute.setSelected(archive.getFlags().contains(ArchiveFlags.SYSTEM));
            archivedAttribute.setSelected(archive.getFlags().contains(ArchiveFlags.ARCHIVED));
            currentArchive = archive;
        }

    }

    private void resetValues() {
        name.setText(Constants.EMPTY_STRING);
        name.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
        extension.setText(Constants.EMPTY_STRING);
        extension.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
        size.setText(Constants.NO_VALUE);
        readOnlyAttribute.setSelected(false);
        systemAttribute.setSelected(false);
        archivedAttribute.setSelected(false);
        userArea.setUserArea(Constants.CPM_DEFAULT_USER_AREA);
        userArea.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
    }

    private void unbindFromCurrentArchive() {
        currentArchive = null;
        resetValues();
    }
}