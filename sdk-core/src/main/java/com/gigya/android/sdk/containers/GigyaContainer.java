package com.gigya.android.sdk.containers;

import android.os.Build;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.accountCacheService;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.GigyaApiRequestFactory;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.encryption.SessionKey;
import com.gigya.android.sdk.encryption.SessionKeyLegacy;
import com.gigya.android.sdk.interruption.IInterruptionResolverFactory;
import com.gigya.android.sdk.interruption.InterruptionResolverFactory;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.oidc.OIDCViewModel;
import com.gigya.android.sdk.oidc.OIDCWrapper;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.ProviderFactory;
import com.gigya.android.sdk.push.GigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.reporting.IReportingManager;
import com.gigya.android.sdk.reporting.IReportingService;
import com.gigya.android.sdk.reporting.ReportingManager;
import com.gigya.android.sdk.reporting.ReportingService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.ISessionVerificationService;
import com.gigya.android.sdk.session.SessionService;
import com.gigya.android.sdk.session.SessionVerificationService;
import com.gigya.android.sdk.ui.IPresenter;
import com.gigya.android.sdk.ui.Presenter;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.GigyaWebBridge;
import com.gigya.android.sdk.ui.plugin.IGigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.IGigyaWebBridge;
import com.gigya.android.sdk.ui.plugin.IWebViewFragmentFactory;
import com.gigya.android.sdk.ui.plugin.WebViewFragmentFactory;
import com.gigya.android.sdk.utils.FileUtils;

public class GigyaContainer extends IoCContainer {
    public GigyaContainer() {
        bind(FileUtils.class, FileUtils.class, true)
                .bind(Config.class, Config.class, true)
                .bind(ConfigFactory.class, ConfigFactory.class, false)
                .bind(IRestAdapter.class, RestAdapter.class, true)
                .bind(IPersistenceService.class, PersistenceService.class, false)
                .bind(IApiService.class, ApiService.class, false)
                .bind(IReportingService.class, ReportingService.class, true)
                .bind(IReportingManager.class, ReportingManager.class, true)
                .bind(IApiRequestFactory.class, GigyaApiRequestFactory.class, true)
                .bind(ISecureKey.class, Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? SessionKey.class
                        : SessionKeyLegacy.class, true)
                .bind(ISessionService.class, SessionService.class, true)
                .bind(IAccountService.class, accountCacheService.class, true)
                .bind(ISessionVerificationService.class, SessionVerificationService.class, true)
                .bind(IProviderFactory.class, ProviderFactory.class, false)
                .bind(IBusinessApiService.class, BusinessApiService.class, true)
                .bind(IWebViewFragmentFactory.class, WebViewFragmentFactory.class, false)
                .bind(IPresenter.class, Presenter.class, false)
                .bind(IInterruptionResolverFactory.class, InterruptionResolverFactory.class, true)
                .bind(IGigyaPluginFragment.class, GigyaPluginFragment.class, false)
                .bind(IGigyaNotificationManager.class, GigyaNotificationManager.class, true)
                .bind(IGigyaWebBridge.class, GigyaWebBridge.class, false)
                .bind(OIDCWrapper.class, OIDCWrapper.class, true)
                .bind(OIDCViewModel.class, OIDCViewModel.class, false)
                .bind(IoCContainer.class, this);
    }
}
