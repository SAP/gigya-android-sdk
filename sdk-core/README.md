# SAP CDC (Gigya) Android Core SDK

## Description
Gigya's Android Core SDK library provides a Java interface for Gigya's API - It allows simplified integration and usage within your Android application
life-cycle.

## Requirements
Android SDK SDK 14 and above.

## Limitations
Following released version 5.+ The Core SDK will require your Application to be AndroidX complient.
For more information please visit [Migrate to AndroidX](https://developer.android.com/jetpack/androidx/migrat)


[Google GSON library](https://github.com/google/gson)
```gradle
implementation 'com.google.code.gson:gson:2.8.6'
```

## Configuration
### Implement using binaries
**Download the latest build and place the .aar file in your */libs* folder**
```gradle
implementation files('libs/gigya-android-sdk-core-v6.2.0.aar')
```

### Implement using Jitpack
**Add the Jitpack reference to your root *build.gradle* file**
```gradle
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
**Add the latest build reference to your app *build.gradle* file**
```gradle
implementation 'com.github.SAP.gigya-android-sdk:sdk-core:core-v6.2.0'
```

**Add a required style to your *styles.xml* file**
```xml
<style name="Theme.AppCompat.Translucent"
parent="Theme.AppCompat.NoActionBar">
<item name="android:windowBackground">@android:color/transparent</item>
<item name="android:colorBackgroundCacheHint">@null</item>
<item name="android:windowIsTranslucent">true</item>
</style>
```

**Update your *AndroidManifest.xml* file** (This is needed If you require the use of Web Screen-Sets).
```xml
<activity
android:name="com.gigya.android.sdk.ui.HostActivity"
android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
android:theme="@style/Theme.AppCompat.Translucent" />
```
```xml
<activity
android:name="com.gigya.android.sdk.ui.WebLoginActivity"
android:allowTaskReparenting="true"
android:launchMode="singleTask"
android:theme="@style/Theme.AppCompat.Translucent">
</activity>
```

**Securing SDK activities with FLAG_SECURE**
If required by the host application, in order to add *FLAG_SECURE* to all SDK specific activities,
you are able to add the following flag. This will not allow the device to take screenshots of the relevant
activities.
```java
/*
Default is set to false.
*/
Gigya.secureActivityWindow(true);
```

## Initialization
**In order to initialize the SDK please add the following lines in your Application extension class**
```java
// Attaching the application context reference.
Gigya.setApplication(this);
```

### Implicit initialization
The SDK will implicitly initialize itself according to one of the following:

**Using a JSON file**
You can add a JSON file named "gigyaSdkConfiguration.json" to your application assets folder.
This will allow the SDK to parse the required configuration fields (ApiKey, ApiDomain, etc) implicitly.
```json
"apiKey":"YOUR-API-KEY-HERE",
"apiDomain": "YOUR-API-DOMAIN-HERE",
"accountCacheTime": 1,
"sessionVerificationInterval": 60
}
```

**DEPRECATED - Please do not use meta-data initialization**
**Option will be removed in later version**
**Using meta-data tags in your *AndroidManifest.xml* file**
```xml
<meta-data
android:name="apiKey"
android:value="YOUR-API-KEY-HERE" />

<meta-data
android:name="apiDomain"
android:value="YOUR-API-DOMAIN-HERE"/>

<meta-data
android:name="accountCacheTime"
android:value="1"/>
 
<meta-data
android:name="sessionVerificationInterval"
android:value="60" />
```

 
### Explicit initialization
As an alternative to implicit initialization, you can initialize the SDK explicitly:

```java
/*
Using default domain (us1-gigya.com)
*/
Gigya.getInstance(MyAccount.class).init("YOUR-API-KEY-HERE");
```
```java
/*
Supplying Api-Key & Api-Domain
*/
Gigya.getInstance(MyAccount.class).init("YOUR-API-KEY-HERE", "YOUR-API-DOMAIN-HERE");
```

### CNAME initialization

When using implicit initialization of the SDK, add the “cname” property to your gigyaSdkConfigurations.json file:
```json
"cname": "YOUR_CNAME_HERE",
```
When using explicit initialization of the SDK, you can use the following method:
```java
public void init(@NonNull String apiKey, @NonNull String apiDomain, @NonNull String cname)
```

## Sending a Request
You can send anonymous requests to Gigya using the SDK using one of two overloads:
General - this will return an instance of GigyaApiResponse class (see the section below on how to access its data).
Typed - this will return an instance of the provided class.

The following example sends an "accounts.verifyLogin" request using the current logged in user's UID field to verify that the current session is still
valid.

```java
/*
Setup a map of parameters.
*/
final Map<String,Object> params = new HashMap<>();
params.put("UID", "YOUR-ACCOUNT-UID");
```
```java
/*
Sending "verifyLogin" REST api.
*/
final String API = "accounts.verifyLogin";
```
```java
/*
Send a POST request. Will receive a general purpose
GigyaApiResponse.class object in the success block.
*/
mGigya.send(API, params, new GigyaCallback<GigyaApiResponse>() {
    @Override
    public void onSuccess(GigyaApiResponse obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
```java
/*
Send a typed POST request. Will receive parsed MyAccount object in the
success block.
RestAdapter.GET is also available depending on the api in question.
*/
mGigya.send(API, params, RestAdapter.POST, MyAccount.class, new
GigyaCallback<MyAccount>() {
@Override
    public void onSuccess(GigyaAccount obj) {
    // Success
    }
    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
You can find the list of available Gigya API endpoints and their required parameters in the REST API Reference.


## The GigyaApiResponse Class

The SDK provides a custom response class for encapsulating Gigya API's responses.
This class exposes multiple methods that can help simplify your flow.

Here are a few examples of a given response:

```java
private void evaluateGigyaResponse(GigyaApiResponse response) {
```
```java
// Get JSON formatted response String.
final String JSON = response.asJson();
```
```java
// Get response error code.
final int errorCode = response.getErrorCode();
```
```java
// Get the account profile (optional: if response was from an account related API)
final Profile gigyaProfile = response.getField("profile",Profile.class);
final String firstName = response.getField("profile.firstName",String.class);
```
```java
// Parse to the response data to a MyAccount class instance (optional: if response was from an account related API)
final MyAccount myAccount = response.parseTo(MyAccount.class);
}
```
## Login & Registration

Site login & registration via API calls (to differ from social login & registration) is available using the login/register methods.
These two actions will eventually end with calling the GigyaLoginCallback, so we'll start with it:

## Site Login & Registration

Here are a few examples for login/register usage:

### Login via loginID & password:

```java
mGigya.login("LOGIN-ID", "PASSWORD", new GigyaLoginCallback<MyAccount>()
{
@Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
### Register via email & password:

```java
/*
Defining custom parameters for the request. In this case setting the
session expiration to 10 minutes.
*/
final Map<String, Object> params = new HashMap<>();
params.put("sessionExpiration", 10);
```
```java
mGigya.register("email", "password", params, new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
## Social Login

Logging-in using a social network is one of the key features of the Gigya Android SDK.
The following social providers currently support the login operation:

```
Amazon
Apple
Blogger
Facebook
FourSquare
GooglePlus
Kakao
LINE
LinkedIn
Livedoor
Messenger
mixi
Naver
Netlog
Odnoklassniki
Orange France
PayPalOAuth
Tencent QQ
Renren
Sina Weibo
Spiceworks
Twitter
VKontakte
WeChat
WordPress
Xing
Yahoo
Yahoo Japan
```
## Provider Selection Screen
You can show a dialog with defined social providers in the following way:

All supported providers constants are available using GigyaDefinitions.Providers class.
The following providers support native login using their own SDKs:
```
Facebook
Google
Line
WeChat
```
Please make sure to follow each configuration implementation mentioned in the Configuring Native Login section.
While native support will require you to add the provider's library dependency to your application's build.gradle file, for Google Sign-In
adding the library is not mandatory. In this case the login process will be initiated via Chrome using Intent.ACTION_VIEW.

```java
/*
Define the list of providers your application supports.
*/
final List<String> providers = new ArrayList<>();
providers.add(FACEBOOK);
providers.add(GOOGLE);
providers.add(LINE);
```
```java
/*
Show providers selection UI
*/
mGigya.socialLoginWith(providers, null, new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }
    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
Here is a screenshot using the above implementation:


### Login With A Specified Provider
Alternatively, you can initiate social login flow to a specific social provider:

```java
/*
Sign in with Facebook.
*/
mGigya.login(FACEBOOK, new HashMap<>(), new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
    });
```

## Configuring Native Login
For some social providers, the SDK supports social login via the social provider's native implementation.
It is done by using the provider's native SDK, so it will require you to add its required libraries as dependencies to your Android project.

We will review the relevant providers and their implementation flow.

### Facebook
Adding Facebook native login SDK to your Android app is mandatory if you want to login via Facebook.
```
On October 5, 2021, Facebook Login will no longer support Android embedded browsers (WebViews) for authenticating users.
```
Android SDK currently supports up to Facebook Android SDK v12.3.0.

Android SDK currently supports up to Facebook Android SDK v14.1.1.

To set up your Facebook app in your
Android Studio project using the following instructions:

Setting up the Facebook dependency:

Add the following line to your application's build.gradle file.

```gradle
implementation 'com.facebook.android:facebook-android-sdk:14.1.1'
```
Add the following lines to your *AndroidManifest.xml* file.
It is recommended that the **facebook_app_id** String be placed in the your *res/values/strings.xml* file.

```
If you do not yet have an active Facebook app please see our Facebook documentation.
For mobile specific. please go to Facebook Mobile App Setup.
```

```xml
<activity
android:name="com.facebook.FacebookActivity"
android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
android:label="@string/app_name"
android:theme="@android:style/Theme.Translucent.NoTitleBar"
tools:replace="android:theme" />
```
```xml
<meta-data
android:name="com.facebook.sdk.ApplicationId"
android:value="@string/facebook_app_id" />

<meta-data
android:name="com.facebook.sdk.ClientToken"
android:value="@string/facebook_client_token" />
```

### Google
Android enables users to set their Google account on their device.
Using Google Sign-In is mandatory if you want users to login via Google.

Instructions for adding Google Sign-in to your Android device can be found at Google Sign-In for Android.

Add the following line to your application's build.gradle file.
```radle
implementation 'com.google.android.gms:play-services-auth:16.0.1'
```
Add the following meta-data tag to your AndroidManifest.xml file. It is recommended that the google_client_id String be placed in the your re
s/values/strings.xml file.

```xml
<meta-data
android:name="googleClientId"
android:value="@string/google_client_id" />
```

```
Important!
From version 4.2.+ it is now mandatory to implement the Google auth library if using Google sign in in your app.
```
```
Note that the required client_id is the Web client id generated when you create your Google project. This should not be mistaken with
the Android OAuth client id. Make sure that your Google project contains both.
```

### LINE

The Gigya Android SDK allows you to enable LINE native login for users that have the LINE app installed.
Instructions for adding LINE Native Login to your Android device can be found at Integrating LINE Login with an Android app.
Make sure you have the following in your application's build.gradle file.

```gradle
implementation 'com.linecorp:linesdk:5.0.1'
```
In order to support LINE v5 please make sure you enable Java 1.8 support in your app's build.gradle file.

```gradle
compileOptions {
sourceCompatibility JavaVersion.VERSION_1_8
targetCompatibility JavaVersion.VERSION_1_8
}
```
You must generate a package signature to place in your LINE Channels > Technical configuration > Android Package Signature field.
The signature you need is the SHA1 signature, you can generate it according to the following:

```
// Code to generate Package Signature
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias
androiddebugkey -storepass android -keypass android
```
```
//Returns -> SHA1: DD:D3:1B:4.....xxxxx.....
//Convert To -> DDD31B4xxxxxxx.............xxxxxx
```
Once you have removed the colons (":") from the signature, paste it to the respective field in LINE's Developers Console.

Add the following to your AndroidManifest.xml. It is recommended that the line_Channel_ID String be placed in the your res/values/strings.x
ml file

```xml
<meta-data
android:name="lineChannelID"
android:value="@string/line_Channel_ID" />
```
```
The GIgya SDK currently supports LINE SDK v5.1 for Android.
Note that support for LINE v4.x has been discontinued.
It is important that you remove any instance of the previous Line v4 files from your /lib folder (if any).
```

### WeChat
The Gigya Android SDK allows you to enable WeChat native login for users that have the WeChat app installed.
Add the following the dependency to your application's build.gradle file.

```
The current tested WeChat api version is 5.3.1.
```
```gradle
implementation 'com.tencent.mm.opensdk:wechat-sdk-android-without-mta:5.3.1'
```
The the following to your *AndroidManifest.xml* It is recommended that the wechatAppID String be placed
in the your *res/values/strings.xml file*.

```xml
<activity
android:name=".wxapi.WXEntryActivity"
android:exported="true"
android:label="@string/app_name"
android:launchMode="singleTop" />
```
```xml
<meta-data
android:name="wechatAppID"
android:value="@string/wechatAppID" />
```
Create a sub folder in the root of your project called wxapi. In that folder, create an activity named WXEntryActivity which must contain the
following:

```java
public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    WeChatProvider.handleIntent(this, getIntent(), this);
    finish(); 
    }   

@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    WeChatProvider.handleIntent(this, getIntent(), this);
    finish();
    }

@Override
public void onReq(BaseReq baseReq) {
    // Stub. Currently unused.
    }

@Override
public void onResp(BaseResp baseResp) {
    WeChatProvider.onResponse(baseResp);
    }

@Override
public void finish() {
    super.finish();
    // Disable exit animation.
    overridePendingTransition(0, 0);
    }
}
```
Once you've tested your app, run the signature generation tool available from WeChat at https://open.weixin.qq.com/cgi-bin/showdocument?actio
n=dir_list&t=resource/res_list&verify=1&id=open1419319167&token=&lang=en_US.

Use the tool to generate the app signature and update your setting in the WeChat console.

## Logout
A simple logout is available by using:

```
Important Notes
The signature generation tool must be installed on the mobile device.
You will not be able to test WeChat functionality using an emulator. WeChat requires a physical mobile device.
Once you update your app signature in the WeChat console, it could take a couple of hours to update.
If you experience problems and notice errCode -6 from WeChat while debugging, it means the signature isn't correct.
```

```java
mGigya.logout();
```
Logging out will clear all session data from the device.

## Account Handling
The SDK provides various account handling interfaces to simplify fetching and updating the user's data.

### Setting account configuration
In order to align all account related request we recommend that an account configuration setting will be applied.
This is a global SDK setting and will affect all account related requests.

Account configuration will include:
 - cacheTime - The time the SDK will cache your account data until requested again to lower network usage.
 - include - The default include fields used in every account request.
 - extraProfileFieds - The default extra profile fields used in every account request.

In order to set the account configuration use one of the following methods:
#### Implicit settings via the *gigyaSdkConfiguration.json* file
```json
{
  "apiKey": "API-KEY-HERE",
  "apiDomain": "API-DOMAIN-HERE",
  "accountCacheTime": 1,
  "account": {
    "cacheTime": 1,
    "include": [
      "data",
      "profile",
      "emails"
    ],
    "extraProfileFields": [
      "languages",
      "phones"
    ]
  },
  "sessionVerificationInterval": 0
}
```

#### Explicit setting via the Gigya instance.
```java
Gigya.getInstance().setAccountConfig(myAccountConfigObject)
```
We recommend setting this right after you initialize the SDK.

**
NOTE:
The previous ""accountCacheTime" setting in the *gigyaSdkConfiguration.json* file will now be deprecated and is
scheduled to be removed in later versions.
**

### Providing A Custom Account Schema
In your Gigya implementation you have probably extended the default Account Schema according to your business requirements:
Gigya's Android SDK allows you to get a smooth developing experience by binding the SDK's main Gigya instance to a class of the same
structure as your schema.
This will allow the SDK to accept and return account instances according to your specification.
Here is an example of a custom Account Schema class, which corresponds with the above site's Schema.

```java
public class MyAccount extends GigyaAccount {

private MyData data;

public MyData getData() {
    return data;
    }
}

private static class MyData {

    private String comment;
    private Boolean subscribe;
    private Boolean terms;

    public String getComment() {
        return comment;
    }

    public Boolean getSubscribe() {
        return subscribe;
    }

    public Boolean getTerms() {
        return terms;
    }
}
```
We can initialize a Gigya instance with the MyAccount class, and see the account methods operate accordingly.

### Get Account
In order to retrieve the current account you can use the getAccount method:

```java
mGigya.getAccount(new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount account) {
    // Success
    System.out.println(account.terms);
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
In order to improve the end-user's experience by avoiding unnecessary network requests, the SDK caches the current account data for a period
of 5 minutes (by default).

The account cache property can be set via the JSON configuration file or by adding a meta-data tag as show in the initialization section of the
document.

To bypass the account's caching:
Provide true when requesting a new account.

```java
mGigya.getAccount(true, new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
### Set Account

The SDK provides two options for updating a user account data.
**Using getAccount requires you to have a valid session.**

**Provide true when requesting a new account.**
 
Case 1, Update the account using the setAccount method providing an updated account object.

Manually setting the current session.

**The SDK provides an interface for manually managing the current session state using the following command:**

```java
mGigya.setSession(currentSession /*A manually provided instance of the SessionInfo.java class */);
```

This option is useful when your client application is handling the session state on its own. This method will overwrite the current session state
using the currently used encryption type.

```java
/*
Using live data to keep track of account object changes.
*/
mGigya.setAccount(myAccountLiveData.getValue(), new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
Case 2, Update the account using the setAccount method. Providing the update parameters requested.

**Using setAccount requires you to have a valid session.**

In order to avoid unnecessary errors, please make sure that the fields you trying to update are marked as userModify in the site's
schema.
You can verify this using Gigya's Admin Console, in your site's Schema Editor page under the Settings panel.

**As stated in the setAccountInfo REST preferences, complex objects should be serialized into JSON format.**

```java
/*
Creating a map of requested update fields
*/
final Map<String, Object> profile = new HashMap<>();
profile.put("age", 25);
profile.put("firstName", "John");

/*
Adding profile object to updated parameters. Using GSON to format the
data before sending.
*/
final Map<String, Object> params = new HashMap<>();
params.put("profile", new Gson().toJson(profile));

mGigya.setAccount(params, new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```

## Using Screen-Sets

Screen-Sets, as one of Gigya's most powerful features, are available also in your mobile app!
The SDK provides a simple interface for using & displaying screen-sets via the PluginFragment and the GigyaPluginCallback components.

### ShowScreenSets method

Using screen-sets is available using the "showScreenSet" method of the Gigya interface.

**Here is an example of using the SDK's showScreenSet method using the default "Registration-Login" screen set.**

```java
/*
Showing "Registration-Login" screen set in a dialog mode. Overriding
only the onLogin method to be notified when logging in event was fired.
*/
mGigya.showScreenSets("Default-RegistrationLogin", false, null, new GigyaPluginCallback<MyAccount>() {
    @Override
    public void onLogin(@NonNull MyAccount accountObj) {
    // Login success.
    }
});
```

The showScreenSets method available parameters include:
All parameters that the web screen-sets plugin can receive.
Optional Boolean fullScreen field which will force the displaying of the PluginFragment to fit into the screen.
Customizing the look & feel of the PluginFragment is recommended.
It can be done by simply copying the gigya_plugin_fragment.xml file from the SDKs source code to your application res/layout folder
directory.
Once copied you will be able to change & customize the layout to your choosing (with some restrictions of course).

```
Keep in mind that you cannot remove any element or change any existing element id. Doing so could result in unexpected crashes, as
the SDK will still expect these elements to be presented.
```


## GigyaPluginCallback Class

This callback class is an abstract class which is aligned to all optional plugin events fired by the screen-sets plugin. In addition, convenience
methods, such as onLogin and onLogout, are also available for override.

**Here is the callback to its extent. Overriding all methods is optional.**

```java
final GigyaPluginCallback<MyAccount> pluginCallback = new GigyaPluginCallback<MyAccount>() {
    
    @Override
    public void onError(GigyaPluginEvent event) {
        super.onError(event);
    } 

    @Override
    public void onBeforeValidation(@NonNull GigyaPluginEvent event) {
        super.onBeforeValidation(event);
    }
   
     super.onBeforeSubmit(event);
        super.onBeforeSubmit(event);
    }

    @Override
    public void onSubmit(@NonNull GigyaPluginEvent event) {
        super.onSubmit(event);
    }

    @Override
    public void onAfterSubmit(@NonNull GigyaPluginEvent event) {
        super.onAfterSubmit(event);
    }

    @Override
    public void onBeforeScreenLoad(@NonNull GigyaPluginEvent event) {
        super.onBeforeScreenLoad(event);
    }

    @Override
    public void onAfterScreenLoad(@NonNull GigyaPluginEvent event) {
        super.onAfterScreenLoad(event);
    }

    @Override
    public void onFieldChanged(@NonNull GigyaPluginEvent event) {
        super.onFieldChanged(event);
    }   

    @Override
    public void onHide(@NonNull GigyaPluginEvent event, String reason) {
        super.onHide(event, reason);
    }

    @Override
    public void onLogin(@NonNull MyAccount accountObj) {
        super.onLogin(accountObj);
    }

    @Override
    public void onLogout() {
        super.onLogout();
    }

    @Override
    public void onConnectionAdded() {
        super.onConnectionAdded();
    }

    @Override
    public void onConnectionRemoved() {         
        super.onConnectionRemoved();
    }
};
```

## Business APIs

The Gigya SDK provides popular built-in flows for fluent development.

Currently available:
```
login
register
logout
verifyLogin
getAccount
setAccount
forgotPassword
addConnection
removeConnection
```

### Interruptions

Some flows can be "interrupted" due to certain Site policies.
For example, when trying to register but Two Factor Authentication is required - then an "interruption" can occur about "pending TFA registration"
that will require the end user to setup a TFA method before being able to complete the registration flow.

Interruptions map:
```
The plugin callback is also typed to the current Account schema.
```
```
Business APIs are provided in order to give you an easier interface. If a more detailed and customized use is required, you can still use
the generic Gigya.send interface for all request purposes.
```
The SDK's Business APIs are design to help to easily develop a friendly way to face and resolve those interruptions in order to get the end user
logged in and still complying to the site's policies.

### Handling Interruptions

Interruption handling is a key feature introduced as of the Android SDK v4.

The SDK will expose a resolver object for supported interruptions in order to give you as a developer the ability to resolve them within the same
flow that they were triggered.

The current supported interruption flows are:
```
Account linking
TFA registration
TFA verification
```
#### Interruptions handling - Account linking example

We will start with a simple register request for an email address that is already registered:

**All interruption flows are implemented in the provided Sample project.**

```java
mGigya.register("EMAIL-ADDRESS-ALREADY-REGISTERED", "PASSWORD", new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
As expected we will receive an error which indicates that this login identifier already exists in the system (errorCode 403043 ).

Usually when receiving that kind of error, we would trigger an API call to retrieve the conflicting accounts (via accounts.getConflictingAccount ),
then try to login with one of the supported account's identities (using mode:"link" ).

Luckily, the SDK can handle this interruption for us:

To do so, in our our GigyaLoginCallback we will override the onConflictingAccounts method:

```java
mGigya.register("EMAIL-ID-ALREADY-REGISTERED", "PASSWORD", new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }

    @Override
    public void onConflictingAccounts(@NonNull GigyaApiResponse response,
    @NonNull GigyaLinkAccountsResolver resolver) {

    }
});
```

While the response parameter contains the original response from the register API call (accounts.register), the resolver object
 (of type GigyaLinkAccountsResolver) already contains all we need in order to complete the flow:

We can get the conflicting accounts from it and try to link the account to them.

```java
final ConflictingAccounts accounts = resolver.getConflictingAccounts();
final List<String> providers = accounts.getLoginProviders();
final String loginID = accounts.getLoginID();

/*
In this example the providers list contains one "site" provider. Therefore
we are now able to try and resolve the flow.
*/
resolver.linkToSite(loginID,
"PASSWORD-REQUIRED-TO-VERIFY-THE-ORIGINAL-ACCOUNT");
```

Trying the resolve the flow will now try to login with the original conflicted account and link both accounts.
If the operation was successful, the original GigyaLoginCallback will be notified and the flow will be directed to its original onSuccess method.

## Session Features

### Handling Fixed Session Expiration

Starting a new session via register or login is also available with a fixed time span expiration constraint.

For example:
In order to provide the end user with a fluid experience some UI intervention is recommended. Example for this can be found in the
Sample application.

```java
/*
Adding a 10 minutes session expiration constraint.
*/
final Map<String, Object> params = new HashMap<>();
params.put("sessionExpiration", 10);

mGigya.register("EMAIL", "PASSWORD", params, new GigyaLoginCallback<MyAccount>() {
    @Override
    public void onSuccess(MyAccount obj) {
    // Success
    }

    @Override
    public void onError(GigyaError error) {
    // Fail
    }
});
```
When the session expires, the SDK will notify about it via broadcast.
In order to be notified of session changes, you will need to register a broadcast receiver in your Activity, for example:

```java
final BroadcastReceiver sessionLifecycleReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }
        String message;
        switch (action) {
            case GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED:
                message = "Your session has expired";
                break;
            case GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID:
                message = "Your session is invalid";
                break;
        }
    }
};

@Override
protected void onResume() {
    super.onResume();
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED);
    intentFilter.addAction(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_INVALID);
    LocalBroadcastManager.getInstance(this).registerReceiver(sessionLifecycleReceiver, filter);
}

@Override
protected void onPause() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionLifecycleReceiver);
    super.onPause();
}
```

For session expiration updates use INTENT_ACTION_SESSION_EXPIRED action.

### Verify Login Interval - Session validation

The Android SDK can track a user's current session and determine if there were changes to the API schema and require re-authentication for him
when necessary.

For example, this can be used to invalidate a user's session if the version of their agreed terms of use consent has changed.

When using session verification, the client application will be informed, via local broadcast, if the automatic verification fails. This will allow your
application to perform the necessary logic in order to re-authenticate the user.

Updating the *AndroidManifest.xml* file:

```xml
<meta-data
android:name="sessionVerificationInterval"
android:value=60 /> // The verification call interval in seconds.
```
Adding this meta-data tag will cause the SDK to perform a periodic session validation according to provided value (in seconds).
When a session changes state, the verification call will fail and the SDK will invalidate the current session and broadcast an event locally.

## SessionStateObservers

For both session expiration & session verification services our previous versions of the Android core SDK have taken use of the [LocalBroadcastManager](https://developer.android.com/reference/androidx/localbroadcastmanager/content/LocalBroadcastManager) application wide event bus to notify when the session has been invalidated.

Due to Google’s deprecation we have migrated the notification flow accordingly.

You can now register two separate observers/listeners to observe session state changes using the *“SessionStateObserver”* class.

```
private val sessionExpirationObserver = SessionStateObserver {
        
  runOnUiThread {
            checkLoginState()
            // Display error to user.
            Snackbar.make(
                window.decorView.rootView,
                "Session expired!",
                Snackbar.LENGTH_LONG
            ).show()
            result.text = ""
        }
    }
```

Please implement the following in your view controller (Activity/Fragment) instance to register  & unregister the observer classes.

```
override fun onStart() {
        super.onStart()
        // Register session state observers.
  Gigya.getInstance().registerSessionVerificationObserver(sessionVerificationObserver)
  Gigya.getInstance().registerSessionExpirationObserver(sessionExpirationObserver)
   
}

override fun onStop() {
        // Unregister session state observers.
  Gigya.getInstance().unregisterSessionVerificationObserver(sessionVerificationObserver)
  Gigya.getInstance().unregisterSessionExpirationObserver(sessionExpirationObserver)
  super.onStop()
  
}
```

Please make sure you don’t use the same *“SessionStateObserver”* instance for both session expiration traking & session verifcation tracking..

**NOTE:**
In order to avoid immediate breaking changes, localbroadcasts will still be sent along with notifying the new observers.
Make sure to remove localbroadcast receiver code when you implement the use with the *“SessionStateObserver”* classes.
LocalBroadcastManager usage will be completly removed in future versions. Please take the time to migrate.


## Using the GigyaWebBridge explicitly.

You are able to use the GigyaWebBridge.java class explicitly in order to attach Gigya's web sdk actions into your own WebView implementation.
Attaching the GigyaWebBridge will allow you to add Gigya's session management you your custom web implementation. Special cases include
uses of SAML & captcha implementations. The following snippet demonstrates the basic implementation of the GigyaWebBridge.

**Session validation calls will only occur while the application is in the foreground.**

```kotlin
private var _webBridge: IGigyaWebBridge<MyAccount>? = null

/*
Make sure you enable javascript for your WebView instance.
*/
val webSettings = web_view.settings
webSettings.javaScriptEnabled = true

/*
Generate a new GigyaWebBridge instance.
*/
_webBridge = Gigya.getInstance(MyAccount::class.java).createWebBridge()

/*
Attach newly create GigyaWebBridge to WebView instance.
*/
_webBridge?.attachTo(web_view, object: GigyaPluginCallback<MyAccount>() {

    // Implement any optional callback you require.```
    override fun onLogin(accountObj: MyAccount) {
    // Logged in.
    }
}, progress_indicator /* Optional progress indicator view to be displayed on loading events */)

/*
Make sure to attach the GigyaWebBridge to your WebViewClient instance.
*/
web_view.webViewClient = (object: WebViewClient() {
override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
    val uri = request?.url
    val uriString = uri.toString()
    return _webBridge?.invoke(uriString) ?: false
    }
})
```

It is recommended that you also detach the GigyaWebBridge from your WebView instance.
Make sure you implement it in the appropriate lifecycle callback.

```kotlin
_webBridge?.detachFrom(web_view)
```

## SSO (Single Sign-on)
Single Sign-On (SSO) is an authentication method that allows a user to log in to multiple applications that reside within the same site group with a single login credential.

When using the mobile SSO feature, applications within the same group are able to share a valid session with the device browser.
Supported flows:
* Login via SSO feature on mobile. The browser can share the session using the SSO method.
* Login via SSO feature on application 1. Application 2 can share the session when using the SSO method.
* Login via SSO method on the browser. Applications within the same group can share the session when using the SSO method.

You will be required to setup you central login page on your site’s console.

To set up mobile SSO please follow these steps:

Add Custom-Tabs implementation to your application level build.gradle file:
```
implementation 'androidx.browser:browser:1.3.0'
```

Add The following to your application’s AndroidManifest.xml file:
```
 <activity android:name="com.gigya.android.sdk.providers.sso.GigyaSSOLoginActivity"
            android:exported="true"
            android:launchMode="singleTask">

            <intent-filter>

                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <data
                    android:host="${applicationId}"
                    android:path="/login/"
                    android:scheme="gsapi" />

          </intent-filter>
  </activity>
```

**The default redirect needed consists of the following structure:**
“gsapi://{applicationId}/login/ whereas the “applicationId” represent the applicaiton unique bundle identifier.

Please make sure you add your unique redirect URL to the **Trusted Site URLs** section of your parent site.

Finally, to initiate the flow, use the SSO function provided by the Gigya shared interface

```
gigya.sso(mutableMapOf(), object : GigyaLoginCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                //...
            }

            override fun onOperationCanceled() {
                //...
            }

            override fun onError(error: GigyaError?) {
               //...
            }

        })    
```

## FIDO/WebAuthn Authentication
FIDO is a passwordless authentication method that enables password-only logins to be replaced with secure and fast login experiences across multiple websites and apps.
Our Android SDK provides an interface to register a passkey, login, and revoke passkeys from the site or app created using Fido/Passkeys, backed by our WebAuthn service.

### SDK limitations:
Only one passkey is supported at a time. Once registering a new key, the client's previous key will be automatically revoked.

### SDK prerequisites:
Android - minimum SDK version: 23

To use Fido authentication on mobile, make sure you have correctly set up your **Fido Configuration** section under the **Identity -> Security -> Authentication** tab of your SAP Customer Data Cloud console.

### Android setup:
The Google Fido API is required. Add the following to your application's build.gradle file.
```
implementation 'com.google.android.gms:play-services-fido:18.1.0'
```

**Interoperability with your website**
To leverage Google’s FIDO API you are required to seamlessly share credentials across your website and Android application.
Follow [Google guidelines](https://developers.google.com/identity/fido/android/native-apps#interoperability_with_your_website) to add your assetlinks.json file to your RP domain host.

**Android Key Hash verification:**
Android Key Hash is the SHA256 fingerprints of your app’s signing certificate.
Google’s Fido API requires your application origin to be verified with the WebAuthn service. To do so you will need to fetch your Android Key Hash and add it to the FIDO console configuration.
There are several ways you can obtain your key. Either use your gradle “signingReport” task or use this code snippet:
```
try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA256")
                md.update(signature.toByteArray())
                Log.e("MY KEY HASH:", Base64.encodeToString(md.digest(), 
                 Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }
```

### Android Implementation.
The Fido interface contains 3 methods:

**Registration:**
Registering a new passkey can be performed only when a valid session is available.
```
Gigya.getInstance().WebAuthn()
            .register(resultHandler, object : GigyaCallback<GigyaApiResponse>() {

                override fun onSuccess(p0: GigyaApiResponse?) {
                    // Success.
                    Log.d("FIDO", "register success")
                }

                override fun onError(p0: GigyaError?) {
                    visibleProgress(false)
                    // Handle error here.
                    p0?.let {
                        Log.d("FIDO", "register error with:\n" + it.data)
                    }
                }

            })
```

**Login:**
Logging in using a valid passkey.
```
Gigya.getInstance().WebAuthn()
            .login(resultHandler, object : GigyaLoginCallback<GigyaAccount>() {

                override fun onSuccess(p0: GigyaAccount) {
                    // Success.
                    Log.d("FIDO", "login success")
                }

                override fun onError(p0: GigyaError?) {
                    // Handle error here.
                    p0?.let { error ->
                        Log.d("FIDO", "login error with:\n" + error.data)
                        
                    }
                }

            })
```


**Revoke:**
Revoking the current passkey. Logging in will not be available until registering a new one.
```
Gigya.getInstance().WebAuthn()
            .revoke(object : GigyaCallback<GigyaApiResponse>() {

                override fun onSuccess(p0: GigyaApiResponse?) {
                    // Success.
                    Log.d(“FIDO", "revoke success")
                }

                override fun onError(p0: GigyaError?) {
                    // Handle error here.
                    p0?.let {
                        Log.d(“FIDO", "revoke error with:\n" + it.data)
                    }
                }

            })
```


**Note**:
You are able to define you own custom redirect schema.
To do so:
1. update the “intent filter” data segmennt to you desired schema.
2. Add the required redirect-uri String (in the specified above strucutre) to the sso request parameters using “sso-redirect” key mapping.

## Error reporting

The SDK contains an error reporting service that tracks critical SDK specific errors and reports them
back to us.

The service is disabled by default and can be activated using:
```kotlin
Gigya.setErrorReporting(true)
```


## Caveats

### Android WebView

Be advised that the Gigya Android SDK asynchronous nature uses timers to achieve different functionality for a smooth user experience,
specifically for Web plugins such as Screen-Sets. This means that if your application is using WebViews you should avoid calling pauseTimers(),
as this will cause erratic and inconsistent behaviour of the SDKs functions. If you ever do need to call pauseTimers(), be sure to call resumeTim
ers() prior to any further interaction of the Gigya SDK.

Note that calling pauseTimers()/resumeTimers() is a global request and is not restricted to any specific instance. For more information see, http
s://developer.android.com/reference/android/webkit/WebView#pauseTimers().

## Known Issues
None

## How to obtain support
Via SAP standard support.
https://developers.gigya.com/display/GD/Opening+A+Support+Incident

## Contributing
Via pull request to this repository.

## To-Do (upcoming changes)
None

