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
    private static final String DEFAULT_BOOT_IMAGE_RESOURCE = "menu.scr";
    private static final String THEME_RESOURCE = "view/theme.css";

    private static byte[] DEFAULT_BOOT_IMAGE;

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

    public static String getThemeResourceUrl() {
        if (THEME_RESOURCE_URL == null) {
            THEME_RESOURCE_URL = Constants.class.getResource(THEME_RESOURCE)
                    .toExternalForm();
        }
        return THEME_RESOURCE_URL;
    }

}
