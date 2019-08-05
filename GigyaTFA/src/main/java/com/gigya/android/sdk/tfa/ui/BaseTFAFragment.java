package com.gigya.android.sdk.tfa.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory;
import com.gigya.android.sdk.network.GigyaError;

public abstract class BaseTFAFragment extends DialogFragment {

    public abstract int getLayoutId();

    public interface SelectionCallback {

        void onDismiss();

        void onResolved();

        void onError(GigyaError error);
    }

    public static final String ARG_LANGUAGE = "arg_language";

    protected String _language = "en";

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

    private boolean _roundedCorners = false;

    public void setRoundedCorners(boolean roundedCorners) {
        _roundedCorners = roundedCorners;
    }

    @Override
    public void onStart() {
        setCancelable(false);
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            if (window != null && _roundedCorners) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }
}
