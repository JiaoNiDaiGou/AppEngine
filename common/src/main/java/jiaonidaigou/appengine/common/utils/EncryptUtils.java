package jiaonidaigou.appengine.common.utils;

import jiaonidaigou.appengine.common.model.InternalIOException;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtils {
    private static final int AES_KEY_SIZE = 256;
    private static final int AES_IV_LENGTH = 16;

    public static String base64Encode(final String str) {
        if (str == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(str.getBytes(Charsets.UTF_8));
    }

    public static String base64Decode(final String str) {
        if (str == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(str), Charsets.UTF_8);
    }

    public static String md5(final byte[] bytes) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return DigestUtils.md5Hex(inputStream);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    public static String md5(final File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(inputStream);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    public static String md5(final URL url) {
        try (InputStream inputStream = url.openStream()) {
            return DigestUtils.md5Hex(inputStream);
        } catch (Exception e) {
            throw new InternalIOException(e);
        }
    }

    public static byte[] generateSecretKey() {
        getAesCipherWithIv();
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            return keyGen.generateKey().getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] generateIv() {
        getAesCipherWithIv();
        try {
            byte[] result = new byte[AES_IV_LENGTH];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesEncrypt(final byte[] key, final byte[] data) {
        Cipher cipher = getAesCipherWithoutIv();
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesDecrypt(final byte[] key, final byte[] data) {
        Cipher cipher = getAesCipherWithoutIv();
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        try {
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesEncrypt(final byte[] key, final byte[] iv, final byte[] data) {
        Cipher cipher = getAesCipherWithIv();
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesDecrypt(final byte[] key, final byte[] iv, final byte[] data) {
        Cipher cipher = getAesCipherWithIv();
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        try {
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Cipher getAesCipherWithIv() {
        try {
            if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
                throw new IllegalStateException("This environment doesn't support 256 bit key length. Please update JCE policy.");
            }
            return Cipher.getInstance("AES/CBC/PKCS5PADDING");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("this environment doesn't have AES supported", e);
        }
    }

    private static Cipher getAesCipherWithoutIv() {
        try {
            if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
                throw new IllegalStateException("This environment doesn't support 256 bit key length. Please update JCE policy.");
            }
            return Cipher.getInstance("AES/ECB/PKCS5PADDING");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("this environment doesn't have AES supported", e);
        }
    }
}
