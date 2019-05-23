package com.gigya.android;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.account.AccountService;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.GigyaAccountClass;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("ConstantConditions")
@RunWith(PowerMockRunner.class)
public class AccountServiceTest extends BaseGigyaTest {

    @Mock
    private Config mConfig;

    private AccountService cAccountService;

    @Before
    public void setup() {
        when(mConfig.getAccountCacheTime()).thenReturn(5);
        cAccountService = new AccountService(mConfig, GigyaAccountClass.Default);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetAccount() {
        // Act
        cAccountService.setAccount(StaticMockFactory.getMockAccountJson());
        // Assert
        assertNotNull(cAccountService.getAccount());
        assertTrue(System.currentTimeMillis() < cAccountService.getNextInvalidationTimestamp());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAccount() {
        // Arrange
        cAccountService.setAccount(StaticMockFactory.getMockAccountJson());
        // Act
        final GigyaAccount cachedAccount = cAccountService.getAccount();
        // Assert
        assertNotNull(cachedAccount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsCachedAccount() {
        // Arrange
        cAccountService.setAccount(StaticMockFactory.getMockAccountJson());
        // Act
        boolean isCachedAccount = cAccountService.isCachedAccount();
        // Assert
        assertTrue(isCachedAccount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsCachedAccountAfterOverride() {
        // Arrange
        cAccountService.setAccount(StaticMockFactory.getMockAccountJson());
        cAccountService.setAccountOverrideCache(true);
        // Act
        boolean isCachedAccount = cAccountService.isCachedAccount();
        // Assert
        assertFalse(isCachedAccount);
    }

    @Test
    public void testGetAccountScheme() {
        // Assert
        assertEquals(GigyaAccount.class, cAccountService.getAccountSchema());
    }

    @Test
    public void testSetAccountScheme() {
        // Act
        cAccountService.setAccountScheme(TestAccount.class);
        // Assert
        assertEquals(TestAccount.class, cAccountService.getAccountSchema());

    }

    @Test
    public void testInvalidateAccount() {
        // Arrange
        cAccountService.setAccount(StaticMockFactory.getMockAccountJson());
        // Act
        cAccountService.invalidateAccount();
        // Assert
        assertNull(cAccountService.getAccount());
    }

    @Test
    public void testCalculateDiff() throws Exception {
        // Arrange
        cAccountService.setAccount(StaticMockFactory.getMockAccountJson());
        final GigyaAccount cachedAccount = cAccountService.getAccount();
        final GigyaAccount updateAccount = cAccountService.getAccount(); // Will generate a new new hard copy.
        final GigyaAccount updateAccountWithRegToken = cAccountService.getAccount(); // Will generate a new new hard copy.
        updateAccount.getProfile().setLastName("Chipopo");
        updateAccount.setActive(false);
        updateAccountWithRegToken.setUID(null);
        // Act
        final Map map1 = cAccountService.calculateDiff(new Gson(), cachedAccount, updateAccount);
        final Map map2 = cAccountService.calculateDiff(new Gson(), cachedAccount, updateAccountWithRegToken);
        // Assert map1
        assertNotNull(map1);
        assertEquals(3, map1.size());
        assertFalse((Boolean) map1.get("isActive"));
        assertNotNull(map1.get("profile"));
        String profileString = (String) map1.get("profile");
        final Map<String, Object> profile = ObjectUtils.toMap(new JSONObject(profileString));
        assertEquals("Chipopo", (String) profile.get("lastName"));
        // Assert map2
        assertNotNull(map2);
        assertEquals(1, map2.size());
    }

}
