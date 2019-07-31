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
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.models.EmailModel;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.email.RegisteredEmailsResolver;

import java.util.ArrayList;
import java.util.List;

public class TFAEmailVerificationFragment extends BaseTFAFragment {

    @Nullable
    private RegisteredEmailsResolver _registeredEmailsResolver;

    private ProgressBar _progressBar;
    private Spinner _registeredEmailsSpinner;
    private Button _sendCodeButton, _verifyButton, _dismissButton;
    private View _verificationLayout;
    private EditText _verificationCodeEditText;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_email_verification;
    }

    public static TFAEmailVerificationFragment newInstance() {
        return new TFAEmailVerificationFragment();
    }

    public static TFAEmailVerificationFragment newInstance(String language) {
        final Bundle args = new Bundle();
        args.putString(ARG_LANGUAGE, language);
        TFAEmailVerificationFragment fragment = new TFAEmailVerificationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initUI(view);
        setActions();
        initFlow();
    }

    private void initUI(View view) {
        _progressBar = view.findViewById(R.id.fev_progress);
        _registeredEmailsSpinner = view.findViewById(R.id.fev_selection_spinner);
        _sendCodeButton = view.findViewById(R.id.fev_send_code_button);
        _verificationLayout = view.findViewById(R.id.fev_verification_layout);

        _verifyButton = view.findViewById(R.id.fev_verify_button);
        _dismissButton = view.findViewById(R.id.fev_dismiss_button);

        _verificationCodeEditText = view.findViewById(R.id.fev_verification_code_edit_text);
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

        _registeredEmailsResolver = _resolverFactory.getResolverFor(RegisteredEmailsResolver.class);
        if (_registeredEmailsResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }

        // Fetch emails.
        _registeredEmailsResolver.getRegisteredEmails(registeredEmailsResolverResultCallback);
    }

    private final RegisteredEmailsResolver.ResultCallback registeredEmailsResolverResultCallback = new RegisteredEmailsResolver.ResultCallback() {
        @Override
        public void onRegisteredEmails(List<EmailModel> registeredEmailList) {
            _progressBar.setVisibility(View.INVISIBLE);
            populateRegisteredEmailsList(registeredEmailList);
        }

        @Override
        public void onEmailVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver) {
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

    private void populateRegisteredEmailsList(List<EmailModel> registeredEmailList) {
        if (_registeredEmailsResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }

        final ArrayList<EmailHelper> helpers = new ArrayList<>(registeredEmailList.size());
        for (EmailModel email : registeredEmailList) {
            helpers.add(new EmailHelper(email));
        }

        // Set registered email spinner adapter.
        final ArrayAdapter emailAdapter = new ArrayAdapter<>(_registeredEmailsSpinner.getContext(),
                android.R.layout.simple_spinner_dropdown_item, helpers);
        _registeredEmailsSpinner.setAdapter(emailAdapter);

        // Click action for send code is only relevant at this point.
        _sendCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send code.
                _progressBar.setVisibility(View.VISIBLE);
                final EmailHelper helper = (EmailHelper) _registeredEmailsSpinner.getSelectedItem();
                _registeredEmailsResolver.sendEmailCode(
                        helper.email,
                        _language,
                        registeredEmailsResolverResultCallback);
            }
        });
    }

    private void updateToVerificationState(final IVerifyCodeResolver verifyCodeResolver) {
        _verificationLayout.setVisibility(View.VISIBLE);
        _sendCodeButton.setText(getString(R.string.tfa_send_again));

        // Click action for verify action.
        _verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verify code.
                _verificationCodeEditText.setError(null);
                _progressBar.setVisibility(View.VISIBLE);
                final String verificationCode = _verificationCodeEditText.getText().toString().trim();
                verifyCodeResolver.verifyCode(GigyaDefinitions.TFAProvider.EMAIL, verificationCode, false, new VerifyCodeResolver.ResultCallback() {
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
                        _verificationCodeEditText.setError(getString(R.string.tfa_invalid_verification_code));
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

    private static class EmailHelper {

        final private EmailModel email;

        EmailHelper(EmailModel email) {
            this.email = email;
        }

        @NonNull
        @Override
        public String toString() {
            return this.email.getObfuscated();
        }
    }
}
