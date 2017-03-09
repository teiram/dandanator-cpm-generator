package com.grelobites.dandanator.cpm.view;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import com.grelobites.dandanator.cpm.util.ArchiveUtil;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.util.OperationResult;
import com.grelobites.dandanator.cpm.view.util.DialogUtil;
import com.grelobites.dandanator.cpm.view.util.DirectoryAwareFileChooser;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.transformation.FilteredList;
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
    private Pane archiveInformationPane;

    @FXML
    private TableView<Archive> archiveTable;

    private FilteredList<Archive> filteredArchiveList;

    @FXML
    private TableColumn<Archive, String> archiveNameColumn;

    @FXML
    private TableColumn<Archive, Number> archiveSizeColumn;

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
    private CheckBox userFilterEnable;

    @FXML
    private Button userFilterDecrementButton;

    @FXML
    private Label userFilterValue;

    @FXML
    private Button userFilterIncrementButton;

    @FXML
    private TextField archiveName;

    @FXML
    private TextField archiveExtension;

    @FXML
    private Label archiveSize;

    @FXML
    private Button archiveUserAreaDecrementButton;

    @FXML
    private Button archiveUserAreaIncrementButton;

    @FXML
    private Label archiveUserArea;

    @FXML
    private CheckBox archiveReadOnlyAttribute;

    @FXML
    private CheckBox archiveSystemFileAttribute;

    @FXML
    private CheckBox archiveArchivedAttribute;

    private UserAreaPicker filterUserAreaPicker;
    private ArchiveView archiveView;

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

    private void addArchivesFromFiles(List<File> files) {
        files.forEach(file -> {
            try {
                List<Archive> archives = ArchiveUtil.getArchivesInFile(applicationContext, file);
                LOGGER.debug("Returned list of archives " + archives);
                for (Archive archive : archives) {
                    getRomSetHandler().addArchive(archive);
                }
            } catch (Exception e) {
                LOGGER.error("In addArchivesFromFiles", e);

            }
        });
    }

    @FXML
    private void initialize() throws IOException {
        applicationContext.setSelectedArchiveProperty(archiveTable.getSelectionModel().selectedItemProperty());

        purgeArchivesButton.disableProperty()
                .bind(Bindings.size(applicationContext.getArchiveList())
                        .isEqualTo(0));

        filteredArchiveList = new FilteredList<>(applicationContext.getArchiveList());
        archiveTable.setItems(filteredArchiveList);
        archiveTable.setPlaceholder(new Label(LocaleUtil.i18n("dropArchivesMessage")));

        filterUserAreaPicker = new UserAreaPicker(userFilterDecrementButton,
                userFilterIncrementButton,
                userFilterValue);
        applicationContext.setUserFilter(filterUserAreaPicker);

        filterUserAreaPicker.disableProperty().bind(userFilterEnable.selectedProperty().not());

        userFilterEnable.setSelected(false);
        userFilterEnable.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                filteredArchiveList.setPredicate(f -> f.getUserArea() == filterUserAreaPicker.getUserArea());
            } else {
                filteredArchiveList.setPredicate(f -> true);
            }
        });

        filterUserAreaPicker.userAreaProperty().addListener((observable, oldvalue, newValue) -> {
            if (userFilterEnable.isSelected()) {
                filteredArchiveList.setPredicate(f -> f.getUserArea() == filterUserAreaPicker.getUserArea());
            }
        });

        operationInProgressIndicator.visibleProperty().bind(
                applicationContext.backgroundTaskCountProperty().greaterThan(0));

        archiveView = new ArchiveView(applicationContext, archiveName, archiveExtension, archiveSize,
                new UserAreaPicker(archiveUserAreaDecrementButton,
                        archiveUserAreaIncrementButton,
                        archiveUserArea),
                archiveReadOnlyAttribute, archiveSystemFileAttribute, archiveArchivedAttribute);

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
                cellData -> cellData.getValue().nameProperty()
                        .concat(Constants.FILE_EXTENSION_SEPARATOR)
                        .concat(cellData.getValue().extensionProperty()));

        archiveSizeColumn.setCellValueFactory(
                cellData -> cellData.getValue().sizeProperty());

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
                addArchivesFromFiles(db.getFiles());
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
            chooser.setTitle(LocaleUtil.i18n("addArchiveDialog"));
            final List<File> snapshotFiles = chooser.showOpenMultipleDialog(addArchiveButton.getScene().getWindow());
            if (snapshotFiles != null) {
                try {
                    addArchivesFromFiles(snapshotFiles);
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
                    .buildAlert(LocaleUtil.i18n("archiveDeletionConfirmTitle"),
                            LocaleUtil.i18n("archiveDeletionConfirmHeader"),
                            LocaleUtil.i18n("archiveDeletionConfirmContent"))
                    .showAndWait();

            if (result.orElse(ButtonType.CANCEL) == ButtonType.OK){
                applicationContext.getArchiveList().clear();
            }
        });
    }

    private void onArchiveSelection(Archive oldArchive, Archive newArchive) {
        LOGGER.debug("onArchiveSelection oldArchive=" + oldArchive + ", newArchive=" + newArchive);
        archiveView.bindToArchive(newArchive);
        archiveInformationPane.setDisable(newArchive == null);
        if (newArchive == null) {
            removeSelectedArchiveButton.setDisable(true);
        } else {
            removeSelectedArchiveButton.setDisable(false);
        }
    }

}
