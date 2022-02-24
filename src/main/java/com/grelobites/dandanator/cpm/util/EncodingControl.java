package com.grelobites.dandanator.cpm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

public class EncodingControl extends Control {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncodingControl.class);

    private String encoding;

    public EncodingControl(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException,
            IOException {
        String bundleName = toBundleName(baseName, locale);
        ResourceBundle bundle = null;
        if (format.equals("java.class")) {
            try {
                Class<?> bundleClass = loader.loadClass(bundleName);

                // If the class isn't a ResourceBundle subclass, throw a
                // ClassCastException.
                if (ResourceBundle.class.isAssignableFrom(bundleClass)) {
                    bundle = bundleClass.asSubclass(ResourceBundle.class).newInstance();
                } else {
                    throw new ClassCastException(bundleClass.getName() + " cannot be cast to ResourceBundle");
                }
            } catch (ClassNotFoundException e) {
                LOGGER.trace("Class not found", e);
            }
        } else if (format.equals("java.properties")) {
            final String resourceName = toResourceName(bundleName, "properties");
            final ClassLoader classLoader = loader;
            final boolean reloadFlag = reload;
            InputStream stream;
            try {
                stream = AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) () -> {
                    InputStream is = null;
                    if (reloadFlag) {
                        URL url = classLoader.getResource(resourceName);
                        if (url != null) {
                            URLConnection connection = url.openConnection();
                            if (connection != null) {
                                // Disable caches to get fresh data
                                // for
                                // reloading.
                                connection.setUseCaches(false);
                                is = connection.getInputStream();
                            }
                        }
                    } else {
                        is = classLoader.getResourceAsStream(resourceName);
                    }
                    return is;
                });
            } catch (PrivilegedActionException e) {
                throw (IOException) e.getException();
            }
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, encoding));
                } finally {
                    stream.close();
                }
            }
        } else {
            throw new IllegalArgumentException("unknown format: " + format);
        }
        return bundle;
    }
}
