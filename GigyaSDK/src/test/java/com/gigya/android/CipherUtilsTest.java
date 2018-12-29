package com.gigya.android;

import com.gigya.android.sdk.encryption.CipherUtils;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CipherUtilsTest {

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
}
