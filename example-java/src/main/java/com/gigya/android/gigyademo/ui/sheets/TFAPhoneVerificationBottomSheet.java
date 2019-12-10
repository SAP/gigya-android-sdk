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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.DataEvent;
import com.gigya.android.gigyademo.sms.SMSRetrieverReceiver;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.models.RegisteredPhone;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.phone.RegisteredPhonesResolver;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class TFAPhoneVerificationBottomSheet extends AbstractLoginBottomSheet implements SMSRetrieverReceiver.ISMSRetrieverCallback {

    public static final String TAG = "TFAPhoneVerificationBottomSheet";

    private RegisteredPhonesResolver mRegisteredPhonesResolver;
    private IVerifyCodeResolver mVerifyCodeResolver;

    private SMSRetrieverReceiver mSmsReceiver;

    private TextInputLayout mVerificationCodeInputLayout;
    private Spinner mRegisteredPhonesSpinner;
    private ProgressBar mProgress;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_tfa_phone_verification_sheet;
    }

    public static TFAPhoneVerificationBottomSheet newInstance() {
        return new TFAPhoneVerificationBottomSheet();
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

    final RegisteredPhonesResolver.ResultCallback registeredPhonesCallback = new RegisteredPhonesResolver.ResultCallback() {
        @Override
        public void onRegisteredPhones(List<RegisteredPhone> list) {
            updateProgress(false);
            if (mRegisteredPhonesSpinner == null) {
                return;
            }
            final ArrayList<PhoneNumberWrapper> wrappers = new ArrayList<>(list.size());
            for (RegisteredPhone phone : list) {
                wrappers.add(new PhoneNumberWrapper(phone));
            }
            final ArrayAdapter phonesAdapter = new ArrayAdapter<>(mRegisteredPhonesSpinner.getContext(),
                    android.R.layout.simple_spinner_dropdown_item, wrappers);
            mRegisteredPhonesSpinner.setAdapter(phonesAdapter);
        }

        @Override
        public void onVerificationCodeSent(IVerifyCodeResolver iVerifyCodeResolver) {
            updateProgress(false);
            mVerifyCodeResolver = iVerifyCodeResolver;
        }

        @Override
        public void onError(GigyaError gigyaError) {
            mViewModel.getDataRouter().postValue(new DataEvent(DataEvent.ROUTE_OPERATION_CANCELED, null));
            dismiss();
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        if (mViewModel.getTFAResolverFactory() == null) {
            // Cancel operation.
            dismiss();
        }

        mProgress = view.findViewById(R.id.sheet_progress);
        mRegisteredPhonesSpinner = view.findViewById(R.id.phone_spinner);
        mVerificationCodeInputLayout = view.findViewById(R.id.verification_code_layout);


        // Close button.
        view.findViewById(R.id.close_button).setOnClickListener(v -> {
            mViewModel.getDataRouter().postValue(new DataEvent(DataEvent.ROUTE_OPERATION_CANCELED, null));
            dismiss();
        });

        mRegisteredPhonesResolver = mViewModel.getTFAResolverFactory().getResolverFor(RegisteredPhonesResolver.class);
        if (mRegisteredPhonesResolver == null) {
            return;
        }
        mRegisteredPhonesResolver.getPhoneNumbers(registeredPhonesCallback);
        updateProgress(true);


        final Button sendVerificationCodeButton = view.findViewById(R.id.send_button);
        sendVerificationCodeButton.setOnClickListener(v -> {
            if (mRegisteredPhonesSpinner.getChildCount() == 0) {
                return;
            }
            updateProgress(true);
            final PhoneNumberWrapper wrapper = (PhoneNumberWrapper) mRegisteredPhonesSpinner.getSelectedItem();
            mRegisteredPhonesResolver.sendVerificationCode(
                    wrapper.phone.getId(),
                    wrapper.phone.getLastMethod(),
                    registeredPhonesCallback
            );
        });

        final Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(buttonView -> {
            if (mVerifyCodeResolver != null && mVerificationCodeInputLayout.getEditText() != null) {
                final String verificationCode = mVerificationCodeInputLayout.getEditText().getText().toString().trim();
                updateProgress(true);
                mVerifyCodeResolver.verifyCode(
                        mRegisteredPhonesResolver.getProvider(),
                        verificationCode,
                        false,
                        new VerifyCodeResolver.ResultCallback() {
                            @Override
                            public void onResolved() {
                                /*
                                Do what ever you want to do here.
                                 */
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
                        }
                );
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

    public static class PhoneNumberWrapper {

        final private RegisteredPhone phone;

        PhoneNumberWrapper(RegisteredPhone phone) {
            this.phone = phone;
        }

        @NonNull
        @Override
        public String toString() {
            return this.phone.getObfuscated();
        }
    }
}
