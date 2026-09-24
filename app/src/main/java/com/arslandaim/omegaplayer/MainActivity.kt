package com.arslandaim.omegaplayer

import android.os.Bundle
import android.os.Build
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.arslandaim.omegaplayer.ui.feature.library.HomeScreen
import com.arslandaim.omegaplayer.ui.navigation.Screen
import com.arslandaim.omegaplayer.ui.feature.player.VideoPlayerScreen
import com.arslandaim.omegaplayer.ui.feature.settings.SettingsScreen
import com.arslandaim.omegaplayer.ui.theme.OmegaPlayerTheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import androidx.hilt.navigation.compose.hiltViewModel
import com.arslandaim.omegaplayer.viewmodel.VideoViewModel
import com.arslandaim.omegaplayer.viewmodel.AudioViewModel
import com.arslandaim.omegaplayer.viewmodel.ThemeViewModel
import com.arslandaim.omegaplayer.ui.feature.player.AudioPlayerScreen
import com.arslandaim.omegaplayer.media.PlaybackConnection
import com.arslandaim.omegaplayer.ui.common.NowPlayingBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(ExperimentalSharedTransitionApi::class)
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var isPlayerActive = false
    private var isInPictureInPictureModeState by mutableStateOf(false)
    private val videoViewModel: VideoViewModel by viewModels()

    @Inject
    lateinit var playbackConnection: PlaybackConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val audioViewModel: AudioViewModel = hiltViewModel()
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val appTheme by themeViewModel.theme.collectAsState()
            val dynamicColor by themeViewModel.dynamicColor.collectAsState()
            
            val isDarkTheme = when (appTheme) {
                com.arslandaim.omegaplayer.data.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.arslandaim.omegaplayer.data.AppTheme.LIGHT -> false
                com.arslandaim.omegaplayer.data.AppTheme.DARK -> true
            }

            OmegaPlayerTheme(appTheme = appTheme, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    DisposableEffect(Unit) {
                        val consumer = androidx.core.util.Consumer<Intent> { intent ->
                            navController.handleDeepLink(intent)
                        }
                        addOnNewIntentListener(consumer)
                        onDispose { removeOnNewIntentListener(consumer) }
                    }

                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Main.route
                        ) {
                            composable(
                                route = Screen.Main.route,
                                arguments = listOf(navArgument("tab") { 
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }),
                                deepLinks = listOf(
                                    navDeepLink { uriPattern = "omegaplayer://main?tab={tab}" }
                                )
                            ) { backStackEntry ->
                                val tab = backStackEntry.arguments?.getString("tab")
                                isPlayerActive = false
                                
                                val homeTab = when(tab) {
                                    "videos" -> com.arslandaim.omegaplayer.ui.feature.library.MediaTab.VIDEOS
                                    "audios" -> com.arslandaim.omegaplayer.ui.feature.library.MediaTab.AUDIOS
                                    else -> null
                                }

                                MainScreen(
                                    videoViewModel, 
                                    audioViewModel,
                                    playbackConnection,
                                    navController,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    isDarkTheme = isDarkTheme,
                                    initialTab = homeTab
                                )
                            }
                            composable(
                                route = Screen.Player.route,
                                arguments = listOf(
                                    navArgument("videoUri") { type = NavType.StringType },
                                    navArgument("from") { 
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("pos") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    }
                                ),
                                enterTransition = {
                                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                            scaleIn(initialScale = 0.95f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                            scaleOut(targetScale = 0.95f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                            scaleIn(initialScale = 0.95f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                                },
                                popExitTransition = {
                                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                            scaleOut(targetScale = 0.95f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                                }
                            ) { backStackEntry ->
                                val encodedUri = backStackEntry.arguments?.getString("videoUri") ?: ""
                                val decodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeDecodeUri(encodedUri)
                                val initialPos = backStackEntry.arguments?.getLong("pos") ?: -1L
                                DisposableEffect(Unit) {
                                    isPlayerActive = true
                                    onDispose { isPlayerActive = false }
                                }
                                val fromParam = backStackEntry.arguments?.getString("from")
                                VideoPlayerScreen(
                                    videoUri = decodedUri, 
                                    from = fromParam,
                                    viewModel = videoViewModel,
                                    isDarkTheme = isDarkTheme,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    initialPosition = initialPos,
                                    isInPiPMode = isInPictureInPictureModeState,
                                    onBack = { 
                                        if (!navController.popBackStack()) {
                                            navController.navigate(Screen.Main.createRoute("videos")) {
                                                popUpTo(Screen.Main.route) { inclusive = false }
                                            }
                                        }
                                    },
                                    onAudioTransition = { audioUri ->
                                        navController.navigate(Screen.AudioPlayer.createRoute(audioUri)) {
                                            popUpTo(Screen.Player.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(
                                route = Screen.AudioPlayer.route,
                                arguments = listOf(
                                    navArgument("audioUri") { type = NavType.StringType },
                                    navArgument("from") { 
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument("pos") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    }
                                )
                            ) { backStackEntry ->
                                val encodedUri = backStackEntry.arguments?.getString("audioUri") ?: ""
                                val decodedUri = com.arslandaim.omegaplayer.util.MediaUtils.safeDecodeUri(encodedUri)
                                val initialPos = backStackEntry.arguments?.getLong("pos") ?: -1L
                                
                                DisposableEffect(Unit) {
                                    isPlayerActive = false
                                    onDispose {}
                                }
                                val fromParam = backStackEntry.arguments?.getString("from")
                                AudioPlayerScreen(
                                    audioUri = decodedUri,
                                    from = fromParam,
                                    viewModel = audioViewModel,
                                    initialPosition = initialPos,
                                    onBack = { 
                                        if (!navController.popBackStack()) {
                                            navController.navigate(Screen.Main.createRoute("audios")) {
                                                popUpTo(Screen.Main.route) { inclusive = false }
                                            }
                                        }
                                    },
                                    onVideoTransition = { videoUri ->
                                        navController.navigate(Screen.Player.createRoute(videoUri)) {
                                            popUpTo(Screen.AudioPlayer.route) { inclusive = true }
                                        }
                                    },
                                    onAudioTransition = { newAudioUri ->
                                        navController.navigate(Screen.AudioPlayer.createRoute(newAudioUri)) {
                                            popUpTo(Screen.AudioPlayer.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(
                                route = Screen.Settings.route,
                                enterTransition = {
                                    fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                            slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(200, easing = FastOutSlowInEasing))
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                            slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(200, easing = FastOutSlowInEasing))
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                            slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(200, easing = FastOutSlowInEasing))
                                },
                                popExitTransition = {
                                    fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                            slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(200, easing = FastOutSlowInEasing))
                                }
                            ) {
                                SettingsScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (isPlayerActive) {
            if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
                videoViewModel.dispatchVolumeKeyEvent(keyCode)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPictureInPictureModeState = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val autoPip = videoViewModel.autoPip.value
        if (autoPip && isPlayerActive) {
            val player = playbackConnection.mediaController.value
            if (player != null && player.isPlaying) {
                val isVideo = player.videoSize.width > 0 || player.videoSize.height > 0
                if (isVideo) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val rational = if (player.videoSize.width > 0 && player.videoSize.height > 0) {
                            val aspect = player.videoSize.width.toFloat() / player.videoSize.height.toFloat()
                            if (aspect in 0.45f..2.35f) Rational(player.videoSize.width, player.videoSize.height) else Rational(16, 9)
                        } else Rational(16, 9)
                        val builder = PictureInPictureParams.Builder().setAspectRatio(rational)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            builder.setAutoEnterEnabled(true)
                        }
                        try {
                            enterPictureInPictureMode(builder.build())
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    videoViewModel: VideoViewModel,
    audioViewModel: AudioViewModel,
    playbackConnection: PlaybackConnection,
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isDarkTheme: Boolean,
    initialTab: com.arslandaim.omegaplayer.ui.feature.library.MediaTab? = null
) {
    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.9f else 0.95f))
            ) {
                NowPlayingBar(
                    playbackConnection = playbackConnection,
                    onAudioClick = { uri ->
                        navController.navigate(Screen.AudioPlayer.createRoute(uri))
                    },
                    onVideoClick = { uri ->
                        navController.navigate(Screen.Player.createRoute(uri))
                    }
                )
            }
        }
    ) { padding ->
        HomeScreen(
            viewModel = videoViewModel,
            audioViewModel = audioViewModel,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onVideoClick = { videoUri, pos, from ->
                navController.navigate(Screen.Player.createRoute(videoUri, from = from, pos = pos)) 
            },
            onAudioClick = { audioUri, pos, from ->
                navController.navigate(Screen.AudioPlayer.createRoute(audioUri, from = from, pos = pos))
            },
            onSettingsClick = { navController.navigate(Screen.Settings.route) },
            bottomPadding = padding.calculateBottomPadding(),
            isFocused = true,
            initialTab = initialTab
        )
    }
}
