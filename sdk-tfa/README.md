# SAP CDC (Gigya) Android SDK

## Description
Gigya's Android Two Factor Authentication (TFA) SDK library provides a Java interface for applying two factor authentication flows.

## Requirements
Gigya's Android Core library implementation.
Android SDK 14 and above is required.

## Limitations
The Android SDK v4.x does not currently support Gradle plugins v3.6.x.
To avoid any AndroidX related errors and warnings, be sure to use only Gradle plugin v3.5.x.

## Integration
Update your gradle.build file with one of two options:

Implementation using a binary file.
Download the tfa library and copy it to your applications libs/ folder.
```gradle
implementation files('libs/gigya-android-biometric-1.0.4.aar')
```

-Or-

Implementation using **JitPack**
```gradle
implementation 'com.github.SAP:gigya-android-sdk:tfa-v1.0.4'
```

**In addition you will need to add this dependency as well.**
```gradle
implementation 'com.android.support.design:28.0.0'
// If using push TFA.
implementation 'com.google.firebase:firebase-core:16.0.9'
implementation 'com.google.firebase:firebase-messaging:18.0.0'
```

The Android TFA package version provides the ability to integrate native Two Factor Authentication flows within your Android application
without using the ScreenSets feature.

Current supported TFA providers are:

```
gigyaPhone
gigyaEmail
gigyaPush
gigyaTotp
```

## Initialization

In order to use Two Factor Authentication for your site please please read:
[Risk Based Authentication](https://developers.gigya.com/display/GD/Risk+Based+Authentication)

```
String resource ids have changed with this revision (v1.0.3). If you have previously used the provided UI elements and overridden their
string resources for additional customization, you must add a "gig_" prefix to all of your string ids.
example:
```
#### PREVIOUS
```xml
<string name="tfa_title">TFA title</string>
```

#### CURRENT
```xml
<string name="gig_tfa_title">TFA title</string>
```

```java
/*
* Call this method to correctly initialize the libraries push TFA feature.
* Make sure you call this method when your activity context is attached.
*/
GigyaTFA.getInstance().registerForRemoteNotifications(this /* Activity instance */);
```
## Two Factor Authentication interruptions

When using login/register flows, you are able to override two additional callback methods within the GigyaLoginCallback class:

```java
@Override
public void onPendingTwoFactorRegistration(@NonNull GigyaApiResponse response, @NonNull List<TFAProviderModel> inactiveProviders,
     @NonNull TFAResolverFactory resolverFactory) {
// The login/register flow was interrupted with error 403102 (Account
Pending TFA Registration)
}
@Override
public void onPendingTwoFactorVerification(@NonNull GigyaApiResponse
response, @NonNull List<TFAProviderModel> activeProviders, @NonNull
TFAResolverFactory resolverFactory) {
    // The login/register flow was interrupted with error 403101 (Account Pending TFA Verification)
}
```
These callbacks are called interruption callbacks. Their main purpose is to inform the client that a Two Factor Authentication interruption has
happened. In addition they provide the user with the relevant data needed to resolve the interruption in the same context they were initiated.

### Initial interruption data

response: GigyaApiResponse - The initial interruption response received by the login/register attempt.
**inactiveProviders: List<TFAProviderModel>** - A list containing the Two Factor Authentication providers available for registration.
**activeProviders: List<TFAProviderModel>** - A list containing the registered Two Factor Authentication providers for this account.
**resolverFactory: TFAResolverFactory** - A provided factory class which allows you to fetch the appropriate resolver class in order to continue
the login/register flow.

```
Notes:
The TFA package contains various Fragment classes which you can use in order to resolve various Two Factor Authentication
flows. All of which are implemented in the provided sample application.
All resolver flows will end with redirecting the finalized logged-in/registered account to the original "onSuccess" callback. In
addition, at the end of each successful flow an "onResolved" callback will be called in order to give an optional logic check
point if any other application tasks are needed to be performed.
```

## Email Verification

Resolving email verification Two Factor Authentication is done using the RegisteredEmailsResolver class.

Email verification requires you to have a valid registered email account.

Code example for email verification flow. Note that this is just a partial representation of the flow and will require additional UI intervention. A
complete sample is available in the provided TFAEmailVerificationFragment class.

```java
final RegisteredEmailsResolver registeredEmailResolver =  resolverFactory.getResolverFor(RegisteredEmailsResolver.class)
registeredEmailResolver.getRegisteredEmails(new RegisteredEmailsResolver.ResultCallback() {
    @Override
    public void onRegisteredEmails(List<EmailModel> registeredEmailList) {
        // Populate registered emails display list.
        // Once selected call:
        registeredEmailResolver.sendEmailCode(selectedEmail, ...)
    }
    
    @Override
    public void onEmailVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver) {
        // Email verification code was successfully sent to provided email address
        // Once code is available call:
        verifyCodeResolver.verifyCode(GigyaDefinitions.TFAProvider.EMAIL, verificationCode, false, new VerifyCodeResolver.ResultCallback() {
    
            @Override
            public void onResolved() {
            // Flow completed.
            }

            @Override
            public void onInvalidCode() {
            // Invalid code inserted. Try again.
            }

            @Override
            public void onError(GigyaError error) {
            // handle error.
            }
        );
    }

    @Override
    public void onError(GigyaError error) {
    // Handle error.
    }
});
```

## Phone Registration

Resolving phone Two Factor Authentication registration is done using the RegisterPhoneResolver class.

Code example for phone registration flow. Note that this is just a partial representation of the flow and will require additional UI intervention. A
complete sample is available in the provided TFAPhoneRegistrationFragment class.
    
```java
final RegisterPhoneResolver registerPhoneResolver = resolverFactory.getResolverFor(RegisterPhoneResolver.class)
registerPhoneResolver.registerPhone(phoneNumber, new RegisterPhoneResolver.ResultCallback() {

    @Override
    public void onVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver) {
    // Verification code was sent to registered phone number. At this point you should update your UI to support verification input.
    // After UI has been updated and the verification code is available, you are able to use:
    verifyCodeResolver.verifyCode(GigyaDefinitions.TFAProvider.PHONE, vverificationCode, false /* set true to remember device */ , 
        new VerifyCodeResolver.ResultCallback() {
            
            @Override
            public void onResolved() {
            // Flow completed.
            }

            @Override
            public void onInvalidCode() {
            // Invalid code inserted. Try again.
            }

            @Override
            public void onError(GigyaError error) {
            // handle error.
            }
        );
    }

    @Override
    public void onError(GigyaError error) {
    // Handle error.
    }
});
```

## Phone Verification

Resolving phone Two Factor Authentication verification is done using the RegisteredPhonesResolver class.

Code example for phone verification flow. Note that this is just a partial representation of the flow and will require additional UI intervention. A
complete sample is available in the provided TFAPhoneVerificationFragment class.

```java
final RegisteredPhonesResolver registeredPhonesResolver = resolverFactory.getResolverFor(RegisteredPhonesResolver.class)
registeredPhonesResolver.getPhoneNumbers(new RegisteredPhonesResolver.ResultCallback() {

    @Override
    public void onRegisteredPhones(List<RegisteredPhone> registeredPhonesList) {
        // Display list of registered phones to the user so he will be able to choose where to send verification code sms/voice call.
        // After user chooses call:
        registeredPhonesResolver.sendVerificationCode(...);
    }

    @Override
    public void onVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver) {
    // Verification code successfully sent.
    // You are now able to verify the code received calling:
    verifyCodeResolver.verifyCode(GigyaDefinitions.TFAProvider.PHONE, verificationCode, false /* set true to remember device */ , 
        new VerifyCodeResolver.ResultCallback() {
        
            @Override
            public void onResolved() {
            // Flow completed.
            }
            
            @Override
            public void onInvalidCode() {
            // Invalid code inserted. Try again.
            }
            
            @Override
            public void onError(GigyaError error) {
            // handle error.
            }
        );
    }

    @Override
    public void onError(GigyaError error) {
    // handle error.
    }
});
```


## TOTP Registration

Resolving TOTP Two Factor Authentication registration is done using the RegisterTOTPResolver class.

Code example for TOTP registration flow. Note that this is just a partial representation of the flow and will require additional UI intervention. A
complete sample is available in the provided TOTPRegistrationFragment class.

```java
final RegisterTOTPResolver registerTOTPResolver = resolverFactory.getResolverFor(RegisterTOTPResolver.class)
registerTOTPResolver.registerTOTP(new RegisterTOTPResolver.ResultCallback() {
    
    @Override
    public void onQRCodeAvailable(@NonNull String qrCode, IVerifyTOTPResolver verifyTOTPResolver) {
        // Base64 encoded QR code is available. Decode it to a bitmap and display for the user to scan.
        // Once the user scans the code and a verification code is available via the authenticator application you are able to call:
        verifyTOTPResolver.verifyTOTPCode(verificationCode, false /* or true to save device */, new VerifyTOTPResolver.ResultCallback() {

            @Override
            public void onResolved() {
            // Flow completed.
            }
            
            @Override
            public void onInvalidCode() {
            // Verification code invalid. Display error and try again.
            }
            
            @Override
            public void onError(GigyaError error) {
            // Handle error.
            }

        });
    }

    @Override
    public void onError(GigyaError error) {
    // Handle error.
    }

});
```

## TOTP Verification

Resolving TOTP Two Factor Authentication verification is done using the VerifyTOTPResolver class.

Code example for TOTP verification flow. Note that this is just a partial representation of the flow and will require additional UI intervention. A
complete sample is available in the provided TOTPVerificationFragment class.

```java
final VerifyTOTPResolver verifyTOTPResolver = resolverFactory.getResolverFor(VerifyTOTPResolver.class)
// At this point the code is already available to the user using his preferred authenticator application.
verifyTOTPResolver.verifyTOTPCode(verificationCode, false /* true for to save device */, new VerifyTOTPResolver.ResultCallback() {

        @Override
        public void onResolved() {
        // Flow completed.
        }
        
        @Override
        public void onInvalidCode() {
        // Verification code invalid. Display error and try again.
        }
        
        @Override
        public void onError(GigyaError error) {
        // Handle error.
        }

});
```
## Push TFA

The push TFA feature allows you to secure your login using push notifications to any registered devices.

RBA Push Notifications

### Messaging service

In order to add the push TFA feature to your application you will need a working push notification/messaging service.

```
This feature currently uses all registered mobile devices to verify any login process made from a website for a specific account.
*Mobile login with push TFA is not currently available.
```

Add the following dependencies to your build.gradle file:

```gradle
implementation 'com.google.firebase:firebase-core:16.0.9'
implementation 'com.google.firebase:firebase-messaging:18.0.0'
```
### Using Google Firebase

An active Firebase account is needed in order to integrate the push TFA service.

Instructions on how to add Firebase into your Android application can be found here: https://firebase.google.com/docs/android/setup

### Setting up your application to use cloud messaging

Once you have your Firebase up and running, you are able to register your application in the Cloud Messaging tab of your project Settings pag
e.

```
Go to your Firebase console. Select your project settings and navigate to "Cloud Messaging" tag. Copy your Server key as shown:

Use the copied Server key and update your CDC console RBA settings.

Currently we support only Google Firebase.

It is not possible to add a registered Google project into a running Firebase project (even if you have just opened a new one). If you
already have a registered Google project you will need to link it to Firebase.
To do this, first make sure you are logged in with the same account your Android project is registered to and then link it by choosing the
project from your registered projects when creating a new Firebase project.
```

### Adding Gigya's messaging service

Adding the messaging service
The Android SDK provides a GigyaFirebaseMessagingService class for you. In order to integrate it please add the following to
your *AndroidManifest.xml*.

```xml
<service android:name="com.gigya.android.sdk.push.GigyaFirebaseMessagingService"
 android:exported="false">
    <intent-filter>
    <action
        android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```
Our GigyaFirebaseMessagingService extends the provided FIrebaseMessagingService. If your application already uses
the FirebaseMessagingService your will you will need to make your class extend the GigyaFirebaseMessagingService.
In order for all flows to remain intact make sure to call the relevant super methods:

```java
@Override
public void onNewToken(String newToken) {
    // Calling the super method to enable the Gigya messaging flow.
    super.onNewToken(newToken);
}

@Override
public void onMessageReceived(RemoteMessage remoteMessage) {
    // Calling the super method to enable the Gigya messaging flow.
    super.onMessageReceived(remoteMessage);
}
```

#Step 2#
Adding the TFAPushReceiver & the relevant content activity
The push TFA notification contains a content pending intent (setContentIntent) which will trigger an Activity to open in order to
handle the notification content. You will have to declare that activity in your AndroidManifest.xml. The Android SDK already
provides you with a template PushTFAActivity class which handles the content intent for you and will display the relevant action
UI & handle the selection.
In order to use the provided PushTFAActivity class please add the following to your AndroidManifest.xml file:

```xml
<activity android:name="com.gigya.android.sdk.tfa.ui.PushTFAActivity"
    android:excludeFromRecents="true"
    android:launchMode="singleTask"
    android:taskAffinity=""
    android:theme="@style/Theme.AppCompat.Translucent" />
```

Additionally, all action-based push notifications will add the relevant buttons to the notification body. In order to allow the SDK to
handle these actions please register the following Broadcast Receiver:

```xml
<receiver android:name="com.gigya.android.sdk.tfa.push.TFAPushReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="@string/gig_tfa_action_approve" />
        <action android:name="@string/gig_tfa_action_deny" />
    </intent-filter>
</receiver>
```
### Available customization options

The Android SDK provides additional customization options for your TFA specific remote messages & the option to use your own customized
activity for remote actions (approve/deny).

In order to provide customization please use the following method:

```
Using a customized action activity is mandatory when using fingerprint session encryption. A detailed example is provided further down
this tutorial.
```

```java
GigyaTFA.getInstance().setPushCustomizer(new IGigyaPushCustomizer() {

    @Override
    public Class getCustomActionActivity() {
    return MyCustomActionActivity.class;
    }
    
    @Override
    public int getSmallIcon() {
    return R.drawable.my_custom_tfa_notification_icon;
    }
    
    /*
    * Note: These icons will only display until Android Nougat (api level 25).
    */
    @Override
    public int getApproveActionIcon() {
    return R.drawable.my_custom_tfa_approve_action_icon_api_25;
    }
    
    /*
    * Note: These icons will only display until Android Nougat (api level 25).
    */
    @Override
    public int getDenyActionIcon() {
    return R.drawable.my_custom_tfa_deny_action_icon_api_25;
    }

});
```

## Push TFA Flow

### Opt-In process

In order for a client to opt-in to use the push TFA feature you will need to add the option to opt-in after the user have successfully logged in.

```java
GigyaTFA.getInstance().optInForPushTFA(new GigyaCallback<GigyaApiResponse>() {
    @Override
    public void onSuccess(GigyaApiResponse obj) {
    // Step one of the opt-in process has been completed.
    // Wait for approval push notification and to complete flow.
    }

    @Override
    public void onError(GigyaError error) {
    // Handle error.
    }
});
```

Select Approve in order to finalize the opt-in process.
You should receive another notification to indicate the flow has been successfully completed.

### Verification process

Once you opt-in to use the Push TFA service your client will login to his account on the website and an approval notification will be sent to all
registered devices (which have completed the opt-in process).

Once you choose to approve your client will be logged into the system.

```
Push TFA messages are sent using PendingIntent.FLAG_CANCEL_CURRENT flag. This is done to avoid approving or disapproving
push notification data which may contain invalid tokens.
```

## Push TFA authentication with fingerprint encrypted session

Push TFA actions are session dependent. Therefore, when your session in encrypted using a fingerprint, you must authenticate the user in order
to complete the notification authentication flow.

The Android TFA library & The Android Biometric libraries are two independent libraries and do not depend on on the other. However, to achieve
the right authentication flow please follow these steps:

```
When using biometric session encryption, your TFA notification will not contain Approve/Deny buttons. Action handing will be done via
notification click using an extension of the PushTFAActivity.class.
```

### 1 Create an Activity extension class for your TFA action handling

You will need to evaluate the session state when your extension activity starts. If the session is encrypted using the "FINGERPRINT" tag. You will
first need to unlock it before displaying the action alert.

Additionally, If the session was previously locked it is recommended to lock it again after approving the push action to avoid irregular behaviours.

Example taken from sample application used to combine the biometric support and the push TFA feature.

```java
class BiometricPushTFAActivity : PushTFAActivity() {
 
    // Helper state for locking back the session if was previously unlocked.
    private var shouldLockSessionOnApproval: Boolean = false
 
    // Referencing the biometric library.
    private val biometric: GigyaBiometric = GigyaBiometric.getInstance()
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
 
        evaluateSessionState()
    }
     
    // Override and return false in order the control when the action alert will show.
    override fun alertOnCreate(): Boolean = false
 
    /**
    * Evaluate the current session encryption state.
    */
    private fun evaluateSessionState() {
        when (_tfaLib.sessionEncryption == "FINGERPRINT") {
            true -> {
                if (!biometric.isAvailable) {
                    GigyaLogger.error("BiometricPushTFAActivity",
                            "Session is FINGERPRINT locked but biometric support is not available")
                    finish()
                    return
                }
                if (!biometric.isLocked) {
                    showActionAlert()
                    return
                }
                biometric.unlock(
                        this,
                        GigyaPromptInfo(
                                getString(R.string.tfa_biometric_locked_session_title),
                                getString(R.string.tfa_biometric_locked_session_subtitle),
                                getString(R.string.tfa_biometric_locked_session_description)
                        ),
                        object : IGigyaBiometricCallback {
                            override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
                                GigyaLogger.debug("BiometricPushTFAActivity", "onBiometricOperationSuccess: Okay to approve push action")
                                shouldLockSessionOnApproval = true
                                showActionAlert()
                            }
 
                            override fun onBiometricOperationFailed(reason: String?) {
                                GigyaLogger.debug("BiometricPushTFAActivity", "onBiometricOperationFailed: - $reason - Available for retry")
 
                            }
 
                            override fun onBiometricOperationCanceled() {
                                GigyaLogger.debug("BiometricPushTFAActivity", "onBiometricOperationFailed: Push action is lost. Will call onDeny")
                                onDeny()
                            }
                        }
                )
            }
            false -> {
                showActionAlert()
            }
        }
    }
 
    /**
    * Overriding the approval action.
    */
    override fun onApprove(extras: Bundle?) {
        super.onApprove(extras)
        if (shouldLockSessionOnApproval) {
            shouldLockSessionOnApproval = false
            if (biometric.isAvailable) {
                biometric.lock(object : IGigyaBiometricOperationCallback {
                    override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
                        GigyaLogger.debug("BiometricPushTFAActivity", "onBiometricOperationSuccess: ")
                    }
 
                    override fun onBiometricOperationFailed(reason: String?) {
                     GigyaLogger.error("BiometricPushTFAActivity ", "onBiometricOperationFailed: Session will remain unlocked")
                    }
                 
                })
            }
        }
    }
 
}
```


### 2 Create a custom extension class for the GigyaFirebaseMessagingService class

We are aware that multiple application are already using the FirebaseMessagingService for their remote messaging services. Because of the
fact that multiple messaging services from the same provider are not applicable, in order to use the Push TFA feature you will need your current
FirebaseMessagingService to extend the GigyaFirebaseMessagingService class.

Using this option will not harm your current remote message handling in any way.

```java
public class MyCustomMessagingService extends GigyaFirebaseMessagingService {

    /*
    * Make sure to call the super function to allow parent logic to remain intact.
    */
    @Override
    public void onNewToken(String newToken) {
    super.onNewToken(newToken);
    }

    /*
    * Make sure to call the super function to allow parent logic to remain intact.
    */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
    super.onMessageReceived(remoteMessage);
    }

}
```

**Don't forget to correctly declare your extension classes in your AndroidManifest.xml.**
Example taken from the provided sample application:

```xml
<activity android:name=".ui.BiometricPushTFAActivity"
    android:excludeFromRecents="true"
    android:launchMode="singleTask"
    android:taskAffinity=""
    android:theme="@style/Theme.AppCompat.Translucent" />

<service android:name="GigyaFirebaseMessagingExt" 
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## Additional Information

[Risk Based Authentication](https://developers.gigya.com/display/GD/Risk+Based+Authentication)

## Known Issues
None

## How to obtain support
Via SAP standard support.
https://developers.gigya.com/display/GD/Opening+A+Support+Incident

## Contributing
Via pull request to this repository.

## To-Do (upcoming changes)
None

## License
Copyright © 2020 SAP SE or an SAP affiliate company. All rights reserved. This file is licensed under the Apache License, v 2.0 except as noted otherwise in the LICENSE file.