package com.gigya.android.sdk.network.adapter;

import android.os.Handler;
import android.os.Looper;

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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.zip.GZIPInputStream;

public class HttpNetworkProvider extends NetworkProvider {

    private Queue<HttpTask> _queue = new ConcurrentLinkedQueue<>();

    public HttpNetworkProvider(IApiRequestFactory requestFactory) {
        super(requestFactory);
    }

    @Override
    public void addToQueue(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        if (_blocked) {
            _queue.add(new HttpTask(_requestFactory, new ExecutorAsyncTask(networkCallbacks), request));
            return;
        }
        // If not blocked send the request.
        new ExecutorAsyncTask(networkCallbacks).execute(
                _requestFactory.sign(request));
    }

    @Override
    public void addToQueueUnsigned(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        new ExecutorAsyncTask(networkCallbacks).execute(_requestFactory.unsigned(request));
    }

    @Override
    public void sendBlocking(GigyaApiRequest request, IRestAdapterCallback networkCallbacks) {
        _requestFactory.sign(request);
        new ExecutorAsyncTask(networkCallbacks).execute(_requestFactory.sign(request));
        _blocked = true;
    }

    @Override
    public void release() {
        super.release();
        if (_queue.isEmpty()) {
            return;
        }

        HttpTask queued = _queue.poll();

        while (queued != null) {
            // Requests need to be re-signed when released from the blocking queue.
            if (!queued.request.getParams().containsKey("regToken")) {
                _requestFactory.sign(queued.getRequest());
            } else {
                _requestFactory.unsigned(queued.getRequest());
            }
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

        private ExecutorAsyncTask asyncTask;
        private GigyaApiRequest request;
        private IApiRequestFactory requestFactory;

        public GigyaApiRequest getRequest() {
            return request;
        }

        public void setRequest(GigyaApiRequest request) {
            this.request = request;
        }

        HttpTask(IApiRequestFactory requestFactory, ExecutorAsyncTask asyncTask, GigyaApiRequest request) {
            this.requestFactory = requestFactory;
            this.asyncTask = asyncTask;
            this.request = request;
        }

        void run() {
            if (!request.getParams().containsKey("regToken")) {
                // If the request contains a regToken, it should be unsigned.
                this.asyncTask.execute(this.requestFactory.unsigned(request));
            } else {
                this.asyncTask.execute(this.requestFactory.sign(request));
            }
        }
    }

    private static class ExecutorAsyncTask {

        private static final String LOG_TAG = "ExecutorAsyncTask";
        private final IRestAdapterCallback callback;
        private final ExecutorService executor;

        public ExecutorAsyncTask(IRestAdapterCallback networkCallbacks) {
            callback = networkCallbacks;
            executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    final Thread t = new Thread(runnable);
                    t.setDaemon(true);
                    return t;
                }
            });
        }

        private Handler handler;

        private Handler getHandler() {
            if (handler == null) {
                synchronized (ExecutorAsyncTask.class) {
                    handler = new Handler(Looper.getMainLooper());
                }
            }
            return handler;
        }

        public void execute(final GigyaApiHttpRequest request) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    GigyaLogger.debug(LOG_TAG, "Executor: execute request with " + request.getUrl());
                    final AsyncResult result = doInBackground(request);
                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            GigyaLogger.debug(LOG_TAG, "Executor: post execute request with " + request.getUrl());
                            onPostExecute(result);
                            if (executor != null) {
                                executor.shutdownNow();
                            }
                        }
                    });
                }
            });
        }

        private AsyncResult doInBackground(GigyaApiHttpRequest request) {
            if (request != null) {
                HttpURLConnection connection = null;
                OutputStreamWriter outputStreamWriter = null;
                BufferedReader bufferedReader = null;
                StringBuilder response = new StringBuilder();
                try {
                    URL url = new URL(request.getUrl());
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setUseCaches(false);
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(15000);
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setRequestProperty("connection", "close");

                    // Add custom headers if available.
                    if (request.getHeaders() != null) {
                        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                            connection.setRequestProperty(entry.getKey(), entry.getValue());
                        }
                    }

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

        private void onPostExecute(AsyncResult asyncResult) {
            if (callback == null) {
                return;
            }
            if (asyncResult == null) {
                callback.onError(GigyaError.generalError());
                return;
            }

            final boolean badRequest = asyncResult.getCode() >= HttpURLConnection.HTTP_BAD_REQUEST;
            if (badRequest) {
                final boolean noNetworkRequest = asyncResult.getCode() == 400106;
                if (noNetworkRequest) {
                    final GigyaError noNetworkError = new GigyaError(400106, "User is not connected to the required network or to any network", null);
                    GigyaLogger.debug("GigyaApiResponse", "No network error");
                    callback.onError(noNetworkError);
                    return;
                }

                // Generate gigya error.
                final GigyaError gigyaError = new GigyaError(asyncResult.code, asyncResult.result, null);
                callback.onError(gigyaError);
                return;
            }

            callback.onResponse(asyncResult.result, asyncResult.date);
        }
    }

}
