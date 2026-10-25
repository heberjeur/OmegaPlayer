package com.mohamedaminelouati.oplayer.ui.feature.security

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinLockView(
    title: String,
    subtitle: String? = null,
    pinLength: Int = 4,
    errorMessage: String? = null,
    onPinComplete: (String) -> Unit,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            enteredPin = ""
            for (i in 0..2) {
                shakeOffset.animateTo(16f, tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-16f, tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, tween(50, easing = LinearEasing))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!subtitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.offset(x = shakeOffset.value.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < enteredPin.length
                val isError = !errorMessage.isNullOrEmpty()
                val dotColor by animateColorAsState(
                    targetValue = when {
                        isError -> MaterialTheme.colorScheme.error
                        isFilled -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    },
                    animationSpec = tween(150),
                    label = "dotColor"
                )
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isError -> MaterialTheme.colorScheme.error
                        isFilled -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    animationSpec = tween(150),
                    label = "borderColor"
                )

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(2.dp, borderColor, CircleShape)
                )
            }
        }

        if (!errorMessage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Spacer(modifier = Modifier.height(28.dp))
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (key in row) {
                        when (key) {
                            "" -> {
                                Spacer(modifier = Modifier.size(68.dp))
                            }
                            "DEL" -> {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            onClearError()
                                            if (enteredPin.isNotEmpty()) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            onClearError()
                                            if (enteredPin.length < pinLength) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val updated = enteredPin + key
                                                enteredPin = updated
                                                if (updated.length == pinLength) {
                                                    onPinComplete(updated)
                                                }
                                            }
                                        },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
