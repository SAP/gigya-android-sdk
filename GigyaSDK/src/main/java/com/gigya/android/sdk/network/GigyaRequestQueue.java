package com.gigya.android.sdk.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.gigya.android.sdk.log.GigyaLogger;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class GigyaRequestQueue {

    private static final String LOG_TAG = "GigyaRequestQueue";

    private RequestQueue _requestQueue;
    private Queue<GigyaRequest> _blockingQueue = new LinkedBlockingDeque<>();
    private boolean blocked = false;

    public GigyaRequestQueue(Context appContext) {
        _requestQueue = Volley.newRequestQueue(appContext);
    }

    /*
    Block queue execution until an explicit call to release()
     */
    public void block() {
        GigyaLogger.debug(LOG_TAG, "block: blocking queue size = " + _blockingQueue.size());
        this.blocked = true;
    }

    /*
    Release queue -> dispatch all blocked requests.
     */
    public void release() {
        GigyaLogger.debug(LOG_TAG, "release: blocking queue size = " + _blockingQueue.size());
        this.blocked = false;
        // Clear blocked queue.
        GigyaRequest queued = _blockingQueue.poll();
        while (queued != null) {
            _requestQueue.add(queued);
            queued = _blockingQueue.poll();
        }
    }

    /*
    Add new requests. Evaluate blocking status.
     */
    public void add(GigyaRequest newRequest) {
        GigyaLogger.debug(LOG_TAG, "add: is blocked = " + String.valueOf(blocked) + " blocking queue size = " + _blockingQueue.size());
        if (blocked) {
            _blockingQueue.add(newRequest);
            GigyaLogger.debug(LOG_TAG, "api: " + newRequest.getTag() + " queued block");
            return;
        }
        // Clean the blocking queue if needed.
        if (!_blockingQueue.isEmpty()) {
            release();
        }
        _requestQueue.add(newRequest);
        GigyaLogger.debug(LOG_TAG, "api: " + newRequest.getTag() + " queued execute");
    }

    /*
    Cancel all queued requests.
     */
    public void cancelAll() {
        _blockingQueue.clear();
        _requestQueue.cancelAll(new RequestQueue.RequestFilter() {

            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }
}
