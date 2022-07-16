package com.gigya.android.session;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.text.TextUtils;

import com.gigya.android.BaseGigyaTest;
import com.gigya.android.StaticMockFactory;
import com.gigya.android.sdk.GigyaInterceptor;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.session.SessionService;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class, CountDownTimer.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionServiceTest extends BaseGigyaTest {

    @Mock
    private SessionInfo mSessionInfo;

    @Mock
    private ISecureKey mSecureKey;

    @Mock
    private CountDownTimer mCountdownTimer;

    private SessionService cSessionService;

    @Before
    public void setup() throws Exception {
        super.setup();
        // Shared preferences mock
        mockSharedPreferences();
        // TextUtils mock.
        mockAndroidTextUtils();
        // Config mock.
        mockConfig();
        // Session Info mock.
        when(mSessionInfo.isValid()).thenReturn(true);
        when(mSessionInfo.getSessionToken()).thenReturn("mSessionToken");
        when(mSessionInfo.getSessionSecret()).thenReturn("mSessionSecret");
        when(mSessionInfo.getExpirationTime()).thenReturn(0L);

        // Generating mock SecretKey instance.
        final KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        final SecretKey secretKey = generator.generateKey();
        when(mSecureKey.getKey()).thenReturn(secretKey);

        cSessionService = new SessionService(mContext, mConfig, mPersistenceService, mSecureKey);
    }

    @Test
    public void testEncryptSession() {
        // Act
        final String encryptedString = cSessionService.encryptSession(StaticMockFactory.getSessionMock(), mSecureKey.getKey());
        // Assert
        assertNotNull(encryptedString);
    }

    @Test
    public void testDecryptSession() {
        // Arrange
        final String encryptedString = cSessionService.encryptSession(StaticMockFactory.getSessionMock(), mSecureKey.getKey());
        // Act
        final String decryptedString = cSessionService.decryptSession(encryptedString, mSecureKey.getKey());
        // Assert
        assertNotNull(decryptedString);
    }

    @Test
    public void testSave() {
        // Arrange
        when(mPersistenceService.getSessionEncryptionType()).thenReturn("DEFAULT");
        when(mEditor.putString(anyString(), anyString())).thenAnswer(new Answer<SharedPreferences.Editor>() {
            @Override
            public SharedPreferences.Editor answer(InvocationOnMock invocation) {
                // Assert.
                assertEquals("GS_PREFS", invocation.getArgument(0));
                return mEditor;
            }
        });
        // Act
        cSessionService.save(mSessionInfo);
    }

    @Test
    public void testLoadNotLegacySession() {
        // Arrange
        final String encryptedString = cSessionService.encryptSession(StaticMockFactory.getSessionMock(), mSecureKey.getKey());
        when(mPersistenceService.isSessionAvailable()).thenReturn(true);
        when(mPersistenceService.getSession()).thenReturn(encryptedString);
        when(mPersistenceService.getSessionEncryptionType()).thenReturn("DEFAULT");
        when(mSharedPreferences.getString("GS_PREFA", null)).thenReturn(null);
        // Act
        cSessionService.load();
        SessionInfo sessionInfo = cSessionService.getSession();
        // Assert
        assertNotNull(sessionInfo);
        assertEquals(StaticMockFactory.TOKEN, sessionInfo.getSessionToken());
        assertEquals(StaticMockFactory.SECRET, sessionInfo.getSessionSecret());
        assertEquals(0L, sessionInfo.getExpirationTime());
    }

    @Test
    public void testLoadLegacySession() {
        // Arrange
        final String encryptedString = cSessionService.encryptSession(StaticMockFactory.getLegacySessionMock(), mSecureKey.getKey());
        when(mPersistenceService.getString("session.Token", null)).thenReturn(StaticMockFactory.TOKEN);
        when(mPersistenceService.getString("session.Secret", null)).thenReturn(StaticMockFactory.SECRET);
        when(mPersistenceService.getLong("session.ExpirationTime", 0L)).thenReturn(0L);
        when(mPersistenceService.getString("ucid", null)).thenReturn(StaticMockFactory.UCID);
        when(mPersistenceService.getString("gmid", null)).thenReturn(StaticMockFactory.GMID);
        when(mPersistenceService.isSessionAvailable()).thenReturn(true);
        when(mPersistenceService.getSession()).thenReturn(encryptedString);
        when(mPersistenceService.getSessionEncryptionType()).thenReturn("DEFAULT");
        when(mEditor.putString(anyString(), anyString())).thenAnswer(new Answer<SharedPreferences.Editor>() {
            @Override
            public SharedPreferences.Editor answer(InvocationOnMock invocation) {
                // Assert.
                assertEquals("GS_PREFS", invocation.getArgument(0));
                return mEditor;
            }
        });
        // Act
        cSessionService.load();
        SessionInfo sessionInfo = cSessionService.getSession();
        // Assert
        assertNotNull(sessionInfo);
        assertEquals(StaticMockFactory.TOKEN, sessionInfo.getSessionToken());
        assertEquals(StaticMockFactory.SECRET, sessionInfo.getSessionSecret());
        assertEquals(0L, sessionInfo.getExpirationTime());
    }

    @Test
    public void testSetSession() {
        // Arrange
        when(mPersistenceService.getSessionEncryptionType()).thenReturn("DEFAULT");
        when(mEditor.putString(anyString(), anyString())).thenAnswer(new Answer<SharedPreferences.Editor>() {
            @Override
            public SharedPreferences.Editor answer(InvocationOnMock invocation) {
                // Assert.
                assertEquals("GS_PREFS", invocation.getArgument(0));
                return mEditor;
            }
        });
        // Act
        cSessionService.setSession(mSessionInfo);
    }

    @Test
    public void testIsValid() {
        // Arrange
        when(mPersistenceService.getSessionEncryptionType()).thenReturn("DEFAULT");
        when(mEditor.putString(anyString(), anyString())).thenReturn(mEditor);
        // Act
        cSessionService.setSession(mSessionInfo);
        // Assert
        assertTrue(cSessionService.isValid());
    }

    @Test
    public void testClear() {
        // Arrange
        when(mPersistenceService.getSessionEncryptionType()).thenReturn("FINGERPRINT");
        doNothing().when(mPersistenceService).removeSession();
        doNothing().when(mPersistenceService).setSessionEncryptionType(anyString());
        cSessionService.setSession(mSessionInfo);
        // Act
        cSessionService.clear(true);
        // Assert
        assertNull(cSessionService.getSession());
    }

    @Test
    public void testAddInterceptions() {
        // Arrange
        when(mPersistenceService.getSessionEncryptionType()).thenReturn("DEFAULT");
        when(mEditor.putString(anyString(), anyString())).thenAnswer(new Answer<SharedPreferences.Editor>() {
            @Override
            public SharedPreferences.Editor answer(InvocationOnMock invocation) {
                // Assert.
                assertEquals("GS_PREFS", invocation.getArgument(0));
                return mEditor;
            }
        });
        GigyaInterceptor interceptor = new GigyaInterceptor("test") {
            @Override
            public void intercept() {
                final String intercepted = "done";
                // Assert
                assertEquals("done", intercepted);
            }
        };
        // Act
        cSessionService.addInterceptor(interceptor);
        cSessionService.setSession(mSessionInfo);
    }

    @Test
    public void testSessionExpiration() throws Exception {
        // Arrange
        when(mPersistenceService.getSessionEncryptionType()).thenReturn("DEFAULT");
        when(mEditor.putString(anyString(), anyString())).thenAnswer(new Answer<SharedPreferences.Editor>() {
            @Override
            public SharedPreferences.Editor answer(InvocationOnMock invocation) {
                // Assert.
                assertEquals("GS_PREFS", invocation.getArgument(0));
                return mEditor;
            }
        });
        when(mCountdownTimer.start()).thenReturn(mCountdownTimer);
        doNothing().when(mCountdownTimer).cancel();
        whenNew(CountDownTimer.class).withAnyArguments().thenReturn(mCountdownTimer);
        final SessionInfo sessionInfo = new Gson().fromJson(StaticMockFactory.getSessionMockWithFiveMinutesExpiration(), SessionInfo.class);
        // Act
        cSessionService.setSession(sessionInfo);

    }

}
