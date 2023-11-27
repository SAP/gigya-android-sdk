# SAP CDC (Gigya) Android Core SDK

## Description
Gigya's Android Core SDK library provides a Java interface for Gigya's API - It allows simplified integration and usage within your Android application
life-cycle.

## Requirements
Android SDK SDK 23 and above.

## Upgrading to v7
To align with current security standards following the Android core SDK [v6.0.0](https://github.com/SAP/gigya-android-sdk/releases/tag/core-v6.0.0) release, v7 removes all redundant code related to old cryptography logic targeted for devices lower than Android 23.
This change is crucial to avoid future flagging of the SDK by Google Play.
As a result, session retention is only available when upgrading from v6 to v7.
Users running applications that are using v5 and below of the Android core SDK will need to re-authenticate their session.

[Google GSON library](https://github.com/google/gson)
```gradle
implementation 'com.google.code.gson:gson:2.8.9'
```

## Configuration
### Implement using binaries
**Download the latest build and place the .aar file in your */libs* folder**
```gradle
implementation files('libs/gigya-android-sdk-core-v7.0.5.aar')
```

### Implement using **MavenCentral**
**Add the latest build reference to your app *build.gradle* file**
```gradle
implementation 'com.sap.oss.gigya-android-sdk:sdk-core:7.0.6'
```

### Implement using Jitpack (Will be soon deprecated - moving to Maven Central)
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
implementation 'com.github.SAP:gigya-android-sdk:core-v7.0.6'
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

### OKHttp Support

The core SDK now supports using the OKHttp library to make network requests (which deprecates the use of the obsolete HttpUrlConnection & Volley).
To use the OKHttp library, please add the following to your application dependencies:
```
implementation "com.squareup.okhttp3:okhttp:4.10.0"
implementation "com.squareup.okhttp3:logging-interceptor:4.10.0"
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

**Note:**
**This method is now marked for deprecation.**
If your application creates a social provider selection dialog, we recommend that you implement your own UI and selection trigger
and use the direct "login with Social provider" option (https://sap.github.io/gigya-android-sdk/sdk-core/#login-with-a-specified-provider)."


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

**Moving to external social provider implementation:**

To match our [Swift SDK](https://github.com/SAP/gigya-swift-sdk), for Android core SDK v7 have moved social provider implementation out of the SDK’s scope.
This change has been done to avoid upgrading the SDK once a specific provider’s SDK requires an update.

The code for specific to the relevant providers (Google, Facebook, Line, WeChat) will be removed from the core SDK and placed in special “ProviderWrapper” classes.
All provider classes should be stored in your application source root under the ***“gigya.providers”*** package.
In addition, implementation of the provider’s library should be added to the application *gradle* build file.

### Facebook

Add the following line to your application’s build.gradle file:
```
implementation 'com.facebook.android:facebook-android-sdk:14.1.1'
```

Add the provided “FacebookProviderWrapper” class provided in your ../gigya/providers package.

Add the following lines to your AndroidManifest.xml file using String references to your String resource file:

```
<activity
  android:name="com.facebook.FacebookActivity"
  android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
  android:label="@string/app_name"
  android:theme="@android:style/Theme.Translucent.NoTitleBar"
  tools:replace="android:theme" />

<meta-data
  android:name="com.facebook.sdk.ApplicationId"
  android:value="@string/facebook_app_id" />

<meta-data
  android:name="com.facebook.sdk.ClientToken"
  android:value="@string/facebook_client_token" />
```

**Note:** 
The FacebookProviderWrapper class is set to reference the “R.string.facebook_app_id" string reference.
If you plan to bundle the wrapper within library use "context.getResources().getIdentifier("facebook_app_id", "string", context.getPackageName()" instead.

### Google

Add the following line to your application’s build.gradle file:
```
implementation 'com.google.android.gms:play-services-auth:16.0.1'
```

Add the “GoogleProviderWrapper” class provided in your ../gigya/providers package.

Add the following lines to your AndroidManifest.xml file using String references to your String resource file:
```
<meta-data
android:name="googleClientId"
android:value="@string/google_client_id" />
```

**Note:** 
The GoogleProviderWrapper class is set to reference the “R.string.google_client_id" string reference.
If you plan to bundle the wrapper within library use "context.getResources().getIdentifier("google_client_id", "string", context.getPackageName()" instead.

As for previous Google login implementations, the required client_id is the **Web client id** generated when you create your Google project.
This should not be mistaken with the Android OAuth client id. Make sure that your Google project contains both.

### LINE

Add the following line to your application’s build.gradle file:
```
 implementation 'com.linecorp:linesdk:5.1.1'
```

Add the “LineProviderWrapper” class provided in your ../gigya/providers package.

Add the following lines to your AndroidManifest.xml file using String references to your String resource file:
```
<meta-data
android:name="lineChannelID"
android:value="@string/line_channel_id" />
```

Follow LINE login guidelines to set up your channel id in the [Line developer console](https://developers.line.biz/console/).

**Note:** 
The LineProviderWrapper class is set to reference the “R.string.line_channel_id" string reference.
If you plan to bundle the wrapper within library use "context.getResources().getIdentifier("line_channel_id", "string", context.getPackageName()" instead.

If you have not already changed your JAVA compatibility within your build file, please do so, as Line integration requires it.
```
compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
```

### WeChat

Add the following line to your application’s build.gradle file:
```
implementation 'com.tencent.mm.opensdk:wechat-sdk-android-without-mta:5.5.3'
```

Add the “WeChatProviderWrapper” class provided in your ../gigya/providers package.

Add the following lines to your AndroidManifest.xml file using String references to your String resource file:
```
<activity
  android:name=".wxapi.WXEntryActivity"
  android:exported="true"
  android:label="@string/app_name"
  android:launchMode="singleTop" />

<meta-data
  android:name="wechatAppID"
  android:value="@string/wechat_app_id" />
```

**Note:**
The WeChatProviderWrapper class is set to reference the “R.string.wechat_app_id" string reference.
If you plan to bundle the wrapper within library use "context.getResources().getIdentifier("wechat_app_id", "string", context.getPackageName()" instead.

Create a sub folder in the root of your project called "wxapi".
In that folder, create an activity named WXEntryActivity, which must contain the following:

```
public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WechatProviderWrapper provider = getProvider();
        if (provider != null) {
            provider.handleIntent(getIntent(), this);
        }
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        WechatProviderWrapper provider = getProvider();
        if (provider != null) {
            provider.handleIntent(getIntent(), this);
        }
    }

    @Override
    public void onReq(BaseReq baseReq) {
        // Stub. Currently unused.
        Log.d("sd", "sd");
    }

    @Override
    public void onResp(BaseResp baseResp) {
        WechatProviderWrapper provider = getProvider();
        if (provider != null) {
            provider.onResponse(baseResp);
        }
    }

    @Override
    public void finish() {
        super.finish();
        // Disable exit animation.
        overridePendingTransition(0, 0);
    }

    @Nullable
    private WechatProviderWrapper getProvider() {
        ExternalProvider provider = (ExternalProvider) Gigya.getInstance(MyAccount.class).getUsedSocialProvider("wechat");
        return (WechatProviderWrapper) provider.getWrapper();
    }
}
```
The code above will be provided in a separate file along with the WeChatProviderWrapper file.

## Logout
A simple logout is available by using:

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

The available parameters for the "showScreenSetts" method are the same as those for the webSDK, as described here: https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/413a5b7170b21014bbc5a10ce4041860.html

An optional Boolean fullScreen field which will force the displaying of the PluginFragment to fit into the screen.
Customizing the look & feel of the PluginFragment is recommended.
It can be done by simply copying the gigya_fragment_webview.xml file from the SDK's source code to your application res/layout folder
directory.
Once copied you will be able to change & customize the layout to your choosing (with some restrictions of course).

```
Keep in mind that you cannot remove any element or change any existing element id. Doing so could result in unexpected crashes, as the SDK will still expect these elements to be presented.
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

```
The plugin callback is also typed to the current Account schema.
```
```
Business APIs are provided in order to give you an easier interface. If a more detailed and customized use is required, you can still use
the generic Gigya.send interface for all request purposes.
```

### Interruptions

Some flows can be "interrupted" due to certain Site policies.
For example, when trying to register but Two Factor Authentication is required - then an "interruption" can occur about "pending TFA registration"
that will require the end user to setup a TFA method before being able to complete the registration flow.

**Interruptions map:**
The SDK's Business APIs are design to help to easily develop a friendly way to face and resolve those interruptions in order to get the end user
logged in and still complying to the site's policies.

### Handling Interruptions

Interruption handling is a key feature introduced as of the Android SDK v4.

The SDK will expose a resolver object for supported interruptions in order to give you as a developer the ability to resolve them within the same
flow that they were triggered.

The current supported interruption flows are:
```
Pending registration
Account linking
Pending TFA registration 
Pending TFA verification
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
Single Sign-On (SSO) is an authentication method that allows a user to log in to multiple applications that reside within the same site group using a single login credential.
When using the mobile SSO feature, applications within the same group are able to share a valid session with the device browser.

When using the mobile SSO feature, applications within the same group are able to share a valid session with the device browser.
Supported flows:
* Login via SSO feature on the client application running on a mobile device. The browser on the device can then obtain the session using the SSO method.
* Login via SSO feature on Client Application 1 on a mobile device. Client Application 2 within the same site group can obtain the session using the SSO method.
* Login via SSO method on a browser running on a mobile device. Client applications within the same site group can obtain the session when using the SSO method.

You will be required to set up your central login page on your site’s console.

**Instructions**

To set up mobile SSO, please follow these steps:

1. Add the Custom-Tabs implementation to your application level build.gradle file:
```
implementation 'androidx.browser:browser:1.3.0'
```

2. Add the following to your application’s AndroidManifest.xml file:
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

3. The required default redirect consists of the following structure: “gsapi://{applicationId}/login/”, where the “applicationId” represents the application’s unique bundle identifier."
Please make sure you add your unique redirect URL to the **Trusted Site URLs** section of your parent site.

4. Finally, to initiate the flow, use the SSO function provided by the Gigya shared interface

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

The available parameters map is a baseline for adding additional parameters to the initial authentication endpoint.
Currently supported parameters:
•	"rp_context" - An available dynamic object which will be JSON serialized upon request. For more information.
Usage example:

```kotlin
gigya.sso(mutableMapOf("rp_context" to mutableMapOf("contextKey" to "contextValue"), 
        object : GigyaLoginCallback<MyAccount>() {
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

A result handler is required to process the FIDO library result intent.
Add the following to your activity:

```
// Custom result handler for FIDO sender intents.
val resultHandler: ActivityResultLauncher<IntentSenderRequest> =
       registerForActivityResult(
               ActivityResultContracts.StartIntentSenderForResult()
       ) { activityResult ->
           val extras =
                   activityResult.data?.extras?.keySet()?.map { "$it: ${intent.extras?.get(it)}" }
                           ?.joinToString { it }
           Gigya.getInstance().WebAuthn().handleFidoResult(activityResult)
       }

```

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

