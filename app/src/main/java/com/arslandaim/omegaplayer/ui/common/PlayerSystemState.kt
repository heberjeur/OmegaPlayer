/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.ui.common

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.round

private const val TAG = "PlayerSystemState"

private const val DEFAULT_MAX_VOLUME = 15
private const val DEFAULT_VOLUME = 0.5f
private const val DEFAULT_BRIGHTNESS = 0.5f
private const val MIN_BRIGHTNESS = 0.01f
private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
private const val STREAM_MUTE_CHANGED_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION"

@Composable
fun rememberSystemAudioManager(): AudioManager? {
    val context = LocalContext.current
    return remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
}

@Composable
fun rememberSystemMaxVolume(audioManager: AudioManager?): Int =
    remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: DEFAULT_MAX_VOLUME
    }

@Composable
fun rememberInitialSystemVolume(audioManager: AudioManager?, maxVolume: Int): Float =
    remember(audioManager, maxVolume) {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)
        if (maxVolume > 0) (current.toFloat() / maxVolume).coerceIn(0f, 1f) else DEFAULT_VOLUME
    }

@Composable
fun rememberInitialSystemBrightness(): Float {
    val context = LocalContext.current
    return remember(context) {
        val windowBrightness = (context as? Activity)?.window?.attributes?.screenBrightness ?: -1f
        if (windowBrightness >= MIN_BRIGHTNESS) {
            windowBrightness.coerceIn(MIN_BRIGHTNESS, 1f)
        } else {
            try {
                val systemBrightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                (systemBrightness / 255f).coerceIn(MIN_BRIGHTNESS, 1f)
            } catch (_: Settings.SettingNotFoundException) {
                DEFAULT_BRIGHTNESS
            }
        }
    }
}

fun applySystemMusicVolume(audioManager: AudioManager?, volume: Float, maxVolume: Int) {
    val manager = audioManager ?: return
    if (maxVolume <= 0) return
    manager.setStreamVolume(AudioManager.STREAM_MUSIC, systemVolumeFor(volume, maxVolume), 0)
}

private fun systemVolumeFor(volume: Float, maxVolume: Int): Int =
    round(volume.coerceAtMost(1f) * maxVolume).toInt()

@Composable
fun SystemVolumeSyncEffect(
    audioManager: AudioManager?,
    volumeProvider: () -> Float,
    onVolumeChange: (Float) -> Unit,
    onVolumeBoostReset: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(context, audioManager) {
        val syncFromSystem = {
            audioManager?.let { manager ->
                val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (max > 0) {
                    val currentVolume = volumeProvider()
                    val systemVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (systemVolume != systemVolumeFor(currentVolume, max)) {
                        val newVolume = (systemVolume.toFloat() / max).coerceIn(0f, 1f)
                        if (currentVolume > 1f && newVolume < 1f) {
                            onVolumeBoostReset()
                            onVolumeChange(newVolume)
                        } else if (currentVolume <= 1f) {
                            onVolumeChange(newVolume)
                        }
                    }
                }
            }
        }

        syncFromSystem()

        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                syncFromSystem()
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to register volume observer", e)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                syncFromSystem()
            }
        }
        val filter = IntentFilter().apply {
            addAction(VOLUME_CHANGED_ACTION)
            addAction(STREAM_MUTE_CHANGED_ACTION)
        }
        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to register volume receiver", e)
        }

        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to unregister volume observer", e)
            }
            try {
                context.unregisterReceiver(receiver)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to unregister volume receiver", e)
            }
        }
    }
}
