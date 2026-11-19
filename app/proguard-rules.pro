# Sajda AI ProGuard / R8 Keep Rules for Release Builds

# Preserve stack trace line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlinx Serialization & Type-Safe Navigation Routes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.data.local.**$$serializer { *; }
-keep,includedescriptorclasses class com.example.data.remote.**$$serializer { *; }
-keep,includedescriptorclasses class com.example.ui.navigation.**$$serializer { *; }
-keepclassmembers class com.example.data.local.** {
    *** Companion;
}
-keepclassmembers class com.example.data.remote.** {
    *** Companion;
}
-keepclassmembers class com.example.ui.navigation.** {
    *** Companion;
}
-keep class com.example.data.remote.**Dto { *; }
-keep interface com.example.data.remote.UzbekistanPrayerTimesApi { *; }

# WorkManager Workers & BroadcastReceivers
-keep class com.example.alarm.** extends androidx.work.CoroutineWorker { *; }
-keep class com.example.alarm.** extends android.content.BroadcastReceiver { *; }

# Room Database Entities, DAOs, and Generated Implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# ML Kit Pose Detection & Vision Common
-keep class com.google.mlkit.vision.pose.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-dontwarn com.google.mlkit.vision.**

# CameraX & Media3 ExoPlayer
-keep class androidx.camera.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Remote Data DTOs (IslomApi & Aladhan) and Retrofit Services
-keep,includedescriptorclasses class com.example.data.remote.**$$serializer { *; }
-keepclassmembers class com.example.data.remote.** {
    *** Companion;
}
-keep class com.example.data.remote.** { *; }
-keepattributes Signature, EnclosingMethod
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

