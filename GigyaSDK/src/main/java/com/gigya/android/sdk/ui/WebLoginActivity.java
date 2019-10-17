package com.gigya.android.sdk.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class WebLoginActivity extends Activity {

    private static final String LOG_TAG = "WebLoginActivity";

    private static final String EXTRA_LIFECYCLE_CALLBACK_ID = "web_login_lifecycle_callback";
    private static final String EXTRA_URI = "web_login_uri";

    private static final int REQUEST_CODE = 4040;

    /*
    Result handling and cancel dismissal timer.
     */
    private boolean _handledResult = false;
    private CountDownTimer _cancelResultTimer;

    public interface WebLoginActivityCallback {
        void onResult(Activity activity, Map<String, Object> parsed);

        void onCancelled();
    }

    private WebLoginActivityCallback _webLoginLifecycleCallbacks;
    private int _webLoginLifecycleCallbacksId = -1;
    private String _uri;

    public static void present(Context context, String uri, WebLoginActivityCallback lifecycleCallback) {
        Intent intent = new Intent(context, WebLoginActivity.class);
        intent.putExtra(EXTRA_LIFECYCLE_CALLBACK_ID, Presenter.addWebLoginLifecycleCallback(lifecycleCallback));
        intent.putExtra(EXTRA_URI, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getExtras() != null) {
            _webLoginLifecycleCallbacksId = getIntent().getIntExtra(EXTRA_LIFECYCLE_CALLBACK_ID, -1);
            _uri = getIntent().getStringExtra(EXTRA_URI);
            if (_webLoginLifecycleCallbacksId == -1) {
                finish();
                return;
            }
            if (_uri == null) {
                finish();
                return;
            }
            // Reference the callback using static getter from the Presenter. Same as the HostActivity.
            _webLoginLifecycleCallbacks = Presenter.getWebLoginCallback(_webLoginLifecycleCallbacksId);
        }

        // Now that we have the callback.

        final Uri uri = Uri.parse(_uri);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivityForResult(browserIntent, REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            // Start short timer to see if result is being handled in the onNewIntent.
            // Explanation: When using startActivityForResult with an ACTION_VIEW intent we will not receive the correct
            // resultCode (Will always be 0 == Activity.RESULT_CANCELED). Therefore in order to determine when the user
            // actually dismissed the browser we will use this short countdown timer as the onNewIntentCall should trigger
            // immediately.
            _cancelResultTimer = new CountDownTimer(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1)) {

                @Override
                public void onTick(long millisUntilFinished) {
                    GigyaLogger.debug(LOG_TAG, "Result countdown tick:");
                }

                @Override
                public void onFinish() {
                    if (!_handledResult && !isFinishing()) {
                        _webLoginLifecycleCallbacks.onCancelled();
                        finish();
                    }
                }
            }.start();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


        invalidateCancelTimer();

        GigyaLogger.debug(LOG_TAG, "onNewIntent: " + intent.getAction());
        final Uri data = intent.getData();
        if (data == null) {
            finish();
            return;
        }

        // Reference identifiers to verify intent deep link.
        final String packageName = getPackageName();
        final String scheme = data.getScheme();
        final String host = data.getHost();
        final String pathPrefix = data.getPath();
        if (scheme == null || host == null || pathPrefix == null) {
            finish();
            return;
        }
        // Evaluate intent-filter params,
        if (scheme.equals("gigya") && host.equals("gsapi") && pathPrefix.equalsIgnoreCase("/" + packageName + "/login_result")) {

            _handledResult = true;

            final String encodedFragment = data.getEncodedFragment();
            final Map<String, Object> parsed = new HashMap<>();
            UrlUtils.parseUrlParameters(parsed, encodedFragment);
            if (_webLoginLifecycleCallbacks != null && !isFinishing()) {
                _webLoginLifecycleCallbacks.onResult(this, parsed);
            }
            finish();
        } else {
            finish();
        }
    }

    private void invalidateCancelTimer() {
        if (_cancelResultTimer != null) {
            _cancelResultTimer.cancel();
        }
        _cancelResultTimer = null;
    }

    @Override
    public void finish() {

        _handledResult = false;

        invalidateCancelTimer();

        Presenter.flushWebLoginLifecycleCallback(_webLoginLifecycleCallbacksId);
        super.finish();
        /*
        Disable exit animation.
         */
        overridePendingTransition(0, 0);
    }
}
