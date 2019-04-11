package com.gigya.android;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.IoCContainer;
import com.gigya.android.sdk.account.AccountService;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionService;

import org.junit.Before;
import org.mockito.Mock;

import static org.powermock.api.mockito.PowerMockito.when;

public class BaseGigyaTest {

    @Mock
    Context mContext;

    @Mock
    Config mConfig;

    @Mock
    SessionService mSessionService;

    @Mock
    AccountService mAccountService;

    @Mock
    PersistenceService mPersistenceService;

    protected IoCContainer container;

    @Before
    public void setup() {
        container = new IoCContainer();
        container.bind(Context.class, mContext);
        container.bind(Config.class, mConfig);
        container.bind(ISessionService.class, mSessionService);
        container.bind(IAccountService.class, mAccountService);
        container.bind(IPersistenceService.class, mPersistenceService);
    }

    protected void mockApiKey(String apiKey) {
        when(mConfig.getApiKey()).thenReturn(apiKey);
    }

    protected void mockApiDomain(String domain) {
        when(mConfig.getApiDomain()).thenReturn(domain);
    }

    protected void mockSession() {
        final SessionInfo sessionInfo = new SessionInfo(StaticMockFactory.getMockSecret(), StaticMockFactory.getMockToken());
        when(mSessionService.getSession()).thenReturn(sessionInfo);
    }

    protected void mockSessionWithExpiration(long expiration) {
        final SessionInfo sessionInfo = new SessionInfo(StaticMockFactory.getMockSecret(), StaticMockFactory.getMockToken(), expiration);
        when(mSessionService.getSession()).thenReturn(sessionInfo);
    }

    protected void mockLegacySession() {

    }

    protected static class TestAccount extends GigyaAccount {
    }

    protected static class TestGigya extends Gigya<TestAccount> {

        TestGigya(@NonNull Context context, Class<TestAccount> accountScheme, IoCContainer container) {
            super(context, accountScheme, container);
        }
    }
}
