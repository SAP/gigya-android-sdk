# SAP CDC (Gigya) Android SDK

## Description
Gigya's Android Authentication SDK library provides a Java interface for applying additional authentication options.

## Requirements
Gigya's Android Core library implementation.

## Limitations
Following released version 2.+ the Auth extension will require your Application to be AndroidX complient.
For more information please visit [Migrate to AndroidX](https://developer.android.com/jetpack/androidx/migrat)

## Integration

Update your gradle.build file with one of two options:

Implementation using a binary file.
Download the auth library and copy it to your applications libs/ folder.
```gradle
implementation files('libs/gigya-android-auth-2.0.0.aar')
```

-Or-

Implementation using **JitPack**
```gradle
implementation 'com.github.SAP:gigya-android-sdk-auth:auth-v2.0.0'
```

**In addition you will need to add this dependency as well.**
```gradle
implementation 'androidx.appcompat:appcompat:1.2.0'
```

## Remote Login Verification

Remote login verification is an authentication flow that uses the Android device as a verification factor when initiating a login request from your
site.

To enable this authentication method, follow the below steps to configure your site.

For more information on configuring your Polices for TFA, see Authentication Types.

Registering your application to receive remote authentication push messages is available using the following implementation steps:

For successfully receiving a push notification in your application you will need to define the GigyaFirebaseMessagingService in your 
**AndroidManifest.xml file.**

if your application already uses FirebaseMessagingService, you will be required to make your service class implementation to extend
the GigyaFirebaseMessagingService. This will not break any of your remote messaging flows in any way. Please make sure to call the
main "super" functions to allow the GigyaFirebaseMessagingService to perform its own logic.
For example:

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

Update your AndroidManifest.xml file as follows:

```xml
<serviceandroid:name="com.gigya.android.sdk.push.GigyaFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

In addition, you are required to register the AuthPushReceiver.java class. This class will intercept verification actions that are visible in the
incoming remote notifications.

```java
/*
Action names are library specific. If you wish, you are able to override the strings ids but we storgly recommend that you use these to avoid errors.
*/
  
<receiver
    android:name="com.gigya.android.sdk.auth.push.AuthPushReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="@string/gig_auth_action_approve" /> /* "com.gigya.android.sdk.auth.push_approve" */
        <action android:name="@string/gig_auth_action_deny" /> /* "com.gigya.android.sdk.auth.push_deny" */
    </intent-filter>
</receiver>
```

In order to correctly initialize the library, call the "registerForPushNotification" library function from your entry point activity.
This function will query your application settings to make sure that notification permission is enabled for this service.
In the case, settings are disabled the SDK will issue a notice dialog.

```java
GigyaAuth.getInstance().registerForPushNotifications(this /* activity context */)
```

## The Verification Flow

In order to begin the verification flow, your device needs to be registered for this service.

Device registration is done by calling the library registerForAuthPush method.

```java
GigyaAuth.getInsance().registerForAuthPush(new GigyaCallback<GigyaApiResponse>() {
	@Override
	public void onSuccess(GigyaApiResponse obj) {
    	// Device successfully registered.
	}
  
	@Override
	public void onError(GigyaError error) {
    	// Error registring device.
	}
});
```
Once your device is successfully registered, in the event of a login attempt from the web, your device will receive a push notification allowing us to
choose if you wish to approve or deny the login attempt. thus providing an additional layer of authentication.


## Available Customization Options

The SDK provides additional customization options for your TFA specific remote messages & the option to use your own customized activity for
remote actions (approve/deny).
Note: Using a customized action activity is mandatory when using fingerprint session encryption. A detailed example is provided further down this
tutorial.

In order to provide customization please use the following method:

```java
GigyaAuth.getInstance().setPushCustomizer(new IGigyaPushCustomizer() {
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

## Known Issues
None

## How to obtain support
Via SAP standard support.
https://developers.gigya.com/display/GD/Opening+A+Support+Incident

## Contributing
Via pull request to this repository.

## To-Do (upcoming changes)
None

