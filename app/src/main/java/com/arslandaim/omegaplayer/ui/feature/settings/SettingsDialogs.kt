package com.arslandaim.omegaplayer.ui.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslandaim.omegaplayer.R
import com.arslandaim.omegaplayer.data.MediaSortOrder
import com.arslandaim.omegaplayer.util.StartupTrace

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
fun SettingsStartupReportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val report = remember { StartupTrace.buildReport(context) }
    val reportPath = remember { StartupTrace.reportFile(context).absolutePath }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.startup_report_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.startup_report_file, reportPath),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = report,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("OmegaPlayer startup report", report))
                    Toast.makeText(context, context.getString(R.string.startup_report_copied), Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.action_copy)) }
                TextButton(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "OmegaPlayer startup report")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }) { Text(stringResource(R.string.action_share)) }
            }
        },
        dismissButton = {
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
        shape = RoundedCornerShape(24.dp)
    )
}
