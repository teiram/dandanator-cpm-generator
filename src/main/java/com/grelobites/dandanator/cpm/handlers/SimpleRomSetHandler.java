package com.grelobites.dandanator.cpm.handlers;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.Preferences;
import com.grelobites.dandanator.cpm.filesystem.CpmFileSystem;
import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import com.grelobites.dandanator.cpm.util.LocaleUtil;
import com.grelobites.dandanator.cpm.util.Util;
import javafx.beans.InvalidationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SimpleRomSetHandler implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRomSetHandler.class);

    private ApplicationContext applicationContext;

    private CpmFileSystem fileSystem;

    private InvalidationListener romUsageUpdater = e -> updateRomUsage();

    public SimpleRomSetHandler(ApplicationContext context) {
        this.applicationContext = context;
        this.fileSystem = new CpmFileSystem(Constants.ROMSET_FS_PARAMETERS);
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

    @Override
    public void mergeRomSet(InputStream romset) throws IOException {
        LOGGER.debug("mergeRomSet " + romset);
        //Skip EMS and sector zero data
        romset.skip(16384 * 3);
        byte[] fsData = new byte[16384 * 29];
        romset.read(fsData);
        CpmFileSystem fileSystem = CpmFileSystem.fromByteArray(
                fsData, Constants.ROMSET_FS_PARAMETERS);
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
        ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(SimpleRomSetHandler.class
                .getResourceAsStream("/loader.bin")));
        buffer.order(ByteOrder.LITTLE_ENDIAN).position(4);
        buffer.putShort(Integer.valueOf(1).shortValue()); //TODO: Set proper version here

        int offset = buffer.array().length;
        LOGGER.debug("Writing driver at offset " + offset);

        byte[] driver = Util.fromInputStream(SimpleRomSetHandler.class
            .getResourceAsStream("/dandanator_fid_driver.bin"));
        buffer.putShort(Integer.valueOf(offset).shortValue());
        buffer.putShort(Integer.valueOf(driver.length).shortValue());

        offset += driver.length;
        LOGGER.debug("Writing screen at offset " + offset);

        byte[] screen = Preferences.getInstance().getBootImage();
        buffer.putShort(Integer.valueOf(offset).shortValue());
        buffer.putShort(Integer.valueOf(screen.length).shortValue());

        romset.write(buffer.array());
        romset.write(driver);
        romset.write(screen);

        offset += screen.length;

        fillWithValue(romset, (byte) 0, Constants.SLOT_SIZE - offset);

        //Write EMS on second slot
        offset = Constants.SLOT_SIZE;

        byte[] emsFile = Preferences.getInstance().getEmsBinary();
        romset.write(emsFile);
        offset += emsFile.length;

        fillWithValue(romset, (byte) 0, (Constants.SLOT_SIZE * 3) - offset);

        romset.write(fileSystem.asByteArray());
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
