package com.grelobites.dandanator.cpm;

import com.grelobites.dandanator.cpm.model.FileSystemParameters;
import com.grelobites.dandanator.cpm.util.Util;
import javafx.scene.image.Image;

import java.io.IOException;
import java.util.Objects;

public class Constants {
    private static final String DEFAULT_VERSION = "2.0";

    public static final int SLOT_SIZE = 0x4000;
    public static final int MAX_EMS_FILE_SIZE = 0x4000 * 2;
	public static final int SPECTRUM_SCREEN_WIDTH = 256;
	public static final int SPECTRUM_SCREEN_HEIGHT = 192;
	public static final int SPECTRUM_SCREEN_SIZE = 6144;
	public static final int SPECTRUM_COLORINFO_SIZE = 768;
	public static final int SPECTRUM_FULLSCREEN_SIZE = SPECTRUM_SCREEN_SIZE +
			SPECTRUM_COLORINFO_SIZE;
	public static final int CPM_FILENAME_MAXLENGTH = 8;
	public static final int CPM_DEFAULT_USER_AREA = 0;
	public static final int CPM_FILEEXTENSION_MAXLENGTH = 3;
	public static final String FILE_EXTENSION_SEPARATOR = ".";
	public static final String EMPTY_STRING = "";
	public static final String NO_VALUE = "-";
	public static final String TEXT_ERROR_STYLE = "red-text";
	public static final String GREEN_BACKGROUND_STYLE = "green-background";
	public static final String NORMAL_BACKGROUND_STYLE = "normal-background";

    public static final Image SPECTRUM_ICON = new Image(Constants.class.getResourceAsStream("/color-logo.png"));
    public static final Image CPC_ICON = new Image(Constants.class.getResourceAsStream("/color-logo-cpc.png"));



    public static final FileSystemParameters ROMSET_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(232)
                    .withBlockSize(2048)
                    .withDirectoryEntries(128)
                    .withSectorsByTrack(9)
                    .withTrackCount(96)
                    .withSectorSize(512)
                    .withReservedTracks(2)
                    .build();
    // 9 * 512 = 4608 bytes per track
    // 4096 as scratch buffer (slot 0?)
    // Write procedure:
    // - Erase the scratch buffer
    // - Load sector by sector of 512 bytes using the source, except the one to write (7 sectors) - They could be on different sector
    // - Load the sector to be modified 475.136

    public static final FileSystemParameters CPC_ROMSET_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(234)
                    .withBlockSize(2048)
                    .withDirectoryEntries(128)
                    .withSectorsByTrack(9)
                    .withTrackCount(104)
                    .withSectorSize(512)
                    .withReservedTracks(2)
                    .build();

    public static final FileSystemParameters CPCPLUS_ROMSET_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(238)
                    .withBlockSize(2048)
                    .withDirectoryEntries(128)
                    .withSectorsByTrack(9)
                    .withTrackCount(106)
                    .withSectorSize(512)
                    .withReservedTracks(0)
                    .build();

    public static final FileSystemParameters PLUS3_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(175)
                    .withBlockSize(1024)
                    .withDirectoryEntries(64)
                    .withSectorsByTrack(9)
                    .withTrackCount(40)
                    .withSectorSize(512)
                    .withReservedTracks(1)
                    .build();

    public static final FileSystemParameters CPC_SYSTEM_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(180)
                    .withBlockSize(1024)
                    .withDirectoryEntries(64)
                    .withSectorsByTrack(9)
                    .withTrackCount(40)
                    .withSectorSize(512)
                    .withReservedTracks(2)
                    .build();

    public static final FileSystemParameters CPC_DATA_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(180)
                    .withBlockSize(1024)
                    .withDirectoryEntries(64)
                    .withSectorsByTrack(9)
                    .withTrackCount(40)
                    .withSectorSize(512)
                    .withReservedTracks(0)
                    .build();


    private static final String DEFAULT_BOOT_IMAGE_RESOURCE = "bootImage.scr";
    private static final String EMS_FILE_RESOURCE = "S10CPM3.EMS";
    private static final String THEME_RESOURCE = "view/theme.css";
    public static final int ROMSET_HEADER_SIZE = 4;
    public static final byte[] ROMSET_HEADER = new byte[] {(byte) 0xF3, (byte) 0xC3, (byte) 0x00, (byte) 0x01};


    private static byte[] DEFAULT_BOOT_IMAGE;
    private static byte[] EMS_FILE;
    private static String THEME_RESOURCE_URL;
    public static final long ROMSET_SIZE = SLOT_SIZE * 32;


    public static String currentVersion() {
        String version = Constants.class.getPackage()
                .getImplementationVersion();
        if (version == null) {
            version = DEFAULT_VERSION;
        }
        return version;
    }

    public static byte[] getDefaultBootScreen() throws IOException {
        if (DEFAULT_BOOT_IMAGE == null) {
            DEFAULT_BOOT_IMAGE = Util.fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_BOOT_IMAGE_RESOURCE),
                    Constants.SPECTRUM_FULLSCREEN_SIZE);
        }
        return DEFAULT_BOOT_IMAGE;
    }

    public static byte[] getEmsFileByteStream() throws IOException {
        if (EMS_FILE == null) {
            EMS_FILE = Util.fromInputStream(
                    Objects.requireNonNull(Constants.class.getClassLoader()
                            .getResourceAsStream(EMS_FILE_RESOURCE)));
        }
        return EMS_FILE;
    }

    public static String getThemeResourceUrl() {
        if (THEME_RESOURCE_URL == null) {
            THEME_RESOURCE_URL = Constants.class.getResource(THEME_RESOURCE)
                    .toExternalForm();
        }
        return THEME_RESOURCE_URL;
    }

}
