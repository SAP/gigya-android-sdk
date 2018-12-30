package com.gigya.android;

import com.gigya.android.sdk.utils.CipherUtils;

import org.junit.Test;

import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CipherUtilsTest {

    private SecretKey secretKey = new SecretKeySpec(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, "AES");

    @Test
    public void testBytesToString() {
        final byte[] expectedRead = new byte[]{-127, -126, -125};
        final String str = CipherUtils.bytesToString(expectedRead);
        System.out.println(str);
        assertEquals(str, "f1if7");
    }

    @Test
    public void testStringToBytes() {
        final String str = "f1if7";
        final byte[] bytes = CipherUtils.stringToBytes(str);
        System.out.println(Arrays.toString(bytes));
        assertArrayEquals(bytes, new byte[]{-127, -126, -125});
    }

    @Test
    public void testEncrypt() {
        final String encrypted = CipherUtils.encrypt("toEncrypt", "AES", secretKey);
        System.out.println(encrypted);
        assertEquals(encrypted, "rcosq1vij78dtegb0ozgfg6my");
    }

    @Test
    public void testDecrypt() {
        final String decrypted = CipherUtils.decrypt("rcosq1vij78dtegb0ozgfg6my", "AES", secretKey);
        System.out.println(decrypted);
        assertEquals(decrypted, "toEncrypt");
    }
}
