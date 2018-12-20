package com.gigya.android.sdk.utils;

import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

public class UrlUtils {

    public static String buildEncodedQuery(Map<String, Object> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (TreeMap.Entry<String, Object> item : params.entrySet()) {
            final String value = urlEncode(item.getValue().toString());
            final String key = item.getKey();
            if (value != null) {
                sb.append(key);
                sb.append('=');
                sb.append(value);
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
}
