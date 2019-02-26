package com.gigya.android;

import android.content.Context;
import android.text.TextUtils;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.PersistenceManager;
import com.gigya.android.sdk.SDKContext;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.SessionInfo;

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
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SessionManager.class, TextUtils.class, SDKContext.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionManagerTest {

    @Mock
    private PersistenceManager persistenceManager;

    @Mock
    private Configuration configuration;

    @Mock
    private Context context;

    @Mock
    private Gigya gigya;

    @Mock
    private IEncryptor encryptor;

    @Mock
    private SDKContext SDKContext;

    private SecretKey secretKey = new SecretKeySpec(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, "AES");

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockStatic(SDKContext.class);
        doReturn(context).when(gigya).getContext();
        when(SDKContext.getInstance()).thenReturn(SDKContext);
        when(SDKContext.getPersistenceManager()).thenReturn(persistenceManager);
        when(SDKContext.getEncryptor()).thenReturn(encryptor);
        when(SDKContext.getConfiguration()).thenReturn(configuration);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                SessionManager sessionManager = invocation.getArgument(0);
                sessionManager.inject(
                        SDKContext.getConfiguration(),
                        SDKContext.getEncryptor(),
                        SDKContext.getPersistenceManager());
                return null;
            }
        }).when(SDKContext).inject(any(SessionManager.class));

        // Persistence handler mocks.
        when(persistenceManager.getString("session.Token", null)).thenReturn(null);
        when(persistenceManager.contains("GS_PREFS")).thenReturn(false);
        doNothing().when(persistenceManager).remove(anyString());
        // Encryptor mocks.
        when(encryptor.getKey(context, persistenceManager)).thenReturn(secretKey);
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
        SessionManager sessionManager = new SessionManager(context);
        // Assert
        assertNull(sessionManager.getSession());
    }

    @Test
    public void testIsValidSession() { // Also tests setSession.
        // Arrange
        SessionManager sessionManager = new SessionManager(context);
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        sessionManager.setSession(sessionInfo);
        // Act
        final boolean isValidSession = sessionManager.isValidSession();
        // Assert
        assertTrue(isValidSession);
    }

    @Test
    public void testClear() {
        // Arrange
        SessionManager sessionManager = new SessionManager(context);
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        sessionManager.setSession(sessionInfo);
        // Act
        sessionManager.clear();
        // Assert
        assertNull(sessionManager.getSession());
    }

    @Test
    public void testNewInstanceWithEncryptedSession() {
        // Arrange
        final String encryptedSession = "94jlrvylz9th5cv35vjcfap1ip295dqws1u430je3n8ctzoctagh6vdukiyz83qoog6bk2lq8m520q1y9livgt2ae1zomh0xvlls3rtg7" +
                "d6b71d4kk9jets8bv3pv79238tzgqmzhlzabev3z9q1p8xy1f5s8gmeq01xpw4lczdz90uoqnqua6js44yxqn3a8o64vddaw1ix319plvc234zldjyxtncu2tqi7r7lqqiqonjke1cp2add";
        when(persistenceManager.contains("GS_PREFS")).thenReturn(true);
        when(persistenceManager.getString(eq("GS_PREFS"), (String) eq(null))).thenReturn(encryptedSession);
        // Act
        SessionManager sessionManager = new SessionManager(context);
        // Assert
        assertNotNull(sessionManager.getSession());
        assertEquals("mockSessionSecret", sessionManager.getSession().getSessionSecret());
        assertEquals("mockSessionToken", sessionManager.getSession().getSessionToken());
        assertEquals(9223372036854775807L, sessionManager.getSession().getExpirationTime());
    }

    @Test
    public void testNewInstanceWithLegacySession() {
        // Arrange
        when(persistenceManager.getString(eq("session.Token"), (String) eq(null))).thenReturn("mockLegacySessionToken");
        when(persistenceManager.getString(eq("session.Secret"), (String) eq(null))).thenReturn("mockLegacySessionSecret");
        when(persistenceManager.getLong(eq("session.ExpirationTime"), eq(0L))).thenReturn(9223372036854775807L);
        when(persistenceManager.getString(eq("ucid"), (String) eq(null))).thenReturn("mockUcid");
        when(persistenceManager.getString(eq("gmid"), (String) eq(null))).thenReturn("mockGmid");
        // Act
        SessionManager sessionManager = new SessionManager(context);
        // Assert
        assertNotNull(sessionManager.getSession());
        assertEquals("mockLegacySessionSecret", sessionManager.getSession().getSessionSecret());
        assertEquals("mockLegacySessionToken", sessionManager.getSession().getSessionToken());
        assertEquals(9223372036854775807L, sessionManager.getSession().getExpirationTime());
    }
}
