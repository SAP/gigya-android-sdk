package com.gigya.android.gigyademo.ui.sheets;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.gigya.android.gigyademo.ui.activity.LoginViewModel;

public abstract class AbstractLoginBottomSheet extends BottomSheetDialogFragment {

    LoginViewModel mViewModel;

    protected abstract int getLayoutId();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() != null) {
            mViewModel = ViewModelProviders.of(getActivity()).get(LoginViewModel.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container,
                false);
    }
}
