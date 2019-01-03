package com.gigya.android.sdk.network.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VolleyNetworkProvider extends NetworkProvider {

    private RequestQueue _requestQueue;
    private Queue<VolleyNetworkRequest> _blockedQueue = new ConcurrentLinkedQueue<>();
    VolleyNetworkProvider(Context appContext) {
        _requestQueue = Volley.newRequestQueue(appContext);
        // Enable Volley logs.
        VolleyLog.DEBUG = GigyaLogger.isDebug();
    }

    public static boolean isAvailable() {
        try {
            Class.forName("com.android.volley.toolbox.Volley");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    void addToQueue(GigyaRequest request, INetworkCallbacks networkCallbacks) {
        VolleyNetworkRequest newRequest = newRequest(request, networkCallbacks);
        if (_blocked) {
            _blockedQueue.add(newRequest);
            return;
        }
        if (!_blockedQueue.isEmpty()) {
            release();
        }
        _requestQueue.add(newRequest);
    }

    @Override
    void sendBlocking(GigyaRequest request, INetworkCallbacks networkCallbacks) {
        VolleyNetworkRequest newRequest = newRequest(request, networkCallbacks);
        _requestQueue.add(newRequest);
        _blocked = true;
    }

    @Override
    void release() {
        super.release();
        if (_blockedQueue.isEmpty()) {
            return;
        }
        VolleyNetworkRequest queued = _blockedQueue.poll();
        while (queued != null) {
            _requestQueue.add(queued);
            queued = _blockedQueue.poll();
        }
    }

    @Override
    void cancel(String tag) {
        if (tag == null) {
            // Cancel all.
            _requestQueue.cancelAll(new RequestQueue.RequestFilter() {

                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
            _blockedQueue.clear();
            return;
        }
        _requestQueue.cancelAll(tag);
        if (!_blockedQueue.isEmpty()) {
            Iterator it = _blockedQueue.iterator();
            while (it.hasNext()) {
                final VolleyNetworkRequest request = (VolleyNetworkRequest) it.next();
                final String requestTag = (String) request.getTag();
                if (requestTag.equals(tag)) {
                    it.remove();
                }
            }
        }
    }

    //region Volley specific implementation.

    /*
    Generate a new Volley request.
     */
    private VolleyNetworkRequest newRequest(GigyaRequest request, final INetworkCallbacks networkCallbacks) {
        return new VolleyNetworkRequest(request.getMethod().getValue(), request.getUrl(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        GigyaLogger.debug("GigyaResponse", response);
                        if (networkCallbacks != null) {
                            networkCallbacks.onResponse(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        int errorCode = 0;
                        if (error.networkResponse != null) {
                            errorCode = error.networkResponse.statusCode;
                        }
                        final String localizedMessage = error.getLocalizedMessage() == null ? "" : error.getLocalizedMessage();
                        final GigyaError gigyaError = new GigyaError(errorCode, localizedMessage, null);
                        GigyaLogger.debug("GigyaResponse", "GigyaResponse: Error " + gigyaError.toString());
                        if (networkCallbacks != null) {
                            networkCallbacks.onError(gigyaError);
                        }
                    }
                }
                , request.getEncodedParams()
                , request.getTag()
        );
    }

    private static class VolleyNetworkRequest extends StringRequest {

        @Nullable
        private String body;

        VolleyNetworkRequest(int method,
                             String url,
                             @NonNull Response.Listener<String> listener,
                             @NonNull Response.ErrorListener errorListener,
                             @Nullable String body,
                             String tag) {
            super(method, url, listener, errorListener);
            setTag(tag);
            this.body = body;
            setShouldCache(false);
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("connection", "close");
            return headers;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            if (body != null) {
                return this.body.getBytes();
            }
            return super.getBody();
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            String jsonString;
            try {
                final String encoding = response.headers.get("Content-Encoding");
                if (encoding != null && encoding.equals("gzip")) {
                    // Response contains GZIP encoding.
                    jsonString = UrlUtils.gzipDecode(response.data);
                } else {
                    jsonString = new String(
                            response.data,
                            HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                }
                return Response.success(
                        jsonString, HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new ParseError(e));
            }
        }
    }

    //endregion

}
