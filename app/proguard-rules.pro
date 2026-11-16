# ==============================================================================
# VELORA — Production R8 / ProGuard Configuration (WP-4 Release Hardening)
# ==============================================================================

-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# 1. AndroidX Media3 ExoPlayer & MediaSessionService (rely on library consumer rules + service entry point)
-dontwarn androidx.media3.**
-keep class com.example.data.playback.VeloraPlaybackService {
    public <init>();
}

# 2. AndroidX Room SQLite v3 Entities, DAOs & Database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 3. Koin Dependency Injection & ViewModels
-dontwarn org.koin.**
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# 4. Firebase Firestore, Auth & Google Identity (Credential Manager)
-dontwarn com.google.firebase.**
-keepclassmembers class com.example.data.firebase.CloudPlaylistDto {
    <init>(...);
    public <methods>;
    public <fields>;
}
-keepclassmembers class com.example.data.firebase.CloudEqPresetDto {
    <init>(...);
    public <methods>;
    public <fields>;
}

# 5. Jetpack DataStore Preferences & Kotlinx Coroutines
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn kotlinx.coroutines.**

# 6. Coil Image Loader
-dontwarn coil.**
-dontwarn okhttp3.**
-dontwarn okio.**
