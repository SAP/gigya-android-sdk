package com.gigya.android.sdk.tfa.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.tfa.R;
import com.gigya.android.sdk.tfa.resolvers.push.PushVerificationResolver;

import static com.gigya.android.sdk.tfa.GigyaDefinitions.Broadcast.INTENT_FILTER_PUSH_TFA_VERIFY;

public class TFAPushVerificationFragment extends BaseTFAFragment {

    private static final String LOG_TAG = "TFAPushVerificationFragment";

    @Override
    public int getLayoutId() {
        return R.layout.fragment_push_verification;
    }

    public static TFAPushVerificationFragment newInstance() {
        return new TFAPushVerificationFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(_verifyEventReceiver,
                    new IntentFilter(INTENT_FILTER_PUSH_TFA_VERIFY));
        }
    }

    @Override
    public void onStop() {
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(_verifyEventReceiver);
        }
        super.onStop();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final Button dismissButton = view.findViewById(R.id.fpuv_dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_selectionCallback != null) {
                    _selectionCallback.onDismiss();
                }
                dismiss();
            }
        });
    }

    final private BroadcastReceiver _verifyEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GigyaLogger.debug(LOG_TAG, "Verify push TFA event received");
            onVerificationReceived();
        }
    };

    private void onVerificationReceived() {
        PushVerificationResolver pushVerificationResolver = _resolverFactory.getResolverFor(PushVerificationResolver.class);
        if (pushVerificationResolver == null) {
            if (_selectionCallback != null) {
                _selectionCallback.onError(GigyaError.cancelledOperation());
            }
            dismiss();
            return;
        }

        pushVerificationResolver.verify(new PushVerificationResolver.ResultCallback() {

            @Override
            public void onResolved() {
                if (_selectionCallback != null) {
                    _selectionCallback.onResolved();
                }
                dismiss();
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
