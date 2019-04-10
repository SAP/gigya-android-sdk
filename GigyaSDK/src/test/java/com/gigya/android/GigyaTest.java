package com.gigya.android;

import android.content.Context;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.model.account.GigyaAccount;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class GigyaTest {

    @Mock
    Context mContext;

    private static class TestAccount extends GigyaAccount { }

    @Test
    public void testNewInstance() {
        Gigya gigya = Gigya.getInstance(mContext);
    }

    @Test
    public void testNewInstanceWithScheme() {
        Gigya gigya = Gigya.getInstance(mContext, TestAccount.class);
    }
}
