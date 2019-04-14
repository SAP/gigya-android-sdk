package com.gigya.android.sdk.ui.provider;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.utils.UrlUtils;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class ProviderFragment extends WebViewFragment {
    public static final String LOG_TAG = "ProviderFragment";

    /* Arguments. */
    public static final String ARG_TITLE = "arg_title";
    public static final String ARG_URL = "arg_url";
    public static final String ARG_BODY = "arg_body";
    public static final String ARG_REDIRECT_PREFIX = "arg_redirect_prefix";

    @Nullable
    private WebViewFragment.WebViewFragmentLifecycleCallbacks _resultCallback;

    public static void present(AppCompatActivity activity, Bundle args, WebViewFragment.WebViewFragmentLifecycleCallbacks resultCallback) {
        ProviderFragment fragment = new ProviderFragment();
        fragment._resultCallback = resultCallback;
        fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(fragment, LOG_TAG);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void parseArguments() {
        if (getArguments() == null) {
            return;
        }
        Bundle args = getArguments();
        _url = args.getString(ARG_URL);
        _body = args.getString(ARG_BODY);
        _redirectPrefix = args.getString(ARG_REDIRECT_PREFIX);
        _title = args.getString(ARG_TITLE);
        _params = (HashMap<String, Object>) args.getSerializable(ARG_PARAMS);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed") // <- Only from API 19.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpWebView();
        if (_url != null) {
            if (_body != null) {
                _webView.postUrl(_url, _body.getBytes());
            } else {
                // Load url.
                _webView.loadUrl(_url);
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (_resultCallback != null) {
            _resultCallback.onWebViewCancel();
        }
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void setUpWebView() {
        super.setUpWebView();

        /* Content UI & scrolling */
        _webView.setVerticalScrollBarEnabled(true);
        _webView.setHorizontalScrollBarEnabled(true);
        _webView.setInitialScale(1);
        _webView.setFocusable(true);

        /* Web settings */
        final WebSettings webSettings = _webView.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);

        _webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                result.confirm();
                return true;
            }
        });
        _webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                GigyaLogger.debug(LOG_TAG, "onPageStarted: with Url = " + url);
                _progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                GigyaLogger.debug(LOG_TAG, "onPageFinished: with Url = " + url);
                _progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                final String result = getOverrideResult(url);
                if (result != null) {
                    handleResult(UrlUtils.parseUrlParameters(result));
                    return true;
                }
                return false;
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                final String result = getOverrideResult(request.getUrl().toString());
                if (result != null) {
                    handleResult(UrlUtils.parseUrlParameters(result));
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                GigyaLogger.debug(LOG_TAG, error.toString());
            }

            @Nullable
            private String getOverrideResult(String url) {
                if (_redirectPrefix != null && _url != null) {
                    if (url.startsWith(_redirectPrefix)) {
                        try {
                            final String decodedUrl = URLDecoder.decode(url, "UTF8");
                            return decodedUrl.replace("gsapi", "http");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                return null;
            }
        });
        _webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100 && _webView.getVisibility() == View.INVISIBLE) {
                    // Actual loading finished.
                    // Stub.
                }
            }
        });
    }

    //region RESULT HANDLING

    private void handleResult(@NonNull Map<String, Object> resultObject) {
        GigyaLogger.debug(LOG_TAG, "handleResult: " + resultObject.toString());
        if (getActivity() == null) {
            // Irrelevant if activity instance is not available.
            return;
        }
        if (_resultCallback != null) {
            _resultCallback.onWebViewResult(resultObject);
        }
        // Remove fragment from stack. Do not finish host activity.
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.remove(this).commitAllowingStateLoss();

        getActivity().finish();
    }

    //endregion
}
