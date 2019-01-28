package com.gigya.android.sdk.ui.plugin;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.WebBridge;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UiUtils;

import org.json.JSONObject;

import java.util.HashMap;

public class PluginFragment extends WebViewFragment {

    /* Plugin variants. */
    public static final String PLUGIN_SCREENSETS = "accounts.screenSet";
    public static final String PLUGIN_COMMENTS = "comments.commentsUI";

    private static final String LOG_TAG = "PluginFragment";

    /* Arguments. */
    public static final String ARG_API_KEY = "arg_api_key";
    public static final String ARG_API_DOMAIN = "arg_api_domain";
    public static final String ARG_OBFUSCATE = "arg_obfuscate";
    public static final String ARG_PLUGIN = "arg_plugin";
    public static final String ARG_PARAMS = "arg_params";

    /* Private descriptors. */
    private static final String REDIRECT_URL_SCHEME = "gsapi";
    private static final String ON_JS_LOAD_ERROR = "on_js_load_error";
    private static final String ON_JS_EXCEPTION = "on_js_exception";
    private static final String CONTAINER_ID = "pluginContainer";
    private static final int JS_TIMEOUT = 10000;

    public static void present(AppCompatActivity activity, Bundle args, @NonNull PluginFragmentCallbacks pluginCallbacks) {
        PluginFragment fragment = new PluginFragment();
        fragment.setArguments(args);
        fragment._pluginCallbacks = pluginCallbacks;
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(fragment, LOG_TAG);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private String _apiKey, _apiDomain, _plugin;
    private HashMap<String, Object> _params;

    private boolean _obfuscate;

    private WebBridge _webBridge;

    private PluginFragmentCallbacks _pluginCallbacks;

    @SuppressWarnings("unchecked")
    @Override
    protected void parseArguments() {
        if (getArguments() == null) {
            return;
        }
        Bundle args = getArguments();
        _apiKey = args.getString(ARG_API_KEY);
        _apiDomain = args.getString(ARG_API_DOMAIN);
        _obfuscate = args.getBoolean(ARG_OBFUSCATE);
        _plugin = args.getString(ARG_PLUGIN);
        _params = (HashMap<String, Object>) args.getSerializable(ARG_PARAMS);

        if (_apiKey == null || _plugin == null) {
            /* Implementation error. */
            dismiss();
        }
    }

    @Override
    protected void setUpWebView() {
        super.setUpWebView();

        _webBridge = new WebBridge(_obfuscate);
        _webBridge.attach(_webView);

        _webView.getSettings().setAllowFileAccess(true);
        _webView.setWebViewClient(new WebViewClient() {

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                overrideUrlLoad(uri);
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
                Uri uri = Uri.parse(urlString);
                overrideUrlLoad(uri);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                _progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // TODO: 27/01/2019 Error. 
            }

            private void overrideUrlLoad(Uri uri) {
                if (ObjectUtils.safeEquals(uri.getScheme(), REDIRECT_URL_SCHEME) && ObjectUtils.safeEquals(uri.getHost(), ON_JS_LOAD_ERROR)) {
                    _pluginCallbacks.onError(GigyaError.generalError());
                } else if (ObjectUtils.safeEquals(uri.getScheme(), REDIRECT_URL_SCHEME) && ObjectUtils.safeEquals(uri.getHost(), ON_JS_EXCEPTION)) {
                    _pluginCallbacks.onError(GigyaError.generalError());
                } else if (!_webBridge.handleUrl(uri.toString())) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(browserIntent);
                }
            }
        });

        _webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return true;
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        _pluginCallbacks.onCancel();
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.post(new Runnable() {
            @Override
            public void run() {
                final String html = getHTML();
                _webView.loadDataWithBaseURL("http://www.gigya.com", html, "text/html", "utf-8", null);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // TODO: 27/01/2019 Handle permission result.
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TODO: 27/01/2019 Handle result
    }

    private String getHTML() {
        organizeParameters();
        String flattenedParams = new JSONObject(_params).toString();
        final String template =
                "<head>" +
                        "<meta name='viewport' content='initial-scale=1,maximum-scale=1,user-scalable=no' />" +
                        "<script>" +
                        "function onJSException(ex) {" +
                        "document.location.href = '%s://%s?ex=' + encodeURIComponent(ex);" +
                        "}" +
                        "function onJSLoad() {" +
                        "if (gigya && gigya.isGigya)" +
                        "window.__wasSocializeLoaded = true;" +
                        "}" +
                        "setTimeout(function() {" +
                        "if (!window.__wasSocializeLoaded)" +
                        "document.location.href = '%s://%s';" +
                        "}, %s);" +
                        "</script>" +
                        "<script src='https://cdns." + _apiDomain + "/JS/gigya.js?apikey=%s' type='text/javascript' onLoad='onJSLoad();'>" +
                        "{" +
                        "deviceType: 'mobile'" +
                        "}" +
                        "</script>" +
                        "</head>" +
                        "<body>" +
                        "<div id='%s'></div>" +
                        "<script>" +
                        "%s" +
                        "try {" +
                        "gigya._.apiAdapters.mobile.showPlugin('%s', %s);" +
                        "} catch (ex) { onJSException(ex); }" +
                        "</script>" +
                        "</body>";
        return String.format(template, REDIRECT_URL_SCHEME, ON_JS_EXCEPTION, REDIRECT_URL_SCHEME, ON_JS_LOAD_ERROR, JS_TIMEOUT, _apiKey, CONTAINER_ID, "", _plugin, flattenedParams);
    }

    private void organizeParameters() {
        _params.put("containerID", CONTAINER_ID);
        if (_params.containsKey("commentsUI")) {
            _params.put("hideShareButtons", true);
            if (_params.get("version") != null && (int) _params.get("version") == -1) {
                _params.put("version", 2);
            }
        }
        if (_params.containsKey("RatingUI") && _params.get("showCommentButton") == null) {
            _params.put("showCommentButton", false);
        }

        if (getView() != null) {
            int width = getView().getWidth();
            if (width == 0) {
                width = (int) (UiUtils.getScreenSize((Activity) getView().getContext()).first * 0.8);
            }
            _params.put("width", width);
        }
        // TODO: 27/01/2019 Add disabled providers...
    }

    public interface PluginFragmentCallbacks {
        void onEvent(String event);

        void onCancel();

        void onError(GigyaError error);
    }
}
