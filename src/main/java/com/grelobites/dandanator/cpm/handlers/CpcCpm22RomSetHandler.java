package com.grelobites.dandanator.cpm.handlers;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.filesystem.CpmFileSystem;
import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import com.grelobites.dandanator.cpm.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CpcCpm22RomSetHandler extends CpcCpmRomSetHandlerBase implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpcCpm22RomSetHandler.class);

    public CpcCpm22RomSetHandler(ApplicationContext context) {
        this.applicationContext = context;
        this.fileSystem = new CpmFileSystem(Constants.CPC_ROMSET_FS_PARAMETERS);
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
    public void exportRomSet(OutputStream romset) throws IOException {
        LOGGER.debug("exportRomSet " + romset);

        byte[] loader = Util.fromInputStream(Objects.requireNonNull(CpcCpm22RomSetHandler.class
                .getResourceAsStream("/cpc/loader.bin")));

        byte[] patchedAmsdos = Util.fromInputStream(Objects.requireNonNull(CpcCpm22RomSetHandler.class
                .getResourceAsStream("/cpc/patched-amsdos.rom")));

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

        byte[] systemTracks = Util.fromInputStream(Objects.requireNonNull(CpcCpm22RomSetHandler.class
                .getResourceAsStream("/cpc/system-tracks.bin")));

        romset.write(systemTracks);

        byte[] fsByteArray = fileSystem.asByteArray();
        LOGGER.debug("Filesystem size is " + fsByteArray.length);
        romset.write(fsByteArray);
        fillWithValue(romset, Integer.valueOf(0).byteValue(),
                Constants.SLOT_SIZE * 30 - (systemTracks.length + fsByteArray.length));
        romset.flush();

    }

    @Override
    public String getSystemArchivePath() {
        return Constants.CPC_CPM22_RESOURCES_PATH;
    }

    @Override
    public List<String> getSystemArchives() {
        return Constants.CPC_CPM22_RESOURCE_NAMES;
    }
}
