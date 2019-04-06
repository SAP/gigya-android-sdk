package com.gigya.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.IoCContainer;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.services.SessionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionServiceTest {

    @Mock
    private Context mContext;

    @Mock
    private SessionInfo mSessionInfo;

    @Mock
    private SharedPreferences mSharedPreferences;

    @Mock
    private SharedPreferences.Editor mEditor;

    @Mock
    private Config mConfig;

    @InjectMocks
    PersistenceService mPsService;

    @Mock
    private ISecureKey mSecureKey;

    private SessionService cSessionService;

    private String mockSessionJson = "{ \"sessionToken\": \"mSessionToken\", \"sessionSecret\": \"mSessionSecret\", \"expirationTime\": 0 }";
    private String mockLegacySessionJson = "{ \"session.Token\": \"mSessionToken\", \"session.Secret\": \"mSessionSecret\", \"session.ExpirationTime\": 0 }";

    @Before
    public void setup() throws NoSuchAlgorithmException, IllegalAccessException, InvocationTargetException, InstantiationException {

        IoCContainer c = new IoCContainer();
        c.bind(Context.class, mContext);
        c.bind(IPersistenceService.class, PersistenceService.class, true);

        IPersistenceService ps = c.get(IPersistenceService.class);

        // Shared preferences mock
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);
        when(mSharedPreferences.edit()).thenReturn(mEditor);
        when(mEditor.putString(anyString(), anyString())).thenReturn(mEditor);
        when(mEditor.remove(anyString())).thenReturn(mEditor);
        doNothing().when(mEditor).apply();

        // TextUtils mock.
        mockStatic(TextUtils.class);
        when(TextUtils.isEmpty((CharSequence) any())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                String s = (String) invocation.getArguments()[0];
                return s == null || s.length() == 0;
            }
        });

        // Config mock.
        when(mConfig.getUcid()).thenReturn("mUcid");
        when(mConfig.getGmid()).thenReturn("mGmid");
        when(mConfig.updateWith((Config) any())).thenReturn(mConfig);

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

        cSessionService = new SessionService(mContext,mConfig, mPsService, mSecureKey);
    }

    @Test
    public void testEncryptSession() {
        // Act
        final String encryptedString = cSessionService.encryptSession(mockSessionJson, mSecureKey.getKey());
        // Assert
        assertNotNull(encryptedString);
    }

    @Test
    public void testDecryptSession() {
        // Arrange
        final String encryptedString = cSessionService.encryptSession(mockSessionJson, mSecureKey.getKey());
        // Act
        final String decryptedString = cSessionService.decryptSession(encryptedString, mSecureKey.getKey());
        // Assert
        assertNotNull(decryptedString);
    }

    @Test
    public void testSave() {
        // Arrange
        when(mSharedPreferences.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, "DEFAULT")).thenReturn("DEFAULT");
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
        final String encryptedString = cSessionService.encryptSession(mockSessionJson, mSecureKey.getKey());
        when(mSharedPreferences.getString("session.Token", null)).thenReturn(null);
        when(mSharedPreferences.contains(PersistenceService.PREFS_KEY_SESSION)).thenReturn(true);
        when(mSharedPreferences.getString(PersistenceService.PREFS_KEY_SESSION, null)).thenReturn(encryptedString);
        when(mSharedPreferences.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, "DEFAULT")).thenReturn("DEFAULT");
        when(mSharedPreferences.getString("GS_PREFA", null)).thenReturn(null);
        // Act
        cSessionService.load();
        SessionInfo sessionInfo = cSessionService.getSession();
        // Assert
        assertNotNull(sessionInfo);
        assertEquals("mSessionToken", sessionInfo.getSessionToken());
        assertEquals("mSessionSecret", sessionInfo.getSessionSecret());
        assertEquals(0L, sessionInfo.getExpirationTime());
    }

    @Test
    public void testLoadLegacySession() {
        // Arrange
        final String encryptedString = cSessionService.encryptSession(mockLegacySessionJson, mSecureKey.getKey());
        when(mSharedPreferences.getString("session.Token", null)).thenReturn("mSessionToken");
        when(mSharedPreferences.getString("session.Secret", null)).thenReturn("mSessionSecret");
        when(mSharedPreferences.getLong("session.ExpirationTime", 0L)).thenReturn(0L);
        when(mSharedPreferences.getString("ucid", null)).thenReturn("mUcid");
        when(mSharedPreferences.getString("gmid", null)).thenReturn("mGmid");
        when(mSharedPreferences.getString(PersistenceService.PREFS_KEY_SESSION, null)).thenReturn(encryptedString);
        when(mSharedPreferences.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, "DEFAULT")).thenReturn("DEFAULT");
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
        assertEquals("mSessionToken", sessionInfo.getSessionToken());
        assertEquals("mSessionSecret", sessionInfo.getSessionSecret());
        assertEquals(0L, sessionInfo.getExpirationTime());
    }

    @Test
    public void testSetSession() {
        // Arrange
        when(mSharedPreferences.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, "DEFAULT")).thenReturn("DEFAULT");
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
        when(mSharedPreferences.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, "DEFAULT")).thenReturn("DEFAULT");
        when(mEditor.putString(anyString(), anyString())).thenReturn(mEditor);
        // Act
        cSessionService.setSession(mSessionInfo);
        // Assert
        assertTrue(cSessionService.isValid());
    }

}
