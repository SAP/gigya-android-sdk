package com.gigya.android.encryption;

import android.content.Context;
import android.content.SharedPreferences;

import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.persistence.PersistenceService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.crypto.*")
public class SessionKeyTest {

    @Mock
    private Context mContext;

    @Mock
    private SharedPreferences mSharedPreferences;

    @Mock
    private SharedPreferences.Editor mEditor;

    @InjectMocks
    PersistenceService mPsService;

    private ISecureKey cSecureKey;

    @Before
    public void setup() {
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);
        when(mSharedPreferences.edit()).thenReturn(mEditor);
        when(mEditor.putString(anyString(), anyString())).thenReturn(mEditor);
        doNothing().when(mEditor).apply();

        // Test instance.
    }

    @Test
    public void testAlias() {
        // Act
        final String alias = cSecureKey.getAlias();
        // Assert
        assertNotNull(alias);
    }

    @Test
    public void testGetEncryptionCipher() throws NoSuchAlgorithmException {
        // Arrange
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        // Act
        final Cipher cipher = cSecureKey.getEncryptionCipher(kp.getPrivate());
        // Assert
        assertNotNull(cipher);
        assertNotNull(cipher.getAlgorithm());
    }

    @Test
    public void testGetDecryptionCipher() throws NoSuchAlgorithmException {
        // Arrange
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        // Act
        final Cipher cipher = cSecureKey.getDecryptionCipher(kp.getPrivate());
        // Assert
        assertNotNull(cipher);
        assertNotNull(cipher.getAlgorithm());
    }
}
