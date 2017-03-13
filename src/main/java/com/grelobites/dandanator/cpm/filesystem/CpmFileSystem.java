package com.grelobites.dandanator.cpm.filesystem;

import com.grelobites.dandanator.cpm.model.Archive;
import com.grelobites.dandanator.cpm.model.FileSystem;
import com.grelobites.dandanator.cpm.model.FileSystemParameters;
import com.grelobites.dandanator.cpm.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CpmFileSystem implements FileSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpmFileSystem.class);

    private final FileSystemParameters parameters;

    private List<Archive> archiveList;
    private int freeBytes;
    private int totalBytes;
    private int freeDirectoryEntries;

    public CpmFileSystem(FileSystemParameters parameters) {
        this.parameters = parameters;
        this.archiveList = new ArrayList<>();

        this.totalBytes = parameters.getBlockCount() * parameters.getBlockSize();
        this.freeBytes = this.totalBytes - parameters.getDirectoryEntries() * CpmConstants.DIRECTORY_ENTRY_SIZE;
        this.freeDirectoryEntries = parameters.getDirectoryEntries();
    }

    private int getNeededDirectoryEntries(Archive archive) {
        return (archive.getSize() >> CpmConstants.LOGICAL_EXTENT_MASK)
                + ((archive.getSize() & (CpmConstants.LOGICAL_EXTENT_SIZE - 1)) != 0 ? 1 : 0);
    }

    private int getNeededBytes(Archive archive) {
        return Util.roundToNearestMultiple(archive.getSize(), parameters.getBlockSize());
    }

    @Override
    public void addArchive(Archive archive) {
        if (archiveList.contains(archive)) {
            throw new IllegalArgumentException("Archive already added to filesystem");
        } else {
            int neededBytes = getNeededBytes(archive);
            if (neededBytes > freeBytes) {
                throw new IllegalArgumentException("Not enough free space to add file");
            }
            int neededDirectoryEntries = getNeededDirectoryEntries(archive);
            if (neededDirectoryEntries > freeDirectoryEntries()) {
                throw new IllegalArgumentException("Not enough free directory entries");
            }
            LOGGER.debug("Archive " + archive + " needs " + neededBytes + " bytes "
                + " and " + neededDirectoryEntries + " directory entries");
            archiveList.add(archive);
            freeBytes -= neededBytes;
            freeDirectoryEntries -= neededDirectoryEntries;
        }
    }

    @Override
    public void removeArchive(Archive archive) {
        if (archiveList.contains(archive)) {
            freeBytes += getNeededBytes(archive);
            freeDirectoryEntries += getNeededDirectoryEntries(archive);
            archiveList.remove(archive);
        } else {
            throw new IllegalArgumentException("Archive " + archive + " is not in the filesystem");
        }
    }

    @Override
    public List<Archive> getArchiveList() {
        return archiveList;
    }

    @Override
    public int totalBytes() {
        return totalBytes;
    }

    @Override
    public int freeBytes() {
        return freeBytes;
    }

    @Override
    public int totalDirectoryEntries() {
        return parameters.getDirectoryEntries();
    }

    @Override
    public int freeDirectoryEntries() {
        return freeDirectoryEntries;
    }

    @Override
    public byte[] asByteArray() {
        return new ByteArrayExporter().export();
    }

    @Override
    public byte[] getSector(int logicalTrack, int logicalSector) {
        int offset = (logicalTrack * parameters.getSectorsByTrack() + logicalSector) * parameters.getSectorSize();
        //TODO: Optimize if finally needed
        return Arrays.copyOfRange(asByteArray(), offset, offset + parameters.getSectorSize());
    }

    private static Archive getArchiveFromDirectoryEntry(CpmDirectoryEntry[] directoryEntries, int index,
                                                        byte[] data, FileSystemParameters parameters) {
        final CpmDirectoryEntry entry = directoryEntries[index];
        if (entry.getUserArea() != CpmConstants.UNUSED_ENTRY_USER) {
            LOGGER.debug("Getting extents for archive " + entry.getName()
                + "." + entry.getExtension() + " in user area " + entry.getUserArea());
            int extentNumber = entry.getExtent();
            List<CpmDirectoryEntry> fileExtents = Stream.of(directoryEntries).filter(e ->
                    entry.getName().equals(e.getName()) &&
                            entry.getExtension().equals(e.getExtension()) &&
                            entry.getUserArea() == e.getUserArea())
                    .collect(Collectors.toList());
            fileExtents.sort(Comparator.comparingInt(CpmDirectoryEntry::getExtent));
            LOGGER.debug("File has " + fileExtents.size() + " extents");
            if (extentNumber == fileExtents.get(0).getExtent()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                //Assuming that the blocks are ordered
                for (CpmDirectoryEntry extent : fileExtents) {
                    LOGGER.debug("Processing extent " + extent);
                    int records = extent.getRecordCount();
                    for (int block : extent.getAllocatedBlocks()) {
                        if (block > 0) {
                            LOGGER.debug("Fetching data for block " + block);
                            try {
                                int from = block * parameters.getBlockSize();
                                int to = Math.min(
                                        (block + 1) * parameters.getBlockSize(),
                                        from + (records << 7));
                                LOGGER.debug("Calculated data range from " + from + " to " + to
                                    + ", size: " + (to - from) + " bytes");
                                bos.write(Arrays.copyOfRange(data, from, to));
                                records -= (to - from) >> 7;
                            } catch (Exception e) {
                                LOGGER.error("Getting file data", e);
                            }
                        }
                    }
                }
                return new Archive(entry.getName(), entry.getExtension(), entry.getUserArea(),
                        bos.toByteArray(), entry.getFlags());

            } else {
                LOGGER.debug("Not the first extent of file. Skipping");
            }
        }

        return null;
    }

    public static CpmFileSystem fromByteArray(byte[] data, FileSystemParameters parameters) {
        CpmDirectoryEntry[] directoryEntries = new CpmDirectoryEntry[parameters.getDirectoryEntries()];
        int offset = parameters.getReservedTracks() * parameters.getSectorsByTrack() * parameters.getSectorSize();
        for (int i = 0; i < directoryEntries.length; i++) {
            directoryEntries[i] = CpmDirectoryEntry.fromByteArray(parameters, data, offset);
            offset += CpmConstants.DIRECTORY_ENTRY_SIZE;
        }
        CpmFileSystem fileSystem = new CpmFileSystem(parameters);
        //Recreate now the files
        for (int i = 0; i < directoryEntries.length; i++) {
            Archive archive = getArchiveFromDirectoryEntry(directoryEntries, i, data, parameters);
            if (archive != null) {
                LOGGER.debug("Adding archive " + archive);
                fileSystem.addArchive(archive);
            }
        }

        return fileSystem;
    }

    private class ByteArrayExporter {
        private int currentDirectoryEntry;
        private CpmDirectoryEntry[] directoryEntries;
        private int currentBlock;
        private byte[] data;

        private void exportArchive(Archive archive) {
            int remaining = archive.getSize();
            int extent = 0;
            int dataOffset = 0;
            do {
                int realRequestedSize = Math.min(remaining, CpmConstants.LOGICAL_EXTENT_SIZE);
                int requestedSize = Util.roundToNearestMultiple(realRequestedSize, parameters.getBlockSize());
                int[] allocatedBlocks = new int[parameters.getBlockCount() > 255 ? 8 : 16];
                int neededBlocks = requestedSize / parameters.getBlockSize();
                for (int i = 0; i < neededBlocks; i++) {
                    allocatedBlocks[i] = currentBlock;
                    byte[] blockData = Util.paddedByteArray(archive.getData(), dataOffset,
                            parameters.getBlockSize(), CpmConstants.EMPTY_BYTE);
                    System.arraycopy(blockData, 0, data, currentBlock * parameters.getBlockSize(),
                            parameters.getBlockSize());
                    currentBlock++;
                    dataOffset += parameters.getBlockSize();
                }
                CpmDirectoryEntry directoryEntry = new CpmDirectoryEntry(
                        archive.getName(),
                        archive.getExtension(),
                        archive.getUserArea(),
                        archive.getFlags(),
                        extent++,
                        allocatedBlocks,
                        Util.roundToNearestMultiple(realRequestedSize, CpmConstants.RECORD_SIZE) / CpmConstants.RECORD_SIZE,
                        0);
                LOGGER.debug("Creating new directory entry " + directoryEntry);
                directoryEntries[currentDirectoryEntry++] = directoryEntry;
                remaining -= requestedSize;
            } while (remaining > 0);
        }

        private byte[] getDirectoryByteArray(int size) {
            byte[] directory = new byte[size];
            Arrays.fill(directory, CpmConstants.EMPTY_BYTE);
            for (int entryIndex = 0; entryIndex < directoryEntries.length; entryIndex++) {
                CpmDirectoryEntry entry = directoryEntries[entryIndex];
                if (entry != null) {
                    System.arraycopy(entry.asByteArray(parameters), 0, directory,
                            entryIndex * CpmConstants.DIRECTORY_ENTRY_SIZE,
                            CpmConstants.DIRECTORY_ENTRY_SIZE);
                } else {
                    break;
                }
            }
            return directory;
        }

        public byte[] export() {
            data = new byte[parameters.getBlockSize() * parameters.getBlockCount()];
            directoryEntries = new CpmDirectoryEntry[parameters.getDirectoryEntries()];
            int directorySize = Util.roundToNearestMultiple(
                    parameters.getDirectoryEntries() * CpmConstants.DIRECTORY_ENTRY_SIZE,
                    parameters.getBlockSize());
            currentBlock = directorySize / parameters.getBlockSize();
            currentDirectoryEntry = 0;
            for (Archive archive : archiveList) {
                exportArchive(archive);
            }
            System.arraycopy(getDirectoryByteArray(directorySize), 0, data, 0, directorySize);
            return data;
        }
    }

    @Override
    public String toString() {
        return "CpmFileSystem{" +
                "parameters=" + parameters +
                ", archiveList=" + archiveList +
                ", freeBytes=" + freeBytes +
                ", totalBytes=" + totalBytes +
                ", freeDirectoryEntries=" + freeDirectoryEntries +
                '}';
    }
}
