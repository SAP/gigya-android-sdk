package com.gigya.android.gigyademo.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

/**
 * This broadcast receiver is used to auto read the SMS verification code using Google's new SMS retriever library.
 *
 * @see {https://developers.google.com/identity/sms-retriever/overview}
 */
public class SMSRetrieverReceiver extends BroadcastReceiver {

    public interface ISMSRetrieverCallback {

        void onMessageCodeReceived(String code);
    }

    final ISMSRetrieverCallback mCallback;

    public SMSRetrieverReceiver(ISMSRetrieverCallback callback) {
        mCallback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(SmsRetriever.SMS_RETRIEVED_ACTION)) {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                final Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
                if (status != null) {
                    switch (status.getStatusCode()) {
                        case CommonStatusCodes.SUCCESS:
                            final String message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE, "");
                            if (TextUtils.isEmpty(message)) {
                                /*
                                Note:
                                Pattern used here is specifically set in the site RBA sms configuration.
                                If your message setup is different, parsing the code will fail. Or crash. Or blow up. Whatever.
                                 */
                                final String code = message.split(":")[1].trim().split("\n\n\n")[0].trim();
                                mCallback.onMessageCodeReceived(code);
                            }
                            break;
                        case CommonStatusCodes.TIMEOUT:
                            // Handle timeout error here... that's your job. Enjoy.
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

}
