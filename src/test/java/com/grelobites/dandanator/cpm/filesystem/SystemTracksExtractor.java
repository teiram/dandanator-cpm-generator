package com.grelobites.dandanator.cpm.filesystem;

import com.grelobites.dandanator.cpm.dsk.DskContainer;
import com.grelobites.dandanator.cpm.dsk.Track;
import com.grelobites.dandanator.cpm.model.FileSystemParameters;
import com.grelobites.dandanator.cpm.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SystemTracksExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemTracksExtractor.class);

    @Test
    public void extractSystemTracks() throws IOException {
        DskContainer dsk = DskContainer.fromInputStream(SystemTracksExtractor.class
                .getResourceAsStream("/cpm22-tpa-178.dsk"));
        try (FileOutputStream fos = new FileOutputStream("target/system-tracks.bin")) {
            for (Track track : Arrays.asList(dsk.getTrack(0), dsk.getTrack(1))) {
                track.orderedSectorList().forEach(sector -> {
                    try {
                        fos.write(track.getSectorData(sector));
                    } catch (Exception e) {
                        LOGGER.error("Extracting sector data", e);
                    }
                });
            }
        }
    }
}
