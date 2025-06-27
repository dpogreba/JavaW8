plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.antbear.javaw8"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antbear.javaw8"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Configure signing with keystore
    signingConfigs {
        create("release") {
            // Update these with your actual keystore details
            storeFile = file("../JavaW8.jks")
            storePassword = "your-keystore-password"
            keyAlias = "key0"
            keyPassword = "your-key-password"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        
        // Use default debug signing config for testing on device
        debug {
            // Default debug signing config is automatically applied
            // No explicit signingConfig needed for debug
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Core dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Location services dependency (still needed for FusedLocationProvider)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // osmdroid dependencies
    implementation("org.osmdroid:osmdroid-android:6.1.17")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.17")
    
    // OkHttp for making API requests to Overpass API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
