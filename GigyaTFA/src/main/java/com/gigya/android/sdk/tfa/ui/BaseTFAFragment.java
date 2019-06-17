package com.gigya.android.sdk.tfa.ui;

import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory;
import com.gigya.android.sdk.network.GigyaError;

public class BaseTFAFragment extends DialogFragment {

    public interface SelectionCallback {

        void onDismiss();

        void onResolved();

        void onError(GigyaError error);
    }

    @Nullable
    protected TFAResolverFactory _resolverFactory;

    public void setResolverFactory(TFAResolverFactory resolverFactory) {
        _resolverFactory = resolverFactory;
    }

    @Nullable
    protected TFAPhoneRegistrationFragment.SelectionCallback _selectionCallback;

    public void setSelectionCallback(TFAPhoneRegistrationFragment.SelectionCallback selectionCallback) {
        _selectionCallback = selectionCallback;
    }

}
