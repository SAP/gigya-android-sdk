package com.gigya.android.sdk.network.adapter;

import android.os.AsyncTask;

import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPInputStream;

class HttpNetworkProvider extends NetworkProvider {

    private Queue<GigyaRequest> _queue = new ConcurrentLinkedQueue<>();

    @Override
    void addToQueue(GigyaRequest request, INetworkCallbacks networkCallbacks) {
        if (_blocked) {
            _queue.add(request);
            return;
        }
        // If not blocked send the request.
        new GigyaNetworkAsyncTask(networkCallbacks).execute(request);
    }

    @Override
    void release() {
        if (_queue.isEmpty()) {
            return;
        }
        GigyaRequest queued = _queue.poll();
        while (queued != null) {

        }
    }

    @Override
    void cancel(String tag) {
        if (tag == null) {
            _queue.clear();
            // Unable to cancel already sent requests.
        }
        if (!_queue.isEmpty()) {
            Iterator it = _queue.iterator();
            while (it.hasNext()) {
                final GigyaRequest request = (GigyaRequest) it.next();
                final String requestTag = request.getTag();
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

        public AsyncResult(int code, String result) {
            this.code = code;
            this.result = result;
        }

        public int getCode() {
            return code;
        }

        public String getResult() {
            return result;
        }
    }

    private static class GigyaNetworkAsyncTask extends AsyncTask<GigyaRequest, Void, AsyncResult> {

        private INetworkCallbacks networkCallbacks;

        GigyaNetworkAsyncTask(INetworkCallbacks networkCallbacks) {
            this.networkCallbacks = networkCallbacks;
        }

        @Override
        protected AsyncResult doInBackground(GigyaRequest... gigyaRequests) {
            GigyaRequest request = gigyaRequests[0];
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
                    connection.setRequestMethod(request.getMethod().name());
                    if (request.getMethod().equals(NetworkAdapter.Method.POST)) {
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
                    return new AsyncResult(responseStatusCode, response.toString());
                } catch (Exception ex) {
                    ex.printStackTrace();
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
            boolean badRequest = asyncResult.getCode() >= HttpURLConnection.HTTP_BAD_REQUEST;
            if (badRequest) {
                // Generate gigya error.
                final GigyaError gigyaError = new GigyaError(asyncResult.code, asyncResult.result, null);
                networkCallbacks.onError(gigyaError);
                return;
            }
            networkCallbacks.onResponse(asyncResult.result);
        }
    }

    //endregion

}
