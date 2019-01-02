package com.gigya.android.sdk.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.gigya.android.sdk.log.GigyaLogger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/*
Volley request extension.
 */
public class GigyaRequestOld extends StringRequest {

    protected static final String PROTOCOL_CHARSET = "utf-8";
    private static final String LOG_TAG = "GigyaRequestOld";
    private final Map<String, String> headers = new HashMap<>();
    private final Priority priority;
    private String body;

    public GigyaRequestOld(@NonNull String url,
                           @NonNull Response.Listener<String> listener,
                           @NonNull Response.ErrorListener errorListener,
                           @Nullable Priority priority,
                           String tag) {
        super(Method.GET, url, listener, errorListener);
        this.priority = priority;
        setTag(tag);

        /*
        Disable caching -> will cause duplicate nonce error.
         */
        setShouldCache(false);
    }

    public GigyaRequestOld(@NonNull String url,
                           @NonNull String body,
                           @NonNull Response.Listener<String> listener,
                           @NonNull Response.ErrorListener errorListener,
                           @Nullable Priority priority,
                           String tag) {
        super(Method.POST, url, listener, errorListener);
        this.priority = priority;
        this.body = body;
        setTag(tag);

         /*
        Disable caching -> will cause duplicate nonce error.
         */
        setShouldCache(false);
    }

    @Override
    public Priority getPriority() {
        return priority != null ? priority : super.getPriority();
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        responseLogger(response);
        String jsonString;
        try {
            final String encoding = response.headers.get("Content-Encoding");
            if (encoding != null && encoding.equals("gzip")) {
                // Response contains GZIP encoding.
                jsonString = gzipDecode(response);
            } else {
                jsonString = new String(
                        response.data,
                        HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            }
            return Response.success(
                    jsonString, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public Map<String, String> getHeaders() {
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("connection", "close");
        return headers;
    }

    @Override
    public byte[] getBody() {
        return this.body.getBytes();
    }

    /*
    Decode Gzipped stream.
     */
    private String gzipDecode(NetworkResponse response) throws IOException {
        StringBuilder output = new StringBuilder();
        final GZIPInputStream gStream = new GZIPInputStream(new ByteArrayInputStream(response.data));
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

    /*
    Log response specific fields.
     */
    private void responseLogger(NetworkResponse response) {
        final String xServer = response.headers.get("x-server");
        if (xServer != null) {
            GigyaLogger.debug("GigyaResponse", "Server: " + xServer);
        }
    }
}
