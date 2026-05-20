package com.autosort.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
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
import android.content.ContextWrapper
import android.os.Build
import com.autosort.service.AutoSortService
import com.autosort.receiver.BootReceiver
import com.autosort.data.config.AppConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import com.autosort.ui.components.BannerAdView
import com.autosort.utils.AdManager
import com.autosort.data.config.ExternalConfig
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autosort.data.model.Rule
import com.autosort.ui.theme.Primary
import com.autosort.ui.theme.StatusSuccess
import com.autosort.ui.viewmodel.RuleViewModel
import com.autosort.ui.viewmodel.AiViewModel
import com.autosort.service.ai.SuggestionEngine.RuleSuggestion
import androidx.compose.ui.res.stringResource
import com.autosort.R

private fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: RuleViewModel,
    aiViewModel: AiViewModel,
    onAddRule: () -> Unit,
    onEditRule: (String) -> Unit,
    onViewLogs: () -> Unit,
    onSettings: () -> Unit
) {
    val rules by viewModel.rules.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoadingSuggestions by viewModel.isLoadingSuggestions.collectAsState()
    val context = LocalContext.current
    val appConfig = remember { AppConfig(context) }

    // Preload the rewarded ad for instant playback later
    LaunchedEffect(Unit) {
        AdManager.loadRewardedAd(context.applicationContext)
    }

    // Auto-update UI when paused state changes
    var isPaused by remember { mutableStateOf(appConfig.isPaused()) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var aiAnalyzeRule by remember { mutableStateOf<Rule?>(null) }

    fun setPause(hours: Int?) {
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? AlarmManager ?: return
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
            val resumeTime = System.currentTimeMillis() + (hours.toLong() * 60 * 60 * 1000)
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
                        text       = stringResource(R.string.app_title),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = onViewLogs) {
                        Icon(
                            imageVector        = Icons.Default.History,
                            contentDescription = stringResource(R.string.view_logs_desc),
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, AutoSortService::class.java).apply {
                            action = "ACTION_FORCE_SCAN"
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }) {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.scan_now),
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector        = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_desc),
                            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Box {
                        IconButton(onClick = { showPauseMenu = true }) {
                            Icon(
                                imageVector        = Icons.Default.PowerSettingsNew,
                                contentDescription = stringResource(R.string.power),
                                tint               = if (isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showPauseMenu,
                            onDismissRequest = { showPauseMenu = false }
                        ) {
                            if (isPaused) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.resume_service)) }, onClick = { setPause(0) })
                            } else {
                                DropdownMenuItem(text = { Text(stringResource(R.string.pause_1_hour)) }, onClick = { setPause(1) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.pause_8_hours)) }, onClick = { setPause(8) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.pause_1_day)) }, onClick = { setPause(24) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.pause_2_days)) }, onClick = { setPause(48) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.stop_indefinitely)) }, onClick = { setPause(null) })
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
                    contentDescription = stringResource(R.string.add_rule_fab)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (rules.isEmpty()) {
                EmptyRulesPlaceholder(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier            = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                // V3: Suggestions at the top
                if (isLoadingSuggestions) {
                    item { SuggestionLoadingCard() }
                } else {
                    items(suggestions, key = { it.name }) { suggestion ->
                        SuggestionCard(
                            suggestion = suggestion,
                            onAccept = {
                            viewModel.addRule(
                                name = suggestion.name,
                                type = suggestion.matchType,
                                value = suggestion.matchValue,
                                target = suggestion.suggestedFolder,
                                destinationType = com.autosort.data.model.DestinationType.LOCAL,
                                keepLocalAfterUpload = false
                            )
                            viewModel.dismissSuggestion(suggestion)
                        },
                        onDismiss = { viewModel.dismissSuggestion(suggestion) }
                    )
                }
                }

                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule     = rule,
                        onToggle = { viewModel.toggleActive(rule) },
                        onEdit   = { onEditRule(rule.id) },
                        onDelete = { viewModel.deleteRule(rule.id) },
                        onAiAnalyze = {
                            // Intercept the action with AdManager
                            context.findActivity()?.let { activity ->
                                AdManager.showRewardedAd(
                                    activity = activity,
                                    onRewardEarned = { aiAnalyzeRule = rule },
                                    onFailed = { aiAnalyzeRule = rule } // Fallback to allow usage
                                )
                            }
                        }
                    )
            }
            }
            }
            // ── Banner Ad Anchor ───────────────────────────────────────────
            BannerAdView(
                adUnitId = ExternalConfig.AdConfig.ID_BANNER_DASHBOARD,
                isEnabled = ExternalConfig.AdConfig.SHOW_DASHBOARD_BANNER
            )
        }
    }

    aiAnalyzeRule?.let { rule ->
        FolderInsightsSheet(
            rule = rule,
            viewModel = aiViewModel,
            onDismissRequest = { aiAnalyzeRule = null }
        )
    }
}

@Composable
private fun SuggestionCard(
    suggestion: RuleSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.15f),
                            Primary.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.smart_suggestion),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.suggestion_text, suggestion.emoji, suggestion.fileCount, suggestion.suggestedFolder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dismiss), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(stringResource(R.string.create_rule), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionLoadingCard() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.height(16.dp).width(120.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.2f)))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.height(14.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.2f)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.height(14.dp).width(200.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = 0.2f)))
        }
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAiAnalyze: () -> Unit
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
                IconButton(onClick = onAiAnalyze) {
                    Icon(
                        imageVector        = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(R.string.ai_analyze),
                        tint               = Primary,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_rule),
                        tint               = Primary,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_rule),
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
                text  = stringResource(R.string.no_rules_emoji),
                fontSize = 48.sp
            )
            Text(
                text     = stringResource(R.string.no_rules_text),
                style    = MaterialTheme.typography.titleMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text     = stringResource(R.string.no_rules_hint_text),
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

