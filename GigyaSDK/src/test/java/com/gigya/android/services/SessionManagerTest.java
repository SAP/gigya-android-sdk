package com.gigya.android.services;

import android.content.Context;
import android.text.TextUtils;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.PersistenceService;
import com.gigya.android.sdk.services.SessionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SessionService.class, TextUtils.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionManagerTest {

    @Mock
    private PersistenceService persistenceService;

    @Mock
    private Config config;

    @Mock
    private Context context;

    @Mock
    private Gigya gigya;

    @Mock
    private IEncryptor encryptor;

    private SecretKey secretKey = new SecretKeySpec(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, "AES");

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(context).when(gigya).getContext();
        // Persistence handler mocks.
        when(persistenceService.getString("session.Token", null)).thenReturn(null);
        when(persistenceService.contains("GS_PREFS")).thenReturn(false);
        doNothing().when(persistenceService).remove(anyString());
        // Encryptor mocks.
        when(encryptor.getKey(context, persistenceService)).thenReturn(secretKey);
        // Android specific mocks.
        PowerMockito.mockStatic(TextUtils.class);
        when(TextUtils.isEmpty((CharSequence) any())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                String s = (String) invocation.getArguments()[0];
                return s == null || s.length() == 0;
            }
        });
    }

    @Test
    public void testNewInstance() {
        // Act
        SessionService sessionService = new SessionService(context, config, persistenceService, encryptor);
        // Assert
        assertNull(sessionService.getSession());
    }

    @Test
    public void testIsValidSession() { // Also tests setSession.
        // Arrange
        SessionService sessionService = new SessionService(context, config, persistenceService, encryptor);
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        sessionService.setSession(sessionInfo);
        // Act
        final boolean isValidSession = sessionService.isValidSession();
        // Assert
        assertTrue(isValidSession);
    }

    @Test
    public void testClear() {
        // Arrange
        SessionService sessionService = new SessionService(context, config, persistenceService, encryptor);
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        sessionService.setSession(sessionInfo);
        // Act
        sessionService.clear();
        // Assert
        assertNull(sessionService.getSession());
    }

    @Test
    public void testNewInstanceWithEncryptedSession() {
        // Arrange
        final String encryptedSession = "94jlrvylz9th5cv35vjcfap1ip295dqws1u430je3n8ctzoctagh6vdukiyz83qoog6bk2lq8m520q1y9livgt2ae1zomh0xvlls3rtg7" +
                "d6b71d4kk9jets8bv3pv79238tzgqmzhlzabev3z9q1p8xy1f5s8gmeq01xpw4lczdz90uoqnqua6js44yxqn3a8o64vddaw1ix319plvc234zldjyxtncu2tqi7r7lqqiqonjke1cp2add";
        when(persistenceService.contains("GS_PREFS")).thenReturn(true);
        when(persistenceService.getString(eq("GS_PREFS"), (String) eq(null))).thenReturn(encryptedSession);
        // Act
        SessionService sessionService = new SessionService(context, config, persistenceService, encryptor);
        // Assert
        assertNotNull(sessionService.getSession());
        assertEquals("mockSessionSecret", sessionService.getSession().getSessionSecret());
        assertEquals("mockSessionToken", sessionService.getSession().getSessionToken());
        assertEquals(9223372036854775807L, sessionService.getSession().getExpirationTime());
    }

    @Test
    public void testNewInstanceWithLegacySession() {
        // Arrange
        when(persistenceService.getString(eq("session.Token"), (String) eq(null))).thenReturn("mockLegacySessionToken");
        when(persistenceService.getString(eq("session.Secret"), (String) eq(null))).thenReturn("mockLegacySessionSecret");
        when(persistenceService.getLong(eq("session.ExpirationTime"), eq(0L))).thenReturn(9223372036854775807L);
        when(persistenceService.getString(eq("ucid"), (String) eq(null))).thenReturn("mockUcid");
        when(persistenceService.getString(eq("gmid"), (String) eq(null))).thenReturn("mockGmid");
        // Act
        SessionService sessionService = new SessionService(context, config, persistenceService, encryptor);
        // Assert
        assertNotNull(sessionService.getSession());
        assertEquals("mockLegacySessionSecret", sessionService.getSession().getSessionSecret());
        assertEquals("mockLegacySessionToken", sessionService.getSession().getSessionToken());
        assertEquals(9223372036854775807L, sessionService.getSession().getExpirationTime());
    }
}
