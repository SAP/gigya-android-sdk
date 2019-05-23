package com.gigya.android.sdk.ui.new_plugin_impl;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.R;
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent;
import com.gigya.android.sdk.ui.plugin.IWebBridgeFactory;

@SuppressLint("ValidFragment")
public class GigyaPluginFragment extends DialogFragment implements IGigyaPluginFragment {

    // Dependencies.
    final private Config _config;
    final private IWebBridgeFactory _wbFactory;

    // Setter data.
    private GigyaPluginCallback _pluginCallback;
    private String _html;

    private WebView _webView;
    private ProgressBar _progressBar;

    public GigyaPluginFragment(Config config, IWebBridgeFactory webBridgeFactory) {
        _config = config;
        _wbFactory = webBridgeFactory;
    }

    @Override
    public void setCallback(GigyaPluginCallback gigyaPluginCallback) {
        _pluginCallback = gigyaPluginCallback;
    }

    @Override
    public void setHtml(String html) {
        _html = html;
    }

    //region LIFE CYCLE

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gigya_fragment_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setUpUiElements(view);
        setUpWebViewElement();
        setUpFileInteractions();
        loadUrl(view);
    }

    //endregion

    @Override
    public void setUpUiElements(final View fragmentView) {
        // Reference UI elements. Must be called first!
        _webView = fragmentView.findViewById(R.id.web_frag_web_view);
        _progressBar = fragmentView.findViewById(R.id.web_frag_progress_bar);
    }

    @Override
    public void setUpWebViewElement() {
        _webView.setWebViewClient(_webViewClient);
    }

    @Override
    public void loadUrl(final View fragmentView) {
        fragmentView.post(new Runnable() {
            @Override
            public void run() {
                _webView.loadDataWithBaseURL("http://www.gigya.com", _html, "text/html", "utf-8", null);
            }
        });
    }

    @Override
    public void setUpFileInteractions() {

    }

    @Override
    public void dismissWhenDone() {

    }

    @Override
    public void evaluateActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void evaluatePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }

    private GigyaPluginWebViewClient _webViewClient = new GigyaPluginWebViewClient(new IGigyaPluginWebViewClientInteractions() {
        @Override
        public void onPageStarted() {
            _progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageError(GigyaPluginEvent errorEvent) {
            _pluginCallback.onError(errorEvent);
        }

        @Override
        public void onUrlInvoke(String url) {
            // TODO: 2019-05-23 Bridge invocation.
        }

        @Override
        public void onBrowserIntent(Uri uri) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(browserIntent);
        }
    });
}
