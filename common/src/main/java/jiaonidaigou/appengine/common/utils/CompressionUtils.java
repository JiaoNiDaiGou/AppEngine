package jiaonidaigou.appengine.common.utils;

import jiaonidaigou.appengine.common.model.RuntimeIOException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class CompressionUtils {
    public static byte[] gzipCompress(final byte[] bytes) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream outputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
