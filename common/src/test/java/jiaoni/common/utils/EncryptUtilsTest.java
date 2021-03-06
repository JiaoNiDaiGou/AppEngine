package jiaoni.common.utils;

import com.google.common.base.Charsets;
import org.junit.Test;

import java.util.Base64;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class EncryptUtilsTest {
    @Test
    public void testAes_encrypt_decrypt_withIv() {
        byte[] key = EncryptUtils.generateSecretKey();
        byte[] iv = EncryptUtils.generateIv();

        System.out.println(Base64.getEncoder().encodeToString(key));
        System.out.println(Base64.getEncoder().encodeToString(iv));

        byte[] rawBytes = "helloworld".getBytes(Charsets.UTF_8);
        byte[] encryptedBytes = EncryptUtils.aesEncrypt(key, iv, rawBytes);
        assertTrue(encryptedBytes.length > 0);

        byte[] decryptedBytes = EncryptUtils.aesDecrypt(key, iv, encryptedBytes);
        assertArrayEquals(rawBytes, decryptedBytes);
    }

    @Test
    public void testAes_encrypt_decrypt_withoutIv() {
        byte[] key = EncryptUtils.generateSecretKey();

        System.out.println(Base64.getEncoder().encodeToString(key));

        byte[] rawBytes = "helloworld".getBytes(Charsets.UTF_8);
        byte[] encryptedBytes = EncryptUtils.aesEncrypt(key, rawBytes);
        assertTrue(encryptedBytes.length > 0);

        byte[] decryptedBytes = EncryptUtils.aesDecrypt(key, encryptedBytes);
        assertArrayEquals(rawBytes, decryptedBytes);
    }
}
