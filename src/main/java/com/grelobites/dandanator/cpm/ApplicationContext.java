package com.grelobites.dandanator.cpm;

import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import com.grelobites.dandanator.cpm.util.ArchiveUtil;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.util.OperationResult;
import com.grelobites.dandanator.cpm.view.UserAreaPicker;
import com.grelobites.dandanator.cpm.view.util.DialogUtil;
import com.grelobites.dandanator.cpm.view.util.DirectoryAwareFileChooser;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);
    private Stage applicationStage;
    private final ObservableList<Archive> archiveList;
    private ReadOnlyObjectProperty<Archive> selectedArchive;
    private BooleanProperty archiveSelected;
    private StringProperty romUsageDetail;
    private DoubleProperty romUsage;
    private StringProperty directoryUsageDetail;
    private DoubleProperty directoryUsage;
    private IntegerProperty backgroundTaskCount;
    private DirectoryAwareFileChooser fileChooser;

    private RomSetHandler romSetHandler;
    private UserAreaPicker userFilter;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("RomGenerator executor service");
        return t;
    });

    public ApplicationContext() {
        this.archiveList = FXCollections.observableArrayList(Archive::getObservables);
        this.archiveSelected = new SimpleBooleanProperty(false);
        this.romUsage = new SimpleDoubleProperty(0);
        this.romUsageDetail = new SimpleStringProperty();
        this.backgroundTaskCount = new SimpleIntegerProperty();
        this.directoryUsage = new SimpleDoubleProperty(0);
        this.directoryUsageDetail = new SimpleStringProperty();

        Preferences preferences = Preferences.getInstance();

        this.romSetHandler = preferences.getHandlerType().handler(this);
        this.romSetHandler.bind();
        preferences.handlerTypeProperty().addListener((e, oldValue, newValue) -> {
            LOGGER.debug("Setting handler as {}", newValue);
            if (this.romSetHandler != null) {
                this.romSetHandler.unbind();
            }
            this.romSetHandler = newValue.handler(this);
            this.romSetHandler.bind();
        });
    }

    public ObservableList<Archive> getArchiveList() {
        return archiveList;
    }

    public boolean getArchiveSelected() {
        return archiveSelected.get();
    }

    public void setArchiveSelected(boolean archiveSelected) {
        this.archiveSelected.set(archiveSelected);
    }

    public BooleanProperty archiveSelectedProperty() {
        return archiveSelected;
    }

    public DirectoryAwareFileChooser getFileChooser() {
        if (this.fileChooser == null) {
            this.fileChooser = new DirectoryAwareFileChooser();
        }
        fileChooser.setInitialFileName(null);
        return fileChooser;
    }
    public void setRomUsage(double romUsage) {
        this.romUsage.set(romUsage);
    }

    public double getRomUsage() {
        return romUsage.get();
    }

    public DoubleProperty romUsageProperty() {
        return romUsage;
    }

    public void setRomUsageDetail(String romUsageDetail) {
        this.romUsageDetail.set(romUsageDetail);
    }

    public String getRomUsageDetail() {
        return romUsageDetail.get();
    }

    public StringProperty romUsageDetailProperty() {
        return romUsageDetail;
    }

    public String getDirectoryUsageDetail() {
        return directoryUsageDetail.get();
    }

    public StringProperty directoryUsageDetailProperty() {
        return directoryUsageDetail;
    }

    public void setDirectoryUsageDetail(String directoryUsageDetail) {
        this.directoryUsageDetail.set(directoryUsageDetail);
    }

    public double getDirectoryUsage() {
        return directoryUsage.get();
    }

    public DoubleProperty directoryUsageProperty() {
        return directoryUsage;
    }

    public void setDirectoryUsage(double directoryUsage) {
        this.directoryUsage.set(directoryUsage);
    }

    public IntegerProperty backgroundTaskCountProperty() {
        return backgroundTaskCount;
    }

    public Future<OperationResult> addBackgroundTask(Callable<OperationResult> task) {
        Platform.runLater(() -> backgroundTaskCount.set(backgroundTaskCount.get() + 1));
        return executorService.submit(new BackgroundTask(task, backgroundTaskCount));
    }

    public RomSetHandler getRomSetHandler() {
        return romSetHandler;
    }

    public void setRomSetHandler(RomSetHandler romSetHandler) {
        this.romSetHandler = romSetHandler;
    }

    public ReadOnlyObjectProperty<Archive> selectedArchiveProperty() {
        return selectedArchive;
    }

    public void setSelectedArchiveProperty(ReadOnlyObjectProperty<Archive> selectedInstallableProperty) {
        selectedArchive = selectedInstallableProperty;
        archiveSelected.bind(selectedArchive.isNotNull());
    }

    public Stage getApplicationStage() {
        return applicationStage;
    }

    public void setApplicationStage(Stage applicationStage) {
        this.applicationStage = applicationStage;
    }

    public UserAreaPicker getUserFilter() {
        return userFilter;
    }

    public void setUserFilter(UserAreaPicker userFilter) {
        this.userFilter = userFilter;
    }

    public void addSystemArchives() {
        String basePath = romSetHandler.getSystemArchivePath();
        romSetHandler.getSystemArchives().forEach(fileName -> {
           try (InputStream is = ApplicationContext.class.getResourceAsStream(
                   String.format("%s%s", basePath, fileName))) {
               romSetHandler.addArchive(ArchiveUtil.createArchiveFromStream(fileName, is, this));
           } catch (IOException ioe) {
              LOGGER.error("Unable to get file for resource {} on path {}", fileName, basePath, ioe);
           }
        });
    }

    public int getCurrentUserArea() {
        return userFilter.isDisable() ? 0 : userFilter.getUserArea();
    }

    public void exportCurrentArchive() {
        Archive archive = selectedArchive.get();
        if (archive != null) {
            DirectoryAwareFileChooser chooser = getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("exportCurrentArchive"));
            chooser.setInitialFileName(String.format("%s.%s", archive.getName().trim(), archive.getExtension().trim()));
            final File saveFile = chooser.showSaveDialog(applicationStage.getScene().getWindow());
            if (saveFile != null) {
                try {
                    ArchiveUtil.exportAsFile(archive, saveFile);
                } catch (IOException e) {
                    LOGGER.error("Exporting Installable", e);
                }
            }
        } else {
            DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentArchiveErrorTitle"),
                    LocaleUtil.i18n("exportCurrentArchiveErrorHeader"),
                    LocaleUtil.i18n("exportCurrentArchiveNoArchiveSelected")).showAndWait();
        }
    }

    private static boolean confirmRomSetDeletion() {
        Optional<ButtonType> result = DialogUtil
                .buildAlert(LocaleUtil.i18n("romSetDeletionConfirmTitle"),
                        LocaleUtil.i18n("romSetDeletionConfirmHeader"),
                        LocaleUtil.i18n("romSetDeletionConfirmContent"))
                .showAndWait();
        return result.orElse(ButtonType.CLOSE) == ButtonType.OK;
    }

    public void importRomSet(File romSetFile) throws IOException {
        if (getArchiveList().isEmpty() || confirmRomSetDeletion()) {
            try (InputStream is = new FileInputStream(romSetFile)) {
                romSetHandler.importRomSet(is);
            }
        }
    }

    public void mergeRomSet(File romSetFile) throws IOException {
        try (InputStream is = new FileInputStream(romSetFile)) {
            romSetHandler.mergeRomSet(is);
        }
    }

    static class BackgroundTask implements Callable<OperationResult> {
        private IntegerProperty backgroundTaskCount;
        private Callable<OperationResult> task;

        public BackgroundTask(Callable<OperationResult> task, IntegerProperty backgroundTaskCount) {
            this.backgroundTaskCount = backgroundTaskCount;
            this.task = task;
        }

        @Override
        public OperationResult call() throws Exception {
            final OperationResult result = task.call();
            Platform.runLater(() -> {
                backgroundTaskCount.set(backgroundTaskCount.get() - 1);
                if (result.isError()) {
                    DialogUtil.buildErrorAlert(result.getContext(),
                            result.getMessage(),
                            result.getDetail())
                            .showAndWait();
                }
            });
            return result;
        }
    }
}


