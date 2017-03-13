package com.grelobites.dandanator.cpm.util;

import com.grelobites.dandanator.cpm.ApplicationContext;
import com.grelobites.dandanator.cpm.Constants;
import com.grelobites.dandanator.cpm.filesystem.CpmFileSystem;
import com.grelobites.dandanator.cpm.model.Archive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArchiveUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveUtil.class);

    private static final Set<Character> INVALID_CPM_CHARS = new HashSet<>(Arrays.asList(
            new Character[] {
            '<', '>', '.', ',', ';',
            ':' ,'=' ,'?', '*', '[', ']'}));

    private static final char REPLACEMENT_CPM_CHAR = '_';

    public static Pair<String, String> getBestName(String name) {
        return getBestNameWithSuffix(name, null);
    }

    private static String cutToLength(String value, int length) {
        return value.length() < length ? value : value.substring(0, length);
    }

    public static Pair<String, String> getBestNameWithSuffix(String name, String suffix) {
        int lastSeparator = name.lastIndexOf(Constants.FILE_EXTENSION_SEPARATOR);

        String candidateName = lastSeparator > -1 ? name.substring(0, lastSeparator) : name;
        String candidateExtension = lastSeparator > -1 ? name.substring(lastSeparator + 1) : "";

        candidateName = cutToLength(candidateName,
                suffix != null ? Constants.CPM_FILENAME_MAXLENGTH - suffix.length() :
                        Constants.CPM_FILENAME_MAXLENGTH).toUpperCase();
        candidateExtension = cutToLength(candidateExtension, Constants.CPM_FILEEXTENSION_MAXLENGTH)
                .toUpperCase();
        if (suffix != null) {
            candidateName += suffix;
        }
        return new Pair(candidateName, candidateExtension);
    }

    public static boolean isNameInUseAtUserArea(Pair<String, String> name, int userArea, ApplicationContext context) {
        return context.getArchiveList().filtered(a ->
                a.getUserArea() == userArea &&
                a.getName().equals(name.left()) &&
                a.getExtension().equals(name.right())).size() > 0;
    }

    public static Pair<String, String> calculateArchiveName(File file, ApplicationContext context) {
        Pair<String, String> candidate = getBestName(file.getName());
        int index = 0;
        while (isNameInUseAtUserArea(candidate, context.getCurrentUserArea(), context)) {
            LOGGER.info("Name " + candidate + " already in use");
            candidate = getBestNameWithSuffix(file.getName(), String.format("%02d", index++));
        }
        return candidate;
    }


    public static FileType guessFileType(File file) {
        //TODO: Implement properly for DSK and ROMSET detection
        if (file.getName().endsWith(".raw")) {
            return FileType.RAWFS;
        } else if (file.getName().endsWith(".dsk")) {
            return FileType.DSK;
        } else if (file.getName().endsWith(".rom") && file.length() == 16386 * 32) {
            return FileType.ROMSET;
        } else {
            return FileType.ARCHIVE;
        }
    }

    public static Archive createArchiveFromFile(File file, ApplicationContext context) throws IOException {
        Pair<String, String> name = calculateArchiveName(file, context);
        Archive archive = new Archive(name.left(), name.right(), context.getCurrentUserArea(),
                Files.readAllBytes(file.toPath()));
        return archive;
    }

    public static List<Archive> getArchivesInFile(ApplicationContext context, File file) throws IOException {
        LOGGER.debug("getArchivesInFile " + file);
        switch (guessFileType(file)) {
            case ARCHIVE:
                Archive archive = createArchiveFromFile(file, context);
                return Collections.singletonList(archive);
            case RAWFS:
                CpmFileSystem fs = CpmFileSystem.fromByteArray(Files.readAllBytes(file.toPath()),
                        Constants.PLUS3_FS_PARAMETERS);
                return fs.getArchiveList();
            default:
                throw new IllegalArgumentException("Not implemented yet");
        }
    }

    public static void exportAsFile(Archive sourceArchive, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(sourceArchive.getData());
        }
    }

    public static String toCpmValidName(String newValue, int maxLength) {
        StringBuilder result = new StringBuilder();
        for (Character c : newValue.toCharArray()) {
            result.append(INVALID_CPM_CHARS.contains(c) ? REPLACEMENT_CPM_CHAR : c);
            if (result.length() == maxLength) {
                break;
            }
        }
        return result.toString().toUpperCase();
    }
}
