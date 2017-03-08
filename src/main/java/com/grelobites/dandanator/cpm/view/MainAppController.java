package com.grelobites.dandanator.cpm.view;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.model.Installable;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import com.grelobites.dandanator.cpm.util.InstallableUtil;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.util.OperationResult;
import com.grelobites.dandanator.cpm.view.util.DialogUtil;
import com.grelobites.dandanator.cpm.view.util.DirectoryAwareFileChooser;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainAppController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainAppController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    private ApplicationContext applicationContext;

    @FXML
    private Pane applicationPane;

    @FXML
    private TableView<Installable> installableTable;

    @FXML
    private TableColumn<Installable, String> nameColumn;

    @FXML
    private Button createRomButton;

    @FXML
    private Button addInstallableButton;

    @FXML
    private Button removeSelectedInstallableButton;

    @FXML
    private Button clearRomsetButton;

    @FXML
    private ProgressIndicator operationInProgressIndicator;

    @FXML
    private Pane romSetHandlerInfoPane;

    public MainAppController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private RomSetHandler getRomSetHandler() {
        return applicationContext.getRomSetHandler();
    }

    private String getInstallableName(File file) {
        //TODO: Avoid collisions with other names in the list and adhere to CP/M conventions
        return file.getName();
    }

    private void addInstallablesFromFiles(List<File> files) {
        files.forEach(file ->
            applicationContext.addBackgroundTask(() -> {
                Optional<Installable> installableOptional = Installable.fromFile(getInstallableName(file), file);
                if (installableOptional.isPresent()) {
                    Platform.runLater(() -> getRomSetHandler().addInstallable(installableOptional.get()));
                } else {
                    Platform.runLater(() -> {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            if (getApplicationContext().getInstallableList().isEmpty()) {
                                getRomSetHandler().importRomSet(fis);
                            } else {
                                getRomSetHandler().mergeRomSet(fis);
                            }
                        } catch (Exception e) {
                            LOGGER.error("Importing ROMSet", e);
                            DialogUtil.buildErrorAlert(
                                    LocaleUtil.i18n("fileImportError"),
                                    LocaleUtil.i18n("fileImportErrorHeader"),
                                    LocaleUtil.i18n("fileImportErrorContent"))
                                    .showAndWait();
                        }
                    });
                }
                return OperationResult.successResult();
            }));
    }

    @FXML
    private void initialize() throws IOException {
        applicationContext.setSelectedInstallableProperty(installableTable.getSelectionModel().selectedItemProperty());

       clearRomsetButton.disableProperty()
                .bind(Bindings.size(applicationContext.getInstallableList())
                        .isEqualTo(0));

        installableTable.setItems(applicationContext.getInstallableList());
        installableTable.setPlaceholder(new Label(LocaleUtil.i18n("dropInstallablesMessage")));

        operationInProgressIndicator.visibleProperty().bind(
                applicationContext.backgroundTaskCountProperty().greaterThan(0));

        onInstallableSelection(null, null);

        installableTable.setRowFactory(rf -> {
            TableRow<Installable> row = new TableRow<>();
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Integer index = row.getIndex();
                    LOGGER.debug("Dragging content of row " + index);
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (row.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                LOGGER.debug("row.setOnDragDropped: " + db);
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    Installable draggedInstallable = installableTable.getItems().remove(draggedIndex);

                    int dropIndex ;

                    if (row.isEmpty()) {
                        dropIndex = installableTable.getItems().size();
                    } else {
                        dropIndex = row.getIndex();
                    }

                    installableTable.getItems().add(dropIndex, draggedInstallable);

                    event.setDropCompleted(true);
                    installableTable.getSelectionModel().select(dropIndex);
                    event.consume();
                } else {
                    LOGGER.debug("Dragboard content is not of the required type");
                }
            });

            row.setOnMouseClicked(e -> {
                if (row.isEmpty()) {
                    installableTable.getSelectionModel().clearSelection();
                }
            });
            return row;
        });

        nameColumn.setCellValueFactory(
                cellData -> cellData.getValue().nameProperty());

        installableTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onInstallableSelection(oldValue, newValue));


        installableTable.setOnDragOver(event -> {
            if (event.getGestureSource() != installableTable &&
                    event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        installableTable.setOnDragEntered(Event::consume);

        installableTable.setOnDragExited(Event::consume);

        installableTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            LOGGER.debug("onDragDropped. Transfer modes are " + db.getTransferModes());
            boolean success = false;
            if (db.hasFiles()) {
                addInstallablesFromFiles(db.getFiles());
                success = true;
            }
            /* let the source know whether the files were successfully
             * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });


        createRomButton.setOnAction(c -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("saveRomSet"));
            chooser.setInitialFileName("dandanator_cpm_" + Constants.currentVersion() + ".rom");
            final File saveFile = chooser.showSaveDialog(createRomButton.getScene().getWindow());
            if (saveFile != null) {
                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    getApplicationContext().getRomSetHandler().exportRomSet(fos);
                } catch (IOException e) {
                    LOGGER.error("Creating ROM Set", e);
                }
            }
        });

        addInstallableButton.setOnAction(c -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("openSnapshot"));
            final List<File> snapshotFiles = chooser.showOpenMultipleDialog(addInstallableButton.getScene().getWindow());
            if (snapshotFiles != null) {
                try {
                    addInstallablesFromFiles(snapshotFiles);
                } catch (Exception e) {
                    LOGGER.error("Opening snapshots from files " + snapshotFiles, e);
                }
            }
        });

        removeSelectedInstallableButton.setOnAction(c -> {
            Optional<Installable> selectedInstallable = Optional.of(installableTable.getSelectionModel().getSelectedItem());
            selectedInstallable.ifPresent(index -> applicationContext.getRomSetHandler()
                    .removeInstallable(selectedInstallable.get()));
        });

        clearRomsetButton.setOnAction(c -> {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();

            if (result.orElse(ButtonType.CANCEL) == ButtonType.OK){
                applicationContext.getInstallableList().clear();
            }
        });
    }

    private void onInstallableSelection(Installable oldInstallable, Installable newInstallable) {
        LOGGER.debug("onGameSelection oldGame=" + oldInstallable + ", newGame=" + newInstallable);
        if (newInstallable == null) {
            removeSelectedInstallableButton.setDisable(true);
        } else {
            removeSelectedInstallableButton.setDisable(false);
        }
    }

}
