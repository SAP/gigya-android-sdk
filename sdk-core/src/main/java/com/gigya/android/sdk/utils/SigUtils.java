package com.gigya.android.sdk.utils;

import android.util.Base64;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.reporting.ReportingManager;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SigUtils {

    private static final String ENCODING_ALGORITHM = "HmacSHA1";

    @SuppressWarnings("StringBufferReplaceableByString")
    public static String getSignature(String secret, String httpMethod, String url, TreeMap<String, Object> params) {
        if (params == null || url == null || httpMethod == null || secret == null) {
            return null;
        }
        try {
            StringBuilder normalizedUrl = new StringBuilder();
            java.net.URL u = new java.net.URL(url);

            normalizedUrl.append(u.getProtocol().toLowerCase(Locale.ENGLISH));
            normalizedUrl.append("://");
            normalizedUrl.append(u.getHost().toLowerCase(Locale.ENGLISH));
            if ((u.getProtocol().toUpperCase(Locale.ENGLISH).equals("HTTP") && u.getPort() != 80 && u.getPort() != -1)
                    || (u.getProtocol().toUpperCase(Locale.ENGLISH).equals("HTTPS") && u.getPort() != 443 && u.getPort() != -1)) {
                normalizedUrl.append(':');
                normalizedUrl.append(u.getPort());
            }
            normalizedUrl.append(u.getPath());

            String baseSignature = new StringBuilder()
                    .append(httpMethod.toUpperCase(Locale.ENGLISH))
                    .append("&")
                    .append(UrlUtils.urlEncode(normalizedUrl.toString()))
                    .append("&")
                    .append(UrlUtils.urlEncode(UrlUtils.buildEncodedQuery(params)))
                    .toString();

            return encodeSignature(baseSignature, secret);
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingManager.get().error(Gigya.VERSION, "core", "Exception while generating signature");
            GigyaLogger.error("SigUtils", "getSignature: Exception while generating signature");
        }
        return null;
    }

    private static String encodeSignature(String baseSignature, String secret) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        byte[] keyBytes = Base64.decode(secret, Base64.DEFAULT);
        byte[] textData = baseSignature.getBytes("UTF-8");
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, ENCODING_ALGORITHM);
        Mac mac = Mac.getInstance(ENCODING_ALGORITHM);
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(textData);
        return Base64.encodeToString(rawHmac, Base64.NO_WRAP | Base64.URL_SAFE);
    }
}
