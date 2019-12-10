package com.gigya.android.gigyademo.ui.sheets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RadioGroup;


import com.gigya.android.gigyademo.R;

public class DemoSiteChangeBottomSheet extends AbstractLoginBottomSheet {

    public static final String TAG = "DemoSiteChangeBottomSheet";

    public static DemoSiteChangeBottomSheet newInstance() {
        return new DemoSiteChangeBottomSheet();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_demo_site_change_sheet;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        final RadioGroup rg = view.findViewById(R.id.selection_group);

        // Check the active setup.
        final String activeApiKey = mViewModel.getActiveApiKey();
        if (activeApiKey.equals(view.getContext().getString(R.string.default_api_key))) {
            rg.check(R.id.default_setup_button);
        } else if (activeApiKey.equals(view.getContext().getString(R.string.force_pending_registration_api_key))) {
            rg.check(R.id.pending_registration_button);
        } else if (activeApiKey.equals(view.getContext().getString(R.string.rba_20_api_key))) {
            rg.check(R.id.rba_level_20_button);
        }

        // Connect submit action.
        view.findViewById(R.id.submit_button).setOnClickListener(buttonView -> {
            final int id = rg.getCheckedRadioButtonId();
            mViewModel.changeSiteConfiguration(id);
            dismiss();
        });
    }
}
