package com.gigya.android.gigyademo.ui.sheets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import com.gigya.android.gigyademo.R;
import com.gigya.android.gigyademo.model.DataEvent;

/**
 * Bottom sheet dialog used for available TFA providers.
 * The demo application is using configuration for phone/totp provider setup only. Therefore this
 * is hardcoded within the sheet ui.
 * For a more dynamic setup feel free to look at:
 *
 * @see com.gigya.android.sdk.tfa.ui.TFAProviderSelectionFragment class.
 */
public class TFAProviderSelectionBottomSheet extends AbstractLoginBottomSheet {

    public static final String TAG = "TFAProviderSelectionBottomSheet";

    public static TFAProviderSelectionBottomSheet newInstance() {
        return new TFAProviderSelectionBottomSheet();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_tfa_provider_sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Close button.
        view.findViewById(R.id.close_button).setOnClickListener(v -> {
            mViewModel.getDataRouter().postValue(new DataEvent(DataEvent.ROUTE_OPERATION_CANCELED, null));
            dismiss();
        });

        final RadioGroup rg = view.findViewById(R.id.selection_group);

        final Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(buttonView -> {
            final int id = rg.getCheckedRadioButtonId();
            if (id == R.id.phone_tfa) {
                mViewModel.getDataRouter().postValue(
                        new DataEvent(
                                DataEvent.ROUTE_TFA_REGISTER_PHONE,
                                null)
                );
            } else if (id == R.id.totp_tfa) {

            }
            dismiss();
        });
    }
}
