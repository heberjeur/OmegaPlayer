package com.mohamedaminelouati.oplayer

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
import com.mohamedaminelouati.oplayer.ui.feature.library.HomeScreen
import com.mohamedaminelouati.oplayer.ui.navigation.Screen
import com.mohamedaminelouati.oplayer.ui.feature.player.VideoPlayerScreen
import com.mohamedaminelouati.oplayer.ui.feature.settings.SettingsScreen
import com.mohamedaminelouati.oplayer.ui.theme.OMediaPlayerTheme
import androidx.compose.runtime.saveable.rememberSaveable
import com.mohamedaminelouati.oplayer.data.SecurityLockType
import com.mohamedaminelouati.oplayer.ui.feature.security.AppLockScreen

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedaminelouati.oplayer.viewmodel.VideoViewModel
import com.mohamedaminelouati.oplayer.viewmodel.AudioViewModel
import com.mohamedaminelouati.oplayer.viewmodel.ThemeViewModel
import com.mohamedaminelouati.oplayer.ui.feature.player.AudioPlayerScreen
import com.mohamedaminelouati.oplayer.media.PlaybackConnection
import com.mohamedaminelouati.oplayer.ui.common.NowPlayingBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(ExperimentalSharedTransitionApi::class)
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var isPlayerActive = false
    private var isInPictureInPictureModeState by mutableStateOf(false)
    private var videoViewModel: VideoViewModel? = null
    private var audioViewModel: AudioViewModel? = null

    @Inject
    lateinit var playbackConnection: PlaybackConnection

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = com.mohamedaminelouati.oplayer.util.LocaleHelper.getSavedLanguage(newBase)
        com.mohamedaminelouati.oplayer.util.LocaleHelper.useIndianNumerals = com.mohamedaminelouati.oplayer.util.LocaleHelper.getSavedIndianNumerals(newBase)
        super.attachBaseContext(com.mohamedaminelouati.oplayer.util.LocaleHelper.createConfigurationContext(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val themeViewModel: ThemeViewModel = hiltViewModel()
            val mainVideoViewModel: VideoViewModel = hiltViewModel(this@MainActivity)
            val securityLockType by mainVideoViewModel.securityLockType.collectAsState()
            val securityAppLockEnabled by mainVideoViewModel.securityAppLockEnabled.collectAsState()

            var isAppUnlocked by rememberSaveable { mutableStateOf(false) }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, securityAppLockEnabled, securityLockType) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && securityAppLockEnabled && securityLockType != SecurityLockType.NONE) {
                        isAppUnlocked = false
                    }
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                        mainVideoViewModel.autoSync()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val appTheme by themeViewModel.theme.collectAsState()
            val dynamicColor by themeViewModel.dynamicColor.collectAsState()
            val textTruncationMode by themeViewModel.textTruncationMode.collectAsState()
            val savePositionOnQuit by themeViewModel.savePositionOnQuit.collectAsState()
            val darkPrimaryColor by themeViewModel.darkPrimaryColor.collectAsState()
            val darkSecondaryColor by themeViewModel.darkSecondaryColor.collectAsState()
            val lightPrimaryColor by themeViewModel.lightPrimaryColor.collectAsState()
            val lightSecondaryColor by themeViewModel.lightSecondaryColor.collectAsState()
            
            val isDarkTheme = when (appTheme) {
                com.mohamedaminelouati.oplayer.data.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.mohamedaminelouati.oplayer.data.AppTheme.LIGHT -> false
                com.mohamedaminelouati.oplayer.data.AppTheme.DARK -> true
            }

            val appLanguage by themeViewModel.appLanguage.collectAsState()
            val useIndianNumerals by themeViewModel.useIndianNumerals.collectAsState()
            LaunchedEffect(useIndianNumerals) {
                com.mohamedaminelouati.oplayer.util.LocaleHelper.useIndianNumerals = useIndianNumerals
            }
            val baseActivityContext = androidx.compose.ui.platform.LocalContext.current
            val localizedContext = remember(appLanguage, useIndianNumerals, baseActivityContext) {
                com.mohamedaminelouati.oplayer.util.LocaleHelper.getLocalizedContext(baseActivityContext, appLanguage)
            }
            val localizedConfiguration = remember(appLanguage, useIndianNumerals, baseActivityContext) {
                com.mohamedaminelouati.oplayer.util.LocaleHelper.getLocalizedConfiguration(baseActivityContext, appLanguage)
            }
            val layoutDirection = remember(appLanguage) {
                com.mohamedaminelouati.oplayer.util.LocaleHelper.getLayoutDirection(appLanguage)
            }

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides localizedConfiguration,
                androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
            ) {
                OMediaPlayerTheme(
                    appTheme = appTheme,
                    dynamicColor = dynamicColor,
                    textTruncationMode = textTruncationMode,
                    savePositionOnQuit = savePositionOnQuit,
                    darkPrimaryColor = androidx.compose.ui.graphics.Color(darkPrimaryColor),
                    darkSecondaryColor = androidx.compose.ui.graphics.Color(darkSecondaryColor),
                    lightPrimaryColor = androidx.compose.ui.graphics.Color(lightPrimaryColor),
                    lightSecondaryColor = androidx.compose.ui.graphics.Color(lightSecondaryColor)
                ) {
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

                    Box(modifier = Modifier.fillMaxSize()) {
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
                                    navDeepLink { uriPattern = "omediaplayer://main?tab={tab}" }
                                )
                            ) { backStackEntry ->
                                val tab = backStackEntry.arguments?.getString("tab")
                                isPlayerActive = false
                                
                                val homeTab = when(tab) {
                                    "videos" -> com.mohamedaminelouati.oplayer.ui.feature.library.MediaTab.VIDEOS
                                    "audios" -> com.mohamedaminelouati.oplayer.ui.feature.library.MediaTab.AUDIOS
                                    else -> null
                                }

                                val activity = this@MainActivity
                                val videoVM: VideoViewModel = hiltViewModel(activity)
                                val audioVM: AudioViewModel = hiltViewModel(activity)
                                videoViewModel = videoVM
                                audioViewModel = audioVM

                                MainScreen(
                                    videoVM, 
                                    audioVM,
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
                                val decodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeDecodeUri(encodedUri)
                                val initialPos = backStackEntry.arguments?.getLong("pos") ?: -1L
                                DisposableEffect(Unit) {
                                    isPlayerActive = true
                                    onDispose { isPlayerActive = false }
                                }
                                val fromParam = backStackEntry.arguments?.getString("from")
                                VideoPlayerScreen(
                                    videoUri = decodedUri, 
                                    from = fromParam,
                                    viewModel = videoViewModel!!,
                                    isDarkTheme = isDarkTheme,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    initialPosition = initialPos,
                                    isInPiPMode = isInPictureInPictureModeState,
                                    onBack = { 
                                        videoViewModel?.setSelectedFolder(null)
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
                                val decodedUri = com.mohamedaminelouati.oplayer.util.MediaUtils.safeDecodeUri(encodedUri)
                                val initialPos = backStackEntry.arguments?.getLong("pos") ?: -1L
                                
                                DisposableEffect(Unit) {
                                    isPlayerActive = false
                                    onDispose {}
                                }
                                val fromParam = backStackEntry.arguments?.getString("from")
                                AudioPlayerScreen(
                                    audioUri = decodedUri,
                                    from = fromParam,
                                    viewModel = hiltViewModel(this@MainActivity),
                                    initialPosition = initialPos,
                                    onBack = { 
                                        audioViewModel?.setSelectedFolder(null)
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

                    if (securityAppLockEnabled && securityLockType != SecurityLockType.NONE && !isAppUnlocked) {
                        AppLockScreen(
                            lockType = securityLockType,
                            videoViewModel = mainVideoViewModel,
                            onUnlocked = { isAppUnlocked = true }
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
                videoViewModel?.dispatchVolumeKeyEvent(keyCode)
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
        val autoPip = videoViewModel?.autoPip?.value ?: false
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
                        enterPictureInPictureMode(builder.build())
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
    initialTab: com.mohamedaminelouati.oplayer.ui.feature.library.MediaTab? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val keepScreenOnWhenPaused by videoViewModel.keepScreenOnWhenPaused.collectAsStateWithLifecycle()
    val currentMediaItem by playbackConnection.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by playbackConnection.isPlaying.collectAsStateWithLifecycle()
    var isSelectionActive by remember { mutableStateOf(false) }

    LaunchedEffect(currentMediaItem, isPlaying, keepScreenOnWhenPaused) {
        activity?.window?.let { window ->
            val shouldKeepOn = currentMediaItem != null && (isPlaying || keepScreenOnWhenPaused)
            if (shouldKeepOn) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = currentMediaItem != null && !isSelectionActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
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
            initialTab = initialTab,
            onSelectionModeChange = { isSelectionActive = it }
        )
    }
}
