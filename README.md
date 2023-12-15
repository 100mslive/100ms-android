<a href="https://100ms.live/">
<img src="https://github.com/100mslive/100ms-ios-sdk/blob/main/100ms.gif" height=256/> 
<img src="https://github.com/100mslive/100ms-ios-sdk/blob/main/100ms.svg" title="100ms logo" float=center height=256>
</a>

[![Latest Version](https://jitpack.io/v/100mslive/android-sdk.svg)](https://docs.100ms.live/android/v2/release-notes/Release-Notes)
[![Documentation](https://img.shields.io/badge/Read-Documentation-blue)](https://docs.100ms.live/android/v2/foundation/Basics)
[![Discord](https://img.shields.io/discord/843749923060711464?label=Join%20on%20Discord)](https://100ms.live/discord)
[![Download App](https://img.shields.io/badge/Download%20via-Firebase-green)](https://appdistribution.firebase.dev/i/d8f1648365a33c3e)
[![Activity](https://img.shields.io/github/commit-activity/m/100mslive/100ms-android.svg)](https://github.com/100mslive/100ms-android/pulls)
[![Email](https://img.shields.io/badge/Contact-Know%20More-blue)](https://dashboard.100ms.live/register)


# 🎉 Sample App using 100ms Android SDK 🚀

Here you will find everything you need to build experiences with video using 100ms Android SDK. Dive into our SDKs, quick starts, add real-time video, voice, and screen sharing to your web and mobile applications.

Sample App of 100ms can be downloaded from Play store : https://play.google.com/store/apps/details?id=live.hms.app2

Meeting links can be generated using [dashboard](https://dashboard.100ms.live/)

## 📦 Prebuilt (Room Kit)

Room Kit is library built to take even building the UI off your hands. It uses 100ms SDK to and a dynamic layout decided by Prebuilt in the dashboard to have fully functional video up and running in your app in minutes.  
To add the library to your own app, take a look at the [Prebuilt Quickstart](https://www.100ms.live/docs/android/v2/quickstart/prebuilt-android) in the docs.  

#### 🛠️ Developing against Prebuilt Room Kit
We don't intend this to be a very common as Room Kit is updated very frequently with new features but the benefit of open source is that if you don't like how something works you can change it yourself! If you wanted to change anything in the Prebuilt library here's how.
1. Clone this repo.
2. Find the line in `app/build.gradle` that says `implementation "live.100ms:room-kit:$HMS_ROOM_KIT_VERSION"` and change it to
   ``implementation project(":room-kit")``
3. Run the app.

Now you'll be loading the room kit library locally with your own changes rather than from the maven library that 100ms publishes to.

To use this in your own code, you can put the library in your local computer and import it in your app.  
To put the room kit library in your computer as a library locally:
1. Open a terminal and cd into the root of this project. Opening the terminal from Android Studio works as well.
2. Run the following command to build and put the library in your local storage. `./gradlew clean publishToMavenLocal`
3. Verify the library was built correctly by seeing if it is in `ls ~/.m2/repository/live/100ms/room-kit`
4. In your app find where the `mavenCentral()` repository is referenced and add `mavenLocal()` before it. Eg:
``` 
   allprojects {
         repositories {
         mavenLocal()
         mavenCentral()
     }
  }
```

5. Add the import for `room kit` as you would do for any library `implementation "live.100ms:room-kit:$HMS_ROOM_KIT_VERSION"` 

That's it. Once you sync, the library should be available to your app.  
This is the recommended method since adding the module to your own app directly would make it difficult to sync with our changes.

## ☝️ Pre-requisites

- Android Studio
- Support for Java 11

## 📱 Supported Devices

The Android SDK supports Android API level 21 and higher. It is built for armeabi-v7a, arm64-v8a, x86, and x86_64 architectures.

## 📑 100ms SDK Documentation

Refer the [Getting Started - Android](https://docs.100ms.live/android/v2/foundation/Basics) guide to get detailed SDK information
