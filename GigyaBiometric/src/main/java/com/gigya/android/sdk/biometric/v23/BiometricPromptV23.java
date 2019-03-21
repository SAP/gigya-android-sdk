package com.gigya.android.sdk.biometric.v23;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;

import java.util.concurrent.TimeUnit;

public class BiometricPromptV23 extends BottomSheetDialog implements View.OnClickListener {

    private TextView _title, _subtitle, _description, _indicatorText;
    private ImageView _indicatorImage;

    private IGigyaBiometricCallback _callback;

    public BiometricPromptV23(@NonNull Context context, IGigyaBiometricCallback callback) {
        super(context);
        _callback = callback;
        bindView();
        referenceViews();
    }

    private void bindView() {
        @SuppressLint("InflateParams") View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_biometric_v23, null);
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

    public void setTitle(@NonNull String title) {
        if (_title != null) {
            _title.setText(title);
        }
    }

    public void setSubtitle(@NonNull String subtitle) {
        if (_subtitle != null) {
            _subtitle.setText(subtitle);
        }
    }

    public void setDescription(String description) {
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

    public void onAuthenticationError() {
        vibrate();
        errorState();
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

    }

    private void errorState() {

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
