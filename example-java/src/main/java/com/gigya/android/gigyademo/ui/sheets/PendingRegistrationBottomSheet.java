package com.gigya.android.gigyademo.ui.sheets;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.gigya.android.gigyademo.R;

public class PendingRegistrationBottomSheet extends AbstractLoginBottomSheet {

    public static final String TAG = "PedingRegistrationBottomSheet";

    public static PendingRegistrationBottomSheet newInstance() {
        return new PendingRegistrationBottomSheet();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_pending_registration_sheet;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Reference views.
        final TextInputLayout zipInputLayout = view.findViewById(R.id.zip_input_layout);
        final Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(view1 -> {
            final String zip = zipInputLayout.getEditText().getText().toString().trim();
            if (TextUtils.isEmpty(zip)) {
                Toast.makeText(getContext(), "Yeah that's not gonna work...", Toast.LENGTH_SHORT).show();
                return;
            }
            mViewModel.resolvePendingRegistration(zip);
            dismiss();
        });
    }


}
