package com.gigya.android.sdk.tfa.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.phone.RegisterPhoneResolver;
import com.gigya.android.sdk.tfa.ui.models.CountryCode;
import com.gigya.android.sdk.utils.FileUtils;
import com.google.gson.Gson;

import java.io.IOException;

import static com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.PHONE;

public class TFAPhoneRegistrationFragment extends BaseTFAFragment {

    private static final String LOG_TAG = "TFAPhoneRegistrationFragment";

    @Nullable
    protected RegisterPhoneResolver _registerPhoneResolver;

    @Nullable
    protected IVerifyCodeResolver _verifyCodeResolver;

    protected String _phoneProvider = PHONE;

    protected CountryCode[] _countryCodes = new CountryCode[240];

    protected ProgressBar _progressBar;
    protected EditText _phoneEditText, _verificationCodeEditText;
    protected View _verificationLayout;
    protected Button _actionButton, _dismissButton;
    protected CheckBox _rememberDeviceCheckbox;
    protected Spinner _countryCodeSpinner;

    protected static final String ARG_PHONE_PROVIDER = "arg_phone_provider";

    public static TFAPhoneRegistrationFragment newInstance(String phoneProvider) {
        TFAPhoneRegistrationFragment fragment = new TFAPhoneRegistrationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE_PROVIDER, phoneProvider);
        fragment.setArguments(args);
        return fragment;
    }

    public static TFAPhoneRegistrationFragment newInstance(String phoneProvider, String language) {
        TFAPhoneRegistrationFragment fragment = new TFAPhoneRegistrationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE_PROVIDER, phoneProvider);
        args.putString(ARG_LANGUAGE, language);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_phone_registraion;
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
        loadCountryCodesFromAssets();
        setActions();
    }

    protected void initUI(View view) {
        _progressBar = view.findViewById(R.id.fpr_progress);
        _phoneEditText = view.findViewById(R.id.fpr_phone_edit_text);
        _verificationLayout = view.findViewById(R.id.fpr_verification_layout);
        _verificationCodeEditText = view.findViewById(R.id.fpr_verification_code_edit_text);
        _actionButton = view.findViewById(R.id.fpr_action_button);
        _dismissButton = view.findViewById(R.id.fpr_dismiss_button);
        _rememberDeviceCheckbox = view.findViewById(R.id.fpr_remember_device_checkbox);
        _countryCodeSpinner = view.findViewById(R.id.fpr_country_code_spinner);
    }

    protected void setActions() {
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

    protected void updateToVerificationState() {
        _actionButton.setText(getString(R.string.gig_tfa_verify));
        _phoneEditText.setVisibility(View.GONE);
        _verificationLayout.setVisibility(View.VISIBLE);
    }

    protected void register() {
        if (_resolverFactory == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }

        final CountryCode countryCode = (CountryCode) _countryCodeSpinner.getSelectedItem();
        final String phoneNumber = countryCode.getDialCode() + _phoneEditText.getText().toString().trim();

        _registerPhoneResolver = _resolverFactory.getResolverFor(RegisterPhoneResolver.class).provider(_phoneProvider);
        if (_registerPhoneResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }
        _progressBar.setVisibility(View.VISIBLE);
        _registerPhoneResolver.registerPhone(phoneNumber, _language, GigyaDefinitions.PhoneMethod.SMS,
                new RegisterPhoneResolver.ResultCallback() {
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

    protected void verify() {
        if (_registerPhoneResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }
        if (_verifyCodeResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }

        _progressBar.setVisibility(View.VISIBLE);
        final String verificationCode = _verificationCodeEditText.getText().toString().trim();
        _verifyCodeResolver.verifyCode(_registerPhoneResolver.getProvider(),
                verificationCode,
                _rememberDeviceCheckbox.isChecked()
                , new VerifyCodeResolver.ResultCallback() {
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

    private void loadCountryCodesFromAssets() {
        if (getContext() == null) {
            return;
        }
        try {
            final String json = FileUtils.assetJsonFileToString(getContext(), "country_codes.json");
            _countryCodes = new Gson().fromJson(json, CountryCode[].class);
            GigyaLogger.debug(LOG_TAG, "Country code list parsed successfully");

            final ArrayAdapter countryCodeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, _countryCodes);
            _countryCodeSpinner.setAdapter(countryCodeAdapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
