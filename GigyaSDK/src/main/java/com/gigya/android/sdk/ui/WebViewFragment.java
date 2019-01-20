package com.gigya.android.sdk.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.utils.UiUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import java.net.URLDecoder;
import java.util.Map;

public class WebViewFragment extends DialogFragment {

    public static final String TAG = "WebViewFragment";

    /* Arguments. */
    public static final String ARG_TITLE = "arg_title";
    public static final String ARG_URL = "arg_url";
    public static final String ARG_BODY = "arg_body";
    public static final String ARG_REDIRECT_PREFIX = "arg_redirect_prefix";
    public static int PROGRESS_COLOR = Color.BLACK; // Default color.
    /* Content views. */
    private WebView _webView;
    private LinearLayout _contentView;
    private ProgressBar _progressBar;

    @Nullable
    private String _url, _body, _redirectPrefix, _title;

    @Nullable
    private WebViewFragmentLifecycleCallbacks _resultCallback;

    public static void present(AppCompatActivity activity, Bundle args, WebViewFragmentLifecycleCallbacks resultCallback) {
        WebViewFragment fragment = new WebViewFragment();
        fragment._resultCallback = resultCallback;
        fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(fragment, TAG);
        fragmentTransaction.commitAllowingStateLoss();
        // TODO: 30/12/2018 Investigate usage of "commitAllowingStateLoss"
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseArguments();
        setUpWebView();
    }

    private void parseArguments() {
        if (getArguments() == null) {
            return;
        }
        Bundle args = getArguments();
        _url = args.getString(ARG_URL);
        _body = args.getString(ARG_BODY);
        _redirectPrefix = args.getString(ARG_REDIRECT_PREFIX);
        _title = args.getString(ARG_TITLE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() == null) {
            return null;
        }
        if (getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(getRoundedCornerBackground());
        }
        createView();
        return _contentView;
    }

    @SuppressWarnings("CharsetObjectCanBeUsed") // <- Only from API 19.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
    private void setUpWebView() {
        _webView = new WebView(getActivity());

        /* Content UI & scrolling */
        _webView.setVerticalScrollBarEnabled(true);
        _webView.setHorizontalScrollBarEnabled(true);
        _webView.setInitialScale(1);
        _webView.setFocusable(true);

        /* Web settings */
        final WebSettings webSettings = _webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
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
                GigyaLogger.debug(TAG, "onPageStarted: with Url = " + url);
                _progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                GigyaLogger.debug(TAG, "onPageFinished: with Url = " + url);
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
                GigyaLogger.debug(TAG, error.toString());
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

    //region WebView setup

    private void createView() {
        if (getActivity() == null) {
            return;
        }

        /* UI sizes. */
        final int dip8 = (int) UiUtils.dpToPixel(8, getActivity());
        final int dip16 = (int) UiUtils.dpToPixel(16, getActivity());
        final Pair<Integer, Integer> screenSize = UiUtils.getScreenSize(getActivity());

        /* Content view. */
        _contentView = new LinearLayout(getActivity());
        _contentView.setOrientation(LinearLayout.VERTICAL);
        final ViewGroup.LayoutParams contentParams = new ViewGroup.LayoutParams(
                Math.min(screenSize.first, screenSize.second) * 9 / 10,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        _contentView.setLayoutParams(contentParams);

        /* Title text view. */
        TextView _titleTextView = new TextView(getActivity());
        _titleTextView.setTextColor(Color.BLACK);
        _titleTextView.setTypeface(_titleTextView.getTypeface(), Typeface.BOLD);
        _titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        if (_title != null) {
            _titleTextView.setText(_title);
        } else {
            _titleTextView.setVisibility(View.GONE);
        }
        final LinearLayout.LayoutParams titleViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleViewParams.setMargins(dip16, dip8, dip8, 0);
        titleViewParams.weight = 1;
        _contentView.addView(_titleTextView, titleViewParams);

        /* Web frame container. */
        FrameLayout webFrame = new FrameLayout(getActivity());
        final LinearLayout.LayoutParams webFrameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        webFrameParams.weight = 1;
        _contentView.addView(webFrame, webFrameParams);

        /* WebView. */
        final FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        webViewParams.setMargins(dip8, dip8, dip8, dip8);
        webViewParams.gravity = Gravity.CENTER;
        webFrame.addView(_webView, webViewParams);

        /* Progress bar. */
        _progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyle);
        _progressBar.setIndeterminate(true);
        _progressBar.getIndeterminateDrawable().setColorFilter(PROGRESS_COLOR, android.graphics.PorterDuff.Mode.SRC_IN);
        _progressBar.setVisibility(View.GONE); // Default behaviour is hidden.
        _progressBar.setPadding(dip16, dip16, dip16, dip16);
        final FrameLayout.LayoutParams progressBarParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT);
        progressBarParams.gravity = Gravity.CENTER;
        webFrame.addView(_progressBar, progressBarParams);
    }

    //endregion

    //region UI Customization

    private Drawable getRoundedCornerBackground() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(Color.WHITE);
        gradientDrawable.setCornerRadius(16f);
        return gradientDrawable;
    }

    //endregion

    //region Result handling

    private void handleResult(@NonNull Map<String, Object> resultObject) {
        GigyaLogger.debug(TAG, "handleResult: " + resultObject.toString());
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
        // TODO: 31/12/2018 Investigate the need of "commitAllowingStateLoss"
        getActivity().finish();
    }

    //endregion

    public interface WebViewFragmentLifecycleCallbacks {

        void onWebViewResult(Map<String, Object> result);

        void onWebViewCancel();
    }
}
