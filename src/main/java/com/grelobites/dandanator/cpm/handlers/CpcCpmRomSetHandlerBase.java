package com.grelobites.dandanator.cpm.handlers;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.filesystem.CpmFileSystem;
import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.util.Util;
import com.grelobites.dandanator.cpm.util.zx7.Zx7OutputStream;
import javafx.beans.InvalidationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public abstract class CpcCpmRomSetHandlerBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpcCpmRomSetHandlerBase.class);

    protected ApplicationContext applicationContext;

    protected CpmFileSystem fileSystem;

    protected InvalidationListener romUsageUpdater = e -> updateRomUsage();

    public void unbind() {
        applicationContext.getArchiveList().removeListener(romUsageUpdater);
    }

    public void bind() {
        fileSystem.clear();
        applicationContext.getArchiveList().forEach(fileSystem::addArchive);
        updateRomUsage();
        applicationContext.getArchiveList().addListener(romUsageUpdater);
    }

    private void updateRomUsage() {
        applicationContext.setRomUsage(1.0 * (fileSystem.totalBytes() - fileSystem.freeBytes()) /
                fileSystem.totalBytes());
        applicationContext.setRomUsageDetail(String.format(LocaleUtil.i18n("romUsageDetail"),
                fileSystem.freeBytes(), fileSystem.totalBytes()));
        applicationContext.setDirectoryUsage(1.0 *
                (fileSystem.totalDirectoryEntries() - fileSystem.freeDirectoryEntries()) /
                    fileSystem.totalDirectoryEntries());
        applicationContext.setDirectoryUsageDetail(String.format(LocaleUtil.i18n("directoryUsageDetail"),
                fileSystem.freeDirectoryEntries(), fileSystem.totalDirectoryEntries()));
    }

    protected static byte[] compress(byte[] source) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        OutputStream os = new Zx7OutputStream(target);
        os.write(source);
        os.close();
        return target.toByteArray();
    }

    protected static byte[] getEepromLoaderCode() throws IOException {
        byte[] eewriter = Util.fromInputStream(Objects.requireNonNull(CpcCpmRomSetHandlerBase.class
                .getResourceAsStream("/cpc/eewriter-code.bin")));
        return compress(eewriter);
    }

    protected static byte[] getEepromLoaderScreen() throws IOException {
        byte[] screen = Util.fromInputStream(Objects.requireNonNull(CpcCpmRomSetHandlerBase.class
                .getResourceAsStream("/cpc/eewriter-screen.bin")));
        byte[] packedScreen = Arrays.copyOf(screen, 16384);
        System.arraycopy(screen, 16384,
                packedScreen, 16384 - 17, 17);
        return compress(packedScreen);
    }

    protected static void fillWithValue(OutputStream os, byte value, int size) throws IOException {
        for (int i = 0; i < size; i++) {
            os.write(value);
        }
    }

    public abstract void mergeRomSet(InputStream romset) throws IOException;

    public void importRomSet(InputStream romset) throws IOException {
        LOGGER.debug("importRomSet " + romset);
        clear();
        mergeRomSet(romset);
    }

    public void removeArchive(Archive archive) {
        LOGGER.debug("removeArchive " + archive);
        fileSystem.removeArchive(archive);
        applicationContext.getArchiveList().remove(archive);
    }

    public void addArchive(Archive archive) {
        LOGGER.debug("addArchive " + archive);
        fileSystem.addArchive(archive);
        applicationContext.getArchiveList().add(archive);
    }

    public void clear() {
        fileSystem.clear();
        applicationContext.getArchiveList().clear();
    }
}
