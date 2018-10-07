package jiaoni.common.utils;

import jiaoni.common.model.InternalIOException;

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
            throw new InternalIOException(e);
        }
    }

    public static void openFile(final String filePath) {
        openFile(new File(filePath));
    }

    public static void openFileContent(final String fileNameOnly, final byte[] bytes) {
        File file = new File(Envs.getLocalTmpDir() + fileNameOnly);
        try {
            Files.write(file.toPath(), bytes);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        openFile(file);
    }
}
