package com.gigya.android;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.Config;

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

    @Mock
    private GigyaAccount mAccount;

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
        cAccountService.setAccount(mAccount);
        // Assert
        assertNotNull(cAccountService.getAccount());
        assertTrue(System.currentTimeMillis() < cAccountService.getNextInvalidationTimestamp());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAccount() {
        // Arrange
        cAccountService.setAccount(mAccount);
        // Act
        final GigyaAccount cachedAccount = cAccountService.getAccount();
        // Assert
        assertNotNull(cachedAccount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsCachedAccount() {
        // Arrange
        cAccountService.setAccount(mAccount);
        // Act
        boolean isCachedAccount = cAccountService.isCachedAccount();
        // Assert
        assertTrue(isCachedAccount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsCachedAccountAfterOverride() {
        // Arrange
        cAccountService.setAccount(mAccount);
        cAccountService.setAccountOverrideCache(true);
        // Act
        boolean isCachedAccount = cAccountService.isCachedAccount();
        // Assert
        assertFalse(isCachedAccount);
    }
}
