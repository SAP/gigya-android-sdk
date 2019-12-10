package com.gigya.android.gigyademo.ui.sheets;

import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.DataEvent;
import com.gigya.android.gigyademo.sms.SMSRetrieverReceiver;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.phone.RegisterPhoneResolver;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.Task;

public class TFAPhoneRegistrationBottomSheet extends AbstractLoginBottomSheet implements SMSRetrieverReceiver.ISMSRetrieverCallback {

    public static final String TAG = "PhoneRegistrationBottomSheet";

    private RegisterPhoneResolver mRegisterPhoneResolver;
    private IVerifyCodeResolver mVerifyCodeResolver;

    private SMSRetrieverReceiver mSmsReceiver;

    private TextInputLayout mPhoneNumberInputLayout, mVerificationCodeInputLayout;
    private ProgressBar mProgress;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_tfa_phone_registration_sheet;
    }

    public static TFAPhoneRegistrationBottomSheet newInstance() {
        return new TFAPhoneRegistrationBottomSheet();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSmsReceiver = new SMSRetrieverReceiver(this);
        setCancelable(false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        final SmsRetrieverClient client = SmsRetriever.getClient(context);
        final Task<Void> task = client.startSmsRetriever();
        task.addOnSuccessListener(aVoid -> Log.d("SmsRetriever", "Successfully started retriever, expect broadcast intent"));
        task.addOnFailureListener(e -> Log.d("SmsRetriever", "Failed to start retriever, inspect Exception for more details"));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null) {
            getActivity().registerReceiver(
                    mSmsReceiver,
                    new IntentFilter("com.google.android.gms.auth.api.phone.SMS_RETRIEVED")
            );
        }
    }

    @Override
    public void onStop() {
        if (getActivity() != null) {
            getActivity().unregisterReceiver(mSmsReceiver);
        }
        super.onStop();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (mViewModel.getTFAResolverFactory() == null) {
            // Cancel operation.
            dismiss();
        }

        mProgress = view.findViewById(R.id.sheet_progress);

        // Close button.
        view.findViewById(R.id.close_button).setOnClickListener(v -> {
            mViewModel.getDataRouter().postValue(new DataEvent(DataEvent.ROUTE_OPERATION_CANCELED, null));
            dismiss();
        });

        mRegisterPhoneResolver = mViewModel.getTFAResolverFactory().getResolverFor(RegisterPhoneResolver.class);

        mPhoneNumberInputLayout = view.findViewById(R.id.phone_input_layout);
        mVerificationCodeInputLayout = view.findViewById(R.id.verification_code_layout);

        /*
        Remember this device checkbox reference.
         */
        final CheckBox rememberThisDeviceCheckbox = view.findViewById(R.id.remember_device_checkbox);

        final Button registerButton = view.findViewById(R.id.register_button);
        registerButton.setOnClickListener(buttonView -> {
            if (mRegisterPhoneResolver != null && mPhoneNumberInputLayout.getEditText() != null) {
                final String selectedPhoneNumber = mPhoneNumberInputLayout.getEditText().getText().toString().trim();
                updateProgress(true);
                mRegisterPhoneResolver.registerPhone(
                        selectedPhoneNumber,
                        "en",
                        GigyaDefinitions.PhoneMethod.SMS,
                        new RegisterPhoneResolver.ResultCallback() {
                            @Override
                            public void onVerificationCodeSent(IVerifyCodeResolver iVerifyCodeResolver) {
                                mVerifyCodeResolver = iVerifyCodeResolver;
                                updateProgress(false);
                                Toast.makeText(getContext(), "Verification code sent", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(GigyaError gigyaError) {
                                updateProgress(false);
                                mPhoneNumberInputLayout.setError("Error registering your phone. Please try again.");
                            }
                        });
            }
        });

        final Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(buttonView -> {
            if (mVerifyCodeResolver != null && mVerificationCodeInputLayout.getEditText() != null) {
                final String verificationCode = mVerificationCodeInputLayout.getEditText().getText().toString().trim();
                updateProgress(true);
                mVerifyCodeResolver.verifyCode(
                        GigyaDefinitions.TFAProvider.PHONE,
                        verificationCode,
                        rememberThisDeviceCheckbox.isChecked(),
                        new VerifyCodeResolver.ResultCallback() {
                            @Override
                            public void onResolved() {
                                dismiss();
                            }

                            @Override
                            public void onInvalidCode() {
                                updateProgress(false);
                                mVerificationCodeInputLayout.setError("Invalid code. Please try again");
                            }

                            @Override
                            public void onError(GigyaError gigyaError) {
                                dismiss();
                            }
                        });
            }

        });
    }

    @Override
    public void onMessageCodeReceived(String code) {
        if (mVerificationCodeInputLayout != null) {
            if (mVerificationCodeInputLayout.getEditText() != null) {
                mVerificationCodeInputLayout.getEditText().setText(code);
            }
            Toast.makeText(getContext(), "Verification automatically pulled from received SMS." +
                    " Now just press submit and watch the magic happen...", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgress(boolean visible) {
        mProgress.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        /*
        Just in case.
         */
        updateProgress(false);
        super.onDismiss(dialog);
    }
}
