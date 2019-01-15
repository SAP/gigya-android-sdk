package com.gigya.android;

import android.content.Context;
import android.text.TextUtils;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.PersistenceHandler;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.encryption.IEncryptor;
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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SessionManager.class, TextUtils.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionManagerTest {

    @Mock
    private PersistenceHandler persistenceHandler;

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
        when(persistenceHandler.getString("session.Token", null)).thenReturn(null);
        when(persistenceHandler.contains("GS_PREFS")).thenReturn(false);
        // Encryptor mocks.
        doReturn(secretKey).when(encryptor.getKey(context, persistenceHandler));
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
    public void testIsValidSession() throws NoSuchFieldException {
        // Arrange
        SessionManager spy = spy(new SessionManager(gigya, encryptor, persistenceHandler));
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        FieldSetter.setField(spy, SessionManager.class.getDeclaredField("_session"), sessionInfo);
        // Act
        final boolean isValidSession = spy.isValidSession();
        // Assert
        assertTrue(isValidSession);
    }

    @Test
    public void testNewInstance() {
        // Act
        SessionManager sessionManager = new SessionManager(gigya, encryptor, persistenceHandler);
        // Assert
        assertNull(sessionManager.getSession());
    }

    @Test
    public void testNewInstanceWithEncryptedSession() {

    }


    @Test
    public void testNewInstanceWithLegacySession()  {

    }

    @Test
    public void testNewInstanceWithNullSession() {

    }

    @Test
    public void testClear() throws NoSuchFieldException {
        // Arrange
        SessionManager spy = spy(new SessionManager(gigya, encryptor, persistenceHandler));
        SessionInfo sessionInfo = new SessionInfo("mockSessionSecret", "mockSessionToken", 0);
        FieldSetter.setField(spy, SessionManager.class.getDeclaredField("_session"), sessionInfo);
        // Act
        spy.clear();
        // Assert
        assertNull(spy.getSession());
    }

    @Test
    public void testIsLegacySession() throws Exception {
        // Arrange
        SessionManager spy = spy(new SessionManager(gigya, encryptor, persistenceHandler));
        // Act
        final boolean isLegacySession = Whitebox.invokeMethod(spy, "isLegacySession");
        // Assert
        assertTrue(isLegacySession);
    }
    @Test
    public void testSetSession() throws Exception {
        // Arrange
        final InputStream in = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaEncryptedSession.txt");
        final String encryptedSession = FileUtils.streamToString(in);

    }

}
