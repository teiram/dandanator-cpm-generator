package com.grelobites.dandanator.cpm.model;

import java.util.List;

public interface FileSystem {
    void addArchive(Archive archive);

    void removeArchive(Archive archive);

    List<Archive> getArchiveList();

    int totalBytes();

    int freeBytes();

    int totalDirectoryEntries();

    int freeDirectoryEntries();

    byte[] getSector(int logicalTrack, int logicalSector);

    byte[] asByteArray();
}
