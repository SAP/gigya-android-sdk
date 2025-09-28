package com.gigya.android.sdk.ui.plugin;

import static com.gigya.android.sdk.ui.plugin.GigyaPluginEvent.EVENT_NAME;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.R;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.HostActivity;
import com.gigya.android.sdk.ui.Presenter;
import com.gigya.android.sdk.ui.WebViewConfig;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

@SuppressLint("ValidFragment")
public class GigyaPluginFragment<A extends GigyaAccount> extends DialogFragment implements IGigyaPluginFragment<A>, HostActivity.OnBackPressListener, IGigyaPluginFileErrorCallback {

    private static final String LOG_TAG = "GigyaPluginFragment";

    private static final String BASE_URL = "https://www.gigya.com";
    private static final String MIME_TYPE = "text/html";
    private static final String ENCODING = "UTF-8";

    public static final String PLUGIN_SCREENSETS = "accounts.screenSet";
    public static final String PLUGIN_COMMENTS = "comments.commentsUI";

    /*
    Web bridge invocation callback. Injected into the web bridge when initializing the fragment.
     */
    public interface IBridgeCallbacks<A extends GigyaAccount> {

        void invokeCallback(String invocation);

        void onPluginEvent(GigyaPluginEvent event, String containerID);

        void onPluginAuthEvent(@PluginAuthEventDef.PluginAuthEvent String method, @Nullable A accountObj);
    }

    // Dependencies.
    private Config _config;
    private IGigyaWebBridge<A> _gigyaWebBridge;

    // Setter data.
    private GigyaPluginCallback<A> _pluginCallback;
    private String _html;
    private boolean _obfuscation = false;

    // Members.
    private WebView _webView;
    private ProgressBar _progressBar;
    private GigyaPluginFileChooser _fileChooserClient;

    public void setConfig(Config config) {
        _config = config;
    }

    private final Gson gson = new Gson();

    public void setWebBridge(IGigyaWebBridge<A> gigyaWebBridge) {
        _gigyaWebBridge = gigyaWebBridge;
    }

    @Override
    public void setCallback(GigyaPluginCallback<A> gigyaPluginCallback) {
        _pluginCallback = gigyaPluginCallback;
    }

    @Override
    public void setHtml(String html) {
        _html = html;
    }

    private WebViewConfig getWebViewConfig() {
        if (_config == null) return new WebViewConfig();
        return _config.getWebViewConfig();
    }

    //region LIFE CYCLE

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (_config != null) {
            String configJson = gson.toJson(_config);
            outState.putString("config_json", configJson);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HostActivity) {
            ((HostActivity) context).addBackPressListener(this);
        }
    }

    @Override
    public void onDetach() {
        if (getContext() instanceof HostActivity) {
            ((HostActivity) getContext()).removeBackPressListener(this);
        }
        super.onDetach();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String configJson = savedInstanceState.getString("config_json");
            if (configJson != null) {
                _config = gson.fromJson(configJson, Config.class);
            }
        }

        // Parse arguments.
        if (getArguments() != null) {
            _obfuscation = getArguments().getBoolean(Presenter.ARG_OBFUSCATE, false);
        }

        if (_config == null) {
            GigyaLogger.error(LOG_TAG, "Config is mandatory - cannot remain null.");

            if (getActivity() != null) {
                getActivity().finish();
            }
        }

        if (!getWebViewConfig().isJavaScriptEnabled()) {
            GigyaLogger.error(LOG_TAG, "JavaScript is disabled. This may cause the plugin to not function properly.");
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (_pluginCallback != null) {
            _pluginCallback.onCanceled();
        }
        return true;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            if (window != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                final Bundle args = getArguments();
                if (args != null) {
                    // Check fullscreen mode request.
                    final boolean fullScreen = args.getBoolean(Presenter.ARG_STYLE_SHOW_FULL_SCREEN, false);
                    if (fullScreen) {
                        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    }
                }
            }
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gigya_fragment_webview, container, false);
    }

    @Override
    public void onDestroyView() {
        if (_fileChooserClient != null) {
            _fileChooserClient.clearCachedImage();
        }
        if (_gigyaWebBridge != null) {
            _gigyaWebBridge.detachFrom(_webView);
        }
        super.onDestroyView();
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

        // Load URL.
        loadUrl(view);
    }

    //endregion

    @Override
    public void setUpUiElements(final View fragmentView) {
        // Reference UI elements. Must be called first!
        _webView = fragmentView.findViewById(R.id.web_frag_web_view);
        _progressBar = fragmentView.findViewById(R.id.web_frag_progress_bar);
    }

    @SuppressLint({"JavascriptInterface", "AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    public void setUpWebViewElement() {
        _fileChooserClient = new GigyaPluginFileChooser(this);

        final WebSettings webSettings = _webView.getSettings();
        webSettings.setJavaScriptEnabled(
                getWebViewConfig().isJavaScriptEnabled());
        webSettings.setAllowFileAccess(getWebViewConfig().isAllowFileAccess());
        webSettings.setDomStorageEnabled(getWebViewConfig().isLocalStorage());
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadWithOverviewMode(true);

        // Setting up a custom veb view client to handle WebView interaction.
        _webView.setWebViewClient(_webViewClient);
        _webView.setWebChromeClient(_fileChooserClient);

        attachBridge();
    }

    private void attachBridge() {
        if (_pluginCallback == null) {
            GigyaLogger.error(LOG_TAG, "Plugin callback is mandatory - cannot remain null.");
            return;
        }
        // Web bridge.
        _gigyaWebBridge.
                withObfuscation(_obfuscation).
                attachTo(
                        _webView,
                        new GigyaPluginCallback<A>() {
                            @Override
                            public void onError(GigyaPluginEvent event) {
                                _pluginCallback.onError(event);
                                if (_progressBar != null) {
                                    _progressBar.setVisibility(View.INVISIBLE);
                                }
                            }

                            @Override
                            public void onCanceled() {
                                if (_progressBar != null) {
                                    _progressBar.setVisibility(View.INVISIBLE);
                                }
                                _pluginCallback.onCanceled();
                            }

                            @Override
                            public void onBeforeValidation(@NonNull GigyaPluginEvent event) {
                                _pluginCallback.onBeforeValidation(event);
                            }

                            @Override
                            public void onAfterValidation(@NonNull GigyaPluginEvent event) {
                                _pluginCallback.onAfterValidation(event);
                            }

                            @Override
                            public void onBeforeSubmit(@NonNull GigyaPluginEvent event) {
                                _pluginCallback.onBeforeSubmit(event);
                            }

                            @Override
                            public void onSubmit(@NonNull GigyaPluginEvent event) {
                                _pluginCallback.onSubmit(event);
                            }

                            @Override
                            public void onAfterSubmit(@NonNull GigyaPluginEvent event) {
                                _pluginCallback.onAfterSubmit(event);
                            }

                            @Override
                            public void onBeforeScreenLoad(@NonNull GigyaPluginEvent event) {
                                _pluginCallback.onBeforeScreenLoad(event);
                            }

                            @Override
                            public void onAfterScreenLoad(@NonNull GigyaPluginEvent event) {
                                _pluginCallback.onAfterScreenLoad(event);
                            }

                            @Override
                            public void onFieldChanged(@NonNull GigyaPluginEvent event) {
                                _pluginCallback.onFieldChanged(event);
                            }

                            @Override
                            public void onHide(@NonNull GigyaPluginEvent event, String reason) {
                                _pluginCallback.onHide(event, reason);
                                final boolean isFinal = isFlowFinalized(event);
                                if (getActivity() != null && isFinal) {
                                    // Force finish the Host activity only when flow is marked as finalized.
                                    getActivity().finish();
                                }
                            }

                            @Override
                            public void onLogin(@NonNull A accountObj) {
                                _pluginCallback.onLogin(accountObj);
                            }

                            @Override
                            public void onLogout() {
                                _pluginCallback.onLogout();
                            }

                            @Override
                            public void onConnectionAdded() {
                                _pluginCallback.onConnectionAdded();
                            }

                            @Override
                            public void onConnectionRemoved() {
                                _pluginCallback.onConnectionRemoved();
                            }
                        },
                        _progressBar);


    }

    boolean isFlowFinalized(GigyaPluginEvent event) {
        Object parameter = event.getEventMap().get("isFlowFinalized");
        if (parameter == null) return false;
        if (parameter instanceof String) {
            return Boolean.parseBoolean((String) parameter);
        } else if (parameter instanceof Boolean) {
            return (boolean) parameter;
        }
        return false;
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
        // File access permission no longer needed.
    }

    /*
    Web View client implementations.
     */
    private final GigyaPluginWebViewClient _webViewClient = new GigyaPluginWebViewClient(
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
                    try {
                        startActivity(browserIntent);
                    } catch (ActivityNotFoundException ex) {
                        //ex.printStackTrace();
                        GigyaLogger.error(LOG_TAG, "Browser not available to handle Intent.ACTION_VIEW");
                    }
                }
            });

    @Override
    public void onFileError(GigyaError error) {
        if (_pluginCallback != null) {
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put(EVENT_NAME, "Error");
            eventMap.put("errorCode", error.getErrorCode());
            eventMap.put("errorMessage", error.getLocalizedMessage());
            _pluginCallback.onError(new GigyaPluginEvent(eventMap));
        }
    }
}
