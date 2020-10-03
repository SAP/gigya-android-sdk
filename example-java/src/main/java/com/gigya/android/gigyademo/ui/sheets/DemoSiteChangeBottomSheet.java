package com.gigya.android.gigyademo.ui.sheets;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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


        if (getActivity() == null) {
            dismiss();
        }

        final RadioGroup rg = view.findViewById(R.id.selection_group);

        // Check the active setup.
        final String activeApiKey = getActivity().getSharedPreferences("demo", Context.MODE_PRIVATE
        ).getString("savedKey", view.getContext().getString(R.string.default_api_key));

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
