package com.gigya.android.sdk.tfa.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.models.RegisteredPhone;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.phone.RegisteredPhonesResolver;

import java.util.ArrayList;
import java.util.List;

public class TFAPhoneVerificationFragment extends BaseTFAFragment {

    @Nullable
    RegisteredPhonesResolver _registeredPhonesResolver;

    private ProgressBar _progressBar;
    private Spinner _registeredPhonesSpinner;
    private Button _sendCodeButton, _verifyButton, _dismissButton;
    private View _verificationLayout;
    private EditText _verificationCodeEditText;

    public static TFAPhoneVerificationFragment newInstance() {
        return new TFAPhoneVerificationFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_phone_verification;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initUI(view);
        setActions();
        initFlow();
    }


    private void initUI(View view) {
        _progressBar = view.findViewById(R.id.fpv_progress);
        _registeredPhonesSpinner = view.findViewById(R.id.fpv_selection_spinner);
        _sendCodeButton = view.findViewById(R.id.fpv_send_code_button);
        _verificationLayout = view.findViewById(R.id.fpv_verification_layout);

        _verifyButton = view.findViewById(R.id.fpv_verify_button);
        _dismissButton = view.findViewById(R.id.fpv_dismiss_button);

        _verificationCodeEditText = view.findViewById(R.id.fpv_verification_code_edit_text);
    }

    private void setActions() {
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

    private void initFlow() {
        _progressBar.setVisibility(View.VISIBLE);
        if (_resolverFactory == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
                dismiss();
            }
            return;
        }

        _registeredPhonesResolver = _resolverFactory.getResolverFor(RegisteredPhonesResolver.class);
        if (_registeredPhonesResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }
        _registeredPhonesResolver.getPhoneNumbers(_registeredPhonesResolverResultCallback);
    }

    private final RegisteredPhonesResolver.ResultCallback _registeredPhonesResolverResultCallback = new RegisteredPhonesResolver.ResultCallback() {
        @Override
        public void onRegisteredPhones(List<RegisteredPhone> registeredPhoneList) {
            _progressBar.setVisibility(View.INVISIBLE);
            populateRegisteredPhones(registeredPhoneList);
        }

        @Override
        public void onVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver) {
            _progressBar.setVisibility(View.INVISIBLE);
            updateToVerificationState(verifyCodeResolver);
        }

        @Override
        public void onError(GigyaError error) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(error);
            }
            dismiss();
        }
    };

    private void populateRegisteredPhones(List<RegisteredPhone> registeredPhoneList) {
        final ArrayList<PhoneHelper> helpers = new ArrayList<>(registeredPhoneList.size());
        for (RegisteredPhone phone : registeredPhoneList) {
            helpers.add(new PhoneHelper(phone));
        }

        // Set registered phones spinner adapter.
        final ArrayAdapter phonesAdapter = new ArrayAdapter<>(_registeredPhonesSpinner.getContext(), android.R.layout.simple_spinner_dropdown_item, helpers);
        _registeredPhonesSpinner.setAdapter(phonesAdapter);

        // Click action for send code is only relevant at this point.
        _sendCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send code.
                _progressBar.setVisibility(View.VISIBLE);
                final PhoneHelper helper = (PhoneHelper) _registeredPhonesSpinner.getSelectedItem();
                _registeredPhonesResolver.sendVerificationCode(
                        helper.phone.getId(),
                        helper.phone.getLastMethod(),
                        _registeredPhonesResolverResultCallback);
            }
        });
    }

    private void updateToVerificationState(final IVerifyCodeResolver verifyCodeResolver) {
        _verificationLayout.setVisibility(View.VISIBLE);
        _sendCodeButton.setText(getString(R.string.send_again));

        // Click action for verify action.
        _verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verify code.
                _verificationCodeEditText.setError(null);
                _progressBar.setVisibility(View.VISIBLE);
                final String verificationCode = _verificationCodeEditText.getText().toString().trim();
                verifyCodeResolver.verifyCode(_registeredPhonesResolver.getProvider(), verificationCode, false, new VerifyCodeResolver.ResultCallback() {
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
                        _verificationCodeEditText.setError(getString(R.string.invalid_verification_code));
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

    private static class PhoneHelper {

        final private RegisteredPhone phone;

        PhoneHelper(RegisteredPhone phone) {
            this.phone = phone;
        }

        @NonNull
        @Override
        public String toString() {
            return this.phone.getObfuscated();
        }
    }
}
