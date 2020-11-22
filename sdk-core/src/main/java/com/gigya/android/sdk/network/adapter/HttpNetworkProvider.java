package com.gigya.android.sdk.network.adapter;

import android.os.AsyncTask;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiHttpRequest;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.network.GigyaError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPInputStream;

public class HttpNetworkProvider extends NetworkProvider {

    private Queue<HttpTask> _queue = new ConcurrentLinkedQueue<>();

    public HttpNetworkProvider(IApiRequestFactory requestFactory) {
        super(requestFactory);
    }

    @Override
    public void addToQueue(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        if (_blocked) {
            _queue.add(new HttpTask(_requestFactory, new GigyaNetworkAsyncTask(networkCallbacks), request));
            return;
        }
        // If not blocked send the request.
        new GigyaNetworkAsyncTask(networkCallbacks).execute(_requestFactory.sign(request));
    }

    @Override
    public void sendBlocking(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        _requestFactory.sign(request);
        new GigyaNetworkAsyncTask(networkCallbacks).execute(_requestFactory.sign(request));
        _blocked = true;
    }

    @Override
    public void release() {
        super.release();
        if (_queue.isEmpty()) {
            return;
        }

        HttpTask queued = _queue.poll();
        // Requests need to be re-signed when released from the blocking queue.
        _requestFactory.sign(queued.getRequest());

        while (queued != null) {
            queued.run();
            queued = _queue.poll();
        }
    }

    @Override
    public void cancel(String tag) {
        if (tag == null) {
            _queue.clear();
            // Unable to cancel already sent requests.
        }
        if (!_queue.isEmpty()) {
            Iterator it = _queue.iterator();
            while (it.hasNext()) {
                final HttpTask task = (HttpTask) it.next();
                final String requestTag = task.request.getTag();
                if (requestTag.equals(tag)) {
                    it.remove();
                }
            }
        }
    }

    // Async implementation.

    private static class AsyncResult {
        private int code;
        private String result;
        private String date;

        AsyncResult(int code, String result, String date) {
            this.code = code;
            this.result = result;
            this.date = date;
        }

        public int getCode() {
            return code;
        }
    }

    private static class HttpTask {

        private GigyaNetworkAsyncTask asyncTask;
        private GigyaApiRequest request;
        private IApiRequestFactory requestFactory;

        public GigyaApiRequest getRequest() {
            return request;
        }

        public void setRequest(GigyaApiRequest request) {
            this.request = request;
        }

        HttpTask(IApiRequestFactory requestFactory, GigyaNetworkAsyncTask asyncTask, GigyaApiRequest request) {
            this.requestFactory = requestFactory;
            this.asyncTask = asyncTask;
            this.request = request;
        }

        void run() {
            final GigyaApiHttpRequest signedRequest = this.requestFactory.sign(request);
            this.asyncTask.execute(signedRequest);
        }
    }

    private static class GigyaNetworkAsyncTask extends AsyncTask<GigyaApiHttpRequest, Void, AsyncResult> {

        private IRestAdapterCallback networkCallbacks;

        GigyaNetworkAsyncTask(IRestAdapterCallback networkCallbacks) {
            this.networkCallbacks = networkCallbacks;
        }

        @Override
        protected AsyncResult doInBackground(GigyaApiHttpRequest... gigyaApiRequests) {
            GigyaApiHttpRequest request = gigyaApiRequests[0];
            if (request != null) {
                HttpURLConnection connection = null;
                OutputStreamWriter outputStreamWriter = null;
                BufferedReader bufferedReader = null;
                StringBuilder response = new StringBuilder();
                try {
                    URL url = new URL(request.getUrl());
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setUseCaches(false);
                    connection.setConnectTimeout(10000);
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setRequestProperty("connection", "close");

                    connection.setRequestMethod(request.getHttpMethod().intValue() == 0 ? "GET" : "POST");
                    if (request.getHttpMethod().intValue() == 1) {
                        connection.setDoOutput(true);
                        outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                        outputStreamWriter.write(request.getEncodedParams());
                        outputStreamWriter.flush();
                    }
                    int responseStatusCode = connection.getResponseCode();
                    boolean badRequest = (responseStatusCode >= HttpURLConnection.HTTP_BAD_REQUEST);
                    InputStream input;
                    if (badRequest)
                        input = connection.getErrorStream();
                    else
                        input = connection.getInputStream();
                    if ("gzip".equals(connection.getContentEncoding())) {
                        input = new GZIPInputStream(input);
                    }
                    bufferedReader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        response.append(line);
                    }

                    final String dateHeader = connection.getHeaderField("Date");

                    return new AsyncResult(responseStatusCode, response.toString(), dateHeader);
                } catch (Exception ex) {
                    // Check for host not found exception.
                    if (ex instanceof UnknownHostException) {
                        return new AsyncResult(400106, null, null);
                    } else {
                        ex.printStackTrace();
                    }
                } finally {
                    if (outputStreamWriter != null) {
                        try {
                            outputStreamWriter.close();
                        } catch (IOException ignored) {
                        }
                    }
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException ignored) {
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(AsyncResult asyncResult) {
            if (networkCallbacks == null) {
                return;
            }
            if (asyncResult == null) {
                networkCallbacks.onError(GigyaError.generalError());
                return;
            }

            final boolean badRequest = asyncResult.getCode() >= HttpURLConnection.HTTP_BAD_REQUEST;
            if (badRequest) {
                final boolean noNetworkRequest = asyncResult.getCode() == 400106;
                if (noNetworkRequest) {
                    final GigyaError noNetworkError = new GigyaError(400106, "User is not connected to the required network or to any network", null);
                    GigyaLogger.debug("GigyaApiResponse", "No network error");
                    networkCallbacks.onError(noNetworkError);
                    return;
                }

                // Generate gigya error.
                final GigyaError gigyaError = new GigyaError(asyncResult.code, asyncResult.result, null);
                networkCallbacks.onError(gigyaError);
                return;
            }

            networkCallbacks.onResponse(asyncResult.result, asyncResult.date);
        }
    }

    //endregion

}
