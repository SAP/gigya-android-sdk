package com.gigya.android.gigyademo.ui.sheets;

import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

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

    public void dismissKeyboardFor(IBinder windowToken) {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(windowToken, 0);
    }


}
