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

import java.security.Key;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.crypto.*")
public class SessionKeyLegacyTest {

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
    public void testGetKeyNew() {
        // Arrange
        when(mSharedPreferences.getString(anyString(), anyString())).thenReturn(null);
        // Act
        final Key key = cSecureKey.getKey();
        // Assert
        assertNotNull(key);
    }

    @Test
    public void testGetKeyExisting() {
        // Arrange
        when(mSharedPreferences.getString("GS_PREFA", null)).thenReturn("mockEncryption");
        // Act
        final Key key = cSecureKey.getKey();
        // Assert
        assertNotNull(key);
    }

}
