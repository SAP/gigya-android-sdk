package com.gigya.android.sdk.utils;

import android.annotation.SuppressLint;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.encryption.EncryptionException;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class CipherUtils {

    public static String bytesToString(byte[] b) {
        byte[] b2 = new byte[b.length + 1];
        b2[0] = 1;
        System.arraycopy(b, 0, b2, 1, b.length);
        return new BigInteger(b2).toString(36);
    }

    public static byte[] stringToBytes(String s) {
        byte[] b2 = new BigInteger(s, 36).toByteArray();
        return Arrays.copyOfRange(b2, 1, b2.length);
    }

    public static byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());

        Arrays.fill(charBuffer.array(), '\u0000'); // clear the cleartext
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear the ciphertext

        return bytes;
    }

    public static char[] toChars(byte[] bytes) {
        Charset charset = Charset.forName("UTF-8");
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        CharBuffer charBuffer = charset.decode(byteBuffer);
        char[] chars = Arrays.copyOf(charBuffer.array(), charBuffer.limit());

        Arrays.fill(charBuffer.array(), '\u0000'); // clear the cleartext
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear the ciphertext

        return chars;
    }

    public static String encrypt(String plain, String algorithm, SecretKey secretKey) throws EncryptionException {
        GigyaLogger.debug("CipherUtils", algorithm + " encrypt: ");
        try {
            @SuppressLint("GetInstance") final Cipher aesCipher = Cipher.getInstance(algorithm);
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] byteCipherText = aesCipher.doFinal(plain.getBytes());
            return CipherUtils.bytesToString(byteCipherText);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("Session encryption exception", ex.getCause());
        }
    }

    public static String decrypt(String encrypted, String algorithm, SecretKey secretKey) throws EncryptionException {
        GigyaLogger.debug("CipherUtils", algorithm + " decrypt: ");
        try {
            @SuppressLint("GetInstance") final Cipher aesCipher = Cipher.getInstance(algorithm);
            aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] encPLBytes = CipherUtils.stringToBytes(encrypted);
            byte[] bytePlainText = aesCipher.doFinal(encPLBytes);
            return new String(bytePlainText);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("Session encryption exception", ex.getCause());
        }
    }

}
