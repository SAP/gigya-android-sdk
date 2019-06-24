package com.gigya.android.sdk.biometric.v23;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.CancellationSignal;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;

import java.util.concurrent.TimeUnit;

public class GigyaBiometricPromptV23 extends BottomSheetDialog implements View.OnClickListener {

    private static final String LOG_TAG = "GigyaBiometricPromptV23";

    private TextView _title, _subtitle, _description, _indicatorText;
    private ImageView _indicatorImage;

    private IGigyaBiometricCallback _biometricCallback;
    private CancellationSignal _cancellationSignal;

    private boolean _animate = true;

    void setCancellationSignal(CancellationSignal signal) {
        _cancellationSignal = signal;
    }

    GigyaBiometricPromptV23(@NonNull Context context, IGigyaBiometricCallback callback) {
        super(context);
        _biometricCallback = callback;
        bindView();
        referenceViews();
    }

    private void bindView() {
        @SuppressLint("InflateParams") View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_gigya_biometric_prompt, null);
        setContentView(bottomSheetView);
    }

    private void referenceViews() {
        _title = findViewById(R.id.title_text);
        _subtitle = findViewById(R.id.subtitle_text);
        _description = findViewById(R.id.description_text);
        _indicatorText = findViewById(R.id.ind_text);
        _indicatorImage = findViewById(R.id.ind_image);

        final Button _cancelButton = findViewById(R.id.cancel_button);
        if (_cancelButton != null) {
            _cancelButton.setOnClickListener(this);
        }
    }

    @Override
    public void dismiss() {
        if (_cancellationSignal != null && !_cancellationSignal.isCanceled()) {
            _cancellationSignal.cancel();
        }
        super.dismiss();
    }

    //region TEXT INJECTIONS

    void setAnimate(boolean animate) {
        _animate = animate;
    }

    void setTitle(@NonNull String title) {
        if (_title != null) {
            _title.setText(title);
        }
    }

    void setSubtitle(@NonNull String subtitle) {
        if (_subtitle != null) {
            _subtitle.setText(subtitle);
        }
    }

    void setDescription(@Nullable String description) {
        if (_description != null) {
            if (TextUtils.isEmpty(description)) {
                _description.setVisibility(View.GONE);
                return;
            }
            _description.setText(description);
        }
    }

    @Override
    public void onClick(View v) {
        _biometricCallback.onBiometricOperationCanceled();
        dismiss();
    }

    //endregion

    //region STATE MACHINE

    /**
     * Do on authentication help.
     *
     * @param helpString Provided help string from fingerprint manager authenticator.
     */
    void onAuthenticationHelp(String helpString) {
        vibrate();
        helpState(helpString);
    }

    /**
     * Do on authentication failure.
     */
    void onAuthenticationFailed() {
        onAuthenticationError(-1, null);
    }

    /**
     * Do on authentication error.
     *
     * @param errMsgId    Error message id provided from fingerprint manager authenticator.
     * @param errorString Error message string provided from fingerprint manager authenticator.
     */
    void onAuthenticationError(int errMsgId, final String errorString) {
        vibrate();
        errorState(errorString);
        switch (errMsgId) {
            case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
            case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT:
                GigyaLogger.error(LOG_TAG, "Fingerprint authentication lockout error");
                break;
            default:
                // Update indicator image & text. Wait for 4 seconds and reset
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (isShowing()) {
                                // Reset views.
                                resetState();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(3));
                break;
        }
    }

    /**
     * Reset to original state.
     */
    private void resetState() {
        if (_indicatorText != null) {
            _indicatorText.setTextColor(ContextCompat.getColor(getContext(), R.color.color_text_secondary));
            _indicatorText.setText(getContext().getString(R.string.bio_touch_sensor));
        }
        if (_indicatorImage == null) {
            return;
        }
        if (_animate) {
            animateBackFromError();
        } else {
            _indicatorImage.setImageResource(R.drawable.v_fingerprint);
        }
    }

    /**
     * Switch to error state.
     *
     * @param errorString Provided error string from fingerprint manager authenticator.
     */
    private void errorState(String errorString) {
        if (_indicatorText != null) {
            _indicatorText.setTextColor(ContextCompat.getColor(getContext(), R.color.color_error));
            if (errorString == null) {
                errorString = getContext().getString(R.string.bio_not_recognized);
            }
            _indicatorText.setText(errorString);
        }
        // Animate Error state.
        if (_indicatorImage == null) {
            return;
        }
        if (_animate) {
            animateToError();
        } else {
            _indicatorImage.setImageResource(R.drawable.v_error_info);
        }
    }

    /**
     * Switch to help state.
     *
     * @param helpString Provided help string from fingerprint manager authenticator.
     */
    private void helpState(String helpString) {
        if (_indicatorText != null) {
            _indicatorText.setTextColor(ContextCompat.getColor(getContext(), R.color.color_text_secondary));
            _indicatorText.setText(helpString);
        }
        if (_indicatorImage == null) {
            return;
        }
        if (_animate) {
            animateToError();
        } else {
            _indicatorImage.setImageResource(R.drawable.v_error_info);
        }
    }

    /**
     * Vibrate on actions (minor).
     */
    private void vibrate() {
        final Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] wave_time = {0, 20};
            int[] wave_amplitude = {0, 10};
            VibrationEffect vibrationEffect;
            vibrationEffect = VibrationEffect.createWaveform(wave_time, wave_amplitude, -1);
            v.vibrate(vibrationEffect);
        } else {
            //deprecated in API 26.
            v.vibrate(20);
        }
    }

    //endregion

    //region ANIMATIONS

    /**
     * Animate fingerprint state to error state (vectors).
     */
    private void animateToError() {
        final int resId = R.drawable.av_fingerprint_to_error;
        AnimatedVectorDrawableCompat av = AnimatedVectorDrawableCompat.create(getContext(), resId);
        _indicatorImage.setImageDrawable(av);
        if (av != null) {
            ((Animatable) av).start();
        }
    }

    /**
     * Animate error stat to fingerprint state (vectors).
     */
    private void animateBackFromError() {
        final int resId = R.drawable.av_error_to_fingerprint;
        AnimatedVectorDrawableCompat av = AnimatedVectorDrawableCompat.create(getContext(), resId);
        _indicatorImage.setImageDrawable(av);
        if (av != null) {
            ((Animatable) av).start();
        }
    }

    //endregion

}
