package com.gigya.android.encryption;

import android.content.Context;

import com.gigya.android.sdk.PersistenceManager;
import com.gigya.android.sdk.encryption.LegacyEncryptor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import javax.crypto.SecretKey;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

public class LegacyEncryptorTest {

    private static final String encryptedSecret = "secret123";

    @Mock
    private
    PersistenceManager persistenceManager;

    @Mock
    private
    Context context;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetKeyWithSavedPreferencesAlias() {
        when(persistenceManager.getString(anyString(), (String) any())).thenReturn(encryptedSecret);
        LegacyEncryptor encryptor = new LegacyEncryptor();
        SecretKey key = encryptor.getKey(context, persistenceManager);
        assert key != null;
        System.out.println(Arrays.toString(key.getEncoded()));

        assertArrayEquals(key.getEncoded(), new byte[]{-35, 115, 3, 4, -101});
    }

    @Test
    public void testGetKeyNew() {
        when(persistenceManager.getString(anyString(), anyString())).thenReturn(null);
        doNothing().when(persistenceManager).add(anyString(), anyString());

        LegacyEncryptor encryptor = new LegacyEncryptor();
        SecretKey key = encryptor.getKey(context, persistenceManager);
        assert key != null;
        System.out.println(Arrays.toString(key.getEncoded()));
        assertNotNull(key);
    }
}
