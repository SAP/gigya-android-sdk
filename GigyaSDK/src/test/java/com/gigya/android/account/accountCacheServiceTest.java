package com.gigya.android.account;

import com.gigya.android.BaseGigyaTest;
import com.gigya.android.StaticMockFactory;
import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.account.accountCacheService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.account.GigyaAccountClass;
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
public class accountCacheServiceTest extends BaseGigyaTest {

    @Mock
    private Config mConfig;

    private accountCacheService cAccountCacheService;

    @Before
    public void setup() {
        when(mConfig.getAccountCacheTime()).thenReturn(5);
        cAccountCacheService = new accountCacheService(mConfig, GigyaAccountClass.Default);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetAccount() {
        // Act
        cAccountCacheService.setAccount(StaticMockFactory.getMockAccountJson());
        // Assert
        assertNotNull(cAccountCacheService.getAccount());
        assertTrue(System.currentTimeMillis() < cAccountCacheService.getNextInvalidationTimestamp());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAccount() {
        // Arrange
        cAccountCacheService.setAccount(StaticMockFactory.getMockAccountJson());
        // Act
        final GigyaAccount cachedAccount = cAccountCacheService.getAccount();
        // Assert
        assertNotNull(cachedAccount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsCachedAccount() {
        // Arrange
        cAccountCacheService.setAccount(StaticMockFactory.getMockAccountJson());
        // Act
        boolean isCachedAccount = cAccountCacheService.isCachedAccount();
        // Assert
        assertTrue(isCachedAccount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsCachedAccountAfterOverride() {
        // Arrange
        cAccountCacheService.setAccount(StaticMockFactory.getMockAccountJson());
        cAccountCacheService.setAccountOverrideCache(true);
        // Act
        boolean isCachedAccount = cAccountCacheService.isCachedAccount();
        // Assert
        assertFalse(isCachedAccount);
    }

    @Test
    public void testGetAccountScheme() {
        // Assert
        assertEquals(GigyaAccount.class, cAccountCacheService.getAccountSchema());
    }

    @Test
    public void testSetAccountScheme() {
        // Act
        cAccountCacheService.setAccountScheme(TestAccount.class);
        // Assert
        assertEquals(TestAccount.class, cAccountCacheService.getAccountSchema());

    }

    @Test
    public void testInvalidateAccount() {
        // Arrange
        cAccountCacheService.setAccount(StaticMockFactory.getMockAccountJson());
        // Act
        cAccountCacheService.invalidateAccount();
        // Assert
        assertNull(cAccountCacheService.getAccount());
    }

    @Test
    public void testCalculateDiff() throws Exception {
        // Arrange
        cAccountCacheService.setAccount(StaticMockFactory.getMockAccountJson());
        final GigyaAccount cachedAccount = cAccountCacheService.getAccount();
        final GigyaAccount updateAccount = cAccountCacheService.getAccount(); // Will generate a new new hard copy.
        final GigyaAccount updateAccountWithRegToken = cAccountCacheService.getAccount(); // Will generate a new new hard copy.
        updateAccount.getProfile().setLastName("Chipopo");
        updateAccount.setActive(false);
        updateAccountWithRegToken.setUID(null);
        // Act
        final Map map1 = cAccountCacheService.calculateDiff(new Gson(), cachedAccount, updateAccount);
        final Map map2 = cAccountCacheService.calculateDiff(new Gson(), cachedAccount, updateAccountWithRegToken);
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
    }

}
