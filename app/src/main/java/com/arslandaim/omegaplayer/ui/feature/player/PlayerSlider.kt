package com.arslandaim.omegaplayer.ui.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMicros
import com.arslandaim.omegaplayer.util.MediaUtils.formatDuration
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(
    videoUri: String,
    currentPosition: Long,
    duration: Long,
    pendingSeekPosition: Long,
    isDraggingSlider: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    textsBelow: Boolean = false
) {
    var debouncedSeekPosition by remember { mutableLongStateOf(0L) }

    LaunchedEffect(pendingSeekPosition, isDraggingSlider) {
        if (isDraggingSlider) {
            delay(100)
            debouncedSeekPosition = pendingSeekPosition
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        val sliderWidth = maxWidth

        if (isDraggingSlider && duration > 0) {
            val fraction = (pendingSeekPosition.toFloat() / duration).coerceIn(0f, 1f)
            val thumbnailWidth = 160.dp
            val halfThumb = thumbnailWidth / 2
            val rawOffset = (sliderWidth * fraction) - halfThumb
            val clampedOffset = rawOffset.coerceIn(0.dp, sliderWidth - thumbnailWidth)

            VideoThumbnailPreview(
                uri = videoUri,
                positionMs = debouncedSeekPosition,
                modifier = Modifier
                    .offset(x = clampedOffset, y = (-90).dp)
                    .align(Alignment.BottomStart)
                    .zIndex(10f)
            )
        }

        if (textsBelow) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (duration > 0) {
                        val pos = if (pendingSeekPosition >= 0) pendingSeekPosition else currentPosition
                        pos.toFloat() / duration
                    } else 0f,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    modifier = Modifier.fillMaxWidth(),
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
                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                            ),
                            sliderState = sliderState,
                            modifier = Modifier.height(2.dp)
                        )
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        color = Color.White.copy(alpha = 0.7f),
                        
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDuration(duration),
                        color = Color.White.copy(alpha = 0.7f),
                        
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    color = Color.White,
                    
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = if (duration > 0) {
                        val pos = if (pendingSeekPosition >= 0) pendingSeekPosition else currentPosition
                        pos.toFloat() / duration
                    } else 0f,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
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
                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                            ),
                            sliderState = sliderState,
                            modifier = Modifier.height(2.dp)
                        )
                    }
                )
                Text(
                    text = formatDuration(duration),
                    color = Color.White,
                    
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun VideoThumbnailPreview(
    uri: String,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(uri, positionMs) {
        ImageRequest.Builder(context)
            .data(uri)
            .videoFrameMicros(positionMs * 1000L)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .width(160.dp)
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.White, RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = formatDuration(positionMs),
            color = Color.White,
            
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
