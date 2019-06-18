package com.gigya.android.sdk.tfa.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.phone.RegisterPhoneResolver;

public class TFAPhoneRegistrationFragment extends BaseTFAFragment {

    @Nullable
    private RegisterPhoneResolver _registerPhoneResolver;

    @Nullable
    private IVerifyCodeResolver _verifyCodeResolver;

    private ProgressBar _progressBar;
    private EditText _phoneEditText, _verificationCodeEditText;
    private View _verificationLayout;
    private Button _actionButton, _dismissButton;

    public static TFAPhoneRegistrationFragment newInstance() {
        return new TFAPhoneRegistrationFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_phone_registraion;
    }

    @Override
    public void onStart() {
        setCancelable(false);
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            if (window != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initUI(view);
        setActions();
    }

    private void initUI(View view) {
        _progressBar = view.findViewById(R.id.fpr_progress);
        _phoneEditText = view.findViewById(R.id.fpr_phone_edit_text);
        _verificationLayout = view.findViewById(R.id.fpr_verification_layout);
        _verificationCodeEditText = view.findViewById(R.id.fpr_verification_code_edit_text);
        _actionButton = view.findViewById(R.id.fpr_action_button);
        _dismissButton = view.findViewById(R.id.fpr_dismiss_button);
    }

    private void setActions() {
        _actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_verificationLayout.getVisibility() != View.VISIBLE) {
                    register();
                } else {
                    verify();
                }
            }
        });

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

    private void updateToVerificationState() {
        _actionButton.setText(getString(R.string.verify));
        _phoneEditText.setVisibility(View.GONE);
        _verificationLayout.setVisibility(View.VISIBLE);
    }

    private void register() {
        if (_resolverFactory == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }
        final String phoneNumber = _phoneEditText.getText().toString().trim();

        _registerPhoneResolver = _resolverFactory.getResolverFor(RegisterPhoneResolver.class);
        if (_registerPhoneResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }
        _progressBar.setVisibility(View.VISIBLE);
        _registerPhoneResolver.registerPhone(phoneNumber, new RegisterPhoneResolver.ResultCallback() {
            @Override
            public void onVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver) {
                _progressBar.setVisibility(View.INVISIBLE);
                _verifyCodeResolver = verifyCodeResolver;
                updateToVerificationState();
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

    private void verify() {
        if (_verifyCodeResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }

        _progressBar.setVisibility(View.VISIBLE);
        final String verificationCode = _verificationCodeEditText.getText().toString().trim();
        _verifyCodeResolver.verifyCode(_registerPhoneResolver.getProvider(), verificationCode, new VerifyCodeResolver.ResultCallback() {
            @Override
            public void onResolved() {
                if (_selectionCallback != null) {
                    _selectionCallback.onResolved();
                }
                dismiss();
            }

            @Override
            public void onInvalidCode() {

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


}
