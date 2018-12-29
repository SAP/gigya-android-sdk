package com.gigya.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.gigya.android.sdk.encryption.LegacyEncryptor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;

import javax.crypto.SecretKey;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

public class LegacyEncryptorTest {

    private static final String encryptedSecret = "secret123";

    @Mock
    private
    SharedPreferences sharedPreferences;

    @Mock
    private
    SharedPreferences.Editor editor;

    @Mock
    private
    Context context;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetKeyWithSavedPreferencesAlias() {
        when(sharedPreferences.getString(anyString(), (String) any())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return encryptedSecret;
            }
        });
        LegacyEncryptor encryptor = new LegacyEncryptor();
        SecretKey key = encryptor.getKey(context, sharedPreferences);
        System.out.println(Arrays.toString(key.getEncoded()));
        assertArrayEquals(key.getEncoded(), new byte[]{-35, 115, 3, 4, -101});
    }

    @Test
    public void testGetKeyNew() {
        when(sharedPreferences.getString(anyString(), (String) any())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return null;
            }
        });
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        LegacyEncryptor encryptor = new LegacyEncryptor();
        SecretKey key = encryptor.getKey(context, sharedPreferences);
        System.out.println(Arrays.toString(key.getEncoded()));
        assertNotNull(key);
    }
}
