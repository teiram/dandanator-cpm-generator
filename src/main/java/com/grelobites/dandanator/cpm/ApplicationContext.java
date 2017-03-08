package com.grelobites.dandanator.cpm;

import com.grelobites.dandanator.cpm.model.Installable;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.util.OperationResult;
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
    private final ObservableList<Installable> installableList;
    private ReadOnlyObjectProperty<Installable> selectedInstallable;
    private BooleanProperty installableSelected;
    private StringProperty romUsageDetail;
    private DoubleProperty romUsage;
    private IntegerProperty backgroundTaskCount;
    private DirectoryAwareFileChooser fileChooser;

    private RomSetHandler romSetHandler;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("RomGenerator executor service");
        return t;
    });

    public ApplicationContext() {
        this.installableList = FXCollections.observableArrayList(Installable::getObservables);
        this.installableSelected = new SimpleBooleanProperty(false);
        this.romUsage = new SimpleDoubleProperty();
        this.romUsageDetail = new SimpleStringProperty();
        this.backgroundTaskCount = new SimpleIntegerProperty();
    }

    public ObservableList<Installable> getInstallableList() {
        return installableList;
    }

    public boolean getInstallableSelected() {
        return installableSelected.get();
    }

    public void setInstallableSelected(boolean installableSelected) {
        this.installableSelected.set(installableSelected);
    }

    public BooleanProperty installableSelectedProperty() {
        return installableSelected;
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

    public ReadOnlyObjectProperty<Installable> selectedInstallableProperty() {
        return selectedInstallable;
    }

    public void setSelectedInstallableProperty(ReadOnlyObjectProperty<Installable> selectedInstallableProperty) {
        selectedInstallable = selectedInstallableProperty;
        installableSelected.bind(selectedInstallable.isNotNull());
    }

    public Stage getApplicationStage() {
        return applicationStage;
    }

    public void setApplicationStage(Stage applicationStage) {
        this.applicationStage = applicationStage;
    }

    public void exportCurrentInstallable() {
        Installable installable = selectedInstallable.get();
        if (installable != null) {
            DirectoryAwareFileChooser chooser = getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("exportCurrentInstallable"));
            chooser.setInitialFileName(installable.getName());
            final File saveFile = chooser.showSaveDialog(applicationStage.getScene().getWindow());
            if (saveFile != null) {
                try {
                    installable.exportAsFile(saveFile);
                } catch (IOException e) {
                    LOGGER.error("Exporting Installable", e);
                }
            }
        } else {
            DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentInstallablerrorTitle"),
                    LocaleUtil.i18n("exportCurrentInstallableErrorHeader"),
                    LocaleUtil.i18n("exportCurrentInstallableErrorContentNoInstallableSelected")).showAndWait();
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
        if (getInstallableList().isEmpty() || confirmRomSetDeletion()) {
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

    class BackgroundTask implements Callable<OperationResult> {
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


