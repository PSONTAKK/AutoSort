package com.autosort.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PsychologyAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autosort.R
import com.autosort.data.model.Rule
import com.autosort.ui.theme.Primary
import com.autosort.ui.theme.StatusSuccess
import com.autosort.ui.theme.StatusWarning
import com.autosort.ui.viewmodel.AiStep
import com.autosort.ui.viewmodel.AiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderInsightsSheet(
    rule: Rule,
    viewModel: AiViewModel,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Start analysis when the sheet opens
    LaunchedEffect(rule) {
        viewModel.startAnalysis(folderPath = rule.target, folderName = rule.name)
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null, // Custom header replaces the default drag handle
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        FolderInsightsContent(rule, viewModel, onDismissRequest)
    }
}

@Composable
private fun FolderInsightsContent(
    rule: Rule,
    viewModel: AiViewModel,
    onDismiss: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val sensitiveFiles by viewModel.sensitiveFiles.collectAsState()
    val excludedFiles by viewModel.excludedFiles.collectAsState()
    val folderInsights by viewModel.folderInsights.collectAsState()
    val deepInsights by viewModel.deepInsights.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Premium Gradient Header ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Primary, Primary.copy(alpha = 0.6f))
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.ai_folder_insights),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = rule.target.substringAfterLast("/").ifBlank { rule.target },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Step Progress Pills ────────────────────────────────────────────
        val steps = listOf(
            AiStep.SCANNING to stringResource(R.string.step_scanning),
            AiStep.FOLDER_ANALYZING to stringResource(R.string.step_folder),
            AiStep.FOLDER_RESULTS to stringResource(R.string.step_results)
        )
        val currentIndex = when (currentStep) {
            AiStep.IDLE, AiStep.SCANNING -> 0
            AiStep.FOLDER_ANALYZING -> 1
            AiStep.FOLDER_RESULTS -> 2
            AiStep.PRIVACY_PROMPT, AiStep.DEEP_ANALYZING, AiStep.RESULTS -> 2
            AiStep.ERROR -> steps.size // All grey
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, (_, label) ->
                val isCompleted = index < currentIndex
                val isActive = index == currentIndex

                val pillColor by animateColorAsState(
                    targetValue = when {
                        isCompleted -> StatusSuccess
                        isActive -> Primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    },
                    animationSpec = tween(500),
                    label = "pill_$index"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .background(pillColor)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = when {
                            isCompleted -> StatusSuccess
                            isActive -> Primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        },
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // ── Step Content ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 4 } + fadeOut()).using(
                        SizeTransform(clip = false)
                    )
                },
                label = "step_content"
            ) { step ->
                when (step) {
                    AiStep.IDLE, AiStep.SCANNING -> ScanningStateUI()
                    AiStep.FOLDER_ANALYZING -> FolderAnalyzingStateUI()
                    AiStep.FOLDER_RESULTS -> FolderResultsUI(
                        insights = folderInsights,
                        onDeepAnalyze = { viewModel.startDeepAnalysis() },
                        onDone = {
                            viewModel.reset()
                            onDismiss()
                        }
                    )
                    AiStep.PRIVACY_PROMPT -> PrivacyScannerUI(
                        sensitiveFiles = sensitiveFiles,
                        excludedFiles = excludedFiles,
                        onToggleExclude = { viewModel.toggleExclusion(it) },
                        onContinue = { viewModel.continueDeepAnalysis() }
                    )
                    AiStep.DEEP_ANALYZING -> DeepAnalyzingStateUI()
                    AiStep.RESULTS -> DeepResultsUI(
                        folderInsights = folderInsights,
                        deepInsights = deepInsights,
                        onDone = {
                            viewModel.reset()
                            onDismiss()
                        }
                    )
                    AiStep.ERROR -> ErrorStateUI(errorMessage = errorMessage, onDismiss = {
                        viewModel.reset()
                        onDismiss()
                    })
                }
            }
        }
    }
}

@Composable
private fun ScanningStateUI() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(color = Primary, modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.scanning_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.scanning_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FolderAnalyzingStateUI() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Default.PsychologyAlt,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.folder_analyzing_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.folder_analyzing_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FolderResultsUI(
    insights: String,
    onDeepAnalyze: () -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Success badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StatusSuccess.copy(alpha = 0.1f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.analysis_complete),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = StatusSuccess
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable folder insights card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = insights,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Deep Analysis button
        OutlinedButton(
            onClick = onDeepAnalyze,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.deep_analyze_button), fontWeight = FontWeight.Bold, color = Primary)
            }
        }
        Text(
            text = stringResource(R.string.deep_analyze_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(stringResource(R.string.done), fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun DeepAnalyzingStateUI() {
    val pulse = rememberInfiniteTransition(label = "deep_pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "deep_pulse_scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.deep_analyzing_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.deep_analyzing_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PrivacyScannerUI(
    sensitiveFiles: List<String>,
    excludedFiles: Set<String>,
    onToggleExclude: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Warning Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StatusWarning.copy(alpha = 0.12f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.privacy_shield_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = StatusWarning
                )
                Text(
                    text = stringResource(R.string.privacy_shield_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusWarning.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        sensitiveFiles.forEach { file ->
            val isExcluded = excludedFiles.contains(file)
            val rowColor by animateColorAsState(
                targetValue = if (isExcluded) MaterialTheme.colorScheme.surface
                              else Primary.copy(alpha = 0.06f),
                animationSpec = tween(300),
                label = "row_$file"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(rowColor)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = !isExcluded,
                    onCheckedChange = { onToggleExclude(file) },
                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = file,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.confirm_and_analyze), fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun DeepResultsUI(
    folderInsights: String,
    deepInsights: String,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Success badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StatusSuccess.copy(alpha = 0.1f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.deep_analysis_complete),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = StatusSuccess
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable deep insights card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = deepInsights,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(stringResource(R.string.done), fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ErrorStateUI(errorMessage: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.analysis_failed),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
        }
    }
}
