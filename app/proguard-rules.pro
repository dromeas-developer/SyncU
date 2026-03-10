# SyncU ProGuard Rules

# Keep data models - vital for Room and Gson
-keep class com.syncu.data.** { *; }
-keepclassmembers class com.syncu.data.** { *; }

# Keep API models
-keep class com.syncu.api.** { *; }
-keepclassmembers class com.syncu.api.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlin
-dontwarn kotlin.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Health Connect
-keep class androidx.health.connect.client.records.** { *; }
-keep class androidx.health.connect.client.units.** { *; }
-keep class androidx.health.connect.client.metadata.** { *; }
-dontwarn androidx.health.connect.client.**

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep the Application class
-keep class com.syncu.SyncUApp { *; }
