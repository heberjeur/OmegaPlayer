/*
 * OmegaPlayer Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/

package com.arslandaim.omegaplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.arslandaim.omegaplayer.MainActivity
import com.arslandaim.omegaplayer.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        const val ACTION_REWIND = "com.arslandaim.omegaplayer.REWIND_10"
        const val ACTION_FAST_FORWARD = "com.arslandaim.omegaplayer.FAST_FORWARD_10"
        const val ACTION_SPEED = "com.arslandaim.omegaplayer.CYCLE_SPEED"
    }

    override fun onCreate() {
        super.onCreate()
        
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                50_000,
                1_500,
                3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val intent = Intent(this, MainActivity::class.java).apply {
            data = Uri.parse("omegaplayer://main?tab=audios")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val rewindButton = CommandButton.Builder()
            .setDisplayName("Rewind 10s")
            .setIconResId(R.drawable.ic_notif_replay_10)
            .setSessionCommand(SessionCommand(ACTION_REWIND, Bundle.EMPTY))
            .build()

        val forwardButton = CommandButton.Builder()
            .setDisplayName("Forward 10s")
            .setIconResId(R.drawable.ic_notif_forward_10)
            .setSessionCommand(SessionCommand(ACTION_FAST_FORWARD, Bundle.EMPTY))
            .build()

        val speedButton = CommandButton.Builder()
            .setDisplayName("Speed")
            .setIconResId(R.drawable.ic_speed)
            .setSessionCommand(SessionCommand(ACTION_SPEED, Bundle.EMPTY))
            .build()

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(ACTION_REWIND, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_FAST_FORWARD, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_SPEED, Bundle.EMPTY))
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(availableCommands)
                    .setCustomLayout(listOf(rewindButton, forwardButton, speedButton))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    ACTION_REWIND -> {
                        val pos = (player.currentPosition - 10000L).coerceAtLeast(0L)
                        player.seekTo(pos)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_FAST_FORWARD -> {
                        val duration = if (player.duration > 0L) player.duration else Long.MAX_VALUE
                        val pos = (player.currentPosition + 10000L).coerceAtMost(duration)
                        player.seekTo(pos)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SPEED -> {
                        val currentSpeed = player.playbackParameters.speed
                        val nextSpeed = when {
                            currentSpeed < 1.1f -> 1.25f
                            currentSpeed < 1.35f -> 1.5f
                            currentSpeed < 1.7f -> 2.0f
                            else -> 1.0f
                        }
                        player.setPlaybackSpeed(nextSpeed)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
        }

        val notificationProvider = object : DefaultMediaNotificationProvider(this) {
            override fun getMediaButtons(
                session: MediaSession,
                playerCommands: Player.Commands,
                customLayout: ImmutableList<CommandButton>,
                showWhenCompact: Boolean
            ): ImmutableList<CommandButton> {
                val prevButton = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .setIconResId(androidx.media3.ui.R.drawable.exo_notification_previous)
                    .setDisplayName(getString(R.string.action_previous))
                    .build()
                val rewindBtn = CommandButton.Builder()
                    .setSessionCommand(SessionCommand(ACTION_REWIND, Bundle.EMPTY))
                    .setIconResId(R.drawable.ic_notif_replay_10)
                    .setDisplayName("Rewind 10s")
                    .build()
                val playPauseBtn = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .setIconResId(if (session.player.isPlaying) androidx.media3.ui.R.drawable.exo_notification_pause else androidx.media3.ui.R.drawable.exo_notification_play)
                    .setDisplayName(if (session.player.isPlaying) getString(R.string.action_pause) else getString(R.string.action_play))
                    .build()
                val fwdBtn = CommandButton.Builder()
                    .setSessionCommand(SessionCommand(ACTION_FAST_FORWARD, Bundle.EMPTY))
                    .setIconResId(R.drawable.ic_notif_forward_10)
                    .setDisplayName("Forward 10s")
                    .build()
                val nextBtn = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                    .setIconResId(androidx.media3.ui.R.drawable.exo_notification_next)
                    .setDisplayName(getString(R.string.action_next))
                    .build()

                return if (showWhenCompact) {
                    ImmutableList.of(prevButton, playPauseBtn, nextBtn)
                } else {
                    ImmutableList.of(prevButton, rewindBtn, playPauseBtn, fwdBtn, nextBtn)
                }
            }
        }
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .setCustomLayout(listOf(rewindButton, forwardButton, speedButton))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession?.player?.pause()
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
