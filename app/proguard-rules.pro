-keep class * extends androidx.room.RoomDatabase
-keep class com.mohamedaminelouati.oplayer.data.** { *; }

-keep class com.mohamedaminelouati.oplayer.service.PlaybackService { *; }
-keep class androidx.media3.session.MediaSessionService { *; }
-keep class androidx.media3.session.MediaSession { *; }
-keep class androidx.media3.exoplayer.ExoPlayer { *; }
-keep class androidx.media3.ui.PlayerView { *; }
-keepclassmembers class * extends androidx.media3.common.Player$Listener { *; }

-keep class * extends dagger.hilt.internal.UnsafeCasts
-keep class **_HiltModules* { *; }
-keep class com.mohamedaminelouati.oplayer.di.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.Provides *;
}

-keep class coil.decode.VideoFrameDecoder { *; }
-keep class coil.decode.VideoFrameDecoder$Factory { *; }



-keep class androidx.datastore.preferences.** { *; }

-keep class com.mohamedaminelouati.oplayer.OMediaPlayerApp { *; }
-keep class com.mohamedaminelouati.oplayer.MainActivity { *; }

