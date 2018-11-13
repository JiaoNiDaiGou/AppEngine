package jiaoni.common.utils;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import jiaoni.common.model.InternalIOException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.UUID;

public class FileUtils {
    public static List<String> readLinesFromResource(final String resourceName) {
        try (Reader reader = new InputStreamReader(Resources.getResource(resourceName).openStream(), Charsets.UTF_8)) {
            return CharStreams.readLines(reader);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    public static void writeLocalFileAndOpen(final String content) {
        String path = Envs.getLocalTmpDir() + UUID.randomUUID().toString();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path), Charsets.UTF_8)) {
            writer.write(content);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    public static void writeLinesLocalFileAndOpen(final List<String> content) {
        String path = Envs.getLocalTmpDir() + UUID.randomUUID().toString();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path), Charsets.UTF_8)) {
            writer.write(String.join("\n", content));
        } catch (Exception e) {
            throw new InternalIOException(e);
        }

        try {
            Runtime.getRuntime().exec("open " + path);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }

    public static void writeLocalImageAndOpen(final byte[] bytes, final String ext) {
        String path = Envs.getLocalTmpDir() + UUID.randomUUID().toString() + "." + ext;
        try (OutputStream outputStream = new FileOutputStream(path)) {
            outputStream.write(bytes);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
        try {
            Runtime.getRuntime().exec("open " + path);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
    }
}
