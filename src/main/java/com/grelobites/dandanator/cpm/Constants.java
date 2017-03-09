package com.grelobites.dandanator.cpm;

import com.grelobites.dandanator.cpm.util.Util;

import java.io.IOException;

public class Constants {
    private static final String DEFAULT_VERSION = "1.0";

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

    private static final String DEFAULT_BOOT_IMAGE_RESOURCE = "bootImage.scr";
    private static final String DEFAULT_EMS_BINARY_RESOURCE = "S10CPM3.EMS";
    private static final String THEME_RESOURCE = "view/theme.css";


    private static byte[] DEFAULT_BOOT_IMAGE;
    private static byte[] DEFAULT_EMS_BINARY;

    private static String THEME_RESOURCE_URL;

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

    public static byte[] getDefaultEmsBinary() throws IOException {
        if (DEFAULT_EMS_BINARY == null) {
            DEFAULT_EMS_BINARY = Util.fromInputStream(
                    Constants.class.getClassLoader()
                            .getResourceAsStream(DEFAULT_EMS_BINARY_RESOURCE));
        }
        return DEFAULT_EMS_BINARY;
    }


    public static String getThemeResourceUrl() {
        if (THEME_RESOURCE_URL == null) {
            THEME_RESOURCE_URL = Constants.class.getResource(THEME_RESOURCE)
                    .toExternalForm();
        }
        return THEME_RESOURCE_URL;
    }

}
