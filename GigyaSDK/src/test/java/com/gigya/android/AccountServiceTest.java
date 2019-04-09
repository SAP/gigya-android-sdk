package com.gigya.android;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.account.AccountService;
import com.gigya.android.sdk.model.account.GigyaAccount;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class AccountServiceTest {

    @Mock
    private Config mConfig;

    private AccountService cAccountService;

    @Before
    public void setup() {
        when(mConfig.getAccountCacheTime()).thenReturn(5);
        cAccountService = new AccountService(mConfig);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetAccount() {
        // Act
        cAccountService.setAccount("");
        // Assert
        assertNotNull(cAccountService.getAccount());
        assertTrue(System.currentTimeMillis() < cAccountService.getNextInvalidationTimestamp());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAccount() {
        // Arrange
        cAccountService.setAccount("");
        // Act
        final GigyaAccount cachedAccount = cAccountService.getAccount();
        // Assert
        assertNotNull(cachedAccount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsCachedAccount() {
        // Arrange
        cAccountService.setAccount("");
        // Act
        boolean isCachedAccount = cAccountService.isCachedAccount();
        // Assert
        assertTrue(isCachedAccount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsCachedAccountAfterOverride() {
        // Arrange
        cAccountService.setAccount("");
        cAccountService.setAccountOverrideCache(true);
        // Act
        boolean isCachedAccount = cAccountService.isCachedAccount();
        // Assert
        assertFalse(isCachedAccount);
    }
}
