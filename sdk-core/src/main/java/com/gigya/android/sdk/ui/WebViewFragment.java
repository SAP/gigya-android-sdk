package com.gigya.android.sdk.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gigya.android.sdk.R;

import java.util.HashMap;
import java.util.Map;

public abstract class WebViewFragment extends DialogFragment {

    private static final String LOG_TAG = "WebViewFragment";

    public static final String ARG_PARAMS = "arg_params";

    // Content views.
    protected WebView _webView;
    protected ProgressBar _progressBar;
    protected HashMap<String, Object> _params;

    // Style parameters.
    protected boolean _fullScreen;
    @Nullable
    protected String _url, _body, _redirectPrefix, _title;

    protected abstract void parseArguments();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseArguments();
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

    protected void dismissAndFinish() {
        _webView.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() == null) {
            return null;
        }
        return inflater.inflate(R.layout.gigya_fragment_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Reference views.
        _webView = view.findViewById(R.id.web_frag_web_view);
        _progressBar = view.findViewById(R.id.web_frag_progress_bar);
        // Title text view.
        final TextView titleTextView = view.findViewById(R.id.web_frag_title_text);
        if (_title != null) {
            titleTextView.setVisibility(View.VISIBLE);
            titleTextView.setText(_title);
        } else {
            titleTextView.setVisibility(View.GONE);
        }

        setUpWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void setUpWebView() {
        final WebSettings webSettings = _webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
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
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                }
            }
        }
    }

    public interface WebViewFragmentLifecycleCallbacks {

        void onWebViewResult(Map<String, Object> result);

        void onWebViewCancel();

    }
}
