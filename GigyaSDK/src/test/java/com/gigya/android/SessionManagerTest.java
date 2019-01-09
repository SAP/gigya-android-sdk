package com.gigya.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.InputStream;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SessionManager.class, TextUtils.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionManagerTest {

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private
    SharedPreferences.Editor editor;

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
        doReturn(sharedPreferences).when(context).getSharedPreferences(anyString(), anyInt());
        when(sharedPreferences.getString(eq("session.Token"), (String) any())).thenReturn("");
        when(sharedPreferences.contains(eq("GS_PREFS"))).thenReturn(false);
        when(sharedPreferences.edit()).thenReturn(editor);

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
        SessionManager sessionManager = new SessionManager(gigya);
        // Assert
        assertNull(sessionManager.getSession());
    }

    @Test
    public void testNewInstanceWithEncryptedSession() {

    }

    // TODO: 06/01/2019 Change load tests to constructor tests.

    @Test
    public void testLoadWithEncryptedSession() throws Exception {
        // Arrange
        final InputStream in = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaEncryptedSession.txt");
        final String encryptedSession = FileUtils.streamToString(in);
        SessionManager spy = spy(new SessionManager(gigya));
        doReturn(encryptor).when(spy, "getEncryptor");
        doReturn(secretKey).when(encryptor).getKey(context, sharedPreferences);
        when(sharedPreferences.contains(eq("GS_PREFS"))).thenReturn(true);
        when(sharedPreferences.getString(eq("GS_PREFS"), (String) any())).thenReturn(encryptedSession);
        when(gigya.getConfiguration()).thenReturn(new Configuration());
        // Act
        Whitebox.invokeMethod(spy, "load");
        // Assert
        assertNotNull(spy.getSession());
        assertEquals(spy.getSession().getSessionToken(), "mockSessionToken");
        assertEquals(spy.getSession().getSessionSecret(), "mockSessionSecret");
        assertEquals(spy.getSession().getExpirationTime(), Long.MAX_VALUE);
    }

    @Test
    public void testSave() throws Exception {
        // Arrange
        final InputStream in = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaEncryptedSession.txt");
        final String encryptedSession = FileUtils.streamToString(in);
        SessionManager spy = spy(new SessionManager(gigya));
        doReturn(encryptor).when(spy, "getEncryptor");
        doReturn(secretKey).when(encryptor).getKey(context, sharedPreferences);
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        FieldSetter.setField(spy, SessionManager.class.getDeclaredField("_session"), sessionInfo);
        when(editor.putString(anyString(), anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                String key = (String) invocation.getArguments()[0];
                // Assert
                String value = (String) invocation.getArguments()[1];
                assertEquals(key, "GS_PREFS");
                assertEquals(value, encryptedSession);
                return editor;
            }
        });
        Configuration mockConfiguration = new Configuration();
        mockConfiguration.updateIds("mockUcid", "mockGmid");
        when(gigya.getConfiguration()).thenReturn(mockConfiguration);
        // Act
        Whitebox.invokeMethod(spy, "save");
    }

    @Test
    public void testClear() throws NoSuchFieldException {
        // Arrange
        SessionManager spy = spy(new SessionManager(gigya));
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        FieldSetter.setField(spy, SessionManager.class.getDeclaredField("_session"), sessionInfo);
        when(editor.remove(anyString())).thenReturn(editor);
        // Act
        spy.clear();
        // Assert
        assertNull(spy.getSession());
    }

    @Test
    public void testIsLegacySession() throws Exception {
        // Arrange
        SessionManager spy = spy(new SessionManager(gigya));
        when(sharedPreferences.getString(eq("session.Token"), (String) any())).thenReturn("mockSessionToken");
        // Act
        final boolean isLegacySession = Whitebox.invokeMethod(spy, "isLegacySession");
        // Assert
        assertTrue(isLegacySession);
    }

    // TODO: 06/01/2019 Change load tests to constructor tests.

    // TODO: 06/01/2019 Document how to create the encrypted session file or generify test. \

    @Test
    public void testLoadLegacySession() throws Exception {
        // Arrange
        final InputStream in = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaEncryptedSession.txt");
        final String encryptedSession = FileUtils.streamToString(in);
        SessionManager spy = spy(new SessionManager(gigya));
        doReturn(encryptor).when(spy, "getEncryptor");
        doReturn(secretKey).when(encryptor).getKey(context, sharedPreferences);
        when(sharedPreferences.getString(eq("session.Token"), (String) any())).thenReturn("mockSessionToken");
        when(sharedPreferences.getString(eq("session.Secret"), (String) any())).thenReturn("mockSessionSecret");
        when(sharedPreferences.getLong(eq("session.ExpirationTime"), anyLong())).thenReturn(9223372036854775807L);
        when(sharedPreferences.getString(eq("ucid"), (String) any())).thenReturn("mockUcid");
        when(sharedPreferences.getString(eq("gmid"), (String) any())).thenReturn("mockGmid");
        Configuration mockConfiguration = new Configuration();
        when(gigya.getConfiguration()).thenReturn(mockConfiguration);
        when(editor.remove(anyString())).thenReturn(editor);
        when(editor.putString(eq("GS_PREFS"), anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                String key = (String) invocation.getArguments()[0];
                String value = (String) invocation.getArguments()[1];
                // Assert
                assertEquals(key, "GS_PREFS");
                assertEquals(value, encryptedSession);
                return editor;
            }
        });
        // Act
        Whitebox.invokeMethod(spy, "loadLegacySession");
    }

    // TODO: 06/01/2019 add test for null session.

    @Test
    public void testSetSession() throws Exception {
        // Arrange
        final InputStream in = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaEncryptedSession.txt");
        final String encryptedSession = FileUtils.streamToString(in);
        SessionManager spy = spy(new SessionManager(gigya));
        doReturn(encryptor).when(spy, "getEncryptor");
        doReturn(secretKey).when(encryptor).getKey(context, sharedPreferences);
        Configuration mockConfiguration = new Configuration();
        mockConfiguration.updateIds("mockUcid", "mockGmid");
        when(gigya.getConfiguration()).thenReturn(mockConfiguration);
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 9223372036854775807L);
        when(editor.putString(eq("GS_PREFS"), anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                String key = (String) invocation.getArguments()[0];
                String value = (String) invocation.getArguments()[1];
                // Assert
                assertEquals(key, "GS_PREFS");
                assertEquals(value, encryptedSession);
                return editor;
            }
        });
        // Act
        spy.setSession(sessionInfo);

        // TODO: 06/01/2019 Add assertion for new session setter with new values.
    }

    // TODO: 06/01/2019 Add test for null session, invalid session.


    @Test
    public void testIsValidSession() throws NoSuchFieldException {
        // Arrange
        SessionManager spy = spy(new SessionManager(gigya));
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        FieldSetter.setField(spy, SessionManager.class.getDeclaredField("_session"), sessionInfo);
        // Act
        final boolean isValidSession = spy.isValidSession();
        // Assert
        assertTrue(isValidSession);
    }

}
