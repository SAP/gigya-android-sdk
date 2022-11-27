# SAP CDC (Gigya) Android Biometric SDK

## Description
Gigya's Android Biometric SDK library provides a Java interface for applying biometric authentication to your active session.

## Requirements
Gigya's Android Core library implementation.
Android SDK 23 and above is required.

## Limitations
Following released version 2.+ the Biometric extension will require your Application to be AndroidX compliant.
For more information please visit [Migrate to AndroidX](https://developer.android.com/jetpack/androidx/migrat)

**Biometric fingerprint support is available for devices running Android Marshmallow and above.**

On Android devices running OS 11+, An active/locked session will be unrecoverable when the user makes
changes to his fingerprint state on the device.
The Android OS will apply an additional security layer that will invalidate any keystore keys related to biometric authentication.
Application state can be handled according to **"Key invalidated"** errors from the biometric **"onBiometricOperationFailed"** callback.

## Integration

Update your gradle.build file with one of two options:

Implementation using a binary file.
Download the biometric library and copy it to your applications libs/ folder.
```gradle
implementation files('libs/gigya-android-biometric-2.0.0.aar')
```

-Or-

Implementation using **JitPack**
```gradle
implementation 'com.github.SAP.gigya-android-sdk:gigya-android-biometric:bio-v2.0.0'
```

**In addition you will need to add this dependency as well.**
```gradle
implementation 'androidx.appcompat:appcompat:1.2.0'
```
The Android SDK v4 differs from the previous releases in the way that is handles the UI prompt for you.
It Utilizes Google's biometric prompt class on Android devices running Android Pie (and above).
On Android devices that run any other version below Pie (M, N, O) - a custom prompt will be shown.

**It is not longer possible to customize the biometric prompt.**

### Authentication flow

The supported end user flow for the biometric authentication feature is:
```
* End user logs in
* End user opts in to biometric authentication
      * This will require the end user to verify his fingerprint
* The app is locked or being cleared from memory
* End user is required to unlock the app in order to restore his session
      * This will require the end user to verify his fingerprint
* End user opts out of biometric authentication
```

In order to use biometric fingerprint authentication, several rules must apply:
```
The device has a fingerprint sensor available.
A minimum of 1 fingerprint already enrolled in the device.
```
If one of the above rules isn't satisfied, the biometric feature availability will return false.

```java
GigyaBiometric biometric = GigyaBiometric.getInstance();
final boolean available = biometric.isAvailable(); // WILL BE false.
```

Available authentication methods:

```
Opt-In - Opts-in the existing session to use fingerprint authentication.
Opt-Out - Opts-out the existing session from using fingerprint authentication.
```

```
The biometric fingerprint feature is a security encryption on top of an existing session of your app, therefore, calling any biometric
operations requires a valid session.
```

```
Relevant permissions are already requested in the library manifest.
android.permission.USE_FINGERPRINT
android.permission.USE_BIOMETRIC
```

```
Lock - Locks the existing session until unlocking it. No authentication based actions can be done while the session is locked.
Unlock - Unlocks the session so the user can continue to make authentication based actions.
```

**Example of fingerprint authentication flow:**

```java
/*
Reference biometric interface.
*/
final GigyaBiometric biometric = GigyaBiometric.getInstance();
if (!biometric.isAvailable()) {
    return;
}

/*
Generate a prompt info class
*/
final GigyaPromptInfo info = new GigyaPromptInfo(
"PROMPT-TITLE",
"PROMPT-SUBTITLE",
"PROMPT-DESCRIPTION-OPTIONAL"
);

/*
Use authentication action
*/
biometric.optIn(this, info, new IGigyaBiometricCallback() {
    @Override
    public void onBiometricOperationSuccess(@NonNull GigyaBiometric.Action action) {
    // Action success.
    }

    @Override
    public void onBiometricOperationFailed(String reason) {
    // Action failed with provided reason.
    }

    @Override
    public void onBiometricOperationCanceled() {
    // Action canceled by user.
    }
});
```

### GigyaPromptInfo Class

A helper class used for adding biometric prompt display data.

### IGigyaBiometricCallback Interface class

Biometric authentication action callback that provides the result interface given a biometric prompt action.

```java
final IGigyaBiometricCallback biometricCallback = new IGigyaBiometricCallback() {

    @Override
    public void onBiometricOperationSuccess(@NonNull GigyaBiometric.Action action) {
    // Action success. Available actions are OPT_IN, OPT_OUT, LOCK, UNLOCK.
    }

    @Override
    public void onBiometricOperationFailed(String reason) {
    // Action failed with provided reason.
    }

    @Override
    public void onBiometricOperationCanceled() {
    // Action canceled by user.
    }
};
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

