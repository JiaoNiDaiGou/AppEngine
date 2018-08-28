package jiaonidaigou.appengine.common.utils;

import jiaonidaigou.appengine.common.model.RuntimeIOException;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

public class EncryptUtils {
    public static String md5(final byte[] bytes) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return DigestUtils.md5Hex(inputStream);
        } catch (Exception e) {
            throw new RuntimeIOException(e);
        }
    }

    public static String md5(final File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(inputStream);
        } catch (Exception e) {
            throw new RuntimeIOException(e);
        }
    }

    public static String md5(final URL url) {
        try (InputStream inputStream = url.openStream()) {
            return DigestUtils.md5Hex(inputStream);
        } catch (Exception e) {
            throw new RuntimeIOException(e);
        }
    }
}
