package com.grelobites.dandanator.cpm.handlers;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.filesystem.CpmFileSystem;
import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.util.Util;
import com.grelobites.dandanator.cpm.util.zx7.Zx7OutputStream;
import javafx.beans.InvalidationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class CpcCpmRomSetHandler implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpcCpmRomSetHandler.class);

    private ApplicationContext applicationContext;

    private CpmFileSystem fileSystem;

    private InvalidationListener romUsageUpdater = e -> updateRomUsage();

    public CpcCpmRomSetHandler(ApplicationContext context) {
        this.applicationContext = context;
        this.fileSystem = new CpmFileSystem(Constants.CPC_ROMSET_FS_PARAMETERS);
    }

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

    private static byte[] compress(byte[] source) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        OutputStream os = new Zx7OutputStream(target);
        os.write(source);
        os.close();
        return target.toByteArray();
    }
    private static byte[] getEepromLoaderCode() throws IOException {
        byte[] eewriter = Util.fromInputStream(CpcCpmRomSetHandler.class
                .getResourceAsStream("/cpc/eewriter-code.bin"));
        return compress(eewriter);
    }

    private static byte[] getEepromLoaderScreen() throws IOException {
        byte[] screen = Util.fromInputStream(CpcCpmRomSetHandler.class
                .getResourceAsStream("/cpc/eewriter-screen.bin"));
        byte[] packedScreen = Arrays.copyOf(screen, 16384);
        System.arraycopy(screen, 16384,
                packedScreen, 16384 - 17, 17);
        return compress(packedScreen);
    }

    @Override
    public void mergeRomSet(InputStream romset) throws IOException {
        LOGGER.debug("mergeRomSet " + romset);
        //Skip EMS and sector zero data
        romset.skip(16384 * 2);
        byte[] fsData = new byte[16384 * 30];
        romset.read(fsData);
        CpmFileSystem fileSystem = CpmFileSystem.fromByteArray(
                fsData, Constants.CPC_ROMSET_FS_PARAMETERS);
        for (Archive archive : fileSystem.getArchiveList()) {
            addArchive(archive);
        }
    }

    @Override
    public void importRomSet(InputStream romset) throws IOException {
        LOGGER.debug("importRomSet " + romset);
        fileSystem.clear();
        applicationContext.getArchiveList().clear();
        mergeRomSet(romset);
    }

    protected static void fillWithValue(OutputStream os, byte value, int size) throws IOException {
        for (int i = 0; i < size; i++) {
            os.write(value);
        }
    }

    @Override
    public void exportRomSet(OutputStream romset) throws IOException {
        LOGGER.debug("exportRomSet " + romset);

        byte[] loader = Util.fromInputStream(CpcCpmRomSetHandler.class
                        .getResourceAsStream("/cpc/loader.bin"));

        byte[] patchedAmsdos = Util.fromInputStream(CpcCpmRomSetHandler.class
                .getResourceAsStream("/cpc/patched-amsdos.rom"));

        byte[] compressedEewriterCode = getEepromLoaderCode();
        byte[] compressedEewriterScreen = getEepromLoaderScreen();


        LOGGER.debug("Loader size: {}", loader.length);
        LOGGER.debug("Compressed eewriter code size: {}", compressedEewriterCode.length);
        LOGGER.debug("Compressed eewriter screen size: {}", compressedEewriterScreen.length);

        ByteBuffer loaderBuffer = ByteBuffer.wrap(loader);
        loaderBuffer.order(ByteOrder.LITTLE_ENDIAN).position(16);
        loaderBuffer.putShort(Integer.valueOf(1024).shortValue());
        loaderBuffer.putShort(Integer.valueOf(1024 + compressedEewriterCode.length).shortValue());

        romset.write(loaderBuffer.array());
        fillWithValue(romset, Integer.valueOf(0).byteValue(), 1024 - loader.length);
        romset.write(compressedEewriterCode);
        romset.write(compressedEewriterScreen);
        fillWithValue(romset, Integer.valueOf(0).byteValue(), Constants.SLOT_SIZE -
                (1024 + compressedEewriterCode.length + compressedEewriterScreen.length));

        romset.write(patchedAmsdos);

        byte[] systemTracks = Util.fromInputStream(CpcCpmRomSetHandler.class
                        .getResourceAsStream("/cpc/system-tracks.bin"));

        romset.write(systemTracks);

        byte[] fsByteArray = fileSystem.asByteArray();
        LOGGER.debug("Filesystem size is " + fsByteArray.length);
        romset.write(fsByteArray);
        fillWithValue(romset, Integer.valueOf(0).byteValue(),
                Constants.SLOT_SIZE * 30 - (systemTracks.length + fsByteArray.length));
        romset.flush();

    }

    @Override
    public void removeArchive(Archive archive) {
        LOGGER.debug("removeArchive " + archive);
        fileSystem.removeArchive(archive);
        applicationContext.getArchiveList().remove(archive);
    }

    @Override
    public void addArchive(Archive archive) {
        LOGGER.debug("addArchive " + archive);
        fileSystem.addArchive(archive);
        applicationContext.getArchiveList().add(archive);
    }

    @Override
    public void clear() {
        fileSystem.clear();
        applicationContext.getArchiveList().clear();
    }
}
