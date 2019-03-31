package com.gigya.android;

import android.content.Context;

import com.gigya.android.sdk.encryption.EncryptionException;
import com.gigya.android.sdk.encryption.SessionKey;
import com.gigya.android.sdk.services.PersistenceService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyStore.class, KeyStoreSpi.class, Cipher.class, SessionKey.class, KeyPairGenerator.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionKeyTest {

    @Mock
    Context mContext;

    @Mock
    PersistenceService mPService;

    private KeyStore mKeyStore;

    private SessionKey TEST_INSTANCE;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        KeyStoreSpi keyStoreSpiMock = mock(KeyStoreSpi.class);
        mKeyStore = new KeyStore(keyStoreSpiMock, null, "test") {
        };
        mKeyStore.load(null);

        mockStatic(KeyStore.class);
        when(KeyStore.getInstance((String) any())).thenReturn(mKeyStore);
    }

    @Test
    public void getEncryptionCipherTest() throws NoSuchAlgorithmException {
        // Arrange
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        // Act
        TEST_INSTANCE = new SessionKey(mContext, mPService);
        final Cipher cipher = TEST_INSTANCE.getEncryptionCipher(kp.getPrivate());
        // Assert
        assertNotNull(cipher);
        assertEquals(TEST_INSTANCE.getTransformation(), cipher.getAlgorithm());
    }

    @Test
    public void getDecryptionCipherTest() throws NoSuchAlgorithmException {
        // Arrange
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        // Act
        TEST_INSTANCE = new SessionKey(mContext, mPService);
        final Cipher cipher = TEST_INSTANCE.getEncryptionCipher(kp.getPrivate());
        // Assert
        assertNotNull(cipher);
        assertEquals(TEST_INSTANCE.getTransformation(), cipher.getAlgorithm());
    }

    @Test
    public void getKeyTest() throws Exception {
        // Arrange
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        final Cipher mCipher = mock(Cipher.class);
        when(mCipher.doFinal((byte[]) any())).thenReturn(new byte[]{1});
        TEST_INSTANCE = new SessionKey(mContext, mPService) {
            @Override
            public Cipher getDecryptionCipher(Key key) throws EncryptionException {
                return mCipher;
            }
        };
        when(mKeyStore.containsAlias(TEST_INSTANCE.getAlias())).thenReturn(true);
        when(mKeyStore.entryInstanceOf(TEST_INSTANCE.getAlias(), KeyStore.PrivateKeyEntry.class)).thenReturn(true);
        when(mPService.getString("GS_PREFA", null)).thenReturn("String");
        when(mKeyStore.getKey(TEST_INSTANCE.getAlias(), null)).thenReturn(kp.getPrivate());
        // Act
        final SecretKey key = TEST_INSTANCE.getKey();
        // Assert
        assertNotNull(key);
    }

}
