package com.gigya.android.sdk.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gigya.android.sdk.utils.ObjectUtils;
import com.gigya.android.sdk.utils.UiUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class WebViewFragment extends DialogFragment {

    private static final String LOG_TAG = "WebViewFragment";

    public static final String ARG_PARAMS = "arg_params";

    /* Content views. */
    protected WebView _webView;
    protected FrameLayout _contentView, _webFrame;
    private LinearLayout _webContentView;
    protected ProgressBar _progressBar;

    protected HashMap<String, Object> _params;

    /* Style parameters. */
    protected boolean _fullScreen;
    private int _progressColorStyle = Color.BLACK;
    private float _cornerRadiusStyle = 16f;
    private float _maxWidth = 0f;
    private float _maxHeight = 0f;

    @Nullable
    protected String _url, _body, _redirectPrefix, _title;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseArguments();
        parseStyleParameters();
        setUpWebView();
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

    protected boolean wrapContent() {
        return true;
    }

    private void parseStyleParameters() {
        if (_params != null) {
            _fullScreen = (boolean) ObjectUtils.firstNonNull(_params.get(GigyaPresenter.SHOW_FULL_SCREEN), false);
            _progressColorStyle = (int) ObjectUtils.firstNonNull(_params.get(GigyaPresenter.PROGRESS_COLOR), Color.BLACK);
            _cornerRadiusStyle = (float) ObjectUtils.firstNonNull(_params.get(GigyaPresenter.CORNER_RADIUS), 16f);
            _maxWidth = (float) ObjectUtils.firstNonNull(_params.get(GigyaPresenter.DIALOG_MAX_WIDTH), 0f);
            _maxHeight = (float) ObjectUtils.firstNonNull(_params.get(GigyaPresenter.DIALOG_MAX_HEIGHT), 0f);
        }
    }

    protected abstract void parseArguments();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() == null) {
            return null;
        }
        if (getDialog().getWindow() != null && wrapContent()) {
            getDialog().getWindow().setBackgroundDrawable(getRoundedCornerBackground());
        }
        createView();
        _contentView.setFocusable(true);
        _contentView.setFocusableInTouchMode(true);

        return _contentView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void setUpWebView() {
        _webView = new WebView(getActivity());

        final WebSettings webSettings = _webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
    }

    private void createView() {
        if (getActivity() == null) {
            return;
        }

        /* UI sizes. */
        final int dip8 = (int) UiUtils.dpToPixel(8, getActivity());
        final int dip16 = (int) UiUtils.dpToPixel(16, getActivity());
        final Pair<Integer, Integer> screenSize = UiUtils.getScreenSize(getActivity());

        /* Content view. */
        setupMainFrames(getActivity(), screenSize);

        /* WebView content. */
        setupWebContent(getActivity(), dip16);

        /* Title text view. */
        setupTitleTextView(dip16, dip8, dip8, 0);

        /* Web frame container. */
        setupWebViewUI();

        /* Progress bar. */
        setupProgressView(dip16);
    }

    //region UI BLOCKS

    private void setupMainFrames(Context context, Pair<Integer, Integer> screenSize) {
        _contentView = new FrameLayout(context);
        // Content view is always match/match
        final ViewGroup.LayoutParams contentParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        );
        _contentView.setLayoutParams(contentParams);

        FrameLayout.LayoutParams webFrameParams;
        _webFrame = new FrameLayout(context);
        if (_maxWidth == 0 || _maxHeight == 0) {
            webFrameParams = new FrameLayout.LayoutParams(
                    wrapContent() ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT,
                    wrapContent() ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT
            );
        } else {
            webFrameParams = getFixedLayoutParams(context, screenSize);
        }
        _contentView.addView(_webFrame, webFrameParams);
    }

    private void setupWebContent(Context context, int margin) {
        _webContentView = new LinearLayout(context);
        _webContentView.setOrientation(LinearLayout.VERTICAL);
        final FrameLayout.LayoutParams webContentParams = new FrameLayout.LayoutParams(
                wrapContent() ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT,
                wrapContent() ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT
        );
        if (wrapContent()) {
            webContentParams.setMargins(margin, margin, margin, margin);
        }
        _webFrame.addView(_webContentView, webContentParams);
    }

    private void setupTitleTextView(int marginLeft, int marginTop, int marginRight, int marginBottom) {
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
        titleViewParams.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        _webContentView.addView(_titleTextView, titleViewParams);
    }

    private void setupWebViewUI() {
        /* WebView. */
        final LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(
                wrapContent() ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT,
                wrapContent() ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT
        );
        webViewParams.gravity = Gravity.CENTER;
        _webContentView.addView(_webView, webViewParams);
    }

    private void setupProgressView(int margin) {
        _progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyle);
        _progressBar.setIndeterminate(true);
        _progressBar.setBackgroundColor(Color.TRANSPARENT);
        _progressBar.getIndeterminateDrawable().setColorFilter(_progressColorStyle, android.graphics.PorterDuff.Mode.SRC_IN);
        _progressBar.setVisibility(View.INVISIBLE); // Default behaviour is hidden.
        _progressBar.setPadding(margin, margin, margin, margin); // The padding is what sets the initial height.
        final FrameLayout.LayoutParams progressBarParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT);
        progressBarParams.gravity = Gravity.CENTER;
        _webFrame.addView(_progressBar, progressBarParams);
    }

    //endregion

    //region UI CUSTOMIZATION

    private Drawable getRoundedCornerBackground() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(Color.WHITE);
        gradientDrawable.setCornerRadius(_cornerRadiusStyle);
        return gradientDrawable;
    }

    private FrameLayout.LayoutParams getFixedLayoutParams(Context context, Pair<Integer, Integer> screenSize) {
        return new FrameLayout.LayoutParams(
                (int) (UiUtils.isPortrait(context) ?
                        Math.min(screenSize.first, screenSize.second) * _maxWidth :
                        Math.max(screenSize.first, screenSize.second) * _maxWidth),
                (int) (UiUtils.isPortrait(context) ?
                        Math.max(screenSize.first, screenSize.second) * _maxHeight :
                        Math.min(screenSize.first, screenSize.second) * _maxHeight
                )
        );
    }

    //endregion

    public interface WebViewFragmentLifecycleCallbacks {

        void onWebViewResult(Map<String, Object> result);

        void onWebViewCancel();

    }
}
