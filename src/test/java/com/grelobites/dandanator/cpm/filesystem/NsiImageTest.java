package com.grelobites.dandanator.cpm.filesystem;

import com.grelobites.dandanator.cpm.model.FileSystemParameters;
import com.grelobites.dandanator.cpm.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NsiImageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NsiImageTest.class);

    @Test
    public void createFromExistingFile() throws IOException {
        FileSystemParameters parameters = FileSystemParameters.newBuilder()
                .withBlockCount(174)
                .withBlockSize(1024)
                .withDirectoryEntries(64)
                .withSectorSize(512)
                .withTrackCount(40)
                .withSectorsByTrack(9)
                .build();
        CpmFileSystem fs1 = CpmFileSystem.fromByteArray(
                Util.fromInputStream(NsiImageTest.class.getResourceAsStream("/cpm-1.raw")),
                parameters);

        CpmFileSystem fs2 = CpmFileSystem.fromByteArray(fs1.asByteArray(), parameters);
        LOGGER.debug("Created file system: " + fs1);
        LOGGER.debug("Recreated file system: " + fs2);

    }
}
