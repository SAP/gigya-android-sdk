package com.gigya.android.sdk.tfa.worker;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class TokenUpdateWorker extends Worker {

    private String token;

    public TokenUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.token = workerParams.getInputData().getString("token");
    }

    @NonNull
    @Override
    public Result doWork() {
        return Result.success();
    }
}
