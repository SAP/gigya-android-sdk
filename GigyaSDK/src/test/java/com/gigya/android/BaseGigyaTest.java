package com.gigya.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.account.AccountService;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.interruption.IInterruptionsHandler;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.ISessionVerificationService;
import com.gigya.android.sdk.session.SessionService;
import com.gigya.android.sdk.session.SessionVerificationService;
import com.gigya.android.sdk.ui.IPresenter;
import com.gigya.android.sdk.ui.plugin.IWebViewFragmentFactory;
import com.gigya.android.sdk.ui.plugin.WebViewFragmentFactory;

import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
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

    @Mock
    ApiService mApiService;

    @Mock
    SessionVerificationService mSessionVerificationService;

    @Mock
    WebViewFragmentFactory mWebViewFragmentFactory;

    protected IoCContainer container;

    //region SHARED PREFERENCES

    @Mock
    protected SharedPreferences mSharedPreferences;

    @Mock
    protected SharedPreferences.Editor mEditor;

    //endregion

    public void setup() throws Exception {
        container = new IoCContainer();
        container.bind(Context.class, mContext);
        container.bind(Config.class, mConfig);
        container.bind(ISessionService.class, mSessionService);
        container.bind(IAccountService.class, mAccountService);
        container.bind(IPersistenceService.class, mPersistenceService);
        container.bind(IApiService.class, mApiService);
        container.bind(ISessionVerificationService.class, mSessionVerificationService);
        container.bind(IWebViewFragmentFactory.class, mWebViewFragmentFactory);
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

    //region HELPER CLASSES

    protected static class TestAccount extends GigyaAccount {
    }

    protected static class TestGigya extends Gigya<TestAccount> {

        TestGigya(@NonNull Context context,
                  Config config,
                  ConfigFactory configFactory,
                  ISessionService sessionService,
                  IAccountService<TestAccount> accountService,
                  IBusinessApiService<TestAccount> businessApiService,
                  ISessionVerificationService sessionVerificationService,
                  IInterruptionsHandler interruptionsHandler,
                  IPresenter presenter,
                  IProviderFactory providerFactory) {
            super(context,
                    config,
                    configFactory,
                    sessionService,
                    accountService,
                    businessApiService,
                    sessionVerificationService,
                    interruptionsHandler,
                    presenter,
                    providerFactory);
        }
    }

    //endregion

    //region MOCK CONFIG

    void mockConfig() {
        when(mConfig.getApiKey()).thenReturn(StaticMockFactory.API_KEY);
        when(mConfig.getApiDomain()).thenReturn(StaticMockFactory.API_DOMAIN);
        when(mConfig.getGmid()).thenReturn(StaticMockFactory.GMID);
        when(mConfig.getUcid()).thenReturn(StaticMockFactory.UCID);
        // Mock update.
        when(mConfig.updateWith((Config) any())).thenReturn(mConfig);
        when(mConfig.getAccountCacheTime()).thenReturn(1);
        when(mConfig.getSessionVerificationInterval()).thenReturn(5);
    }

    //endregion

    //region MOCK SHARED PREFERENCES

    void mockSharedPreferences() {
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);
        when(mSharedPreferences.edit()).thenReturn(mEditor);
        when(mEditor.putString(anyString(), anyString())).thenReturn(mEditor);
        when(mEditor.remove(anyString())).thenReturn(mEditor);
        doNothing().when(mEditor).apply();
    }

    //endregion

    //region STATIC MOCKS

    void mockAndroidTextUtils() {
        mockStatic(TextUtils.class);
        when(TextUtils.isEmpty((CharSequence) any())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                String s = (String) invocation.getArguments()[0];
                return s == null || s.length() == 0;
            }
        });
    }

    //endregion
}
