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

- Host your token generation service [following this guide](https://100ms.gitbook.io/100ms/helpers/runkit)

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

- Paste the exact Room ID as obtained from the [`create-room` API](https://100ms.gitbook.io/100ms/server-side/create-room)

- Click `Join Now`.
- Video Conversation will be started

### Create a Room

- Specify a meeting name and click on `Start Meeting`

# 100ms SDK Documentation

This guide provides an overview of the key objects you'll use with 100ms' android SDK to build a live audio/video application

## Supported Devices

100ms' Android SDK supports Android API level 21 and higher. It is built for armeabi-v7a, arm64-v8a, x86, and x86_64 architectures

## Concepts

- `Room` - A room represents a real-time audio, video session, the basic building block of the 100mslive Video SDK
- `Stream` - A stream represents real-time audio, video streams that are shared to a room. Usually, each stream contains a video track and an audio track (except screenshare streams, which contains only a video track)
- `Track` - A track represents either the audio or video that makes up a stream
- `Peer` - A peer represents all participants connected to a room. Peers can be "local" or "remote"
- `Publish` - A local peer can share its audio, video by "publishing" its tracks to the room
- `Subscribe` - A local peer can stream any peer's audio, video by "subscribing" to their streams
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
		implementation 'com.github.100mslive:android-sdk:0.9.13'
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
To generate a server-side token, follow the steps described here - https://docs.100ms.live/server-side/generate-server-side-token

### 5. Create a room

To create a room, follow the steps described here - https://docs.100ms.live/server-side/create-room

### 6. Generate a client-side token

To generate a client-side token, follow the steps described here - https://docs.100ms.live/server-side/authentication


## Create and instantiate 100ms Client (HMSClient)

This will instantiate an `HMSClient` object

```
//Create a 100ms peer
hmspeer = new HMSPeer(username, authToken);
//Create a room
hmsRoom = new HMSRoom(roomId);

//Create client configuration
config = new HMSClientConfig('wss://prod-in.100ms.live/ws');

//Create a 100ms client
hmsClient = new HMSClient(this, getApplicationContext(), hmspeer, config);

hmsClient.setLogLevel(HMSLogger.LogLevel.LOG_DEBUG);
```

> `authTokenis` the client-side token generated by your token generation service.

>  This `roomId` should be generated using createRoom API

## Connect to 100ms' server

After instantiating `HMSClient`, connect to 100ms' server.

```
//The client will connect to the WebSocket channel provided through the config
hmsClient.connect();
```

## Setup listeners

After successfully connecting, add listeners to listen to peers joining, new streams being added to the room. This must be done before joining the room.

```
HMSEventListener listener = new HMSEventListener()
{
		@Override
    public void onConnect() {
       //When the peer connects to the room
    }
		@Override
    public void onDisconnect() {
      //when the peer disconnected from the room   
    }
    @Override
    public void onPeerJoin(HMSPeer peer) {
			//call actions related to a new peer addition 
    }
    @Override
    public void onPeerLeave(HMSPeer peer) {
			//call actions related to a peer removal   
    }
    @Override
    public void onStreamAdd(HMSPeer peer, HMSStreamInfo mediaInfo) {
			//call actions related to a stream addtion
			//call subscribe    
    }
    @Override
    public void onStreamRemove(HMSPeer peer, HMSStreamInfo mediaInfo) {
			//call actions related to a stream removal
			//call unsubscribe    
    }
    @Override
    public void onBroadcast(HMSPayload payload) {
			//call actions related to a broadcast
    }

    @Override
    public void onDisconnect() {
			//call actions to reconnect
    }

};
```

> Always wait for `connect` message handler after creating client before subscribing/publishing any streams

> If say, 4 streams were already published when client connects to the room, then client receives `stream-add` messages for all those 4 streams as soon as client joins

> Remember to add `disconnected` message handler. Temporary websocket disconnections are common and trying to reconnect on disconnection will ensure the user sees the conference continuing instead of freezing up

## Join a room

```
//Pass the unique id for the room here as a String
hmsClient.join(roomid, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
    //data returns roomid
		Log.v("HMSClient onJoinSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onJoinFailure", errorReason);
	}
});
```

> This roomId should be generated using createRoom API. Currently, we allow clients to connect to roomIds that have not been created using createRoom API. This access will be removed by end of November

## Get local camera/mic streams

```
//Set all the media constraints here.
//You can disable video/audio publishing by changing the settings from the settings activity
//Do it before joining the room
localMediaConstraints = new HMSRTCMediaStreamConstraints(DEFAULT_PUBLISH_AUDIO, DEFAULT_PUBLISH_VIDEO);
localMediaConstraints.setVideoCodec(DEFAULT_CODEC);
localMediaConstraints.setVideoFrameRate(Integer.valueOf(DEFAULT_VIDEO_FRAMERATE));
localMediaConstraints.setVideoResolution(DEFAULT_VIDEO_RESOLUTION);
localMediaConstraints.setVideoMaxBitRate(Integer.valueOf(DEFAULT_VIDEO_BITRATE));
if(frontCamEnabled){
    isFrontCameraEnabled = true;
    localMediaConstraints.setCameraFacing(FRONT_FACING_CAMERA);
}
else {
    isFrontCameraEnabled = false;
    localMediaConstraints.setCameraFacing(REAR_FACING_CAMERA);
}

hmsClient.getUserMedia(this, localMediaConstraints, new HMSStream.GetUserMediaListener() {

    @Override
    public void onSuccess(HMSRTCMediaStream mediaStream) {				//Receive the local media stream
     
				 localStream = mediaStream;
				//Expose Media stream APIs to developers
				// process the stream
    }

    @Override
    public void onFailure(String errorReason) {
			Log.v("HMSClient onLeaveFailure", errorReason);
    }
});
```

> The settings above are recommended settings for most use cases. You can increase resolution to `hd` and bandwidth to `1024` to get higher quality video.

> Getting local screenshare stream will not be covered by v0.x SDK. Please contact support@100ms.live if you want access to sample code to handle it inside your app

## Display local stream

Once `mediaStream` has been received, get the video and audio tracks from the stream object. Call the `VideoTrack` addsink method with `SurfaceviewRenderer`.

```
//The following code is a sample. Developers can make use of the stream object 
//in their own way of rendering
if(mediaStream.getStream().videoTracks.size()>0) {
    localVideoTrack = mediaStream.getStream().videoTracks.get(0);
    localVideoTrack.setEnabled(true);
}
if(mediaStream.getStream().audioTracks.size()>0) {
    localAudioTrack = mediaStream.getStream().audioTracks.get(0);
    localAudioTrack.setEnabled(true);
}

runOnUiThread(() -> {
    try {
        surfaceViewRenderer.setVisibility(View.VISIBLE);
        localVideoTrack.addSink(surfaceViewRenderer);
    } catch (Exception e) {
        e.printStackTrace();
    }
});
```

> Remember to mirror the local front facing camera stream but not the main camera stream

## Publish local stream to room

A local peer can share her audio, video and data tracks by "publishing" its tracks to the room

```
hmsClient.publish(localMediaStream, hmsRoom, localMediaConstraints, new HMSStreamRequestHandler() {
@Override
public void onSuccess(HMSPublishStream data) {
    Log.v(TAG, "publish success "+data.getMid());
}

@Override
public void onFailure(long error, String errorReason) {
    Log.v(TAG, "publish failure");
}
});
```

## Subscribe to a remote peer's stream

This method "subscribes" to a remote peer's stream. This should ideally be called in the `onStreamAdd` listener

```
hmsClient.subscribe(streamInfo, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
		Log.v("HMSClient onSubscribeSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onSubscribeFailure", errorReason);
	}
});
```

## Broadcast

This method broadcasts a payload to all peers

```
hmsClient.broadcast(payload, room, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
		Log.v("HMSClient onBroadcastSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onBroadcastFailure", errorReason);
	}
});
```

## Unpublish local stream

```
hmsClient.unpublish(stream, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
		Log.v("HMSClient onPublishSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onPublishFailure", errorReason);
	}
});
```

## Unsubscribe to a peer's stream

```
hmsClient.unsubscribe(stream, new RequestHandler()
{
	@Override
	public void onSuccess(String data) {
		Log.v("HMSClient onUnSubscribeSuccess", data);
	}
	@Override
	public void onFailure(long error, String errorReason) {
		Log.v("HMSClient onUnSubscribeFailure", errorReason);
	}
});
```

## Disconnect client

```
//The client will disconnect from the WebSocket channel provided
hmsClient.disconnect();

```

# Switch camera

```
//Toggle between front and rear camera. Make sure you have initialized 
//hmsclient before calling this
hmsClient.switchCamera();
```


# Mute/unmute local video/audio

Get the local audio/video tracks from `hmsClient.getUserMedia()` method.

```
// To mute a local video track
localVideoTrack = mediaStream.getStream().videoTracks.get(0);
localVideoTrack.setEnabled(false);

// To mute a local audio track
localAudioTrack = mediaStream.getStream().audioTracks.get(0);
localAudioTrack.setEnabled(false);
```

This will stop sending video/audio frames to remote peers, however the device camera will still be reported as in use. To stop accessing device camera as well use 

```
HMSStream.getCameraCapturer().stop();
```

Later you can restart camera capture using 

```
HMSStream.getCameraCapturer().start();
```

We recommend adding a delay before calling stop() otherwise the last captured camera frame will keep on showing for remote peers.

```

localVideoTrack.setEnabled(false);
handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isVideoEnabled) {
                            HMSStream.getCameraCapturer().stop();
                        }
                    }
                }, 500);
```

## Get user id and role

To access the role and user id that were passed in the token use getRole() and getCustomerUserId() getters.

```
public void onPeerJoin(HMSPeer hmsPeer) {
  Log.v(TAG, "User Id: " + hmsPeer.getCustomerUserId());
  Log.v(TAG, "User Role: " + hmsPeer.getRole());
}
```






Refer the [Getting Started - Android](https://100ms.gitbook.io/100ms/client-side/android) guide in 100ms Gitbook.


