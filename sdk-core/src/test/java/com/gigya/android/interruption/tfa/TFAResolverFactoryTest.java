package com.gigya.android.interruption.tfa;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory;
import com.gigya.android.sdk.tfa.resolvers.phone.RegisterPhoneResolver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TFAResolverFactoryTest {

    private IoCContainer _container;

    @Mock
    public GigyaLoginCallback _callback;

    @Mock
    public GigyaApiResponse _response;

    @Mock
    IBusinessApiService _businessApiService;

    public TFAResolverFactory _factory;

    @Before
    public void setup() {
        _container = new IoCContainer();
        _factory = new TFAResolverFactory(_container, _callback, _response);
    }

    @Test
    public void testGetResolverFor() {
        // Arrange
        _container.bind(IBusinessApiService.class, _businessApiService);
        // Act
        RegisterPhoneResolver resolver = _factory.getResolverFor(RegisterPhoneResolver.class);
        // Assert
        assertNotNull(resolver);
    }

    @Test
    public void testGetResolverForWithMissingDependencies() {
        // Act
        RegisterPhoneResolver resolver = _factory.getResolverFor(RegisterPhoneResolver.class);
        assertNull(resolver);
    }
}
