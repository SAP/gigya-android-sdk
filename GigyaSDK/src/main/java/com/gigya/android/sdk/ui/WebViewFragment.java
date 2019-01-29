package com.gigya.android.sdk.ui;

import android.annotation.SuppressLint;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gigya.android.sdk.utils.UiUtils;

import java.util.Map;

public abstract class WebViewFragment extends DialogFragment {

    private static final String TAG = "WebViewFragment";

    public static int PROGRESS_COLOR = Color.BLACK; // Default color.1

    /* Content views. */
    protected WebView _webView;
    protected LinearLayout _contentView;
    protected ProgressBar _progressBar;

    @Nullable
    protected String _url, _body, _redirectPrefix, _title;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseArguments();
        setUpWebView();
    }

    protected boolean wrapContent() {
        return true;
    }

    protected abstract void parseArguments();

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
        setupContentView(getActivity(), screenSize);

        /* Title text view. */
        setupTitleTextView(dip16, dip8, dip8, 0);

        /* Web frame container. */
        FrameLayout webFrame = setupWebView(getActivity(), dip8, screenSize);

        /* Progress bar. */
        setupProgressView(webFrame, dip16);
    }

    //region UI blocks

    private void setupContentView(Context context, Pair<Integer, Integer> screenSize) {
        _contentView = new LinearLayout(context);
        _contentView.setOrientation(LinearLayout.VERTICAL);
        final ViewGroup.LayoutParams contentParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                wrapContent() ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT
        );
        _contentView.setLayoutParams(contentParams);
    }

    @NonNull
    private FrameLayout setupWebView(Context context, int margin, Pair<Integer, Integer> screenSize) {
        FrameLayout webFrame = new FrameLayout(context);
        final LinearLayout.LayoutParams webFrameParams = new LinearLayout.LayoutParams(
                (int) (UiUtils.isPortrait(context) ?
                        Math.min(screenSize.first, screenSize.second) * 0.9 :
                        Math.max(screenSize.first, screenSize.second) * 0.9),
                wrapContent() ? LinearLayout.LayoutParams.WRAP_CONTENT :
                        (int) (UiUtils.isPortrait(context) ?
                                Math.max(screenSize.first, screenSize.second) * 0.9 :
                                Math.min(screenSize.first, screenSize.second) * 0.9
                        )
        );
        webFrameParams.weight = 1;
        _contentView.addView(webFrame, webFrameParams);

        /* WebView. */
        final FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                wrapContent() ? FrameLayout.LayoutParams.WRAP_CONTENT : FrameLayout.LayoutParams.MATCH_PARENT
        );
        webViewParams.setMargins(margin, margin, margin, margin);
        webViewParams.gravity = Gravity.CENTER;
        webFrame.addView(_webView, webViewParams);
        return webFrame;
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
        titleViewParams.weight = 1;
        _contentView.addView(_titleTextView, titleViewParams);
    }

    private void setupProgressView(FrameLayout webFrame, int margin) {
        _progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyle);
        _progressBar.setIndeterminate(true);
        _progressBar.getIndeterminateDrawable().setColorFilter(PROGRESS_COLOR, android.graphics.PorterDuff.Mode.SRC_IN);
        _progressBar.setVisibility(View.GONE); // Default behaviour is hidden.
        _progressBar.setPadding(margin, margin, margin, margin);
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

    public interface WebViewFragmentLifecycleCallbacks {

        void onWebViewResult(Map<String, Object> result);

        void onWebViewCancel();

    }
}
