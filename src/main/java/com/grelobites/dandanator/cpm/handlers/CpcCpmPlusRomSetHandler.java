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
import java.util.Objects;

public class CpcCpmPlusRomSetHandler extends CpcCpmRomSetHandlerBase implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpcCpmPlusRomSetHandler.class);

    public CpcCpmPlusRomSetHandler(ApplicationContext context) {
        this.applicationContext = context;
        this.fileSystem = new CpmFileSystem(Constants.CPCPLUS_ROMSET_FS_PARAMETERS);
    }

    @Override
    public void mergeRomSet(InputStream romset) throws IOException {
        LOGGER.debug("mergeRomSet " + romset);
        //Skip EMS and sector zero data
        romset.skip(16384 * 2);
        byte[] fsData = new byte[16384 * 30];
        romset.read(fsData);
        CpmFileSystem fileSystem = CpmFileSystem.fromByteArray(
                fsData, Constants.CPCPLUS_ROMSET_FS_PARAMETERS);
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

    @Override
    public void exportRomSet(OutputStream romset) throws IOException {
        LOGGER.debug("exportRomSet " + romset);

        byte[] loader = Util.fromInputStream(Objects.requireNonNull(CpcCpmPlusRomSetHandler.class
                .getResourceAsStream("/cpc/loaderplus.bin")));

        romset.write(loader);
        fillWithValue(romset, Integer.valueOf(0).byteValue(), (Constants.SLOT_SIZE * 2) - loader.length);

        byte[] fsByteArray = fileSystem.asByteArray();
        LOGGER.debug("Filesystem size is " + fsByteArray.length);
        romset.write(fsByteArray);
        fillWithValue(romset, Integer.valueOf(0).byteValue(),
                Constants.SLOT_SIZE * 30 - fsByteArray.length);
        romset.flush();

    }
}
