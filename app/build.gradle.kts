plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}



android {
    namespace = "com.example.streamingcameraapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.streamingcameraapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true // Keep Compose enabled if you plan to use it
        viewBinding = true // Enable viewBinding
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
}

configurations.all {
    resolutionStrategy {
        eachDependency {            
            if (requested.group == "androidx.core" && (requested.name == "core" || requested.name == "core-ktx")) {
                useVersion(libs.versions.coreKtx.get())
                because("Align with AGP 8.4.0 compatibility and project settings")
            }
            
            if (requested.group == "androidx.lifecycle" &&
                (requested.name == "lifecycle-runtime-compose-android" || requested.name == "lifecycle-runtime-compose")) {
                 useVersion(libs.versions.lifecycleRuntimeKtx.get())
                 because("Align with AGP 8.4.0 compatibility and project settings for lifecycle")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx) 
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // For Compose, if used
    implementation(platform(libs.androidx.compose.bom)) // For Compose, if used
    implementation(libs.androidx.ui) // For Compose, if used
    implementation(libs.androidx.ui.graphics) // For Compose, if used
    implementation(libs.androidx.ui.tooling.preview) // For Compose, if used
    implementation(libs.androidx.material3) // For Compose Material 3, if used

    // Added dependencies for AppCompat, Material Components, ConstraintLayout, and WebView
    implementation("androidx.appcompat:appcompat:1.7.0") // For AppCompatActivity
    implementation("com.google.android.material:material:1.12.0") // For Material Theming and Components
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // For ConstraintLayout
    implementation("androidx.webkit:webkit:1.11.0") // For WebView

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.5")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0") { exclude(group = "org.codehaus.mojo", module = "animal-sniffer-annotations") }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") { version { strictly("3.5.1") } }
    androidTestImplementation(platform(libs.androidx.compose.bom)) // For Compose, if used
    androidTestImplementation(libs.androidx.ui.test.junit4) // For Compose, if used
    debugImplementation(libs.androidx.ui.tooling) // For Compose, if used
    debugImplementation(libs.androidx.ui.test.manifest) // For Compose, if used
}
