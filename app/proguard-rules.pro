# ==============================================================================
# VELORA — Production R8 Full-Mode Obfuscation & Resource Shrinking Rules
# ==============================================================================

# Preserve stack-trace line numbers while stripping real source file names for security
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,*Annotation*
-renamesourcefileattribute SourceFile

# Strip verbose and debug Android logs from production release DEX for security & size
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# 1. AndroidX Media3 ExoPlayer & MediaSessionService (rely on library consumer rules + service entry point)
-dontwarn androidx.media3.**
-keep class com.example.data.playback.VeloraPlaybackService {
    public <init>();
}

# 2. AndroidX Room SQLite v4 Entities, DAOs, Relations & Generated Database Impl
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class com.example.data.local.** {
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
}

# 3. Koin Dependency Injection & Lifecycle ViewModels
-dontwarn org.koin.**
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# 4. Firebase Firestore, Auth & Google Identity (Credential Manager)
# R8 full mode strips unreferenced default constructors/getters unless explicitly kept.
-dontwarn com.google.firebase.**
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * {
    <init>(...);
    public <methods>;
    public <fields>;
}
-keep class com.example.data.firebase.FirestoreUserProfile { *; }
-keep class com.example.data.firebase.FirestoreCloudPlaylist { *; }
-keep class com.example.data.firebase.FirestoreCloudEqPreset { *; }

# 5. Jetpack DataStore Preferences & Kotlinx Coroutines
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn kotlinx.coroutines.**

# 6. Coil Image Loader
-dontwarn coil.**
-dontwarn okhttp3.**
-dontwarn okio.**
