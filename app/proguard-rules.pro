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
-keep,includedescriptorclasses class com.example.ui.navigation.**$$serializer { *; }
-keepclassmembers class com.example.data.local.** {
    *** Companion;
}
-keepclassmembers class com.example.ui.navigation.** {
    *** Companion;
}

# Room Database Entities, DAOs, and Generated Implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ML Kit Pose Detection & Vision Common
-keep class com.google.mlkit.vision.pose.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-dontwarn com.google.mlkit.vision.**

# CameraX & Media3 ExoPlayer
-keep class androidx.camera.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
