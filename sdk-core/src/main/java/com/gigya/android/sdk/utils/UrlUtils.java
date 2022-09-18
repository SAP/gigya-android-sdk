package com.gigya.android.sdk.utils;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.ui.Presenter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

public class UrlUtils {

    public static String buildEncodedQuery(@NonNull Map<String, Object> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (TreeMap.Entry<String, Object> item : params.entrySet()) {
            final Object value = item.getValue();
            final String key = item.getKey();
            if (value != null) {
                sb.append(key);
                sb.append('=');
                sb.append(urlEncode(String.valueOf(item.getValue())));
                sb.append('&');
            }
        }
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
                    .replace("*", "%2A").replace("%7E", "~");
        } catch (Exception ex) {
            return null;
        }
    }

    public static String gzipDecode(byte[] data) throws IOException {
        StringBuilder output = new StringBuilder();
        final GZIPInputStream gStream = new GZIPInputStream(new ByteArrayInputStream(data));
        final InputStreamReader reader = new InputStreamReader(gStream);
        final BufferedReader in = new BufferedReader(reader);
        String read;
        while ((read = in.readLine()) != null) {
            output.append(read).append("\n");
        }
        reader.close();
        in.close();
        gStream.close();
        return output.toString();
    }

    public static Map<String, Object> parseUrlParameters(String url) {
        final Map<String, Object> map = new HashMap<>();
        if (url != null) {
            String querySplit[] = url.split("\\?");
            String hashSplit[] = url.split("#");
            if (querySplit.length > 1) {
                parseUrlParameters(map, querySplit[1]);
            } else if (hashSplit.length > 1) {
                parseUrlParameters(map, hashSplit[1]);
            }
        }
        return map;
    }

    public static void parseUrlParameters(Map<String, Object> map, String s) {
        if (s == null) {
            return;
        }
        String parameters[] = s.split("&");
        for (String parameter : parameters) {
            String pair[] = parameter.split("=");
            try {
                if (pair.length > 1) {
                    final String value = URLDecoder.decode(pair[1], "UTF8");
                    map.put(pair[0], value);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static String getBaseUrl(String api, String apiDomain) {
        final StringBuilder sb = new StringBuilder();
        final String[] split = api.split("\\.");
        return sb.append("https://")
                .append(split[0]).append(".")
                .append(apiDomain)
                .append("/")
                .append(api)
                .toString();
    }

    public static boolean isGigyaScheme(String scheme) {
        return ObjectUtils.safeEquals(scheme, Presenter.Consts.REDIRECT_URL_SCHEME);
    }

    public static boolean checkUrl(String origin) {
        try {
            new URL(origin).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
