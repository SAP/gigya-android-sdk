package com.gigya.android.sdk.tfa;

import com.gigya.android.sdk.tfa.persistence.ITFAPersistenceService;
import com.gigya.android.sdk.tfa.push.DeviceInfoBuilder;
import com.gigya.android.sdk.utils.DeviceUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DeviceUtils.class)
public class DeviceInfoBuilderTest {

    @Mock
    ITFAPersistenceService _persistenceService;

    @Before
    public void setup() {
        mockStatic(DeviceUtils.class);
        when(DeviceUtils.getOsVersion()).thenReturn("9.0");
        when(DeviceUtils.getManufacturer()).thenReturn("Random");
    }

    @Test
    public void test_buildWith_availableToken() {
        // Arrange
        final DeviceInfoBuilder builder = new DeviceInfoBuilder(_persistenceService);
        final String mockPushToken = "124kfjkiejgv9u=i1j2i1jgfihd8g1294fjbjasc9s8=jASDASjq013";
        // Act
        final String json = builder.buildWith(mockPushToken);
        // Assert
        assertEquals("{ \"platform\": \"android\", \"os\": \"9.0\", \"man\": \"Random\", \"pushToken\": \"124kfjkiejgv9u=i1j2i1jgfihd8g1294fjbjasc9s8=jASDASjq013\" }", json);
    }

    @Test
    public void test_buildWith_nullToken() {
        // Arrange
        final DeviceInfoBuilder builder = new DeviceInfoBuilder(_persistenceService);
        // Act
        final String json = builder.buildWith(null);
        // Assert
        assertEquals("{ \"platform\": \"android\", \"os\": \"9.0\", \"man\": \"Random\", \"pushToken\": \"null\" }", json);
    }
}
