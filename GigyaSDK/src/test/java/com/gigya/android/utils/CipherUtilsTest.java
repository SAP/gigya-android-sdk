package com.gigya.android.utils;

import com.gigya.android.sdk.utils.CipherUtils;

import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CipherUtilsTest {

    private SecretKey secretKey = new SecretKeySpec(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, "AES");

    @Test
    public void testBytesToString() {
        // Arrange
        final byte[] expectedRead = new byte[]{-127, -126, -125};
        // Act
        final String str = CipherUtils.bytesToString(expectedRead);
        // Assert
        assertEquals(str, "f1if7");
    }

    @Test
    public void testStringToBytes() {
        // Arrange
        final String str = "f1if7";
        // Act
        final byte[] bytes = CipherUtils.stringToBytes(str);
        // Assert
        assertArrayEquals(bytes, new byte[]{-127, -126, -125});
    }

    @Test
    public void testEncrypt() {
        // Act
        final String encrypted = CipherUtils.encrypt("toEncrypt", "AES", secretKey);
        // Assert
        assertEquals(encrypted, "rcosq1vij78dtegb0ozgfg6my");
    }

    @Test
    public void testDecrypt() {
        // Act
        final String decrypted = CipherUtils.decrypt("rcosq1vij78dtegb0ozgfg6my", "AES", secretKey);
        // Assert
        assertEquals(decrypted, "toEncrypt");
    }

    @Test
    public void testToBytes() {
        // Act
        final byte[] bytes = CipherUtils.toBytes(new char[]{ 't', 'e', 's', 't'});
        // Assert
        assertArrayEquals(new byte[]{116, 101, 115, 116}, bytes);
    }

    @Test
    public void testToChars() {
        // Act
        final char[] chars = CipherUtils.toChars(new byte[]{116, 101, 115, 116});
        // Assert
        assertArrayEquals(new char[]{'t', 'e', 's', 't'}, chars);
    }
}
