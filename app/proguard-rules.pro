# ==============================================================================
# VELORA — Production R8 / ProGuard Configuration (WP-4 Release Hardening)
# ==============================================================================

-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# 1. AndroidX Media3 ExoPlayer & MediaSessionService
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.example.data.playback.VeloraPlaybackService { *; }

# 2. AndroidX Room SQLite v3 Entities, DAOs & Database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.data.local.** { *; }

# 3. Koin Dependency Injection & ViewModels
-keep class org.koin.** { *; }
-dontwarn org.koin.**
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# 4. Firebase Firestore, Auth & Google Identity (Credential Manager)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.example.data.firebase.** { *; }
-keepclassmembers class com.example.data.firebase.** {
    <init>();
    public <methods>;
    public <fields>;
}

# 5. Jetpack DataStore Preferences & Kotlinx Coroutines
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn kotlinx.coroutines.**

# 6. Domain Models & Declarative Plugin Manifests
-keep class com.example.domain.model.** { *; }
-keep class com.example.orchestrator.** { *; }

# 7. Coil Image Loader
-dontwarn coil.**
-dontwarn okhttp3.**
-dontwarn okio.**
