package com.gigya.android.sdk.ui.plugin;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.ui.Presenter;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class GigyaPluginWebViewClient extends WebViewClient {

    final private static String LOG_TAG = "GigyaPluginWebViewClient";

    final private IGigyaPluginWebViewClientInteractions _interactions;

    public GigyaPluginWebViewClient(@NonNull IGigyaPluginWebViewClientInteractions interactions) {
        _interactions = interactions;
    }

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
        _interactions.onPageStarted();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventName", "error");
        eventMap.put("errorCode", error.getErrorCode());
        eventMap.put("description", error.getDescription());
        eventMap.put("dismiss", true);
        GigyaLogger.debug(LOG_TAG, "onReceivedError: " + eventMap.toString());
        //_interactions.onPageError(new GigyaPluginEvent(eventMap));
    }

    private void overrideUrlLoad(final Uri uri) {
        final Map<String, Object> eventMap = new HashMap<>();
        final String uriString = uri.toString();
        if (isJSLoadError(uri)) {
            eventMap.put("eventName", "error");
            eventMap.put("description", "Failed loading socialize.js");
            eventMap.put("errorCode", 500032);
            eventMap.put("dismiss", true);
            _interactions.onPageError(new GigyaPluginEvent(eventMap));
        } else if (isJSException(uri)) {
            eventMap.put("eventName", "error");
            eventMap.put("errorCode", 405001);
            eventMap.put("description", "Javascript error while loading plugin. Please make sure the plugin name is correct.");
            eventMap.put("dismiss", true);
            _interactions.onPageError(new GigyaPluginEvent(eventMap));
        } else if (!_interactions.onUrlInvoke(uriString)) {
            _interactions.onBrowserIntent(uri);
        }
    }

    private boolean isJSLoadError(final Uri uri) {
        return UrlUtils.isGigyaScheme(uri.getScheme()) && ObjectUtils.safeEquals(uri.getHost(), Presenter.Consts.ON_JS_LOAD_ERROR);
    }

    private boolean isJSException(final Uri uri) {
        return UrlUtils.isGigyaScheme(uri.getScheme()) && ObjectUtils.safeEquals(uri.getHost(), Presenter.Consts.ON_JS_EXCEPTION);
    }
}

interface IGigyaPluginWebViewClientInteractions {
    void onPageStarted();

    void onPageError(GigyaPluginEvent errorEvent);

    boolean onUrlInvoke(final String url);

    void onBrowserIntent(final Uri uri);
}


