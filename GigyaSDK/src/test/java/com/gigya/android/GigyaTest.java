package com.gigya.android;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertNotNull;

@RunWith(PowerMockRunner.class)
public class GigyaTest extends BaseGigyaTest {

    @Before
    public void setup() {
        super.setup();

        mockApiKey("3_eP-lTMvtVwgjBCKCWPgYfeWH4xVkD5Rga15I7aoVvo-S_J5ZRBLg9jLDgJvDJZag");
        mockSession();
    }

    @Test
    public void testNewInstance() {
        Gigya gigya = new TestGigya(mContext, TestAccount.class, container);
        assertNotNull(gigya.getComponent(Context.class));
        assertNotNull(gigya.getComponent(Config.class));
        assertNotNull(gigya.getComponent(ISessionService.class));
        assertNotNull(gigya.getComponent(IPersistenceService.class));
        assertNotNull(gigya.getComponent(IAccountService.class));
    }

    @Test
    public void testNewInstanceWithScheme() {
        Gigya gigya = Gigya.getInstance(mContext, TestAccount.class);
    }
}
