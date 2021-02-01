# 100 ms - Android Application

Here you will find everything you need to build experiences with video using 100ms Android SDK. Dive into our SDKs, quick starts, add real-time video, voice, and screen sharing to your web and mobile applications.

## Pre requisites

- Android Studio 3.0 or higher
- Support for Android API level 24 or higher
- Support for Java 8
- This application uses build tool version `30.0.2`

## Supported Devices

The Android SDK supports Android API level 24 and higher. It is built for armeabi-v7a, arm64-v8a, x86, and x86_64 architectures.

## Quick start to run the sample application

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

Refer the [Getting Started - Android](https://100ms.gitbook.io/100ms/client-side/android) guide in 100ms Gitbook.
