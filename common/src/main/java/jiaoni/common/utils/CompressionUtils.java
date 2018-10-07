package jiaoni.common.utils;

import jiaoni.common.model.InternalIOException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class CompressionUtils {
    public static byte[] gzipCompress(final byte[] bytes) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream outputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new InternalIOException(e);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
