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
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaNotification
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionError
import com.arslandaim.omegaplayer.MainActivity
import com.arslandaim.omegaplayer.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject
import com.arslandaim.omegaplayer.media.EqManager
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime

import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.arslandaim.omegaplayer.data.ThemePreferences

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var eqManager: EqManager
    
    @Inject
    lateinit var themePreferences: ThemePreferences

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var notifShowPrev = true
    private var notifShowRewind = true
    private var notifShowForward = true
    private var notifShowNext = true
    private var notifShowSpeed = false
    private var notifShowStop = false
    private var notifShowClose = false
    private var notifShowRepeat = false
    private var notifShowShuffle = false
    private var autoPlayNext = true

    companion object {
        const val ACTION_SPEED = "com.arslandaim.omegaplayer.CYCLE_SPEED"
        const val ACTION_CLOSE = "com.arslandaim.omegaplayer.ACTION_CLOSE"
    }

    override fun onCreate() {
        super.onCreate()
        
        serviceScope.launch {
            themePreferences.autoPlayNext.collect { autoPlayNext = it }
        }

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
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: EventTime, audioSessionId: Int) {
                super.onAudioSessionIdChanged(eventTime, audioSessionId)
                eqManager.setupEqualizer(audioSessionId)
            }
        })
        
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !autoPlayNext) {
                    player.pause()
                }
            }
        })

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
            .setPlayerCommand(Player.COMMAND_SEEK_BACK)
            .build()

        val forwardButton = CommandButton.Builder()
            .setDisplayName("Forward 10s")
            .setIconResId(R.drawable.ic_notif_forward_10)
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .build()

        val speedButton = CommandButton.Builder()
            .setDisplayName("Speed")
            .setIconResId(R.drawable.ic_speed)
            .setSessionCommand(SessionCommand(ACTION_SPEED, Bundle.EMPTY))
            .build()
            
        val closeButton = CommandButton.Builder()
            .setDisplayName(getString(R.string.action_close))
            .setIconResId(R.drawable.ic_close)
            .setSessionCommand(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
            .build()
            
        val stopButton = CommandButton.Builder()
            .setDisplayName("Stop")
            .setIconResId(R.drawable.ic_stop)
            .setPlayerCommand(Player.COMMAND_STOP)
            .build()
            
        val repeatButton = CommandButton.Builder()
            .setDisplayName("Repeat")
            .setIconResId(R.drawable.ic_repeat)
            .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
            .build()
            
        val shuffleButton = CommandButton.Builder()
            .setDisplayName("Shuffle")
            .setIconResId(R.drawable.ic_shuffle)
            .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
            .build()

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(ACTION_SPEED, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
                    .build()

                val playerCommands = session.player.availableCommands.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(availableCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .setCustomLayout(listOf(rewindButton, forwardButton))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {

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
                    ACTION_CLOSE -> {
                        player.stop()
                        player.clearMediaItems()
                        stopSelf()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
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
                    .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                    .setIconResId(R.drawable.ic_notif_replay_10)
                    .setDisplayName(getString(R.string.action_rewind_10))
                    .build()
                val playPauseBtn = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .setIconResId(if (session.player.isPlaying) androidx.media3.ui.R.drawable.exo_notification_pause else androidx.media3.ui.R.drawable.exo_notification_play)
                    .setDisplayName(if (session.player.isPlaying) getString(R.string.action_pause) else getString(R.string.action_play))
                    .build()
                val fwdBtn = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                    .setIconResId(R.drawable.ic_notif_forward_10)
                    .setDisplayName(getString(R.string.action_forward_10))
                    .build()
                val nextBtn = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                    .setIconResId(androidx.media3.ui.R.drawable.exo_notification_next)
                    .setDisplayName(getString(R.string.action_next))
                    .build()

                val buttons = mutableListOf<CommandButton>()
                if (notifShowShuffle) buttons.add(shuffleButton)
                if (notifShowPrev) buttons.add(prevButton)
                if (notifShowRewind) buttons.add(rewindBtn)
                
                buttons.add(playPauseBtn)
                
                if (notifShowForward) buttons.add(fwdBtn)
                if (notifShowNext) buttons.add(nextBtn)
                if (notifShowRepeat) buttons.add(repeatButton)
                if (notifShowSpeed) buttons.add(speedButton)
                if (notifShowStop) buttons.add(stopButton)
                if (notifShowClose) buttons.add(closeButton)
                
                return ImmutableList.copyOf(buttons)
            }
        }
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .setCustomLayout(listOf(speedButton, closeButton))
            .build()
            
        serviceScope.launch { themePreferences.notifShowPrevious.collect { notifShowPrev = it; updateNotification() } }
        serviceScope.launch { themePreferences.notifShowRewind.collect { notifShowRewind = it; updateNotification() } }
        serviceScope.launch { themePreferences.notifShowForward.collect { notifShowForward = it; updateNotification() } }
        serviceScope.launch { themePreferences.notifShowNext.collect { notifShowNext = it; updateNotification() } }
        serviceScope.launch { themePreferences.notifShowSpeed.collect { notifShowSpeed = it; updateNotification() } }
        serviceScope.launch { themePreferences.notifShowStop.collect { notifShowStop = it; updateNotification() } }
        serviceScope.launch { themePreferences.notifShowClose.collect { notifShowClose = it; updateNotification() } }
        serviceScope.launch { themePreferences.notifShowRepeat.collect { notifShowRepeat = it; updateNotification() } }
        serviceScope.launch { themePreferences.notifShowShuffle.collect { notifShowShuffle = it; updateNotification() } }
    }

    private fun updateNotification() {
        mediaSession?.setCustomLayout(ImmutableList.of()) // Trigger a notification refresh
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
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        eqManager.release()
        super.onDestroy()
    }
}
