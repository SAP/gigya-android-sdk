package com.gigya.android.sdk.tfa.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.models.RegisteredPhone;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.phone.RegisteredPhonesResolver;

import java.util.ArrayList;
import java.util.List;

import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.PHONE;

public class TFAPhoneVerificationFragment extends BaseTFAFragment {

    @Nullable
    protected RegisteredPhonesResolver _registeredPhonesResolver;

    protected ProgressBar _progressBar;
    protected Spinner _registeredPhonesSpinner;
    protected Button _sendCodeButton, _verifyButton, _dismissButton;
    protected View _verificationLayout;
    protected EditText _verificationCodeEditText;

    private String _phoneProvider = PHONE;

    protected static final String ARG_PHONE_PROVIDER = "arg_phone_provider";

    public static TFAPhoneVerificationFragment newInstance(String phoneProvider) {
        TFAPhoneVerificationFragment fragment = new TFAPhoneVerificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE_PROVIDER, phoneProvider);
        fragment.setArguments(args);
        return fragment;
    }

    public static TFAPhoneVerificationFragment newInstance(String phoneProvider, String language) {
        TFAPhoneVerificationFragment fragment = new TFAPhoneVerificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE_PROVIDER, phoneProvider);
        args.putString(ARG_LANGUAGE, language);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            dismiss();
            return;
        }

        _phoneProvider = getArguments().getString(ARG_PHONE_PROVIDER);
        if (getArguments().containsKey(ARG_LANGUAGE)) {
            _language = getArguments().getString(ARG_LANGUAGE);
        }
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


    protected void initUI(View view) {
        _progressBar = view.findViewById(R.id.fpv_progress);
        _registeredPhonesSpinner = view.findViewById(R.id.fpv_selection_spinner);
        _sendCodeButton = view.findViewById(R.id.fpv_send_code_button);
        _verificationLayout = view.findViewById(R.id.fpv_verification_layout);

        _verifyButton = view.findViewById(R.id.fpv_verify_button);
        _dismissButton = view.findViewById(R.id.fpv_dismiss_button);

        _verificationCodeEditText = view.findViewById(R.id.fpv_verification_code_edit_text);
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
        _progressBar.setVisibility(View.VISIBLE);
        if (_resolverFactory == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
                dismiss();
            }
            return;
        }

        _registeredPhonesResolver = _resolverFactory.getResolverFor(RegisteredPhonesResolver.class).provider(_phoneProvider);
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

    protected void populateRegisteredPhones(List<RegisteredPhone> registeredPhoneList) {
        final ArrayList<PhoneHelper> helpers = new ArrayList<>(registeredPhoneList.size());
        for (RegisteredPhone phone : registeredPhoneList) {
            helpers.add(new PhoneHelper(phone));
        }

        // Set registered phones spinner adapter.
        final ArrayAdapter phonesAdapter = new ArrayAdapter<>(_registeredPhonesSpinner.getContext(),
                android.R.layout.simple_spinner_dropdown_item, helpers);
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
                        _language,
                        helper.phone.getLastMethod(),
                        _registeredPhonesResolverResultCallback);
            }
        });
    }

    protected void updateToVerificationState(final IVerifyCodeResolver verifyCodeResolver) {
        if (_registeredPhonesResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }
        _verificationLayout.setVisibility(View.VISIBLE);
        _sendCodeButton.setText(getString(R.string.gig_tfa_send_again));

        // Click action for verify action.
        _verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verify code.
                _verificationCodeEditText.setError(null);
                _progressBar.setVisibility(View.VISIBLE);
                final String verificationCode = _verificationCodeEditText.getText().toString().trim();
                verifyCodeResolver.verifyCode(
                        _registeredPhonesResolver.getProvider(),
                        verificationCode, false,
                        new VerifyCodeResolver.ResultCallback() {
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
