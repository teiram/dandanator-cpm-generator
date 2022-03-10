package com.grelobites.dandanator.cpm.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface RomSetHandler {
    
    void mergeRomSet(InputStream romset) throws IOException;
    
    void importRomSet(InputStream romset) throws IOException;
    
    void exportRomSet(OutputStream romset) throws IOException;

    void clear();

    void bind();

    void unbind();

    void removeArchive(Archive archive);

    void addArchive(Archive archive);

    String getSystemArchivePath();

    List<String> getSystemArchives();
}
