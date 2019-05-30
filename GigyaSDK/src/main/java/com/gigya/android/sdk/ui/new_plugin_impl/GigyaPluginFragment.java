package com.gigya.android.sdk.ui.new_plugin_impl;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.R;
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent;

import org.json.JSONArray;

import java.util.Locale;

@SuppressLint("ValidFragment")
public class GigyaPluginFragment extends DialogFragment implements IGigyaPluginFragment {

    private static final String LOG_TAG = "GigyaPluginFragment";

    private static final String BASE_URL = "http://www.gigya.com";
    private static final String MIME_TYPE = "text/html";
    private static final String ENCODING = "utf-8";

    /*
    Web bridge invocation callback. Injected into the web bridge when initializing the fragment.
     */
    public interface IBridgeCallbacks {

        void invokeCallback(String invocation);

        void onPluginEvent(GigyaPluginEvent event, String containerID);
    }

    // Dependencies.
    final private Config _config;
    final private GigyaWebBridge _gigyaWebBridge;

    // Setter data.
    private GigyaPluginCallback _pluginCallback;
    private String _html;
    private boolean _obfuscation = false;

    private WebView _webView;
    private ProgressBar _progressBar;
    private GigyaPluginFileChooser _fileChooserClient;

    public GigyaPluginFragment(Config config,
                               GigyaWebBridge gigyaWebBridge) {
        _config = config;
        _gigyaWebBridge = gigyaWebBridge;
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        evaluateActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        evaluatePermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setUpUiElements(view);
        setUpWebViewElement();
        loadUrl(view);
    }

    //endregion

    @Override
    public void setUpUiElements(final View fragmentView) {
        // Reference UI elements. Must be called first!
        _webView = fragmentView.findViewById(R.id.web_frag_web_view);
        _progressBar = fragmentView.findViewById(R.id.web_frag_progress_bar);
    }

    @SuppressLint({"JavascriptInterface", "AddJavascriptInterface"})
    @Override
    public void setUpWebViewElement() {
        // Setting up a custom veb view client to handle WebView interaction.
        _webView.setWebViewClient(_webViewClient);
        _webView.addJavascriptInterface(_JSInterface, "__gigAPIAdapterSettings");
        _fileChooserClient = new GigyaPluginFileChooser(this);
        _webView.setWebChromeClient(_fileChooserClient);

        // Web bridge.
        _gigyaWebBridge.withObfuscation(_obfuscation);
        _gigyaWebBridge.setInvocationCallback(new IBridgeCallbacks() {
            @Override
            public void invokeCallback(final String invocation) {
                if (_webView != null) {
                    _webView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (android.os.Build.VERSION.SDK_INT > 18) {
                                _webView.evaluateJavascript(invocation, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        GigyaLogger.debug("evaluateJavascript Callback", value);
                                    }
                                });
                            } else {
                                _webView.loadUrl(invocation);
                            }
                        }
                    });
                }
            }

            @Override
            public void onPluginEvent(GigyaPluginEvent event, String containerID) {
                // TODO: 2019-05-30 Implement interactions.
            }
        });
    }

    @Override
    public void loadUrl(final View fragmentView) {
        fragmentView.post(new Runnable() {
            @Override
            public void run() {
                _webView.loadDataWithBaseURL(BASE_URL, _html, MIME_TYPE, ENCODING, null);
            }
        });
    }

    @Override
    public void dismissWhenDone() {

    }

    @Override
    public void evaluateActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != GigyaPluginFileChooser.FILE_CHOOSER_MEDIA_REQUEST_CODE) {
                super.onActivityResult(requestCode, resultCode, data);
            } else {
                _fileChooserClient.onActivityResult(resultCode, data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void evaluatePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == GigyaPluginFileChooser.FIRE_ACCESS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GigyaLogger.debug(LOG_TAG, "External storage permission explicitly granted.");
                _fileChooserClient.onRequestPermissionsResult(requestCode, permissions, grantResults);
            } else {
                // Permission denied by the user.
                GigyaLogger.debug(LOG_TAG, "External storage permission explicitly denied.");
            }
        }
    }

    // Web View client implementations.
    private GigyaPluginWebViewClient _webViewClient = new GigyaPluginWebViewClient(
            new IGigyaPluginWebViewClientInteractions() {

                @Override
                public void onPageStarted() {
                    _progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onPageError(GigyaPluginEvent errorEvent) {
                    _pluginCallback.onError(errorEvent);
                }

                @Override
                public boolean onUrlInvoke(String url) {
                    return _gigyaWebBridge.invoke(url);
                }

                @Override
                public void onBrowserIntent(Uri uri) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(browserIntent);
                }
            });

    private Object _JSInterface = new Object() {

        private static final String ADAPTER_NAME = "mobile";

        @JavascriptInterface
        public String getAPIKey() {
            return _config.getApiKey();
        }

        @JavascriptInterface
        public String getAdapterName() {
            return ADAPTER_NAME;
        }

        @JavascriptInterface
        public String getObfuscationStrategy() {
            return _obfuscation ? "base64" : "";
        }

        @JavascriptInterface
        public String getFeatures() {
            JSONArray features = new JSONArray();
            for (GigyaWebBridge.Feature feature : GigyaWebBridge.Feature.values()) {
                features.put(feature.toString().toLowerCase(Locale.ROOT));
            }
            return features.toString();
        }

        @JavascriptInterface
        public boolean sendToMobile(String action, String method, String queryStringParams) {
            return _gigyaWebBridge.invoke(action, method, queryStringParams);
        }

    };
}
