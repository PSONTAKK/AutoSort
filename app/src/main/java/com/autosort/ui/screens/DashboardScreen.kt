package com.autosort.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import com.autosort.service.AutoSortService
import com.autosort.receiver.BootReceiver
import com.autosort.data.config.AppConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autosort.data.model.Rule
import com.autosort.ui.theme.Primary
import com.autosort.ui.theme.StatusSuccess
import com.autosort.ui.viewmodel.RuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: RuleViewModel,
    onAddRule: () -> Unit,
    onEditRule: (String) -> Unit,
    onViewLogs: () -> Unit,
    onSettings: () -> Unit
) {
    val rules by viewModel.rules.collectAsState()
    val context = LocalContext.current
    val appConfig = remember { AppConfig(context) }
    
    // Auto-update UI when paused state changes
    var isPaused by remember { mutableStateOf(appConfig.isPaused()) }
    var showPauseMenu by remember { mutableStateOf(false) }

    fun setPause(hours: Int?) {
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = "com.autosort.ACTION_RESUME_SERVICE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (hours == null) {
            // Stop indefinitely
            appConfig.pauseUntil = Long.MAX_VALUE
            alarmManager.cancel(pendingIntent)
            context.stopService(Intent(context, AutoSortService::class.java))
        } else if (hours == 0) {
            // Resume now
            appConfig.pauseUntil = 0L
            alarmManager.cancel(pendingIntent)
            val serviceIntent = Intent(context, AutoSortService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            // Pause for duration
            val resumeTime = System.currentTimeMillis() + (hours * 60 * 60 * 1000L)
            appConfig.pauseUntil = resumeTime
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                resumeTime,
                60 * 1000L, // 1 min window
                pendingIntent
            )
            context.stopService(Intent(context, AutoSortService::class.java))
        }
        isPaused = appConfig.isPaused()
        showPauseMenu = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "AutoSort",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = onViewLogs) {
                        Icon(
                            imageVector        = Icons.Default.History,
                            contentDescription = "View Logs",
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, AutoSortService::class.java).apply {
                            action = "ACTION_FORCE_SCAN"
                        }
                        context.startService(intent)
                    }) {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = "Scan Now",
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector        = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Box {
                        IconButton(onClick = { showPauseMenu = true }) {
                            Icon(
                                imageVector        = Icons.Default.PowerSettingsNew,
                                contentDescription = "Power",
                                tint               = if (isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showPauseMenu,
                            onDismissRequest = { showPauseMenu = false }
                        ) {
                            if (isPaused) {
                                DropdownMenuItem(text = { Text("Resume Service") }, onClick = { setPause(0) })
                            } else {
                                DropdownMenuItem(text = { Text("Pause for 1 Hour") }, onClick = { setPause(1) })
                                DropdownMenuItem(text = { Text("Pause for 8 Hours") }, onClick = { setPause(8) })
                                DropdownMenuItem(text = { Text("Pause for 1 Day") }, onClick = { setPause(24) })
                                DropdownMenuItem(text = { Text("Pause for 2 Days") }, onClick = { setPause(48) })
                                DropdownMenuItem(text = { Text("Stop Indefinitely") }, onClick = { setPause(null) })
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onAddRule,
                containerColor    = Primary,
                contentColor      = Color.White,
                shape             = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Add Rule"
                )
            }
        }
    ) { innerPadding ->

        if (rules.isEmpty()) {
            EmptyRulesPlaceholder(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule     = rule,
                        onToggle = { viewModel.toggleActive(rule) },
                        onEdit   = { onEditRule(rule.id) },
                        onDelete = { viewModel.deleteRule(rule.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue  = if (rule.active)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label        = "cardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Type badge + name/target
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(label = rule.type.name)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = rule.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text     = "\"${rule.value}\"  →  ${rule.target}",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked         = rule.active,
                    onCheckedChange = { onToggle() },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor  = Color.White,
                        checkedTrackColor  = StatusSuccess
                    )
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = "Edit Rule",
                        tint               = Primary,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Delete Rule",
                        tint               = MaterialTheme.colorScheme.error,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Primary.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text     = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color    = Primary
        )
    }
}

@Composable
private fun EmptyRulesPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "📂",
                fontSize = 48.sp
            )
            Text(
                text     = "No rules yet",
                style    = MaterialTheme.typography.titleMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text     = "Tap + to create your first sort rule",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
