package jiaoni.common.test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import jiaoni.common.model.InternalIOException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class TestUtils {
    public static String readResourcesAsString(final String resourceName) {
        try (Reader reader = new InputStreamReader(Resources.getResource(resourceName).openStream(), Charsets.UTF_8)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    public static File readResourceAsFile(final String resourceName) {
        return new File(Resources.getResource(resourceName).getFile());
    }

    public static byte[] readResourceAsBytes(final String resourceName) {
        try {
            return ByteStreams.toByteArray(Resources.getResource(resourceName).openStream());
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }
}
