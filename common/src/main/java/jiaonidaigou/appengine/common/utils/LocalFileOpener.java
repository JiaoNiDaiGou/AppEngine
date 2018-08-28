package jiaonidaigou.appengine.common.utils;

import jiaonidaigou.appengine.common.model.RuntimeIOException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Open a file locally.
 */
public class LocalFileOpener {
    public static void openFile(final File file) {
        try {
            Runtime.getRuntime().exec("open " + file.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static void openFile(final String filePath) {
        openFile(new File(filePath));
    }

    public static void openFileContent(final String fileNameOnly, final byte[] bytes) {
        File file = new File(Environments.LOCAL_TEMP_DIR + fileNameOnly);
        try {
            Files.write(file.toPath(), bytes);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        openFile(file);
    }
}
