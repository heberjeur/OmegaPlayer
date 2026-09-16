package com.arslandaim.omegaplayer.ui.feature.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.arslandaim.omegaplayer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.arslandaim.omegaplayer.data.RecentPlayback
import com.arslandaim.omegaplayer.ui.feature.library.MediaTab
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun HomeDashboard(
    selectedTab: MediaTab,
    onTabSelected: (MediaTab) -> Unit,
    showHistoryTab: Boolean = false
) {
    val availableTabs = remember(showHistoryTab) {
        if (showHistoryTab) MediaTab.entries else MediaTab.entries.filter { it != MediaTab.HISTORY }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availableTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val background by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "tabBg"
                    )
                    val contentColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "tabContent"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(tab) },
                        shape = RoundedCornerShape(24.dp),
                        color = background
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val tabLabel = when (tab) {
                                MediaTab.VIDEOS -> stringResource(R.string.tab_videos)
                                MediaTab.AUDIOS -> stringResource(R.string.tab_audios)
                                MediaTab.PLAYLISTS -> stringResource(R.string.tab_playlists)
                                MediaTab.HISTORY -> stringResource(R.string.tab_history)
                            }
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                fontSize = if (availableTabs.size > 3) 12.sp else 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentPlaybackSection(
    recentPlayback: List<RecentPlayback>,
    onVideoClick: (String, Long) -> Unit,
    onAudioClick: (String, Long) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.watch_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onViewAllClick) {
                Text(stringResource(R.string.view_all), color = MaterialTheme.colorScheme.primary)
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(recentPlayback, key = { it.uri }) { item ->
                RecentPlaybackItem(
                    item = item,
                    onClick = {
                        val encodedUri = URLEncoder.encode(item.uri, StandardCharsets.UTF_8.toString())
                        if (item.mediaType == "video") onVideoClick(encodedUri, item.position)
                        else onAudioClick(encodedUri, item.position)
                    }
                )
            }
        }
    }
}

@Composable
fun ModernOmegaIcon(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ω",
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.offset(y = (-1).dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ModernOmegaIconPreview() {
    MaterialTheme { Box(modifier = Modifier.padding(16.dp)) { ModernOmegaIcon() } }
}
