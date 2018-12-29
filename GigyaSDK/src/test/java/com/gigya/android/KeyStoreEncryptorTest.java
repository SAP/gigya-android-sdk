package com.gigya.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.gigya.android.sdk.encryption.KeyStoreEncryptor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStoreSpi;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Incomplete -> Problems mocking KeyStore object.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyStore.class, KeyStoreEncryptor.class, LoadStoreParameter.class, KeyPairGenerator.class})
public class KeyStoreEncryptorTest {

    private
    KeyStore keystoreMock;

    @Mock
    private
    KeyStoreSpi keyStoreSpiMock;

    @Mock
    private
    SharedPreferences sharedPreferences;

    @Mock
    private
    SharedPreferences.Editor editor;

    @Mock
    private KeyPairGenerator keyPairGenerator;

    @Mock
    private
    Context context;

    @Before
    public void setup() throws Exception {
        PowerMockito.mockStatic(KeyStore.class);
        PowerMockito.mockStatic(KeyPairGenerator.class);
        MockitoAnnotations.initMocks(this);
        keystoreMock = new KeyStore(keyStoreSpiMock, null, "test") {
        };
        PowerMockito.when(KeyStore.getInstance(anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return keystoreMock;
            }
        });
        PowerMockito.when(KeyPairGenerator.getInstance(anyString(), anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return keyPairGenerator;
            }
        });
    }

    @Test
    public void testGetKey() throws Exception {
        KeyStoreEncryptor encryptor = new KeyStoreEncryptor();
        PowerMockito.when(keystoreMock.containsAlias(anyString())).thenReturn(false);
//        SecretKey key = encryptor.getKey(context, sharedPreferences);
//        System.out.println(Arrays.toString(key.getEncoded()));
    }
}
