package com.gigya.android;

import android.content.Context;

import com.gigya.android.sdk.PersistenceHandler;
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
    PersistenceHandler persistenceHandler;

    @Mock
    private
    Context context;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetKeyWithSavedPreferencesAlias() {
        when(persistenceHandler.getString(anyString(), (String) any())).thenReturn(encryptedSecret);
        LegacyEncryptor encryptor = new LegacyEncryptor();
        SecretKey key = encryptor.getKey(context, persistenceHandler);
        assert key != null;
        System.out.println(Arrays.toString(key.getEncoded()));

        assertArrayEquals(key.getEncoded(), new byte[]{-35, 115, 3, 4, -101});
    }

    @Test
    public void testGetKeyNew() {
        when(persistenceHandler.getString(anyString(), anyString())).thenReturn(null);
        doNothing().when(persistenceHandler).add(anyString(), anyString());

        LegacyEncryptor encryptor = new LegacyEncryptor();
        SecretKey key = encryptor.getKey(context, persistenceHandler);
        assert key != null;
        System.out.println(Arrays.toString(key.getEncoded()));
        assertNotNull(key);
    }
}
