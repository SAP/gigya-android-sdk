package com.gigya.android.sdk.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.gigya.android.sdk.BuildConfig;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.GigyaError;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SessionVerificationService {

    private static final String LOG_TAG = "SessionVerificationService";

    private Timer _timer;

    final private Context _appContext;
    final private ApiService _apiService;
    private long _verificationInterval;
    private long _lastRequestTimestamp = 0;

    public SessionVerificationService(Context appContext, ApiService apiService) {
        _appContext = appContext;
        _apiService = apiService;
    }

    private long getInitialDelay() {
        if (_lastRequestTimestamp == 0) {
            return _verificationInterval;
        }
        final long interval = _verificationInterval;
        final long delta = System.currentTimeMillis() - _lastRequestTimestamp;
        final long initialDelay = delta > interval ? 0 : interval - delta;
        GigyaLogger.debug(LOG_TAG, "getInitialDelay: " + initialDelay / 1000L + " minutes");
        return initialDelay;
    }

    public void start() {
        _verificationInterval = TimeUnit.SECONDS.toMillis(_apiService.getSessionService().getConfig().getSessionVerificationInterval());
        if (_verificationInterval == 0) {
            GigyaLogger.debug(LOG_TAG, "start: Verification interval is 0. Verification flow irrelevant");
            return;
        }
        GigyaLogger.debug(LOG_TAG, "start: Verification interval is " + _verificationInterval / 1000L + " minutes");
        if (_timer == null) {
            _timer = new Timer();
        } else try {
            _timer.cancel();
        } catch (Exception ex) {
            if (BuildConfig.DEBUG)
                ex.printStackTrace();
        }
        _timer.scheduleAtFixedRate(new TimerTask() {
            @SuppressWarnings("unchecked") // Generic reference is irrelevant.
            @Override
            public void run() {
                if (!Thread.currentThread().isInterrupted()) {
                    GigyaLogger.debug(LOG_TAG, "dispatching verifyLogin request " + new Date().toString());
                    _lastRequestTimestamp = System.currentTimeMillis();
                    _apiService.verifyLogin(null, true, new GigyaCallback() {
                        @Override
                        public void onSuccess(Object obj) {
                            GigyaLogger.debug(LOG_TAG, "verifyLogin success");
                        }

                        @Override
                        public void onError(GigyaError error) {
                            evaluateVerifyLoginError(error);
                        }
                    });
                }
            }
        }, getInitialDelay(), _verificationInterval);
    }

    private void evaluateVerifyLoginError(GigyaError error) {
        final int errorCode = error.getErrorCode();
        if (errorCode == GigyaError.Codes.ERROR_NETWORK) {
            return;
        }
        GigyaLogger.error(LOG_TAG, "evaluateVerifyLoginError: error = " + errorCode + " session invalid -> invalidate & notify");
        notifyInvalidSession();
    }

    private void notifyInvalidSession() {
        GigyaLogger.debug(LOG_TAG, "notifyInvalidSession: Invalidating session and cached account. Trigger local broadcast");
        // Clear current session & cached account.
        _apiService.getSessionService().clear();
        _apiService.getAccountService().invalidateAccount();
        // Send "session invalid" local broadcast & flush the timer.
        LocalBroadcastManager.getInstance(_appContext).sendBroadcast(new Intent(GigyaDefinitions.Broadcasts.INTENT_FILTER_SESSION_INVALID));
        flush();
    }

    public void stop() {
        GigyaLogger.debug(LOG_TAG, "stop: ");
        if (_timer != null) {
            _timer.cancel();
        }
        System.gc();
    }

    public void flush() {
        GigyaLogger.debug(LOG_TAG, "flush: ");
        if (_timer != null) {
            _timer.cancel();
            _timer.purge();
            _timer = null;
        }
        System.gc();
    }
}
