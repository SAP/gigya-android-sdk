package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.interruption.Resolver;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvider;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvidersModel;
import com.gigya.android.sdk.network.GigyaError;

import java.util.List;

public class TfaProviderResolver<A extends GigyaAccount> extends Resolver {

    private static final String LOG_TAG = "TfaProviderResolver";

    final IoCContainer _container;

    public TfaProviderResolver(IoCContainer container,
                               GigyaLoginCallback<A> loginCallback,
                               GigyaApiResponse interruption,
                               IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
        _container = container;
        getProviders();
    }

    private void getProviders() {
        GigyaLogger.debug(LOG_TAG, "getProviders: ");
        final String regToken = _interruption.getField("regToken", String.class);

        try {
            final IBusinessApiService service = _container.get(IBusinessApiService.class);
            service.getTFAProviders(regToken, new GigyaLoginCallback<TFAProvidersModel>() {
                @Override
                public void onSuccess(TFAProvidersModel model) {
                    final List<TFAProvider> activeProviders = model.getActiveProviders();
                    final List<TFAProvider> inactiveProviders = model.getInactiveProviders();

                    if (_interruption.getErrorCode() == GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION) {
                        onTwoFactorAuthenticationRegistration(inactiveProviders);
                    } else if (_interruption.getErrorCode() == GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION) {
                        onTwoFactorAuthenticationVerification(activeProviders);
                    }
                }

                @Override
                public void onError(GigyaError error) {
                    _loginCallback.onError(error);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: 2019-06-16 General error?
            _loginCallback.onError(GigyaError.generalError());
        }
    }

    private void onTwoFactorAuthenticationRegistration(List<TFAProvider> providers) {
        final IoCContainer resolverContainer = _container.clone();
        resolverContainer
                .bind(GigyaApiResponse.class, _interruption)
                .bind(GigyaLoginCallback.class, _loginCallback);
        try {
            TfaResolverFactory factory = resolverContainer.createInstance(TfaResolverFactory.class);

            _loginCallback.onPendingTwoFactorRegistration(_interruption, providers, factory);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: 2019-06-16 General error?
            _loginCallback.onError(GigyaError.generalError());
        } finally {
            resolverContainer.dispose();
        }
    }

    private void onTwoFactorAuthenticationVerification(List<TFAProvider> providers) {
        final IoCContainer resolverContainer = _container.clone();
        resolverContainer
                .bind(GigyaApiResponse.class, _interruption)
                .bind(GigyaLoginCallback.class, _loginCallback);
        try {
            TfaResolverFactory factory = resolverContainer.createInstance(TfaResolverFactory.class);

            _loginCallback.onPendingTwoFactorVerification(_interruption, providers, factory);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: 2019-06-16 General error?
            _loginCallback.onError(GigyaError.generalError());
        } finally {
            resolverContainer.dispose();
        }
    }
}
