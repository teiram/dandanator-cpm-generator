package com.grelobites.dandanator.cpm.handlers;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.model.RomSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SimpleRomSetHandler implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRomSetHandler.class);

    private ApplicationContext applicationContext;

    public SimpleRomSetHandler(ApplicationContext context) {
        this.applicationContext = context;
    }

    @Override
    public void mergeRomSet(InputStream romset) throws IOException {
        LOGGER.debug("mergeRomSet " + romset);
    }

    @Override
    public void importRomSet(InputStream romset) throws IOException {
        LOGGER.debug("importRomSet " + romset);
    }

    @Override
    public void exportRomSet(OutputStream romset) throws IOException {
        LOGGER.debug("exportRomSet " + romset);

    }

    @Override
    public void removeArchive(Archive archive) {
        LOGGER.debug("removeArchive " + archive);
        applicationContext.getArchiveList().remove(archive);
    }

    @Override
    public void addArchive(Archive archive) {
        LOGGER.debug("addArchive " + archive);
        applicationContext.getArchiveList().add(archive);
    }
}
