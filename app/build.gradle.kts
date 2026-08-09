plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

// Where each build looks for the media server. Both are stated outright: a
// build points somewhere on purpose, and a missing setting should be a
// compile error rather than something silently filled in.
//
// DEV is the emulator's route to this machine. Testing from a real device
// means editing this line to the host's address on the LAN — visible in the
// diff, which is the point.
val DEV_SERVER_URL = "http://192.168.0.6:4000/"

// The Pi, on the church network. Release builds always point here, so cutting
// a release never depends on remembering to change an address back.
val CHURCH_SERVER_URL = "http://192.168.0.4:3000/"

android {
    namespace = "com.example.churchmusicplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.churchmusicplayer"
        minSdk = 21
        targetSdk = 34
        // Both 1.0 and 1.2.0 went out as versionCode 1 — the name was only ever
        // written in the footer. This is the first release numbered in one place.
        versionCode = 3
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // One codebase, two devices. They differ in how big things are drawn and in
    // whether the app pins itself to the screen — nothing else.
    flavorDimensions += "device"
    productFlavors {
        create("phone") {
            dimension = "device"
            resValue("string", "app_name", "사랑의빛교회 기도음악 재생")
            buildConfigField("boolean", "KIOSK", "false")
        }
        create("tablet") {
            dimension = "device"
            resValue("string", "app_name", "사랑의빛교회 기도음악 재생 (태블릿)")
            buildConfigField("boolean", "KIOSK", "true")
        }
    }

    // The release key. Its details live in ~/.gradle/gradle.properties, outside
    // this repository, because a keystore password in a checkout is a password
    // in every clone and every backup of it.
    //
    // Every release from here on must be signed with this one key: Android
    // refuses to install an update whose signature differs, and the only way
    // back from a lost keystore is uninstalling the app on every device.
    val releaseStore = (project.findProperty("LOVELIGHT_STORE_FILE") as String?)?.let(::File)
    val signed = releaseStore?.exists() == true

    signingConfigs {
        if (signed) {
            create("release") {
                storeFile = releaseStore
                storePassword = project.property("LOVELIGHT_STORE_PASSWORD") as String
                keyAlias = project.property("LOVELIGHT_KEY_ALIAS") as String
                keyPassword = project.property("LOVELIGHT_KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "SERVER_URL", "\"$DEV_SERVER_URL\"")
            // Note(yoochan.kim): the release key here too, so a debug build can
            // be put straight onto a device that is carrying a release. Android
            // refuses an update whose signature differs, and uninstalling first
            // would take the panel's own settings — its server address — with
            // it. Same app, same key; only the address and the optimiser differ.
            if (signed) signingConfig = signingConfigs.getByName("release")
        }
        release {
            buildConfigField("String", "SERVER_URL", "\"$CHURCH_SERVER_URL\"")
            // Note(yoochan.kim): unsigned rather than debug-signed when the key
            // is absent — an APK that installs but carries the wrong signature
            // is worse than one that will not install at all.
            signingConfig = if (signed) signingConfigs.getByName("release") else null
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("io.socket:socket.io-client:2.0.0") {
        exclude(group = "org.json", module = "json")
    }
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.1")

}
