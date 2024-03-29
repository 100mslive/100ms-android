# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=android
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

# https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
-keepnames class live.hms.app2.model.RoomDetails
-keepnames class live.hms.app2.ui.settings.SettingsMode
-keep class * extends androidx.fragment.app.Fragment{}

# Previously required proguard rules for sdk, now included in the library.
#-keep class org.webrtc.** { *; }
#-keep class live.hms.video.** { *; }
