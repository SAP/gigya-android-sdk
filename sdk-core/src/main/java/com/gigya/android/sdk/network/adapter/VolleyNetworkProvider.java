package com.gigya.android.sdk.network.adapter;

import android.content.Context;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiHttpRequest;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class VolleyNetworkProvider extends NetworkProvider {

    private static final String LOG_TAG = "VolleyNetworkProvider";

    private RequestQueue _requestQueue;
    private Queue<HttpVolleyTask> _blockedQueue = new ConcurrentLinkedQueue<>();

    VolleyNetworkProvider(IApiRequestFactory requestFactory, Context appContext) {
        super(requestFactory);
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
    public void addToQueue(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        _requestQueue.getCache().clear();

        if (_blocked) {
            GigyaLogger.debug(LOG_TAG, "addToQueue: is blocked. adding to blocked queued - " + request.getApi());
            _blockedQueue.add(new HttpVolleyTask(request, networkCallbacks));
            return;
        }
        if (!_blockedQueue.isEmpty()) {
            GigyaLogger.debug(LOG_TAG, "addToQueue: blockedQueue is empty releasing it - " + request.getApi());
            release();
        }

        GigyaLogger.debug(LOG_TAG, "addToQueue: adding to queue - " + request.getApi());

        _requestFactory.sign(request);
        VolleyNetworkRequest newRequest = createRequest(request, networkCallbacks);
        _requestQueue.add(newRequest);
    }

    @Override
    public void sendBlocking(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        GigyaLogger.debug(LOG_TAG, "sendBlocking: " + request.getApi());
        _requestQueue.getCache().clear();

        _requestFactory.sign(request);
        VolleyNetworkRequest newRequest = createRequest(request, networkCallbacks);
        _requestQueue.add(newRequest);
        _blocked = true;
    }

    @Override
    public void release() {
        super.release();
        if (_blockedQueue.isEmpty()) {
            return;
        }

        // Traverse over blocked queue and release all.
        while (!_blockedQueue.isEmpty()) {

            final HttpVolleyTask task = _blockedQueue.poll();
            // Need to resign the request.
            _requestFactory.sign(task.getRequest());

            final VolleyNetworkRequest queued = createRequest(task.getRequest(), task.getNetworkCallbacks());
            GigyaLogger.debug(LOG_TAG, "release: polled request  - " + queued.getUrl());

            _requestQueue.add(queued);
        }
    }

    @Override
    public void cancel(String tag) {
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
                final HttpVolleyTask task = (HttpVolleyTask) it.next();
                final String requestTag = task.request.getTag();
                if (requestTag.equals(tag)) {
                    it.remove();
                }
            }
        }
    }

    //region VOLLEY SPECIFIC IMPLEMENTATION

    private static class HttpVolleyTask {

        private GigyaApiRequest request;
        private final IRestAdapterCallback networkCallbacks;

        private HttpVolleyTask(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
            this.request = request;
            this.networkCallbacks = networkCallbacks;
        }

        public GigyaApiRequest getRequest() {
            return request;
        }

        public void setRequest(GigyaApiRequest request) {
            this.request = request;
        }

        public IRestAdapterCallback getNetworkCallbacks() {
            return networkCallbacks;
        }
    }

    /*
    Generate a new Volley request.
     */
    private VolleyNetworkRequest createRequest(final GigyaApiRequest request, final IRestAdapterCallback networkCallbacks) {

        final GigyaApiHttpRequest signedRequest = _requestFactory.sign(request);

        return new VolleyNetworkRequest(
                request.getMethod().intValue(),
                signedRequest.getUrl(),
                new Response.Listener<VolleyResponsePair>() {
                    @Override
                    public void onResponse(VolleyResponsePair response) {
                        GigyaLogger.debug("GigyaApiResponse", "ApiService: " + signedRequest.getUrl() + "\n" + response);
                        if (networkCallbacks != null) {
                            networkCallbacks.onResponse(response.res, response.date);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Check for host not found exception.
                        if (error instanceof com.android.volley.NoConnectionError) {
                            final GigyaError noNetworkError = new GigyaError(400106, "User is not connected to the required network or to any network", null);
                            GigyaLogger.debug("GigyaApiResponse", "No network error");
                            if (networkCallbacks != null) {
                                networkCallbacks.onError(noNetworkError);
                            }
                            return;
                        }
                        int errorCode = 0;
                        if (error.networkResponse != null) {
                            errorCode = error.networkResponse.statusCode;
                        }
                        final String localizedMessage = error.getLocalizedMessage() == null ? "" : error.getLocalizedMessage();
                        final GigyaError gigyaError = new GigyaError(errorCode, localizedMessage, null);
                        GigyaLogger.debug("GigyaApiResponse", "GigyaApiResponse: Error " +
                                "ApiService: " + signedRequest.getUrl() + "\n" +
                                gigyaError.toString());
                        if (networkCallbacks != null) {
                            networkCallbacks.onError(gigyaError);
                        }
                    }
                },
                signedRequest.getEncodedParams(),
                request.getTag()
        );
    }

    private static class VolleyNetworkRequest extends Request<VolleyResponsePair> {

        /**
         * Lock to guard mListener as it is cleared on cancel() and read on delivery.
         */
        private final Object _lock = new Object();

        @Nullable
        @GuardedBy("mLock")
        private Response.Listener<VolleyResponsePair> _listener;

        @Nullable
        private String _body;

        VolleyNetworkRequest(int method,
                             String url,
                             @NonNull Response.Listener<VolleyResponsePair> listener,
                             @NonNull Response.ErrorListener errorListener,
                             @Nullable String body,
                             String tag) {
            super(method, url, errorListener);
            setTag(tag);
            _body = body;
            _listener = listener;
            setShouldCache(false);
            setRetryPolicy(new DefaultRetryPolicy(
                    (int) TimeUnit.SECONDS.toMillis(30), //After the set time elapses the request will timeout
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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
            if (_body != null) {
                return this._body.getBytes();
            }
            return super.getBody();
        }

        @Override
        public void cancel() {
            super.cancel();
            synchronized (_lock) {
                _listener = null;
            }
        }

        @Override
        protected void deliverResponse(VolleyResponsePair response) {
            Response.Listener<VolleyResponsePair> listener;
            synchronized (_lock) {
                listener = _listener;
            }
            if (listener != null) {
                listener.onResponse(response);
            }
        }

        @Override
        protected Response<VolleyResponsePair> parseNetworkResponse(NetworkResponse response) {
            String jsonString;
            try {
                final String dateHeader = response.headers.get("Date");
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
                        new VolleyResponsePair(jsonString, dateHeader),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new ParseError(e));
            }
        }
    }

    static class VolleyResponsePair {

        final private String res;
        final private String date;

        VolleyResponsePair(String res, String date) {
            this.res = res;
            this.date = date;
        }
    }

    //endregion

}
