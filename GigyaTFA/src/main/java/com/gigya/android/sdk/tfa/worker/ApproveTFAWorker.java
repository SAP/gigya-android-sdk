package com.gigya.android.sdk.tfa.worker;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ApproveTFAWorker extends Worker {

    public ApproveTFAWorker(@NonNull Context context,
                            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // TODO: 2019-06-12 Start approval process.

        return Result.success();
    }
}
