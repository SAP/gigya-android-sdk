package com.gigya.android.sdk.session;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaInterceptor;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.ui.Presenter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SessionVerificationService implements ISessionVerificationService {

    private static final String LOG_TAG = "SessionVerificationService";

    final private Application _context;
    final private Config _config;
    final private ISessionService _sessionService;
    final private IAccountService _accountService;
    final private IApiService _apiService;
    final private IApiRequestFactory _requestFactory;
    final private SessionVerificationObservable _observable;

    public SessionVerificationService(Application context,
                                      Config config,
                                      ISessionService sessionService,
                                      IAccountService accountService,
                                      IApiService apiService,
                                      IApiRequestFactory requestFactory,
                                      SessionVerificationObservable observable) {
        _context = context;
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _apiService = apiService;
        _requestFactory = requestFactory;
        _observable = observable;

        /*
        Add a setSession interception in order to make sure that the service starts when a new
        Session is being set.
         */
        _sessionService.addInterceptor(new GigyaInterceptor("VERIFY_LOGIN") {
            @Override
            public void intercept() {
                updateInterval();
                if (_sessionService.isValid() && _verificationInterval != 0) {
                    restart();
                }
            }
        });
    }

    private long _verificationInterval;
    private long _lastRequestTimestamp = 0;
    private Timer _timer;

    @Override
    public void updateInterval() {
        /*
        Update the current interval as set in the configuration.
         */
        _verificationInterval = TimeUnit.SECONDS.toMillis(_config.getSessionVerificationInterval());
    }

    @Override
    public void registerActivityLifecycleCallbacks() {
        _context.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            private int activityReferences = 0;
            private int startedActivityReferences = 0;
            private boolean isActivityChangingConfigurations = false;

            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                ++activityReferences;
            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (++startedActivityReferences == 1 && !isActivityChangingConfigurations) {
                    // App enters foreground

                    _sessionService.refreshSessionExpiration();

                    GigyaLogger.info(LOG_TAG, "Application lifecycle - Foreground started first activity");
                    if (_sessionService.isValid()) {
                        // Make sure interval is updated correctly.
                        updateInterval();
                        // Session verification is only relevant when user is logged in.
                        start();
                    }
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                //Stub.
            }

            @Override
            public void onActivityPaused(Activity activity) {
                // Stub.
            }

            @Override
            public void onActivityStopped(Activity activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations();
                if (--startedActivityReferences == 0 && !isActivityChangingConfigurations) {
                    // App enters background
                    GigyaLogger.info(LOG_TAG, "Application lifecycle - Background stopped first activity");
                    stop();

                    _sessionService.cancelSessionCountdownTimer();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
                // Stub.
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    // Flush the Presenter statics just in case. When all activities have been destroyed.
                    Presenter.flush();
                }
            }
        });
    }

    @Override
    public void start() {
        if (_verificationInterval == 0) {
            GigyaLogger.debug(LOG_TAG, "start: Verification interval is 0. Verification flow irrelevant");
            return;
        }
        GigyaLogger.debug(LOG_TAG, "start: Verification interval is " + TimeUnit.MILLISECONDS.toSeconds(_verificationInterval) + " seconds");
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
                    final Map<String, Object> params = new HashMap<>();
                    params.put("include", "identities-all,loginIDs,profile,email,data");
                    final GigyaApiRequest request = _requestFactory.create(
                            GigyaDefinitions.API.API_VERIFY_LOGIN,
                            params,
                            RestAdapter.HttpMethod.POST);
                    _apiService.send(request, false, new ApiService.IApiServiceResponse() {
                        @Override
                        public void onApiSuccess(GigyaApiResponse response) {
                            if (response == null) {
                                GigyaLogger.error(LOG_TAG, "Verify login unhandled error: null response");
                            } else {
                                if (response.getErrorCode() == 0) {
                                    GigyaLogger.debug(LOG_TAG, "verifyLogin success");
                                } else {
                                    evaluateVerifyLoginError(GigyaError.fromResponse(response));
                                }
                            }
                        }

                        @Override
                        public void onApiError(GigyaError gigyaError) {
                            // Ignore validation error.
                            GigyaLogger.error(LOG_TAG, "Verify login unhandled error: " + gigyaError.toString());
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

    private void restart() {
        stop();
        start();
    }

    /**
     * get the initial timer delay.
     *
     * @return Initial timer task delay in milliseconds.
     */
    @Override
    public long getInitialDelay() {
        if (_lastRequestTimestamp == 0) {
            return _verificationInterval;
        }
        final long interval = _verificationInterval;
        final long delta = System.currentTimeMillis() - _lastRequestTimestamp;
        final long initialDelay = delta > interval ? 0 : interval - delta;
        GigyaLogger.debug(LOG_TAG, "getInitialDelay: " + TimeUnit.MILLISECONDS.toSeconds(initialDelay) + " seconds");
        return initialDelay;
    }

    @Override
    public void addObserver(SessionVerificationObserver observer) {
        _observable.addObserver(observer);
    }

    @Override
    public void removeObserver(SessionVerificationObserver observer) {
        _observable.deleteObserver(observer);
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

        notifyInvalidSession(error.getData());
    }

    /**
     * Session no longer valid.
     * 1. Clear saved session & invalidate cached account.
     * 2. Broadcast a local event to notify that the session is invalid.
     */
    private void notifyInvalidSession(@Nullable String data) {
        stop();
        if (!_sessionService.isValid()) {
            GigyaLogger.debug(LOG_TAG, "notifyInvalidSession: Session is invalid. Only stopping timer");
            return;
        }
        GigyaLogger.debug(LOG_TAG, "notifyInvalidSession: Invalidating session and cached account. Trigger local broadcast");

        logout();

        // Clear current session & cached account.
        _sessionService.clear(true);
        _accountService.invalidateAccount();

        String regToken = null;
        JSONObject jo = null;
        try {
            jo = new JSONObject(data);
            regToken = jo.optString("regToken");
            GigyaLogger.debug(LOG_TAG, "evaluateVerifyLoginError: regToken = " + regToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Send "session invalid" local broadcast & flush the timer.
        Intent intent = new Intent(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID);
        // Add "regToken" value to intent if available.
        if (regToken != null) {
            intent.putExtra("rawError", data);
            if (!regToken.isEmpty())
                intent.putExtra("regToken", regToken);
        }
        // Send "session invalid" local broadcast & flush the timer.
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);

        // Notify session verification observable that the session is invalid.
        _observable.notifyObservers(jo);
    }

    /**
     * Perform a logout call to insure session is invalidated on server side as well.
     */
    private void logout() {
        final GigyaApiRequest request = _requestFactory.create(GigyaDefinitions.API.API_LOGOUT, null, RestAdapter.HttpMethod.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {

            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "logOut: Success");
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.error(LOG_TAG, "logOut: Failed");
            }
        });
    }
}
