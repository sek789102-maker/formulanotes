package com.sek789102.formulabook.ui.components

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sek789102.formulabook.R

/**
 * Shows what can stop exam reminders from arriving on time and links straight to the
 * matching system settings page. Re-checks every time the user comes back to the app.
 */
@Composable
fun ReminderReliabilityCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refreshKey++
        onPauseOrDispose { }
    }

    val exactOk = remember(refreshKey) { canScheduleExact(context) }
    val batteryOk = remember(refreshKey) { isIgnoringBatteryOptimizations(context) }
    val aggressiveOem = remember { isAggressiveOem() }

    if (exactOk && batteryOk && !aggressiveOem) return

    Surface(
        modifier = modifier.fillMaxWidth().testTag("reminder_reliability_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_reliability_title),
                style = MaterialTheme.typography.titleSmall
            )
            if (!exactOk) {
                FixRow(
                    text = stringResource(R.string.reminder_reliability_exact),
                    action = stringResource(R.string.reminder_reliability_allow),
                    tag = "fix_exact_alarm"
                ) { openExactAlarmSettings(context) }
            }
            if (!batteryOk) {
                FixRow(
                    text = stringResource(R.string.reminder_reliability_battery),
                    action = stringResource(R.string.reminder_reliability_open),
                    tag = "fix_battery"
                ) { openAppDetails(context) }
            }
            if (aggressiveOem) {
                FixRow(
                    text = stringResource(R.string.reminder_reliability_oem),
                    action = stringResource(R.string.reminder_reliability_open),
                    tag = "fix_oem"
                ) { openAppDetails(context) }
            }
        }
    }
}

@Composable
private fun FixRow(text: String, action: String, tag: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onClick, modifier = Modifier.testTag(tag)) { Text(action) }
    }
}

private fun canScheduleExact(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return true
    return am.canScheduleExactAlarms()
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun isAggressiveOem(): Boolean {
    val m = Build.MANUFACTURER.orEmpty().lowercase()
    return listOf("xiaomi", "redmi", "poco", "oppo", "vivo", "realme", "oneplus", "huawei", "honor")
        .any { m.contains(it) }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }
    openAppDetails(context)
}

private fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
