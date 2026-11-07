plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hermes.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hermes.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,NOTICE}"
        }
    }
}

dependencies {
    // Core Android
    val core_ktx_version = "1.15.0"
    val activity_compose_version = "1.9.3"
    val lifecycle_version = "2.8.4"
    val room_version = "2.6.1"
    val datastore_version = "1.1.1"
    val compose_version = "2024.04.00"
    val material3_version = "1.2.1"
    val navigation_compose_version = "2.7.7"
    val okhttp_version = "4.12.0"
    val kotlinx_serialization_version = "1.6.3"
    val kotlinx_coroutines_version = "1.8.0"
    val kotlinx_datetime_version = "0.5.0"
    val koin_version = "3.5.6"
    val coeil_version = "2.7.0"

    // AndroidX Core
    implementation("androidx.core:core-ktx:$core_ktx_version")
    implementation("androidx.activity:activity-compose:$activity_compose_version")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    implementation("androidx.navigation:navigation-compose:$navigation_compose_version")

    // Compose
    implementation(platform("androidx.compose:compose-bom:$compose_version"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version")

    // Material Components for XML theme
    implementation("com.google.android.material:material:1.12.0")

    // Room Database
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:$datastore_version")
    implementation("androidx.datastore:datastore-preferences-core:$datastore_version")

    // Networking - OkHttp + WebSocket (WebSocket included in okhttp since 3.x)
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttp_version")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinx_coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")

    // Dependency Injection - Koin
    implementation("io.insert-koin:koin-android:$koin_version")
    implementation("io.insert-koin:koin-androidx-compose:$koin_version")

    // Coil Image Loading
    implementation("io.coil-kt:coil-compose:$coeil_version")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.21")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinx_coroutines_version")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}