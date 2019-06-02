package com.gigya.android.session;

import com.gigya.android.BaseGigyaTest;
import com.gigya.android.sdk.session.SessionVerificationService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class SessionVerificationServiceTest extends BaseGigyaTest {

    private SessionVerificationService cSessionVerificationService;


    @Before
    public void setup() throws Exception {
        super.setup();

        mockConfig();

        cSessionVerificationService = new SessionVerificationService(
                mContext, mConfig, mSessionService, mAccountCacheService, mApiService, mRequestFactory
        );
    }

    @Test
    public void testGetInitialDelay() {
        // Act
        final long initialDelay = cSessionVerificationService.getInitialDelay();
        // Assert
        assertEquals(300000, initialDelay);
    }

    @Test
    public void testGetInitialDelayVolatile()  {
        // Arrange
        Whitebox.setInternalState(cSessionVerificationService, "_lastRequestTimestamp", System.currentTimeMillis() - 4000);
        // Act
        final long initialDelay = cSessionVerificationService.getInitialDelay();
        // Assert
        assertNotEquals(300000, initialDelay);
        assertTrue(initialDelay < 300000);
    }
}
