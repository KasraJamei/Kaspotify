# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Room entities/DAOs and our data models (referenced by generated Room code)
-keep class com.example.kaspotify.data.local.** { *; }
-keep class com.example.kaspotify.data.model.** { *; }
