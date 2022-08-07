package com.gigya.android.sdk.auth;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.util.Base64;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsModel;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnKeyHandles;
import com.gigya.android.sdk.auth.models.WebAuthnOptionsModel;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.utils.UrlUtils;
import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.Attachment;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria;
import com.google.android.gms.fido.fido2.api.common.EC2Algorithm;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kotlin.text.Charsets;

/**
 * Fido api service connector for Android >=M.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class FidoApiServiceV23Impl implements IFidoApiService {

    private static final String LOG_TAG = "FidoApiService";

    private final Context applicationContext;
    private final IPersistenceService persistenceService;

    public FidoApiServiceV23Impl(
            Context applicationContext,
            IPersistenceService persistenceService) {
        this.applicationContext = applicationContext;
        this.persistenceService = persistenceService;
    }

    private String getApplicationName(Context context) {
        return context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    }

    @Override
    public void register(final ActivityResultLauncher<IntentSenderRequest> resultLauncher,
                         final WebAuthnInitRegisterResponseModel responseModel,
                         final IFidoApiFlowError flowError) {
        final WebAuthnOptionsModel options = responseModel.parseOptions();

        PublicKeyCredentialCreationOptions requestOptions;
        try {
            requestOptions = new PublicKeyCredentialCreationOptions.Builder()
                    .setRp(
                            new PublicKeyCredentialRpEntity(
                                    options.rp.id, // RP
                                    getApplicationName(applicationContext), // name //TODO use app name?
                                    null // icon
                            )
                    )
                    .setUser(
                            new PublicKeyCredentialUserEntity(
                                    Base64.decode(options.user.id, Base64.URL_SAFE), // id
                                    Arrays.toString(Base64.decode(options.user.id, Base64.URL_SAFE)), // name
                                    null, // icon
                                    getApplicationName(applicationContext) // display name //TODO use app name?
                            )
                    )
                    .setAuthenticatorSelection(new AuthenticatorSelectionCriteria.Builder()
                            .setAttachment(Attachment.fromString(options.authenticatorSelection.authenticatorAttachment)).build()) //TODO Where do we get the attachment?
                    .setChallenge(Base64.decode(options.challenge, Base64.URL_SAFE))
                    .setParameters(
                            Collections.singletonList(
                                    new PublicKeyCredentialParameters(
                                            PublicKeyCredentialType.PUBLIC_KEY.toString(),
                                            EC2Algorithm.ES256.getAlgoValue()
                                    )
                            )
                    )
                    .build();
        } catch (Attachment.UnsupportedAttachmentException e) {
            GigyaLogger.error(LOG_TAG, "Fido register: unsupported attachment");
            e.printStackTrace();
            flowError.onFlowFailedWith(
                    new GigyaError(200001, e.getLocalizedMessage())
            );
            return;
        }
        final Fido2ApiClient fido2ApiClient = Fido.getFido2ApiClient(applicationContext);
        final Task<PendingIntent> task = fido2ApiClient.getRegisterPendingIntent(requestOptions);
        task.addOnSuccessListener(new OnSuccessListener<PendingIntent>() {
            @Override
            public void onSuccess(PendingIntent pendingIntent) {
                if (pendingIntent == null) {
                    GigyaLogger.error(LOG_TAG, "Fido getRegisterPendingIntent: null pending intent");
                    flowError.onFlowFailedWith(
                            new GigyaError(200001, "Fido getRegisterPendingIntent: null pending intent")
                    );
                    return;
                }
                IntentSenderRequest senderRequest = new IntentSenderRequest.Builder(
                        pendingIntent.getIntentSender())
                        .build();
                resultLauncher.launch(senderRequest);
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                GigyaLogger.error(LOG_TAG, "Fido getRegisterPendingIntent task failed with:\n" + e.getLocalizedMessage());
                flowError.onFlowFailedWith(
                        new GigyaError(200001, e.getLocalizedMessage())
                );
            }
        });
    }

    /**
     * Handle Fido API register response.
     *
     * @param attestationResponse Api response bytes.
     * @param credentialResponse  Api credential response bytes.
     * @return WebAuthnAttestationResponse instance for WebAuthnService registration flow.
     */
    //TODO: Remove rpId parameter.
    @Override
    public WebAuthnAttestationResponse onRegisterResponse(byte[] attestationResponse, byte[] credentialResponse, String rpId) {

        final AuthenticatorAttestationResponse response = AuthenticatorAttestationResponse.deserializeFromBytes(attestationResponse);

        final String attestationObjectBase64 = toBase64Url(response.getAttestationObject());
        GigyaLogger.debug(LOG_TAG, "attestationObjectBase64: " + attestationObjectBase64);

        final PublicKeyCredential credential = PublicKeyCredential.deserializeFromBytes(credentialResponse);

        final String clientDataJson = new String(credential.getResponse().getClientDataJSON(), Charsets.UTF_8);
        GigyaLogger.debug(LOG_TAG, "clientDataJSON: " + clientDataJson);

        final String clientDataJsonBase64 = toBase64Url(credential.getResponse().getClientDataJSON());

        final String idBase64 = credential.getId();
        final String rawIdBase64 = toBase64Url(credential.getRawId());

        GigyaLogger.debug(LOG_TAG, "id: " + credential.getId());
        GigyaLogger.debug(LOG_TAG, "rawID: " + Arrays.toString(credential.getRawId()));

        storePublicId(rawIdBase64);

        return new WebAuthnAttestationResponse(
                clientDataJsonBase64,
                attestationObjectBase64,
                idBase64,
                rawIdBase64
        );
    }

    @Override
    public void sign(final ActivityResultLauncher<IntentSenderRequest> resultLauncher,
                     final WebAuthnGetOptionsResponseModel responseModel,
                     final IFidoApiFlowError flowError) {
        final WebAuthnGetOptionsModel options = responseModel.parseOptions();

        // Populate public key handles.
        final List<String> keyHandles = getKeyHandles();
        List<PublicKeyCredentialDescriptor> publicKeyCredentialDescriptors = new ArrayList<>(keyHandles.size());
        for (String handle : keyHandles) {
            GigyaLogger.debug(LOG_TAG, "Keyhandle: " + handle);
            publicKeyCredentialDescriptors.add(
                    new PublicKeyCredentialDescriptor(
                            PublicKeyCredentialType.PUBLIC_KEY.toString(), // type
                            decodeBase64Url(handle), // Public key
                            null // transports
                    )
            );
        }

        final PublicKeyCredentialRequestOptions requestOptions = new PublicKeyCredentialRequestOptions.Builder()
                .setRpId(options.rpId)
                .setAllowList(publicKeyCredentialDescriptors)
                .setChallenge(decodeBase64Url(options.challenge))
                .build();

        final Fido2ApiClient client = Fido.getFido2ApiClient(applicationContext);
        final Task<PendingIntent> task = client.getSignPendingIntent(requestOptions);
        task.addOnSuccessListener(new OnSuccessListener<PendingIntent>() {
            @Override
            public void onSuccess(PendingIntent pendingIntent) {
                if (pendingIntent == null) {
                    GigyaLogger.error(LOG_TAG, "Fido getSignPendingIntent: null pending intent");
                    flowError.onFlowFailedWith(
                            new GigyaError(200001, "Fido getSignPendingIntent: null pending intent")
                    );
                    return;
                }
                final IntentSenderRequest senderRequest = new IntentSenderRequest.Builder(
                        pendingIntent.getIntentSender())
                        .build();
                resultLauncher.launch(senderRequest);
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                GigyaLogger.error(LOG_TAG, "Fido getSignPendingIntent task failed with:\n" + e.getLocalizedMessage());
                flowError.onFlowFailedWith(
                        new GigyaError(200001, e.getLocalizedMessage())
                );
            }
        });
    }

    /**
     * Handle Fido API sign response.
     *
     * @param fidoApiResponse    Api response bytes.
     * @param credentialResponse Api credential response bytes.
     * @return WebAuthnAssertionResponse instance for WebAuthnService login flow.
     */
    //TODO: Remove rpId parameter.
    @Override
    public WebAuthnAssertionResponse onSignResponse(byte[] fidoApiResponse, byte[] credentialResponse, String rpId) {

        final AuthenticatorAssertionResponse response = AuthenticatorAssertionResponse.deserializeFromBytes(fidoApiResponse);

        final String authenticatorDataBase64 =
                toBase64Url(response.getAuthenticatorData());

        final Object userHandleBase64 =
                response.getUserHandle() != null ? toBase64Url(response.getUserHandle()) : null;
        GigyaLogger.debug(LOG_TAG, "userHandleBase64: " + userHandleBase64);

        final String signatureBase64 = toBase64Url(response.getSignature());

        GigyaLogger.debug(LOG_TAG, "authenticatorDataBase64: " + authenticatorDataBase64);
        GigyaLogger.debug(LOG_TAG, "signatureBase64: " + signatureBase64);

        final PublicKeyCredential credential = PublicKeyCredential.deserializeFromBytes(credentialResponse);

        final String clientDataJson = new String(credential.getResponse().getClientDataJSON(), Charsets.UTF_8);
        GigyaLogger.debug(LOG_TAG, "clientDataJSON: " + clientDataJson);

        final String clientDataJsonBase64 = toBase64Url(credential.getResponse().getClientDataJSON());

        final String idBase64 = credential.getId();
        final String rawIdBase64 = toBase64Url(credential.getRawId());

        return new WebAuthnAssertionResponse(
                userHandleBase64,
                clientDataJsonBase64,
                authenticatorDataBase64,
                signatureBase64,
                idBase64,
                rawIdBase64
        );
    }

    //TODO: Remove unused method. Cannot manipulate origin.
    private String overrideClientDataJsonOrigin(String clientDataJson, String rpId) {
        if (!UrlUtils.checkUrl(rpId)) {
            rpId = "https://" + rpId;
        }
        JSONObject jo;
        try {
            jo = new JSONObject(clientDataJson);
            jo.put("origin", rpId);
            return jo.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return clientDataJson;
    }

    @Override
    public GigyaError onFidoError(byte[] errorBytes) {
        final AuthenticatorErrorResponse authenticatorErrorResponse =
                AuthenticatorErrorResponse.deserializeFromBytes(errorBytes);

        final int errorCode = authenticatorErrorResponse.getErrorCode().getCode();
        final String errorMessage = authenticatorErrorResponse.getErrorMessage();

        GigyaLogger.error(LOG_TAG, "errorCode.name: " + errorCode);
        GigyaLogger.error(LOG_TAG, "errorMessage: " + errorMessage);

        // All fido error will share gigya error code 200001. Description however, will vary according
        // to error output.
        return new GigyaError(
                200001,
                "fido api code: " + errorCode + ", " + errorMessage
        );
    }

    private String toBase64Url(byte[] bytes) {
        return Base64.encodeToString(bytes,
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    public byte[] decodeBase64Url(String origin) {
        return Base64.decode(origin,
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    private void storePublicId(String handle) {
        final String json = this.persistenceService.getKeyHandles();
        final WebAuthnKeyHandles webAuthnKeyHandles = WebAuthnKeyHandles.parse(json);
        webAuthnKeyHandles.handles.add(handle);
        this.persistenceService.saveKeyHandles(webAuthnKeyHandles.toJson());
    }

    public List<String> getKeyHandles() {
        final String json = this.persistenceService.getKeyHandles();
        final WebAuthnKeyHandles webAuthnKeyHandles = WebAuthnKeyHandles.parse(json);
        return webAuthnKeyHandles.handles;
    }

}

