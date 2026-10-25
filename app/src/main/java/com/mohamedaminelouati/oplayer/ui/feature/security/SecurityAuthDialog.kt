package com.mohamedaminelouati.oplayer.ui.feature.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mohamedaminelouati.oplayer.R
import com.mohamedaminelouati.oplayer.data.SecurityLockType
import com.mohamedaminelouati.oplayer.viewmodel.VideoViewModel

@Composable
fun SecurityAuthDialog(
    lockType: SecurityLockType,
    videoViewModel: VideoViewModel,
    title: String = stringResource(R.string.locked_item_prompt),
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }

                SecurityAuthContent(
                    lockType = lockType,
                    videoViewModel = videoViewModel,
                    title = title,
                    onSuccess = onSuccess
                )
            }
        }
    }
}

@Composable
fun SecurityAuthContent(
    lockType: SecurityLockType,
    videoViewModel: VideoViewModel,
    title: String,
    subtitle: String? = null,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lockType) {
        if (lockType == SecurityLockType.NONE) {
            onSuccess()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (lockType) {
            SecurityLockType.PIN -> {
                PinLockView(
                    title = title,
                    subtitle = subtitle,
                    errorMessage = errorMessage,
                    onPinComplete = { pin ->
                        if (videoViewModel.verifyPin(pin)) {
                            errorMessage = null
                            onSuccess()
                        } else {
                            errorMessage = context.getString(R.string.incorrect_pin)
                        }
                    },
                    onClearError = { errorMessage = null }
                )
            }
            SecurityLockType.PATTERN -> {
                PatternLockView(
                    title = title,
                    subtitle = subtitle,
                    errorMessage = errorMessage,
                    onPatternComplete = { pattern ->
                        if (videoViewModel.verifyPattern(pattern)) {
                            errorMessage = null
                            onSuccess()
                        } else {
                            errorMessage = context.getString(R.string.incorrect_pattern)
                        }
                    },
                    onClearError = { errorMessage = null }
                )
            }
            SecurityLockType.NONE -> {
                onSuccess()
            }
        }
    }
}
