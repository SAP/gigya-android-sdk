package com.gigya.android.sdk.tfa.ui;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.resolvers.totp.IVerifyTOTPResolver;
import com.gigya.android.sdk.tfa.resolvers.totp.RegisterTOTPResolver;
import com.gigya.android.sdk.tfa.resolvers.totp.VerifyTOTPResolver;

public class TFATOTPRegistrationFragment extends BaseTFAFragment {

    private static final String LOG_TAG = "TFATOTPRegistrationFragment";

    @Nullable
    protected RegisterTOTPResolver _registerTotpResolver;

    protected ProgressBar _progressBar, _qrImageProgressBar;
    protected ImageView _qrImageView;
    protected Button _registerButton, _dismissButton;
    protected EditText _verificationCodeEditText;
    protected CheckBox _rememberDeviceCheckbox;

    private Bitmap _qrImage;


    public static TFATOTPRegistrationFragment newInstance() {
        return new TFATOTPRegistrationFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_totp_registration;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initUI(view);
        setActions();
        initFlow();
    }

    protected void initUI(View view) {
        _progressBar = view.findViewById(R.id.ftpr_progress);
        _qrImageProgressBar = view.findViewById(R.id.ftpr_qr_code_image_progress);
        _qrImageView = view.findViewById(R.id.ftpr_qr_code_image);
        _registerButton = view.findViewById(R.id.ftpr_action_button);
        _dismissButton = view.findViewById(R.id.ftpr_dismiss_button);
        _verificationCodeEditText = view.findViewById(R.id.ftpr_verification_code_edit_text);
        _rememberDeviceCheckbox = view.findViewById(R.id.ftpr_remember_device_checkbox);
    }

    protected void setActions() {
        _dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_selectionCallback != null) {
                    _selectionCallback.onDismiss();
                }
                dismiss();
            }
        });
    }

    protected void initFlow() {
        if (_resolverFactory == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }

        _registerTotpResolver = _resolverFactory.getResolverFor(RegisterTOTPResolver.class);
        if (_registerTotpResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }

        _registerTotpResolver.registerTOTP(new RegisterTOTPResolver.ResultCallback() {
            @Override
            public void onQRCodeAvailable(@NonNull String qrCode, IVerifyTOTPResolver verifyTOTPResolver) {
                _progressBar.setVisibility(View.INVISIBLE);
                _qrImage = decodeImage(qrCode);
                if (_qrImage == null) {
                    GigyaLogger.debug(LOG_TAG, "Failed to decode QR image");
                }
                _qrImageProgressBar.setVisibility(View.INVISIBLE);
                _qrImageView.setImageBitmap(_qrImage);

                updateToVerificationState(verifyTOTPResolver);
            }

            @Override
            public void onError(GigyaError error) {
                if (_selectionCallback != null) {
                    _selectionCallback.onError(error);
                }
                dismiss();
            }
        });
    }

    @Nullable
    private Bitmap decodeImage(String encodedImage) {
        // Decoding the image received (Base64).
        final byte[] decoded = Base64.decode(encodedImage.split(",")[1], Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
    }

    protected void updateToVerificationState(final IVerifyTOTPResolver verifyTOTPResolver) {
        // Click action for register is now viable.
        _registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _progressBar.setVisibility(View.VISIBLE);
                final String verificationCode = _verificationCodeEditText.getText().toString().trim();
                verifyTOTPResolver.verifyTOTPCode(
                        verificationCode,
                        _rememberDeviceCheckbox.isChecked(),
                        new VerifyTOTPResolver.ResultCallback() {
                            @Override
                            public void onResolved() {
                                if (_selectionCallback != null) {
                                    _selectionCallback.onResolved();
                                }
                                dismiss();
                            }

                            @Override
                            public void onInvalidCode() {
                                _progressBar.setVisibility(View.INVISIBLE);
                                // Clear input text.
                                _verificationCodeEditText.setText("");
                                _verificationCodeEditText.setError(getString(R.string.gig_tfa_invalid_verification_code));
                            }

                            @Override
                            public void onError(GigyaError error) {
                                if (_selectionCallback != null) {
                                    _selectionCallback.onError(error);
                                }
                                dismiss();
                            }
                        });
            }
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (_qrImage != null) {
            _qrImage.recycle();
        }
        super.onDismiss(dialog);
    }
}
