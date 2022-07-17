package com.gigya.android.sdk.auth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import com.gigya.android.sdk.auth.models.WebAuthnOptionsModel;
import com.gigya.android.sdk.network.GigyaError;
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

import java.util.Arrays;
import java.util.Collections;

import kotlin.text.Charsets;

@RequiresApi(api = Build.VERSION_CODES.M)
public class FidoApiServiceV23Impl implements IFidoApiService {

    private static final String LOG_TAG = "FidoApiService";

    private final Context applicationContext;

    public FidoApiServiceV23Impl(Context applicationContext) {
        this.applicationContext = applicationContext;
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

    @Override
    public WebAuthnAttestationResponse onRegisterResponse(byte[] attestationResponse, byte[] credentialResponse) {

        final AuthenticatorAttestationResponse response = AuthenticatorAttestationResponse.deserializeFromBytes(attestationResponse);

        //TODO: Should use keyHandle?
        final String keyHandleBase64 = toBase64Url(response.getKeyHandle());

        final String clientDataJson = new String(response.getClientDataJSON(), Charsets.UTF_8);
        final String clientDataJsonBase64 = toBase64Url(response.getClientDataJSON());

        final String attestationObjectBase64 = toBase64Url(response.getAttestationObject());

        GigyaLogger.debug(LOG_TAG, "keyHandleBase64: " + keyHandleBase64);
        GigyaLogger.debug(LOG_TAG, "clientDataJSON: " + clientDataJson);
        GigyaLogger.debug(LOG_TAG, "attestationObjectBase64: " + attestationObjectBase64);

        final PublicKeyCredential credential = PublicKeyCredential.deserializeFromBytes(credentialResponse);

        final String idBase64 = toBase64Url(credential.getId().getBytes());
        final String rawIdBase64 = toBase64Url(credential.getRawId());

        GigyaLogger.debug(LOG_TAG, "id: " + credential.getId());
        GigyaLogger.debug(LOG_TAG, "rawID: " + Arrays.toString(credential.getRawId()));

        return new WebAuthnAttestationResponse(
                keyHandleBase64,
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
        final PublicKeyCredentialRequestOptions requestOptions = new PublicKeyCredentialRequestOptions.Builder()
                .setRpId(options.rpId)
                .setAllowList( //TODO : is it needed? check removal - retraining for one key?
                        Collections.singletonList(
                                new PublicKeyCredentialDescriptor(
                                        PublicKeyCredentialType.PUBLIC_KEY.toString(), // type
                                        "loadKeyHandle()".getBytes(), // id //TODO is that userVerification?
                                        null // transports
                                )
                        )
                )
                .setChallenge(Base64.decode(options.challenge, Base64.URL_SAFE))
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

    public WebAuthnAssertionResponse onSignResponse(byte[] fidoApiResponse, byte[] credentialResponse) {

        final AuthenticatorAssertionResponse response = AuthenticatorAssertionResponse.deserializeFromBytes(fidoApiResponse);

        //TODO: Should use keyHandle?
        final String keyHandleBase64 = Base64.encodeToString(response.getKeyHandle(), Base64.URL_SAFE);

        final String clientDataJson = new String(response.getClientDataJSON(), Charsets.UTF_8);
        final String clientDataJsonBase64 = Base64.encodeToString(response.getClientDataJSON(), Base64.URL_SAFE);

        final String authenticatorDataBase64 =
                Base64.encodeToString(response.getAuthenticatorData(), Base64.URL_SAFE);

        final String signatureBase64 = Base64.encodeToString(response.getSignature(), Base64.URL_SAFE);

        GigyaLogger.debug(LOG_TAG, "keyHandleBase64: " + keyHandleBase64);
        GigyaLogger.debug(LOG_TAG, "clientDataJSON: " + clientDataJson);
        GigyaLogger.debug(LOG_TAG, "authenticatorDataBase64: " + authenticatorDataBase64);
        GigyaLogger.debug(LOG_TAG, "signatureBase64: " + signatureBase64);

        final PublicKeyCredential credential = PublicKeyCredential.deserializeFromBytes(credentialResponse);

        final String idBase64 = Base64.encodeToString(credential.getId().getBytes(), Base64.URL_SAFE);
        final String rawIdBase64 = Base64.encodeToString(credential.getRawId(), Base64.URL_SAFE);

        return new WebAuthnAssertionResponse(
                keyHandleBase64,
                clientDataJsonBase64,
                authenticatorDataBase64,
                signatureBase64,
                idBase64,
                rawIdBase64
        );
    }

    @Override
    public GigyaError onFidoError(byte[] errorBytes) {
        final AuthenticatorErrorResponse authenticatorErrorResponse =
                AuthenticatorErrorResponse.deserializeFromBytes(errorBytes);

        final int errorCode = authenticatorErrorResponse.getErrorCode().getCode();
        final String errorMessage = authenticatorErrorResponse.getErrorMessage();

        GigyaLogger.error(LOG_TAG, "errorCode.name: " + errorCode);
        GigyaLogger.error(LOG_TAG, "errorMessage: " + errorMessage);

        return new GigyaError(
                200001,
                "fido api code: " + errorCode + ", " + errorMessage
        );
    }

    private String toBase64Url(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

}

