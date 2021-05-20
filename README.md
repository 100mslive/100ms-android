[![](https://jitpack.io/v/100mslive/android-sdk.svg)](https://docs.100ms.live/client-side/android)

# 100ms Sample Android Application in Kotlin

Here you will find everything you need to build experiences with video using 100ms Android SDK. Dive into our SDKs, quick starts, add real-time video, voice, and screen sharing to your web and mobile applications.

## Pre requisites

- Android Studio 3.0 or higher
- Support for Android API level 24 or higher
- Support for Java 8
- This application uses build tool version `30.0.2`

## Supported Devices

The Android SDK supports Android API level 24 and higher. It is built for armeabi-v7a, arm64-v8a, x86, and x86_64 architectures.

## Setup Guide

- Clone this repository

  ```bash
  git clone --depth 1 https://github.com/100mslive/sample-app-android.git
  ```

- Host your token generation service [following this guide](https://app.gitbook.com/@100ms/s/100ms-v2/server-side/generate-client-side-token)

- Create `app/gradle.properties`

  ```bash
  cp app/example.gradle.properties app/gradle.properties
  ```

- Put your endpoint URL as `TOKEN_ENDPOINT` in `app/gradle.properties`. Make sure it ends with a backslash (`/`) For example:
  ```env
  TOKEN_ENDPOINT="https://example-tokenservice.runkit.sh/" # Valid
  TOKEN_ENDPOINT="https://example-tokenservice.runkit.sh" # Invalid
  ```
- Create [firebase project](https://firebase.google.com/docs/android/setup#console) and save the [`google-services.json`](https://support.google.com/firebase/answer/7015592?hl=en) in `app/google-services.json`

# Run the application

## Run using Emulator

Follow the official guide at [developers.android.com](https://developer.android.com/studio/run/emulator) to download and deploying app in a emulator.

## Run on Device (**recommended**)

Follow the official guide at [developers.android.com](https://developer.android.com/studio/run/device) to setup your mobile device for development.

On the first time of launch, user will be prompted with permissions. Then you are good to go to run the application.

## Layout

In the launch screen, here we have two options:

<img src="images/home-page.jpg?raw=true" width="300">

### Join meeting

- Paste the exact Room ID as obtained from the [`create-room` API](https://app.gitbook.com/@100ms/s/100ms-v2/server-side/create-room)

- Click `Join Now`.
- Video Conversation will be started

# 100ms SDK Documentation

This guide provides an overview of the key objects you'll use with 100ms' android SDK to build a live audio/video application

## Supported Devices

100ms' Android SDK supports Android API level 21 and higher. It is built for armeabi-v7a, arm64-v8a, x86, and x86_64 architectures

## Concepts

- `Room` - A room represents a real-time audio, video session, the basic building block of the 100mslive Video SDK
- `Track` - A track represents either the audio or video that makes up a stream
- `Peer` - A peer represents all participants connected to a room. Peers can be "local" or "remote"
- `Broadcast` - A local peer can send any message/data to all remote peers in the room

## Pre-requisites

### 1. Add dependency to 100ms lib

- Add the JitPack repository to your build file. Add it in your root `build.gradle` at the end of repositories of `allprojects`:

```java 
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
	
- Add the dependency in your app-level `gradle`

```java 
dependencies {
		implementation 'com.github.100mslive:android-sdk:x.x.x'
	}
```

### 2. Add other dependencies

Add all the following libraries in your app-level `gradle` file as dependencies.
- If you are using any of the following libraries already in your application, you can use the version you are already using.
- Make sure `okhttp` and `webrtc` use the same version as mentioned below

``` implementation 'org.webrtc:google-webrtc:1.0.32006'
implementation 'com.squareup.okhttp3:okhttp:3.6.0'
implementation 'com.google.code.gson:gson:2.8.6'
implementation 'org.jetbrains:annotations:15.0'
```

### 3. Get Access Keys

Sign up on https://dashboard.100ms.live/register & visit Developer tab to get your access credentials

### 4. Generate a server-side token
To generate a server-side token, follow the steps described here - https://app.gitbook.com/@100ms/s/100ms-v2/server-side/generate-server-side-token

### 5. Create a room

To create a room, follow the steps described here - https://app.gitbook.com/@100ms/s/100ms-v2/server-side/create-room

### 6. Generate a client-side token

To generate a client-side token, follow the steps described here - https://app.gitbook.com/@100ms/s/100ms-v2/server-side/generate-client-side-token


## Create and instantiate 100ms Client (HMSClient)

This will instantiate an `HMSClient` object

```
val hmsSDK = HMSSDK
    .Builder(application) // pass the application context
    .setTrackSettings(hmsTrackSettings) // optional -- to set a track settings different from default
    .setAnalyticEventLevel(HMSAnalyticsEventLevel.ERROR) // optional -- set the analytical level
    .setLogLevel(HMSLogger.LogLevel.VERBOSE) // optional -- set the logging level
    .build()

```

> `authTokenis` the client-side token generated by your token generation service.

>  This `roomId` should be generated using createRoom API

## Provide joining configuration

To join a room created by following the steps described in the above section, clients need to create a `HMSConfig` instance and use that instance to call `join` method of `HMSSDK`

```
// Create a new HMSConfig
val config = HMSConfig(
        roomDetails.username, // the name that the user wants to be displayed while in the room
        roomDetails.authToken, // the auth token to be used
        info.toString(), // optional -- any  json string or metadata that user need to paas while joining
        endpoint // optional -- to override the default endpoint (advanced)
      )

```

## Setup event listeners

100ms SDK provides callbacks to the client app about any change or update happening in the room after a user has joined by implementing `HMSUpdateListener` . These updates can be used to render the video on screen or to display other info regarding the room.

```
      val hmsUpdateListener = object: HMSUpdateListener{

        override fun onJoin(room: HMSRoom) {
          // This will be called on a successful JOIN of the room by the user
          // This is the point where applications can stop showing its loading state
        }

        override fun onPeerUpdate(type: HMSPeerUpdate, peer: HMSPeer) {
          // This will be called whenever there is an update on an existing peer
          // or a new peer got added/existing peer is removed.
          // This callback can be used to keep a track of all the peers in the room
        }

        override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
          // This is called when there is a change in any property of the Room
        }

        override fun onTrackUpdate(type: HMSTrackUpdate, track: HMSTrack, peer: HMSPeer) {
          // This is called when there are updates on an existing track
          // or a new track got added/existing track is removed
          // This callback can be used to render the video on screen whenever a track gets added
        }

        override fun onMessageReceived(message: HMSMessage) {
          // This is called when there is a new broadcast message from any other peer in the room
          // This can be used to implement chat is the room
        }

        override fun onError(error: HMSException) {
          // This will be called when there is an error in the system
          // and SDK has already retried to fix the error
        }

      }
```

## Join a room

Use the HMSConfig and HMSUpdateListener instances to call join method on the instance of HMSSDK created above.
Once Join succeeds, all the callbacks keep coming on every change in the room and the app can react accordingly

```
hmsSDK.join(hmsConfig, hmsUpdateListener) // to join a room

```

## Leave Room

Call the leave method on the HMSSDK instance

```
hmsSDK.leave() // to leave a room
```

## Get Peers/tracks data

`HMSSDK` has other methods which the client app can use to get more info about the `Room` , `Peer` and `Tracks`


```
    fun join(config: HMSConfig, hmsUpdateListener: HMSUpdateListener) {
        // to join a Room
    }

    fun leave() {
        // to leave a Room
    }

    fun getLocalPeer(): HMSPeer {
        // Returns the local peer, which contains the local tracks
    }

    fun getRemotePeers(): List<HMSPeer> {
        // Returns a list of all the remote peers present in the room currently
    }

    fun getPeers(): List<HMSPeer> {
        // Returns a list of all the peers present in the room currently
    }

    fun sendMessage(type: String, message: String) {
        // used to send message to all other peers via broadcast
    }

    fun addAudioObserver(observer: HMSAudioListener) {
        // add a observer to listen to Audio Level Info of all peers. This will be
        // called every second if set
    }

    fun removeAudioObserver() {
        // remove the audio level info observer
    }
```

# Mute/unmute local video/audio

Use the `HMSLocalAudioTrack` and `HMSLocalVideoTrack` to mute/unmute tracks

```
    class HMSTrack {
        val trackId: String // This is the id of a given track
        val type: HMSTrackType // One of AUDIO or VIDEO
        var source: String // This denotes whether the given track is a `regular`, `screen` or `plugin` type
        var description: String // This can be set by client app while creating a HMSTrack. Default value is empty
        var isMute: Boolean // This denotes where the current track is mute or not
    }

    class HMSLocalAudioTrack {
        var volume: Double // Volume of the current Track
        val hmsAudioTrackSettings: HMSAudioTrackSettings // Settings of the given Audio Track
        fun setMute(isMute: Boolean) // to mute or unmute the local audio track
        fun setSettings(newSettings: HMSAudioTrackSettings) // Application can use this to change settings of the audio track.
    }

    class HMSRemoteAudioTrack {
        var isPlaybackAllowed: Boolean // Is local playback allowed of this track or not

        fun setVolume(value: Double) // method to set the playback volume of remote audio track
    }

    class HMSVideoTrack {
        fun addSink(sink: VideoSink) // Call this when app needs to render a track on screen
        fun removeSink(sink: VideoSink) // Call this when app no longer needs to rebder
    }

    class HMSLocalVideoTrack {
        fun setMute(isMute: Boolean) // to mute or unmute the local video track
        fun setSettings(newSettings: HMSVideoTrackSettings) // to set new settings
        fun switchCamera() // To switch camera
        fun switchCamera(deviceId: String) // to change to a particular camera
    }

    class HMSRemoteVideoTrack {
        var isPlaybackAllowed: Boolean // to set or get if local playback is allowed for the remote video track
    }

```






Refer the [Getting Started - Android](https://app.gitbook.com/@100ms/s/100ms-v2/android/getting-started-v2-android/@comments/b0013555092c48558d59c8e70e6474ca) guide in 100ms Gitbook.


