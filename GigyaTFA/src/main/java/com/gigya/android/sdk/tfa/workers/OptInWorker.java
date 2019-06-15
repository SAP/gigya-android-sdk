package com.gigya.android.sdk.tfa.workers;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Worker used for push TFA opt-in process.
 */
public class OptInWorker extends Worker {

    public OptInWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        return null;
    }
}
