package com.grelobites.dandanator.cpm.dsk;

import com.grelobites.dandanator.cpm.filesystem.CpmFileSystem;
import com.grelobites.dandanator.cpm.model.FileSystemParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DskImportTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskImportTest.class);
    @Test
    public void dskImportToFileSystem() throws IOException {
        DskContainer container = DskContainer.fromInputStream(
                DskImportTest.class.getResourceAsStream("/cpm-plus-2.dsk"));

        FileSystemParameters parameters = DskUtil.guessFileSystemParameters(container);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        container.dumpRawData(bos);
        CpmFileSystem fileSystem = CpmFileSystem.fromByteArray(bos.toByteArray(), parameters);
        LOGGER.debug("File system is " + fileSystem);
    }
}
