package com.gigya.android.sdk.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.GigyaError;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SessionVerificationService implements ISessionVerificationService {

    private static final String LOG_TAG = "SessionVerificationService";

    final private Context _context;
    final private Config _config;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private IApiService _apiService;

    public SessionVerificationService(Context context, Config config, ISessionService sessionService,
                                      IAccountService accountService, IApiService apiService) {
        _context = context;
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _apiService = apiService;
    }

    private long _verificationInterval;
    private long _lastRequestTimestamp = 0;
    private Timer _timer;

    @Override
    public void start() {
        _verificationInterval = TimeUnit.MINUTES.toMillis(_config.getSessionVerificationInterval());
        if (_verificationInterval == 0) {
            GigyaLogger.debug(LOG_TAG, "start: Verification interval is 0. Verification flow irrelevant");
            return;
        }
        GigyaLogger.debug(LOG_TAG, "start: Verification interval is " + TimeUnit.MILLISECONDS.toMinutes(_verificationInterval) + " minutes");
        if (_timer == null) {
            _timer = new Timer();
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

    @Override
    public void stop() {
        GigyaLogger.debug(LOG_TAG, "stop: ");
        if (_timer != null) {
            _timer.cancel();
            _timer.purge();
            _timer = null;
        }
        System.gc();
    }

    /**
     * get the initial timer delay.
     *
     * @return Initial timer task delay in milliseconds.
     */
    private long getInitialDelay() {
        if (_lastRequestTimestamp == 0) {
            return _verificationInterval;
        }
        final long interval = _verificationInterval;
        final long delta = System.currentTimeMillis() - _lastRequestTimestamp;
        final long initialDelay = delta > interval ? 0 : interval - delta;
        GigyaLogger.debug(LOG_TAG, "getInitialDelay: " + TimeUnit.MILLISECONDS.toSeconds(initialDelay) + " seconds");
        return initialDelay;
    }

    /**
     * Evaluate notifyLogin endpoint error.
     * Will ignore network error.
     *
     * @param error GigyaError received.
     */
    private void evaluateVerifyLoginError(GigyaError error) {
        final int errorCode = error.getErrorCode();
        if (errorCode == GigyaError.Codes.ERROR_NETWORK) {
            return;
        }
        GigyaLogger.error(LOG_TAG, "evaluateVerifyLoginError: error = " + errorCode + " session invalid -> invalidate & notify");
        notifyInvalidSession();
    }

    /**
     * Session no longer valid.
     * 1. Clear saved session & invalidate cached account.
     * 2. Broadcast a local event to notify that the session is invalid.
     */
    private void notifyInvalidSession() {
        GigyaLogger.debug(LOG_TAG, "notifyInvalidSession: Invalidating session and cached account. Trigger local broadcast");
        // Clear current session & cached account.
        _sessionService.clear(true);
        _accountService.invalidateAccount();
        // Send "session invalid" local broadcast & flush the timer.
        LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID));
        stop();
    }
}
