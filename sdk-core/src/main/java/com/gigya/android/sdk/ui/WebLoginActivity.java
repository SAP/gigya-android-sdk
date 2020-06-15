package com.gigya.android.sdk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.R;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * This activity is used for social login purposes for non-embedded providers (providers which does not require
 * a special SDK to be included).
 * The activity encapsulates a WebView redirect flow to avoid using external browsers.
 * <p>
 * Customizing the ProgressBar is available when overriding the Activity layout with any custom layout (preserving all ids of course).
 */
public class WebLoginActivity extends Activity {

    private static final String LOG_TAG = "WebLoginActivity";

    private static final String EXTRA_LIFECYCLE_CALLBACK_ID = "web_login_lifecycle_callback";
    private static final String EXTRA_URI = "web_login_uri";

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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gigya_activity_web_provider);

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

        final ProgressBar progress = findViewById(R.id.gig_web_provider_progress);

        WebView webView = findViewById(R.id.gig_web_provider_web_view);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                GigyaLogger.debug(LOG_TAG, "shouldOverrideUrlLoading: " + url);
                final Uri uri = Uri.parse(url);
                if (evaluateLoginResponse(uri)) {
                    return false;
                }
                view.loadUrl(url);
                return true;
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                GigyaLogger.debug(LOG_TAG, "shouldOverrideUrlLoading: " + request.getUrl().toString());
                if (evaluateLoginResponse(request.getUrl())) {
                    return false;
                }
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                GigyaLogger.debug(LOG_TAG, "onPageStarted: " + url);
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                GigyaLogger.debug(LOG_TAG, "onPageFinished: " + url);
                progress.setVisibility(View.INVISIBLE);
            }
        });

        webView.loadUrl(_uri);
    }

    /**
     * Evaluating Gigya login response.
     *
     * @param uri redirect Uri.
     * @return True if login response is handled and web view flow should complete.
     */
    private boolean evaluateLoginResponse(Uri uri) {
        final String scheme = uri.getScheme();
        final String host = uri.getHost();
        final String pathPrefix = uri.getPath();
        if (scheme == null || host == null || pathPrefix == null) return false;
        if (scheme.equals("gigya") && host.equals("gsapi")) {
            final String encodedFragment = uri.getEncodedFragment();
            final Map<String, Object> parsed = new HashMap<>();
            UrlUtils.parseUrlParameters(parsed, encodedFragment);
            GigyaLogger.debug(LOG_TAG, "evaluateUri: parsed url parameters = " + parsed.toString());

            // Throttle response.
            if (_webLoginLifecycleCallbacks != null && !isFinishing()) {
                _webLoginLifecycleCallbacks.onResult(this, parsed);
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        // Throttle cancel event.
        if (_webLoginLifecycleCallbacks != null && !isFinishing()) {
            _webLoginLifecycleCallbacks.onCancelled();
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        Presenter.flushWebLoginLifecycleCallback(_webLoginLifecycleCallbacksId);
        super.finish();
        /*
        Disable exit animation.
         */
        overridePendingTransition(0, 0);
    }
}
