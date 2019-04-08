package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.persistence.PersistenceService;

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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Cipher.class, Base64.class})
@PowerMockIgnore("javax.crypto.*")
public class BiometricKeyTest {

    @Mock
    private Context mContext;

    @Mock
    private SharedPreferences mSharedPreferences;

    @Mock
    private SharedPreferences.Editor mEditor;

    @InjectMocks
    PersistenceService mPsService;

    @Mock
    SecretKey mSecretKey;

    @Mock
    Cipher mCipher;

    private ISecureKey cSecureKey;

    @Before
    public void setup() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);
        when(mSharedPreferences.edit()).thenReturn(mEditor);
        when(mEditor.putString(anyString(), anyString())).thenReturn(mEditor);
        doNothing().when(mEditor).apply();

        mockStatic(Cipher.class);
        when(Cipher.getInstance(anyString())).thenReturn(mCipher);
        doNothing().when(mCipher).init(anyInt(), (SecretKey) any());
        doNothing().when(mCipher).init(anyInt(), (SecretKey) any(), (AlgorithmParameterSpec) any());

        mockStatic(Base64.class);
        when(Base64.decode(anyString(), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return java.util.Base64.getMimeDecoder().decode((String) invocation.getArguments()[0]);
            }
        });
        when(Base64.encode(any(byte[].class), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return java.util.Base64.getEncoder().encode((byte[]) invocation.getArguments()[0]);
            }
        });
        when(Base64.encodeToString(any(byte[].class), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return new String(java.util.Base64.getEncoder().encode((byte[]) invocation.getArguments()[0]));
            }
        });

        // Test instance.
        cSecureKey = new BiometricKey(mPsService);
    }

    @Test
    public void testAlias() {
        // Act
        final String alias = cSecureKey.getAlias();
        // Assert
        assertNotNull(alias);
    }

    @Test
    public void testGetEncryptionCipher() {
        // Act
        final Cipher cipher = cSecureKey.getEncryptionCipher(mSecretKey);
        // Assert
        assertNotNull(cipher);
    }

    @Test
    public void testGetDecryptionCipher() {
        // Arrange
        when(mPsService.getString(PersistenceService.PREFS_KEY_IV_SPEC, null)).thenReturn("mockIpvSpec");
        // Act
        final Cipher cipher = cSecureKey.getDecryptionCipher(mSecretKey);
        // Assert
        assertNotNull(cipher);
    }
}
