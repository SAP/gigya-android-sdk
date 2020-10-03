package com.gigya.android.sdk.tfa.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.resolvers.totp.VerifyTOTPResolver;

public class TFATOTPVerificationFragment extends BaseTFAFragment {

    @Nullable
    protected VerifyTOTPResolver _verifyTOTPResolver;

    protected ProgressBar _progressBar;
    protected EditText _verificationCodeEditText;
    protected Button _verifyButton, _dismissButton;

    public static TFATOTPVerificationFragment newInstance() {
        return new TFATOTPVerificationFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_totp_verification;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initUI(view);
        setActions();
        initFlow();
    }

    protected void initUI(View view) {
        _progressBar = view.findViewById(R.id.ftpv_progress);
        _verificationCodeEditText = view.findViewById(R.id.ftpv_verification_code_edit_text);
        _dismissButton = view.findViewById(R.id.ftpv_dismiss_button);
        _verifyButton = view.findViewById(R.id.ftpv_verify_button);
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

        _verifyTOTPResolver = _resolverFactory.getResolverFor(VerifyTOTPResolver.class);
        if (_verifyTOTPResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
        }

        // Click action now available.
        _verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _progressBar.setVisibility(View.VISIBLE);
                final String verificationCode = _verificationCodeEditText.getText().toString().trim();
                _verifyTOTPResolver.verifyTOTPCode(
                        verificationCode,
                        false,
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
}
