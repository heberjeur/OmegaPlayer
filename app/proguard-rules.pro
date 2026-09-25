-keep class * extends androidx.room.RoomDatabase
-keep class com.arslandaim.omegaplayer.data.** { *; }

-keep class com.arslandaim.omegaplayer.service.PlaybackService { *; }
-keep class androidx.media3.session.MediaSessionService { *; }
-keep class androidx.media3.session.MediaSession { *; }
-keep class androidx.media3.exoplayer.ExoPlayer { *; }
-keep class androidx.media3.ui.PlayerView { *; }
-keepclassmembers class * extends androidx.media3.common.Player$Listener { *; }

-keep class * extends dagger.hilt.internal.UnsafeCasts
-keep class **_HiltModules* { *; }
-keep class com.arslandaim.omegaplayer.di.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.Provides *;
}

-keep class coil.decode.VideoFrameDecoder { *; }
-keep class coil.decode.VideoFrameDecoder$Factory { *; }

-keep class androidx.biometric.** { *; }
-keepclassmembers class * extends androidx.biometric.BiometricPrompt$AuthenticationCallback { *; }

-keep class androidx.datastore.preferences.** { *; }
