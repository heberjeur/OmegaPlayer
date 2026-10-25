package com.arslandaim.omegaplayer.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.data.MediaSortOrder

@Composable
fun SettingsAboutDeveloperDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_developer), fontWeight = FontWeight.Bold) },
        text = {
            Text(stringResource(R.string.developer_info))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun SettingsSortOrderDialog(
    currentSortOrder: String,
    onSortOrderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_default_sort_order), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                MediaSortOrder.entries.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOrderSelected(order.name) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOrder == order.name,
                            onClick = { onSortOrderSelected(order.name) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(order.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        dismissButton = if (currentSortOrder != MediaSortOrder.DATE_DESC.name) {
            {
                TextButton(onClick = { onSortOrderSelected(MediaSortOrder.DATE_DESC.name) }) {
                    Text(stringResource(R.string.reset_to_default))
                }
            }
        } else null,
        shape = RoundedCornerShape(24.dp)
    )
}
