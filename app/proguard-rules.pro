# ProGuard rules for Hermes Android App

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlinx.serialization.** { *; }

# Keep Room entities and DAOs
-keep class com.hermes.android.data.db.** { *; }

# Keep data models
-keep class com.hermes.android.data.model.** { *; }

# Keep network classes
-keep class com.hermes.android.network.** { *; }

# Keep UI classes
-keep class com.hermes.android.ui.** { *; }

# Keep Koin modules
-keep class com.hermes.android.di.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Okio
-keep class okio.** { *; }
-dontwarn okio.**

# Kotlinx Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Kotlinx Datetime
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# RxJava
-keep class io.reactivex.** { *; }
-dontwarn io.reactivex.**

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Material3
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Application class
-keep class com.hermes.android.HermesApplication { *; }

# MainActivity
-keep class com.hermes.android.MainActivity { *; }

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Keep names for serialization
-keepnames class * {
    @kotlinx.serialization.SerialName *;
}