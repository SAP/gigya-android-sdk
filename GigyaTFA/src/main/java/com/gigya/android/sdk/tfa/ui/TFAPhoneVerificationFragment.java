package com.gigya.android.sdk.tfa.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.models.RegisteredPhone;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.phone.RegisteredPhonesResolver;

import java.util.List;

public class TFAPhoneVerificationFragment extends BaseTFAFragment {

    private ProgressBar _progressBar;

    public static TFAPhoneVerificationFragment newInstance() {
        return new TFAPhoneVerificationFragment();
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_phone_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initUI(view);
        initFlow();
    }


    private void initUI(View view) {
        _progressBar = view.findViewById(R.id.fpv_progress);
    }

    private void initFlow() {
        if (_resolverFactory == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
                dismiss();
            }
            return;
        }

        RegisteredPhonesResolver registeredPhonesResolver = _resolverFactory.getResolverFor(RegisteredPhonesResolver.class);
        if (registeredPhonesResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }
        registeredPhonesResolver.getPhoneNumbers(new RegisteredPhonesResolver.ResultCallback() {

            @Override
            public void onRegisteredPhones(List<RegisteredPhone> registeredPhoneList) {

            }

            @Override
            public void onVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver) {

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
