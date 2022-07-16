package com.gigya.android.sdk.auth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Base64;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.auth.models.WebAuthnAssertionResponse;
import com.gigya.android.sdk.auth.models.WebAuthnAttestationResponse;
import com.gigya.android.sdk.auth.models.WebAuthnGetOptionsResponseModel;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
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
    public void register(final ComponentActivity activity, final WebAuthnInitRegisterResponseModel responseModel, final IFidoResponseResult fidoResult) {
        PublicKeyCredentialCreationOptions requestOptions = new PublicKeyCredentialCreationOptions.Builder()
                .setRp(
                        new PublicKeyCredentialRpEntity(
                                responseModel.options.rp.id, // RP
                                getApplicationName(applicationContext), // name //TODO use app name?
                                null // icon
                        )
                )
                .setUser(
                        new PublicKeyCredentialUserEntity(
                                Base64.decode(responseModel.options.user.id, Base64.URL_SAFE), // id
                                Arrays.toString(Base64.decode(responseModel.options.user.id, Base64.URL_SAFE)), // name
                                null, // icon
                                getApplicationName(applicationContext) // display name //TODO use app name?
                        )
                )
                .setAuthenticatorSelection(new AuthenticatorSelectionCriteria.Builder()
                        .setAttachment(Attachment.PLATFORM).build()) //TODO Where do we get the attachment?
                .setChallenge(Base64.decode(responseModel.options.challenge, Base64.URL_SAFE))
                .setParameters(
                        Collections.singletonList(
                                new PublicKeyCredentialParameters(
                                        PublicKeyCredentialType.PUBLIC_KEY.toString(),
                                        EC2Algorithm.ES256.getAlgoValue()
                                )
                        )
                )
                .build();
        Fido2ApiClient fido2ApiClient = Fido.getFido2ApiClient(applicationContext);
        Task<PendingIntent> task = fido2ApiClient.getRegisterPendingIntent(requestOptions);
        task.addOnSuccessListener(new OnSuccessListener<PendingIntent>() {
            @Override
            public void onSuccess(PendingIntent pendingIntent) {
                Intent fillIntent = new Intent(applicationContext, activity.getClass());
                fillIntent.putExtra("token", responseModel.token);
                fillIntent.putExtra("requestCode", FidoApiService.FidoApiServiceCodes.REQUEST_CODE_REGISTER.code());
                ActivityResultLauncher<IntentSenderRequest> startForResult = activity.registerForActivityResult(
                        new ActivityResultContracts.StartIntentSenderForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                fidoResult.onIntent(result.getResultCode(), result.getData());
                            }
                        }
                );
                startForResult.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender())
                        .setFillInIntent(fillIntent).build());
            }
        });
    }

    @Override
    public WebAuthnAttestationResponse onRegisterResponse(byte[] attestationResponse, byte[] credentialResponse) {

        AuthenticatorAttestationResponse response = AuthenticatorAttestationResponse.deserializeFromBytes(attestationResponse);
        String keyHandleBase64 = Base64.encodeToString(response.getKeyHandle(), Base64.DEFAULT);
        String clientDataJson = new String(response.getClientDataJSON(), Charsets.UTF_8);
        String attestationObjectBase64 =
                Base64.encodeToString(response.getAttestationObject(), Base64.DEFAULT);

        PublicKeyCredential credential = PublicKeyCredential.deserializeFromBytes(credentialResponse);

        GigyaLogger.debug(LOG_TAG, "id: " + credential.getId());
        GigyaLogger.debug(LOG_TAG, "rawID: " + Arrays.toString(credential.getRawId()));
        GigyaLogger.debug(LOG_TAG, "keyHandleBase64: " + keyHandleBase64);
        GigyaLogger.debug(LOG_TAG, "clientDataJSON: " + clientDataJson);
        GigyaLogger.debug(LOG_TAG, "attestationObjectBase64: " + attestationObjectBase64);

        return new WebAuthnAttestationResponse(
                keyHandleBase64,
                Base64.encodeToString(response.getClientDataJSON(), Base64.DEFAULT),
                attestationObjectBase64,
                credential.getId(),
                Base64.encodeToString(credential.getRawId(), Base64.DEFAULT)
        );
    }

    @Override
    public void sign(final ComponentActivity activity, final WebAuthnGetOptionsResponseModel responseModel, final IFidoResponseResult fidoResult) {
        final PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions.Builder()
                .setRpId(responseModel.options.rpId)
                .setAllowList(
                        Collections.singletonList(
                                new PublicKeyCredentialDescriptor(
                                        PublicKeyCredentialType.PUBLIC_KEY.toString(), // type
                                        "loadKeyHandle()".getBytes(), // id //TODO is that userVerification?
                                        null // transports
                                )
                        )
                )
                .setChallenge(Base64.decode(responseModel.options.challenge, Base64.URL_SAFE))
                .build();

        Fido2ApiClient client = Fido.getFido2ApiClient(applicationContext);
        Task<PendingIntent> task = client.getSignPendingIntent(options);
        task.addOnSuccessListener(new OnSuccessListener<PendingIntent>() {
            @Override
            public void onSuccess(PendingIntent pendingIntent) {
                Intent fillIntent = new Intent(applicationContext, activity.getClass());
                fillIntent.putExtra("token", responseModel.token);
                fillIntent.putExtra("requestCode", FidoApiService.FidoApiServiceCodes.REQUEST_CODE_SIGN.code());
                ActivityResultLauncher<IntentSenderRequest> startForResult = activity.registerForActivityResult(
                        new ActivityResultContracts.StartIntentSenderForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                fidoResult.onIntent(result.getResultCode(), result.getData());
                            }
                        }
                );
                startForResult.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender())
                        .setFillInIntent(fillIntent).build());
            }
        });
    }

    public WebAuthnAssertionResponse onSignResponse(byte[] fidoApiResponse) {
        AuthenticatorAssertionResponse response = AuthenticatorAssertionResponse.deserializeFromBytes(fidoApiResponse);
        String keyHandleBase64 = Base64.encodeToString(response.getKeyHandle(), Base64.DEFAULT);
        String clientDataJson = new String(response.getClientDataJSON(), Charsets.UTF_8);
        String authenticatorDataBase64 =
                Base64.encodeToString(response.getAuthenticatorData(), Base64.DEFAULT);
        String signatureBase64 = Base64.encodeToString(response.getSignature(), Base64.DEFAULT);

        GigyaLogger.debug(LOG_TAG, "keyHandleBase64: " + keyHandleBase64);
        GigyaLogger.debug(LOG_TAG, "clientDataJSON: " + clientDataJson);
        GigyaLogger.debug(LOG_TAG, "authenticatorDataBase64: " + authenticatorDataBase64);
        GigyaLogger.debug(LOG_TAG, "signatureBase64: " + signatureBase64);

        return new WebAuthnAssertionResponse(
                keyHandleBase64,
                Base64.encodeToString(response.getClientDataJSON(), Base64.DEFAULT),
                authenticatorDataBase64,
                signatureBase64
        );
    }

    private void parseErrorResponse(byte[] errorBytes) {
        AuthenticatorErrorResponse authenticatorErrorResponse =
                AuthenticatorErrorResponse.deserializeFromBytes(errorBytes);
        int errorCode = authenticatorErrorResponse.getErrorCode().getCode();
        String errorMessage = authenticatorErrorResponse.getErrorMessage();

        GigyaLogger.debug(LOG_TAG, "errorCode.name: " + errorCode);
        GigyaLogger.debug(LOG_TAG, "errorMessage: " + errorMessage);
    }

}

