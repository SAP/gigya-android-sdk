package com.gigya.android.sdk.biometric.v23;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;

import java.util.concurrent.TimeUnit;

public class GigyaBiometricPromptV23 extends BottomSheetDialog implements View.OnClickListener {

    private TextView _title, _subtitle, _description, _indicatorText;
    private ImageView _indicatorImage;

    private IGigyaBiometricCallback _callback;

    GigyaBiometricPromptV23(@NonNull Context context, IGigyaBiometricCallback callback) {
        super(context);
        _callback = callback;
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

    //region TEXT INJECTIONS

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

    void setDescription(String description) {
        if (_description != null) {
            _description.setText(description);
        }
    }

    @Override
    public void onClick(View v) {
        _callback.onAuthenticationFailed();
        dismiss();
    }

    //endregion

    //region STATE MACHINE

    void onAuthenticationHelp(String helpString) {
        vibrate();
        helpState(helpString);

    }

    public void onAuthenticationError(String errorString) {
        vibrate();
        errorState(errorString);

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
        }, TimeUnit.SECONDS.toMillis(4));
    }

    private void resetState() {
        if (_indicatorText != null) {
            _indicatorText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            _indicatorText.setText(getContext().getString(R.string.touch_sensor));
        }
        // TODO: 21/03/2019 Animate image.
        if (_indicatorImage != null) {
            _indicatorImage.setImageResource(R.drawable.ic_fingerprint);
        }
    }

    private void errorState(String errorString) {
        if (_indicatorText != null) {
            _indicatorText.setTextColor(ContextCompat.getColor(getContext(), R.color.error));
            if (errorString == null) {
                errorString = getContext().getString(R.string.not_recognized);
            }
            _indicatorText.setText(errorString);
        }
        // TODO: 21/03/2019 Animate image.
        if (_indicatorImage != null) {
            _indicatorImage.setImageResource(R.drawable.ic_info_outline);
        }
    }

    private void helpState(String helpString) {
        if (_indicatorText != null) {
            _indicatorText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            _indicatorText.setText(helpString);
        }
        // TODO: 21/03/2019 Animate image.
        if (_indicatorImage != null) {
            _indicatorImage.setImageResource(R.drawable.ic_info_outline);
        }
    }

    private void vibrate() {
        final Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26.
            v.vibrate(500);
        }
    }

    //endregion

}
