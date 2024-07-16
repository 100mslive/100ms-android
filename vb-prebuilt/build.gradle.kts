plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "live.hms.vb_prebuilt"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }

}

dependencies {

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    implementation("androidx.compose.ui:ui-android:1.6.8")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3-android:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    // Optional - Integration with LiveData
    implementation("androidx.compose.ui:ui-viewbinding:1.3.2")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    compileOnly(project(":room-kit"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
//    implementation(platform('androidx.compose:compose-bom:2023.10.01'))
//
//            // Material Design 3
//            implementation 'androidx.compose.material3:material3'
//    implementation 'androidx.compose.foundation:foundation'
//    implementation 'androidx.compose.ui:ui-tooling-preview'
//    debugImplementation 'androidx.compose.ui:ui-tooling'
//// UI Tests
//    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
//    debugImplementation 'androidx.compose.ui:ui-test-manifest'
//// Optional - Integration with ViewModels
//    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1'
//    // Optional - Integration with LiveData
//    implementation "androidx.compose.ui:ui-viewbinding:1.3.2"
//    implementation 'androidx.compose.runtime:runtime-livedata'
//    implementation "com.github.bumptech.glide:compose:1.0.0-beta01"

}