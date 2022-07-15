package com.gigya.android.sdk.auth;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentSender;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.auth.models.WebAuthnInitRegisterResponseModel;
import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.Attachment;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria;
import com.google.android.gms.fido.fido2.api.common.EC2Algorithm;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;
import java.util.Collections;

import kotlin.text.Charsets;

@RequiresApi(api = Build.VERSION_CODES.M)
public class FidoApiService implements IFidoApiService {

    private static final String LOG_TAG = "FidoApiService";

    private final Context applicationContext;

    public FidoApiService(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public enum FidoApiServiceCodes {

        REQUEST_CODE_REGISTER(1),
        REQUEST_CODE_SIGN(2);

        private final int code;

        FidoApiServiceCodes(int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }
    }

    private String getApplicationName(Context context) {
        return context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    }

    @Override
    public void register(final Activity activity, WebAuthnInitRegisterResponseModel responseModel) {
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
        Task<PendingIntent> result = fido2ApiClient.getRegisterPendingIntent(requestOptions);
        result.addOnSuccessListener(new OnSuccessListener<PendingIntent>() {
            @Override
            public void onSuccess(PendingIntent pendingIntent) {
                try {
                    activity.startIntentSenderForResult(
                            pendingIntent.getIntentSender(),
                            FidoApiServiceCodes.REQUEST_CODE_REGISTER.code,
                            null, 0, 0, 0
                    );
                } catch (IntentSender.SendIntentException e) {
                    GigyaLogger.debug(LOG_TAG, "fido2ApiClient.getRegisterPendingIntent: error launching pending intent");
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onRegisterResponse(byte[] fidoApiResponse) {
        AuthenticatorAttestationResponse response = AuthenticatorAttestationResponse.deserializeFromBytes(fidoApiResponse);
        String keyHandleBase64 = Base64.encodeToString(response.getKeyHandle(), Base64.DEFAULT);
        String clientDataJson = new String(response.getClientDataJSON(), Charsets.UTF_8);
        String attestationObjectBase64 =
                Base64.encodeToString(response.getAttestationObject(), Base64.DEFAULT);

    }

    @Override
    public void sign() {

    }

    @Override
    public void onSignResponse(byte[] fidoApiResponse) {

    }


}
