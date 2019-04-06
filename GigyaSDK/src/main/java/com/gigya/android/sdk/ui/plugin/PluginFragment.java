package com.gigya.android.sdk.ui.plugin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaPluginCallback;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.ui.WebViewFragment;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UiUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.AFTER_SCREEN_LOAD;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.AFTER_SUBMIT;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.AFTER_VALIDATION;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.BEFORE_SCREEN_LOAD;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.BEFORE_SUBMIT;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.BEFORE_VALIDATION;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.ERROR;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.FIELD_CHANGED;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.HIDE;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.LOAD;
import static com.gigya.android.sdk.ui.plugin.PluginDefinitions.SUBMIT;

public class PluginFragment<T extends GigyaAccount> extends WebViewFragment implements IWebBridge<T> {

    public static final String LOG_TAG = "PluginFragment";

    public static final String PLUGIN_SCREENSETS = "accounts.screenSet";
    public static final String PLUGIN_COMMENTS = "comments.commentsUI";

    // Argument keys.
    public static final String ARG_OBFUSCATE = "arg_obfuscate";
    public static final String ARG_PLUGIN = "arg_plugin";

    // Constants.
    private static final String REDIRECT_URL_SCHEME = "gsapi";
    private static final String ON_JS_LOAD_ERROR = "on_js_load_error";
    private static final String ON_JS_EXCEPTION = "on_js_exception";
    private static final String CONTAINER_ID = "pluginContainer";
    private static final int JS_TIMEOUT = 10000;

    private Config _config;
    private IWebBridgeFactory _wbFactory;
    private GigyaPluginCallback _gigyaPluginCallback;

    private boolean _obfuscate;
    private String _plugin;

    private WebBridge _webBridge;

    public static void present(AppCompatActivity activity, Bundle args, Config config, IWebBridgeFactory _wbFactory, GigyaPluginCallback gigyaPluginCallback) {
        PluginFragment fragment = new PluginFragment();
        fragment.setArguments(args);
        fragment.inject(config, _wbFactory, gigyaPluginCallback);
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(fragment, LOG_TAG);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    protected boolean wrapContent() {
        return !_fullScreen;
    }


    public void inject(Config config, IWebBridgeFactory wbFactory, GigyaPluginCallback gigyaPluginCallback) {
        _config = config;
        _wbFactory = wbFactory;
        _gigyaPluginCallback = gigyaPluginCallback;
    }

    @Override
    protected void parseArguments() {
        Bundle args = getArguments();
        if (args != null) {
            _obfuscate = args.getBoolean(ARG_OBFUSCATE);
            _plugin = args.getString(ARG_PLUGIN);
            _params = (HashMap<String, Object>) args.getSerializable(ARG_PARAMS);
        } else {
            GigyaLogger.error(LOG_TAG, "Missing arguments. Dismiss fragment");
            dismiss();
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* When using GigyaPluginPresenter.SHOW_FULL_SCREEN option the style attribute will be ignored. */
        if (_fullScreen) {
            setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
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

    //region RESULT & PERMISSIONS


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != FILE_CHOOSER_MEDIA_REQUEST_CODE || _imagePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                if (data != null && data.getDataString() != null) {
                    String dataString = data.getDataString();
                    results = new Uri[]{Uri.parse(dataString)};
                }
                // If there is not data, then we may have taken a photo
                else if (_cameraTempImagePath != null) {
                    results = new Uri[]{Uri.parse(_cameraTempImagePath)};
                }
            }
            _imagePathCallback.onReceiveValue(results);
            _imagePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        GigyaLogger.debug(LOG_TAG, "onRequestPermissionsResult:");
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GigyaLogger.debug(LOG_TAG, "External storage permission explicitly granted.");
                if (_imagePathCallback != null) {
                    sendImageChooserIntent();
                }
            } else {
                // Permission denied by the user.
                GigyaLogger.debug(LOG_TAG, "External storage permission explicitly denied.");
            }
        }
    }

    //endregion

    //region WEBVIEW SETUP

    @Override
    protected void setUpWebView() {
        super.setUpWebView();

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

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                GigyaLogger.error(LOG_TAG, "setWebViewClient: onReceivedError: " + error.getDescription());
                if (_gigyaPluginCallback != null) {
                    Map<String, Object> eventMap = new HashMap<>();
                    eventMap.put("eventName", "error");
                    eventMap.put("errorCode", error.getErrorCode());
                    eventMap.put("description", error.getDescription());
                    eventMap.put("dismiss", true);
                    _gigyaPluginCallback.onError(new GigyaPluginEvent(eventMap));
                }
                dismissAndFinish();
            }

            private void overrideUrlLoad(Uri uri) {
                final Map<String, Object> eventMap = new HashMap<>();
                if (ObjectUtils.safeEquals(uri.getScheme(), REDIRECT_URL_SCHEME) && ObjectUtils.safeEquals(uri.getHost(), ON_JS_LOAD_ERROR)) {
                    eventMap.put("eventName", "error");
                    eventMap.put("description", "Failed loading socialize.js");
                    eventMap.put("errorCode", 500032);
                    eventMap.put("dismiss", true);
                    if (_gigyaPluginCallback != null) {
                        _gigyaPluginCallback.onError(new GigyaPluginEvent(eventMap));
                    }
                    dismissAndFinish();
                } else if (ObjectUtils.safeEquals(uri.getScheme(), REDIRECT_URL_SCHEME) && ObjectUtils.safeEquals(uri.getHost(), ON_JS_EXCEPTION)) {
                    eventMap.put("eventName", "error");
                    eventMap.put("errorCode", 405001);
                    eventMap.put("description", "Javascript error while loading plugin. Please make sure the plugin name is correct.");
                    eventMap.put("dismiss", true);
                    if (_gigyaPluginCallback != null) {
                        _gigyaPluginCallback.onError(new GigyaPluginEvent(eventMap));
                    }
                    dismissAndFinish();
                } else if (!_webBridge.handleUrl(uri.toString())) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(browserIntent);
                }
            }
        });

        setUpFileChooserInteraction();

        // Setup web bridge.
        _webBridge = _wbFactory.create(_obfuscate, this);
        _webBridge.attach(_webView);
    }

    //endregion

    //region FILE CHOOSER

    private static final int PERMISSION_REQUEST_CODE = 14;
    private static final int FILE_CHOOSER_MEDIA_REQUEST_CODE = 15;
    private String _cameraTempImagePath;
    private ValueCallback<Uri[]> _imagePathCallback;

    private void setUpFileChooserInteraction() {
        _webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                GigyaLogger.debug(LOG_TAG, "onShowFileChooser: ");

                if (_imagePathCallback != null) {
                    _imagePathCallback.onReceiveValue(null);
                }

                _imagePathCallback = filePathCallback;

                final int externalStoragePermission = ContextCompat.checkSelfPermission(webView.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (externalStoragePermission == PackageManager.PERMISSION_GRANTED) {
                    sendImageChooserIntent();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                } else {
                    GigyaLogger.error(LOG_TAG, "Application must include Manifest.permission.WRITE_EXTERNAL_STORAGE in order to communicate with file system");
                    return false;
                }
                return true;
            }
        });
    }

    private void sendImageChooserIntent() {
        if (getActivity() == null) {
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = createImageFile();
                takePictureIntent.putExtra("imagePath", _cameraTempImagePath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (imageFile != null) {
                _cameraTempImagePath = "file:" + imageFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
            } else {
                takePictureIntent = null;
            }
        }

        final Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        final Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, takePictureIntent != null ?
                new Intent[]{takePictureIntent} : new Intent[0]);
        startActivityForResult(chooserIntent, FILE_CHOOSER_MEDIA_REQUEST_CODE);
    }

    @SuppressLint("SimpleDateFormat")
    private File createImageFile() throws IOException {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName = "JPEG_" + timeStamp + "_";
        final File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    //endregion

    //region HTML & PARAMETERS

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
                        "<script src='https://cdns." + _config.getApiDomain() + "/JS/gigya.js?apikey=%s' type='text/javascript' onLoad='onJSLoad();'>" +
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
        return String.format(template, REDIRECT_URL_SCHEME, ON_JS_EXCEPTION, REDIRECT_URL_SCHEME, ON_JS_LOAD_ERROR, JS_TIMEOUT,
                _config.getApiKey(), CONTAINER_ID, "", _plugin, flattenedParams);
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
                width = (int) (UiUtils.getScreenSize((Activity) getView().getContext()).first * 0.9);
            }
            _params.put("width", UiUtils.pixelsToDp(width, getView().getContext()));
        }
    }

    //endregion

    //region WEB BRIDGE INTERFACING

    private Handler _uiHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onPluginEvent(GigyaPluginEvent event, String containerID) {
        if (containerID.equals(CONTAINER_ID)) {
            throttleEvents(event);
        }
    }

    @Override
    public void onAuthEvent(WebBridge.AuthEvent authEvent, T obj) {
        throttleAuthEvents(authEvent, obj);
    }

    @Override
    public void onCancel() {
        // Stub.
    }

    @Override
    public void onError(GigyaError error) {
        Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        final Map<String, Object> eventMap = new Gson().fromJson(error.getData(), mapType);
        _gigyaPluginCallback.onError(new GigyaPluginEvent(eventMap));

    }

    private void throttleEvents(final GigyaPluginEvent event) {
        final @PluginDefinitions.PluginEvent String eventName = event.getEvent();
        GigyaLogger.debug(LOG_TAG, "throttleEvents: event = " + eventName);
        if (eventName != null) {
            _uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch (eventName) {
                        case BEFORE_SCREEN_LOAD:
                            _progressBar.setVisibility(View.VISIBLE);
                            _gigyaPluginCallback.onBeforeScreenLoad(event);
                            break;
                        case LOAD:
                            _progressBar.setVisibility(View.INVISIBLE);
                            break;
                        case AFTER_SCREEN_LOAD:
                            _progressBar.setVisibility(View.INVISIBLE);
                            _gigyaPluginCallback.onAfterScreenLoad(event);
                            break;
                        case FIELD_CHANGED:
                            _gigyaPluginCallback.onFieldChanged(event);
                            break;
                        case BEFORE_VALIDATION:
                            _gigyaPluginCallback.onBeforeValidation(event);
                            break;
                        case AFTER_VALIDATION:
                            break;
                        case BEFORE_SUBMIT:
                            _gigyaPluginCallback.onBeforeSubmit(event);
                            break;
                        case SUBMIT:
                            _gigyaPluginCallback.onSubmit(event);
                            break;
                        case AFTER_SUBMIT:
                            _gigyaPluginCallback.onAfterSubmit(event);
                            break;
                        case HIDE:
                            final String reason = (String) event.getEventMap().get("reason");
                            _gigyaPluginCallback.onHide(event, reason);
                            dismissAndFinish();
                            break;
                        case ERROR:
                            _gigyaPluginCallback.onError(event);
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }

    private void throttleAuthEvents(final WebBridge.AuthEvent authEvent, final T obj) {
        GigyaLogger.debug(LOG_TAG, "throttleAuthEvents: event = " + authEvent.ordinal());
        _uiHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (authEvent) {
                    case LOGIN:
                        _gigyaPluginCallback.onLogin(obj);
                        break;
                    case LOGOUT:
                        _gigyaPluginCallback.onLogout();
                        break;
                    case ADD_CONNECTION:
                        _gigyaPluginCallback.onConnectionAdded();
                        break;
                    case REMOVE_CONNECTION:
                        _gigyaPluginCallback.onConnectionRemoved();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    //endregion

}
