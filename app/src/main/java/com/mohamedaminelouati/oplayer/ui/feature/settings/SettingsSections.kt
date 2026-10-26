package com.mohamedaminelouati.oplayer.ui.feature.settings

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.data.AppTheme
import com.mohamedaminelouati.oplayer.data.MediaSortOrder
import com.mohamedaminelouati.oplayer.data.PlaybackSpeedScope
import com.mohamedaminelouati.oplayer.data.BrightnessScope
import com.mohamedaminelouati.oplayer.data.SearchScope
import com.mohamedaminelouati.oplayer.data.TextTruncationMode
import com.mohamedaminelouati.oplayer.ui.feature.player.PlaybackSpeedSlider
import com.mohamedaminelouati.oplayer.ui.feature.player.MIN_PLAYBACK_SPEED
import com.mohamedaminelouati.oplayer.ui.feature.player.MAX_PLAYBACK_SPEED
import com.mohamedaminelouati.oplayer.ui.theme.PresetThemeColors
import com.mohamedaminelouati.oplayer.viewmodel.ThemeViewModel
import com.mohamedaminelouati.oplayer.viewmodel.VideoViewModel
import kotlin.math.abs
import kotlin.math.round

@Composable
fun SettingIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = tint.copy(alpha = 0.1f)
) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SettingHeader(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        SettingIcon(imageVector = icon)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceSection(themeViewModel: ThemeViewModel) {
    val currentTheme by themeViewModel.theme.collectAsState()
    val dynamicColorEnabled by themeViewModel.dynamicColor.collectAsState()
    val folderFlattenThreshold by themeViewModel.folderFlattenThreshold.collectAsState()
    val textTruncationMode by themeViewModel.textTruncationMode.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingHeader(
                title = stringResource(R.string.app_theme),
                icon = Icons.Default.Palette
            )
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppTheme.entries.forEachIndexed { index, theme ->
                    val themeLabel = when (theme) {
                        AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                        AppTheme.LIGHT -> stringResource(R.string.theme_light)
                        AppTheme.DARK -> stringResource(R.string.theme_dark)
                    }
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        selected = currentTheme == theme,
                        onClick = { themeViewModel.setTheme(theme) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AppTheme.entries.size),
                        label = { Text(themeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.dynamic_color), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.dynamic_color_sub)) },
                    leadingContent = { SettingIcon(Icons.Default.AutoAwesome) },
                    trailingContent = {
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = { themeViewModel.setDynamicColor(it) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    AppLanguageCard()

    Spacer(modifier = Modifier.height(16.dp))

    ThemeColorsCard(themeViewModel = themeViewModel)

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingHeader(
                title = stringResource(R.string.setting_text_truncation),
                subtitle = stringResource(R.string.setting_text_truncation_sub),
                icon = Icons.AutoMirrored.Filled.ShortText
            )
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val truncationOptions = listOf(
                    TextTruncationMode.TWO_LINES to stringResource(R.string.text_truncation_two),
                    TextTruncationMode.THREE_LINES to stringResource(R.string.text_truncation_three),
                    TextTruncationMode.FULL_NAME to stringResource(R.string.text_truncation_full)
                )
                truncationOptions.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        selected = textTruncationMode == mode,
                        onClick = { themeViewModel.setTextTruncationMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = truncationOptions.size),
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            SettingHeader(
                title = stringResource(R.string.setting_folder_flatten_threshold),
                subtitle = stringResource(R.string.setting_folder_flatten_threshold_sub),
                icon = Icons.Default.AccountTree
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                @OptIn(ExperimentalMaterial3Api::class)
                Slider(
                    value = folderFlattenThreshold.toFloat(),
                    onValueChange = { themeViewModel.setFolderFlattenThreshold(it.toInt()) },
                    valueRange = 1f..15f,
                    steps = 13,
                    modifier = Modifier.weight(1f),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
                            ),
                            sliderState = sliderState,
                            modifier = Modifier.height(2.dp)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = folderFlattenThreshold.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(24.dp)
                )
            }
        }
    }
}

@Composable
fun AppLanguageCard() {
    var showDialog by remember { mutableStateOf(false) }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (!currentLocales.isEmpty) currentLocales[0]?.language ?: "" else ""

    val currentLabel = when (currentTag) {
        "en" -> stringResource(R.string.language_en)
        "fr" -> stringResource(R.string.language_fr)
        "ar" -> stringResource(R.string.language_ar)
        "es" -> stringResource(R.string.language_es)
        else -> stringResource(R.string.language_system_default)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingHeader(
                title = stringResource(R.string.setting_language),
                subtitle = stringResource(R.string.setting_language_sub),
                icon = Icons.Default.Language
            )
            Spacer(modifier = Modifier.height(12.dp))
            ListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDialog = true },
                headlineContent = {
                    Text(
                        text = currentLabel,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.setting_language_sub)
                    )
                },
                leadingContent = {
                    SettingIcon(Icons.Default.Translate)
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            )
        }
    }

    if (showDialog) {
        val languages = listOf(
            "" to stringResource(R.string.language_system_default),
            "en" to stringResource(R.string.language_en),
            "fr" to stringResource(R.string.language_fr),
            "ar" to stringResource(R.string.language_ar),
            "es" to stringResource(R.string.language_es)
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.setting_language)) },
            text = {
                Column {
                    languages.forEach { (tag, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showDialog = false
                                    val locales = if (tag.isEmpty()) {
                                        LocaleListCompat.getEmptyLocaleList()
                                    } else {
                                        LocaleListCompat.forLanguageTags(tag)
                                    }
                                    AppCompatDelegate.setApplicationLocales(locales)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentTag == tag) || (tag.isEmpty() && currentTag.isEmpty()),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeColorsCard(themeViewModel: ThemeViewModel) {
    val dynamicColorEnabled by themeViewModel.dynamicColor.collectAsState()
    val darkPrimary by themeViewModel.darkPrimaryColor.collectAsState()
    val darkSecondary by themeViewModel.darkSecondaryColor.collectAsState()
    val lightPrimary by themeViewModel.lightPrimaryColor.collectAsState()
    val lightSecondary by themeViewModel.lightSecondaryColor.collectAsState()

    var selectedModeIndex by remember { mutableIntStateOf(0) }
    var colorPaletteDialogFor by remember { mutableStateOf<String?>(null) }

    val isDark = selectedModeIndex == 0
    val activePrimary = if (isDark) darkPrimary else lightPrimary
    val activeSecondary = if (isDark) darkSecondary else lightSecondary

    if (colorPaletteDialogFor != null) {
        val targetColor = when (colorPaletteDialogFor) {
            "dark_primary" -> darkPrimary
            "dark_secondary" -> darkSecondary
            "light_primary" -> lightPrimary
            "light_secondary" -> lightSecondary
            else -> darkPrimary
        }
        ColorDegreePaletteDialog(
            initialColor = targetColor,
            onColorSelected = { colorInt ->
                if (dynamicColorEnabled) {
                    themeViewModel.setDynamicColor(false)
                }
                when (colorPaletteDialogFor) {
                    "dark_primary" -> themeViewModel.setDarkPrimaryColor(colorInt)
                    "dark_secondary" -> themeViewModel.setDarkSecondaryColor(colorInt)
                    "light_primary" -> themeViewModel.setLightPrimaryColor(colorInt)
                    "light_secondary" -> themeViewModel.setLightSecondaryColor(colorInt)
                }
            },
            onDismiss = { colorPaletteDialogFor = null }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingHeader(
                title = stringResource(R.string.theme_colors),
                subtitle = stringResource(R.string.theme_colors_sub),
                icon = Icons.Default.ColorLens
            )
            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    stringResource(R.string.mode_dark),
                    stringResource(R.string.mode_light)
                )
                modes.forEachIndexed { index, title ->
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        selected = selectedModeIndex == index,
                        onClick = { selectedModeIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        label = { Text(title, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.primary_color),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ColorSwatchRow(
                selectedColor = activePrimary,
                onColorSelected = { colorInt ->
                    if (dynamicColorEnabled) {
                        themeViewModel.setDynamicColor(false)
                    }
                    if (isDark) {
                        themeViewModel.setDarkPrimaryColor(colorInt)
                    } else {
                        themeViewModel.setLightPrimaryColor(colorInt)
                    }
                },
                onCustomClicked = {
                    colorPaletteDialogFor = if (isDark) "dark_primary" else "light_primary"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.secondary_color),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ColorSwatchRow(
                selectedColor = activeSecondary,
                onColorSelected = { colorInt ->
                    if (dynamicColorEnabled) {
                        themeViewModel.setDynamicColor(false)
                    }
                    if (isDark) {
                        themeViewModel.setDarkSecondaryColor(colorInt)
                    } else {
                        themeViewModel.setLightSecondaryColor(colorInt)
                    }
                },
                onCustomClicked = {
                    colorPaletteDialogFor = if (isDark) "dark_secondary" else "light_secondary"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { themeViewModel.resetThemeColors() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reset_default_colors))
            }
        }
    }
}

@Composable
fun ColorDegreePaletteDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) }
    }
    var currentHue by remember { mutableFloatStateOf(initialHsv[0]) }
    var currentSaturation by remember { mutableFloatStateOf(if (initialHsv[1] < 0.1f) 0.85f else initialHsv[1]) }
    var currentValue by remember { mutableFloatStateOf(if (initialHsv[2] < 0.1f) 0.85f else initialHsv[2]) }

    val baseHues = remember {
        listOf(
            16f to Color(0xFFFF5500),
            35f to Color(0xFFF59E0B),
            48f to Color(0xFFEAB308),
            84f to Color(0xFF84CC16),
            142f to Color(0xFF10B981),
            174f to Color(0xFF14B8A6),
            199f to Color(0xFF0EA5E9),
            217f to Color(0xFF3B82F6),
            239f to Color(0xFF6366F1),
            271f to Color(0xFF8B5CF6),
            330f to Color(0xFFEC4899),
            350f to Color(0xFFEF4444)
        )
    }

    val currentColorInt = remember(currentHue, currentSaturation, currentValue) {
        android.graphics.Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, currentValue))
    }
    val currentColor = Color(currentColorInt)

    val degreeValues = remember { listOf(0.98f, 0.88f, 0.76f, 0.64f, 0.52f, 0.40f, 0.28f, 0.18f) }
    val degreeSaturations = remember { listOf(0.35f, 0.55f, 0.75f, 0.90f, 0.96f, 1.0f, 1.0f, 1.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.color_degree_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = currentColor)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentColor.luminance() > 0.5f) Color.Black else Color.White
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.color_family_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(baseHues.size) { index ->
                        val (hue, col) = baseHues[index]
                        val isHueSelected = kotlin.math.abs(currentHue - hue) < 10f

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable {
                                    currentHue = hue
                                }
                                .then(
                                    if (isHueSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isHueSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (col.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.color_degrees_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(degreeValues.size) { index ->
                        val v = degreeValues[index]
                        val s = degreeSaturations[index]
                        val degreeColorInt = android.graphics.Color.HSVToColor(floatArrayOf(currentHue, s, v))
                        val degreeColor = Color(degreeColorInt)
                        val isDegreeSelected = kotlin.math.abs(currentValue - v) < 0.08f && kotlin.math.abs(currentSaturation - s) < 0.15f

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(degreeColor)
                                .clickable {
                                    currentSaturation = s
                                    currentValue = v
                                }
                                .then(
                                    if (isDegreeSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDegreeSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (degreeColor.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.color_degree_slider),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(currentValue * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = currentValue,
                        onValueChange = { currentValue = it },
                        valueRange = 0.15f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.color_saturation_slider),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(currentSaturation * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = currentSaturation,
                        onValueChange = { currentSaturation = it },
                        valueRange = 0.15f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentColorInt)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.action_set), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = AlertDialogDefaults.containerColor
    )
}

@Composable
private fun ColorSwatchRow(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    onCustomClicked: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(PresetThemeColors.size) { index ->
            val color = PresetThemeColors[index]
            val colorInt = color.toArgb()
            val isSelected = selectedColor == colorInt

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(colorInt) }
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { onCustomClicked() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = stringResource(R.string.color_degree_palette),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsPrivacySection(videoViewModel: VideoViewModel) {
    val isHistoryPaused by videoViewModel.isHistoryPaused.collectAsStateWithLifecycle()
    val showRecentHistoryOnHome by videoViewModel.showRecentHistoryOnHome.collectAsStateWithLifecycle()
    val showHistoryTab by videoViewModel.showHistoryTab.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.watch_history_setting), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(if (isHistoryPaused) stringResource(R.string.history_paused_sub) else stringResource(R.string.history_active_sub)) },
                leadingContent = { SettingIcon(Icons.Default.History) },
                trailingContent = {
                    Switch(
                        checked = !isHistoryPaused,
                        onCheckedChange = { videoViewModel.toggleHistoryPause(!it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_show_recent_history), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_show_recent_history_sub)) },
                leadingContent = { SettingIcon(Icons.Default.ViewCarousel) },
                trailingContent = {
                    Switch(
                        checked = showRecentHistoryOnHome,
                        onCheckedChange = { videoViewModel.toggleShowRecentHistoryOnHome(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_show_history_tab), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_show_history_tab_sub)) },
                leadingContent = { SettingIcon(Icons.Default.Tab) },
                trailingContent = {
                    Switch(
                        checked = showHistoryTab,
                        onCheckedChange = { videoViewModel.toggleShowHistoryTab(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaybackSection(themeViewModel: ThemeViewModel, videoViewModel: VideoViewModel) {
    val playerOrientation by themeViewModel.playerOrientation.collectAsState()
    val upNextFullyExpanded by themeViewModel.upNextFullyExpanded.collectAsState()
    val controlsTimeout by themeViewModel.controlsTimeout.collectAsState()
    val seekIncrement by themeViewModel.seekIncrement.collectAsState()
    val autoPlayNext by videoViewModel.autoPlayNext.collectAsStateWithLifecycle()
    val volumeBoostEnabled by videoViewModel.volumeBoostEnabled.collectAsStateWithLifecycle()
    val audioNormalizationEnabled by videoViewModel.audioNormalizationEnabled.collectAsStateWithLifecycle()
    val audioNormalizationDb by videoViewModel.audioNormalizationDb.collectAsStateWithLifecycle()
    val autoPip by videoViewModel.autoPip.collectAsStateWithLifecycle()
    val speedScope by videoViewModel.speedScope.collectAsStateWithLifecycle()
    val brightnessScope by videoViewModel.brightnessScope.collectAsStateWithLifecycle()
    val keepScreenOnWhenPaused by videoViewModel.keepScreenOnWhenPaused.collectAsStateWithLifecycle()
    val savePositionOnQuit by videoViewModel.savePositionOnQuit.collectAsStateWithLifecycle()
    val closeAfterQueueEnds by videoViewModel.closeAfterQueueEnds.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.dialog_reset_playback_positions_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.dialog_reset_playback_positions_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        videoViewModel.resetAllPlaybackPositions()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.action_reset), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = AlertDialogDefaults.containerColor
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingHeader(
                    title = stringResource(R.string.setting_player_orientation),
                    icon = Icons.Default.ScreenRotation
                )
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        stringResource(R.string.orientation_auto),
                        stringResource(R.string.orientation_sensor),
                        stringResource(R.string.orientation_fixed)
                    )
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = playerOrientation == index,
                            onClick = { themeViewModel.setPlayerOrientation(index) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_auto_play_next), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_auto_play_next_sub)) },
                leadingContent = { SettingIcon(Icons.Default.SkipNext) },
                trailingContent = {
                    Switch(
                        checked = autoPlayNext,
                        onCheckedChange = { videoViewModel.setAutoPlayNext(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_volume_boost), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_volume_boost_sub)) },
                leadingContent = { SettingIcon(Icons.AutoMirrored.Filled.VolumeUp) },
                trailingContent = {
                    Switch(
                        checked = volumeBoostEnabled,
                        onCheckedChange = { videoViewModel.toggleVolumeBoost(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_audio_normalization), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_audio_normalization_sub)) },
                leadingContent = { SettingIcon(Icons.Default.GraphicEq) },
                trailingContent = {
                    Switch(
                        checked = audioNormalizationEnabled,
                        onCheckedChange = { videoViewModel.toggleAudioNormalization(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            AnimatedVisibility(
                visible = audioNormalizationEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.setting_normalization_level),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "+${audioNormalizationDb.toInt()} dB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.setting_normalization_level_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "0 dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = audioNormalizationDb,
                            onValueChange = { videoViewModel.setAudioNormalizationDb(round(it)) },
                            valueRange = 0f..15f,
                            steps = 14,
                            modifier = Modifier.weight(1f),
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
                                    ),
                                    sliderState = sliderState,
                                    modifier = Modifier.height(2.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+15 dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_auto_pip), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_auto_pip_sub)) },
                leadingContent = { SettingIcon(Icons.Default.PictureInPictureAlt) },
                trailingContent = {
                    Switch(
                        checked = autoPip,
                        onCheckedChange = { videoViewModel.setAutoPip(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_up_next_expanded), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_up_next_expanded_sub)) },
                leadingContent = { SettingIcon(Icons.AutoMirrored.Filled.QueueMusic) },
                trailingContent = {
                    Switch(
                        checked = upNextFullyExpanded,
                        onCheckedChange = { themeViewModel.setUpNextFullyExpanded(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_save_position_on_quit), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_save_position_on_quit_sub)) },
                leadingContent = { SettingIcon(Icons.Default.Bookmark) },
                trailingContent = {
                    Switch(
                        checked = savePositionOnQuit,
                        onCheckedChange = { videoViewModel.setSavePositionOnQuit(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                modifier = Modifier.clickable { showResetDialog = true },
                headlineContent = { Text(stringResource(R.string.setting_reset_playback_positions), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_reset_playback_positions_sub)) },
                leadingContent = { SettingIcon(Icons.Default.RestartAlt) },
                trailingContent = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.setting_reset_playback_positions),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_close_after_queue_ends), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_close_after_queue_ends_sub)) },
                leadingContent = { SettingIcon(Icons.Default.PowerSettingsNew) },
                trailingContent = {
                    Switch(
                        checked = closeAfterQueueEnds,
                        onCheckedChange = { videoViewModel.setCloseAfterQueueEnds(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_keep_screen_on_when_paused), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(stringResource(R.string.setting_keep_screen_on_when_paused_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingContent = { SettingIcon(Icons.Default.WbSunny) },
                trailingContent = {
                    Switch(
                        checked = keepScreenOnWhenPaused,
                        onCheckedChange = { videoViewModel.setKeepScreenOnWhenPaused(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(modifier = Modifier.padding(16.dp)) {
                SettingHeader(
                    title = stringResource(R.string.setting_speed_scope),
                    subtitle = stringResource(R.string.setting_speed_scope_sub),
                    icon = Icons.Default.Speed
                )
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PlaybackSpeedScope.entries.forEachIndexed { index, scope ->
                        val label = when (scope) {
                            PlaybackSpeedScope.GLOBAL -> stringResource(R.string.speed_scope_global)
                            PlaybackSpeedScope.PER_FOLDER -> stringResource(R.string.speed_scope_per_folder)
                        }
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = speedScope == scope,
                            onClick = { videoViewModel.setSpeedScope(scope) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = PlaybackSpeedScope.entries.size),
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                SettingHeader(
                    title = stringResource(R.string.setting_brightness_scope),
                    subtitle = stringResource(R.string.setting_brightness_scope_sub),
                    icon = Icons.Default.BrightnessMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    BrightnessScope.entries.forEachIndexed { index, scope ->
                        val label = when (scope) {
                            BrightnessScope.NONE -> stringResource(R.string.brightness_scope_none)
                            BrightnessScope.GLOBAL -> stringResource(R.string.brightness_scope_global)
                            BrightnessScope.PER_FOLDER -> stringResource(R.string.brightness_scope_per_folder)
                        }
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = brightnessScope == scope,
                            onClick = { videoViewModel.setBrightnessScope(scope) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = BrightnessScope.entries.size),
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                SettingHeader(
                    title = stringResource(R.string.setting_controls_timeout),
                    subtitle = stringResource(R.string.setting_controls_timeout_sub),
                    icon = Icons.Default.Timer
                )
                Spacer(modifier = Modifier.height(12.dp))
                val timeoutOptions = listOf(0, 3, 5, 10)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    timeoutOptions.forEachIndexed { index, option ->
                        val label = if (option == 0) stringResource(R.string.controls_manual) else stringResource(R.string.controls_sec, option)
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = controlsTimeout == option,
                            onClick = { themeViewModel.setControlsTimeout(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = timeoutOptions.size),
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                SettingHeader(
                    title = stringResource(R.string.setting_seek_increment),
                    subtitle = stringResource(R.string.setting_seek_increment_sub),
                    icon = Icons.Default.FastForward
                )
                Spacer(modifier = Modifier.height(12.dp))
                val seekOptions = listOf(5, 10, 15, 30)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    seekOptions.forEachIndexed { index, option ->
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = seekIncrement == option,
                            onClick = { themeViewModel.setSeekIncrement(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = seekOptions.size),
                            label = { Text(stringResource(R.string.controls_sec, option), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDefaultsSection(videoViewModel: VideoViewModel, onOpenSortOrderDialog: () -> Unit) {
    val defaultSpeed by videoViewModel.defaultPlaybackSpeed.collectAsStateWithLifecycle()
    val defaultViewMode by videoViewModel.defaultViewMode.collectAsStateWithLifecycle()
    val defaultSortOrder by videoViewModel.defaultSortOrder.collectAsStateWithLifecycle()
    val defaultSearchScope by videoViewModel.defaultSearchScope.collectAsStateWithLifecycle()
    var currentSpeed by remember(defaultSpeed) {
        mutableFloatStateOf(round(defaultSpeed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED) * 100f) / 100f)
    }
    var showResetDialog by remember { mutableStateOf(false) }
    var clearFolderCustomizations by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingHeader(
                    title = stringResource(R.string.setting_default_speed),
                    subtitle = stringResource(R.string.setting_default_speed_sub),
                    icon = Icons.Default.Speed,
                    trailing = {
                        if (abs(currentSpeed - 1.0f) > 0.001f) {
                            IconButton(
                                onClick = {
                                    currentSpeed = 1.0f
                                    videoViewModel.setDefaultPlaybackSpeed(1.0f)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlaybackSpeedSlider(
                        speed = currentSpeed,
                        onSpeedChange = {
                            currentSpeed = it
                            videoViewModel.setDefaultPlaybackSpeed(it)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "%.2fx", currentSpeed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(56.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(modifier = Modifier.padding(16.dp)) {
                SettingHeader(
                    title = stringResource(R.string.setting_default_view_mode),
                    subtitle = stringResource(R.string.setting_default_view_mode_sub),
                    icon = Icons.Default.GridView,
                    trailing = {
                        if (defaultViewMode != 0) {
                            IconButton(
                                onClick = { videoViewModel.setDefaultViewMode(0) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = listOf(
                        0 to stringResource(R.string.view_mode_list),
                        1 to stringResource(R.string.view_mode_grid),
                        2 to stringResource(R.string.view_mode_card)
                    )
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = defaultViewMode == mode,
                            onClick = { videoViewModel.setDefaultViewMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            val currentSortLabel = MediaSortOrder.values().firstOrNull { it.name == defaultSortOrder }?.label ?: defaultSortOrder
            val isDefaultSort = defaultSortOrder == MediaSortOrder.DATE_DESC.name

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_default_sort_order), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(currentSortLabel) },
                leadingContent = { SettingIcon(Icons.AutoMirrored.Filled.Sort) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isDefaultSort) {
                            IconButton(
                                onClick = { videoViewModel.setDefaultSortOrder(MediaSortOrder.DATE_DESC.name) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        TextButton(onClick = { onOpenSortOrderDialog() }) {
                            Text(stringResource(R.string.action_filter))
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(modifier = Modifier.padding(16.dp)) {
                SettingHeader(
                    title = stringResource(R.string.setting_default_search_scope),
                    subtitle = stringResource(R.string.setting_default_search_scope_sub),
                    icon = Icons.Default.Search,
                    trailing = {
                        if (defaultSearchScope != SearchScope.CURRENT_TYPE) {
                            IconButton(
                                onClick = { videoViewModel.setDefaultSearchScope(SearchScope.CURRENT_TYPE) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = stringResource(R.string.reset_to_default),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val scopes = listOf(
                        SearchScope.CURRENT_FOLDER to stringResource(R.string.search_scope_current_folder),
                        SearchScope.CURRENT_TYPE to stringResource(R.string.search_scope_all_tab),
                        SearchScope.ALL_MEDIA to stringResource(R.string.search_scope_all_media)
                    )
                    scopes.forEachIndexed { index, (scope, label) ->
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = defaultSearchScope == scope,
                            onClick = { videoViewModel.setDefaultSearchScope(scope) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = scopes.size),
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reset_defaults_button))
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.reset_defaults_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reset_defaults_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { clearFolderCustomizations = !clearFolderCustomizations }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = clearFolderCustomizations,
                            onCheckedChange = { clearFolderCustomizations = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.reset_defaults_clear_folders),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        videoViewModel.resetDefaultPreferences(clearFolderCustomizations)
                        currentSpeed = 1.0f
                        showResetDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_reset),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
