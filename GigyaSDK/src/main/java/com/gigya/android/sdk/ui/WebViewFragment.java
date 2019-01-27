package com.gigya.android.sdk.ui;

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
        return _contentView;
    }

    protected abstract void setUpWebView();

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

    public interface WebViewFragmentLifecycleCallbacks {

        void onWebViewResult(Map<String, Object> result);

        void onWebViewCancel();
    }
}
