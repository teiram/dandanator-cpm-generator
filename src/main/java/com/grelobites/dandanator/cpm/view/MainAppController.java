package com.grelobites.dandanator.cpm.view;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.util.OperationResult;
import com.grelobites.dandanator.cpm.view.util.DialogUtil;
import com.grelobites.dandanator.cpm.view.util.DirectoryAwareFileChooser;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
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
    private TableView<Archive> archiveTable;

    @FXML
    private TableColumn<Archive, String> archiveNameColumn;

    @FXML
    private Button createRomButton;

    @FXML
    private Button addArchiveButton;

    @FXML
    private Button removeSelectedArchiveButton;

    @FXML
    private Button purgeArchivesButton;

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

    private String getArchiveName(File file) {
        //TODO: Avoid collisions with other names in the list and adhere to CP/M conventions
        return file.getName();
    }

    private void addInstallablesFromFiles(List<File> files) {
        files.forEach(file ->
            applicationContext.addBackgroundTask(() -> {
                Optional<Archive> archiveOptional = Archive.fromFile(getArchiveName(file), file);
                if (archiveOptional.isPresent()) {
                    Platform.runLater(() -> getRomSetHandler().addArchive(archiveOptional.get()));
                } else {
                    Platform.runLater(() -> {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            if (getApplicationContext().getArchiveList().isEmpty()) {
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
        applicationContext.setSelectedArchiveProperty(archiveTable.getSelectionModel().selectedItemProperty());

       purgeArchivesButton.disableProperty()
                .bind(Bindings.size(applicationContext.getArchiveList())
                        .isEqualTo(0));

        archiveTable.setItems(applicationContext.getArchiveList());
        archiveTable.setPlaceholder(new Label(LocaleUtil.i18n("dropArchivesMessage")));

        operationInProgressIndicator.visibleProperty().bind(
                applicationContext.backgroundTaskCountProperty().greaterThan(0));

        onArchiveSelection(null, null);

        archiveTable.setRowFactory(rf -> {
            TableRow<Archive> row = new TableRow<>();
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
                    Archive draggedArchive = archiveTable.getItems().remove(draggedIndex);

                    int dropIndex ;

                    if (row.isEmpty()) {
                        dropIndex = archiveTable.getItems().size();
                    } else {
                        dropIndex = row.getIndex();
                    }

                    archiveTable.getItems().add(dropIndex, draggedArchive);

                    event.setDropCompleted(true);
                    archiveTable.getSelectionModel().select(dropIndex);
                    event.consume();
                } else {
                    LOGGER.debug("Dragboard content is not of the required type");
                }
            });

            row.setOnMouseClicked(e -> {
                if (row.isEmpty()) {
                    archiveTable.getSelectionModel().clearSelection();
                }
            });
            return row;
        });

        archiveNameColumn.setCellValueFactory(
                cellData -> cellData.getValue().nameProperty());

        archiveTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onArchiveSelection(oldValue, newValue));


        archiveTable.setOnDragOver(event -> {
            if (event.getGestureSource() != archiveTable &&
                    event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        archiveTable.setOnDragEntered(Event::consume);

        archiveTable.setOnDragExited(Event::consume);

        archiveTable.setOnDragDropped(event -> {
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

        addArchiveButton.setOnAction(c -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("openSnapshot"));
            final List<File> snapshotFiles = chooser.showOpenMultipleDialog(addArchiveButton.getScene().getWindow());
            if (snapshotFiles != null) {
                try {
                    addInstallablesFromFiles(snapshotFiles);
                } catch (Exception e) {
                    LOGGER.error("Opening snapshots from files " + snapshotFiles, e);
                }
            }
        });

        removeSelectedArchiveButton.setOnAction(c -> {
            Optional<Archive> selectedInstallable = Optional.of(archiveTable.getSelectionModel().getSelectedItem());
            selectedInstallable.ifPresent(index -> applicationContext.getRomSetHandler()
                    .removeArchive(selectedInstallable.get()));
        });

        purgeArchivesButton.setOnAction(c -> {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();

            if (result.orElse(ButtonType.CANCEL) == ButtonType.OK){
                applicationContext.getArchiveList().clear();
            }
        });
    }

    private void onArchiveSelection(Archive oldArchive, Archive newArchive) {
        LOGGER.debug("onGameSelection oldGame=" + oldArchive + ", newGame=" + newArchive);
        if (newArchive == null) {
            removeSelectedArchiveButton.setDisable(true);
        } else {
            removeSelectedArchiveButton.setDisable(false);
        }
    }

}
