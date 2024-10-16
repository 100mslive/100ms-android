plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.5.0"
}

android {
    namespace = "live.hms.prebuilt_themes"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val HMS_SDK_VERSION: String by project
    implementation("live.100ms:android-sdk:$HMS_SDK_VERSION")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui-android:1.6.8")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

val HMS_ROOM_KIT_VERSION : String by project
val publishing_licence_url : String by project
val publishing_licence_name : String by project
val publishing_project_url : String by project
val publishing_developer_id : String by project
val publishing_developer_name : String by project
val publishing_developer_email : String by project
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "live.100ms.room-kit"
            artifactId = "prebuilt-themes"
            version = HMS_ROOM_KIT_VERSION

            afterEvaluate {
                from(components["release"])
            }

            pom {
                // Avoid trying to sign local builds
                if(rootProject.properties["ossrhUsername"] != "") {
                    signing {
                        sign(publishing.publications["release"])
                    }
                }
                name.set("100ms.live Android Room Kit themes")
                description.set("Base themes for prebuilt/room-kit.")
                url.set(publishing_project_url)
                licenses {
                    license {
                        name.set(publishing_licence_name)
                        url.set(publishing_licence_url)
                    }
                }
                developers {
                    developer {
                        id.set(publishing_developer_id)
                        name.set(publishing_developer_name)
                        email.set(publishing_developer_email)
                    }
                }
                scm {
                    connection.set("SCM is private")
                    developerConnection.set("SCM is private")
                    url.set("https://github.com/100mslive/100ms-android")
                }
            }
        }
    }
}