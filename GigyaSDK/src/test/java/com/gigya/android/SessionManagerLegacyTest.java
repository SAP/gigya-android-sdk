package com.gigya.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.utils.CipherUtils;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(PowerMockRunner.class)
@PrepareForTest({SessionManager.class, TextUtils.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionManagerLegacyTest {

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

    @Mock
    private Configuration configuration;

    private SecretKey MOCK_SECRET_KEY = new SecretKeySpec(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, "AES");
    private String MOCK_TOKEN = "mockSessionToken";
    private String MOCK_SECRET = "mockSessionSecret";
    private long MOCK_EXPIRATION = 9223372036854775807L;
    private String MOCK_UCID = "mockUcid";
    private String MOCK_GMID = "mockGmid";

    @Before
    public void setup() throws Exception {
        PowerMockito.mockStatic(TextUtils.class);
        when(TextUtils.isEmpty((CharSequence) any())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                String s = (String) invocation.getArguments()[0];
                return s == null || s.length() == 0;
            }
        });

        MockitoAnnotations.initMocks(this);
        doReturn(context).when(gigya).getContext();
        when(gigya.getConfiguration()).thenReturn(configuration);
        doNothing().when(configuration).updateIds(anyString(), anyString());

        doReturn(sharedPreferences).when(context).getSharedPreferences(anyString(), anyInt());
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.remove(anyString())).thenReturn(editor);
        when(sharedPreferences.getString(eq("session.Token"), (String) any())).thenReturn(MOCK_TOKEN);
        when(sharedPreferences.contains(eq("GS_PREFS"))).thenReturn(true);

        SessionManager spy = spy(new SessionManager(gigya, null));
        doReturn(encryptor).when(spy, "getEncryptor");
        doReturn(MOCK_SECRET_KEY).when(encryptor).getKey(context, sharedPreferences);

        when(sharedPreferences.getString(eq("session.Token"), (String) any())).thenReturn(MOCK_TOKEN);
        when(sharedPreferences.getString(eq("session.Secret"), (String) any())).thenReturn(MOCK_SECRET);
        when(sharedPreferences.getLong(eq("session.ExpirationTime"), anyLong())).thenReturn(MOCK_EXPIRATION);
        when(sharedPreferences.getString(eq("ucid"), (String) any())).thenReturn(MOCK_UCID);
        when(sharedPreferences.getString(eq("gmid"), (String) any())).thenReturn(MOCK_GMID);
    }

    @Test
    public void testNewInstanceWithLegacySession() {
        // Arrange
        final String encryptedMock = getEncryptedMock();
        // Act
        final SessionManager sm = new SessionManager(gigya, null);
    }

    /*
     Utility class for getting an encrypted session info.
     */
    private String getEncryptedMock() {
        String jsonToEncrypt = "{\"session.Token\":\"" + MOCK_TOKEN + "\",\"session.Secret\":\"" + MOCK_SECRET + "\",\"session.ExpirationTime\":" + MOCK_EXPIRATION
                + ",\"ucid\":\"" + MOCK_UCID + "\",\"gmid\":\"" + MOCK_GMID + "\"}";
        return CipherUtils.encrypt(jsonToEncrypt, "AES", MOCK_SECRET_KEY);
    }


}
