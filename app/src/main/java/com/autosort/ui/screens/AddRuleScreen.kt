package com.autosort.ui.screens

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.autosort.data.model.DestinationType
import com.autosort.data.model.RuleType
import com.autosort.ui.theme.Primary
import com.autosort.ui.theme.StatusSkipped
import com.autosort.ui.viewmodel.RuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleScreen(
    viewModel: RuleViewModel,
    editRuleId: String?,
    onBack: () -> Unit
) {
    val isEditMode = editRuleId != null

    var ruleId           by remember { mutableStateOf(editRuleId ?: "") }
    var name             by remember { mutableStateOf("") }
    var value            by remember { mutableStateOf("") }
    var selectedType     by remember { mutableStateOf(RuleType.CONTAINS) }
    var selectedDestType by remember { mutableStateOf(DestinationType.LOCAL) }
    var targetPath       by remember { mutableStateOf("") }
    var dropdownOpen     by remember { mutableStateOf(false) }
    var destDropdownOpen by remember { mutableStateOf(false) }

    var nameError  by remember { mutableStateOf(false) }
    var valueError by remember { mutableStateOf(false) }
    var pathError  by remember { mutableStateOf(false) }

    // Load existing rule when in edit mode
    if (isEditMode) {
        androidx.compose.runtime.LaunchedEffect(editRuleId) {
            val rule = viewModel.getRuleById(editRuleId!!)
            if (rule != null) {
                ruleId           = rule.id
                name             = rule.name
                value            = rule.value
                selectedType     = rule.type
                selectedDestType = rule.destinationType
                targetPath       = rule.target
            }
        }
    }

    // FolderPicker — resolves URI to real FS path under /storage/emulated/0/
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            targetPath = resolveUriToPath(uri)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = if (isEditMode) "Edit Rule" else "New Rule",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Rule Name ─────────────────────────────────────────────────
            SectionLabel("Rule Name")
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it; nameError = false },
                placeholder   = { Text("e.g. PDFs to Documents") },
                isError       = nameError,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = fieldColors()
            )
            AnimatedVisibility(visible = nameError) {
                Text("Name is required", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            // ── Rule Type Dropdown ────────────────────────────────────────
            SectionLabel("Match Type")
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { dropdownOpen = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = selectedType.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector        = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                DropdownMenu(
                    expanded         = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                    modifier         = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    RuleType.entries.forEach { type ->
                        DropdownMenuItem(
                            text    = {
                                Column {
                                    Text(type.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        text  = typeHint(type),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            onClick = {
                                selectedType = type
                                dropdownOpen = false
                            }
                        )
                    }
                }
            }

            // ── Match Value ───────────────────────────────────────────────
            SectionLabel("Match Value")
            OutlinedTextField(
                value         = value,
                onValueChange = { value = it; valueError = false },
                placeholder   = { Text(typeHint(selectedType)) },
                isError       = valueError,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = fieldColors()
            )
            AnimatedVisibility(visible = valueError) {
                Text("Match value is required", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            // ── Target Folder / Drive Path ───────────────────────────────
            if (selectedDestType == DestinationType.CLOUD_GDRIVE) {
                SectionLabel("Drive Folder Path")
                OutlinedTextField(
                    value         = targetPath,
                    onValueChange = { targetPath = it; pathError = false },
                    placeholder   = { Text("e.g. AutoSort/PDFs") },
                    isError       = pathError,
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = fieldColors()
                )
                Text(
                    text  = "Folders will be created automatically in your Google Drive",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            } else {
                SectionLabel("Target Folder")
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = if (pathError) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { folderPickerLauncher.launch(null) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Folder,
                        contentDescription = null,
                        tint               = Primary,
                        modifier           = Modifier.size(22.dp)
                    )
                    Text(
                        text  = targetPath.ifEmpty { "Tap to pick a folder…" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (targetPath.isEmpty())
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            AnimatedVisibility(visible = pathError) {
                Text("Target is required", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            // ── Destination Type ───────────────────────────────────────
            SectionLabel("Destination Type")
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { destDropdownOpen = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = destTypeLabel(selectedDestType),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector        = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                DropdownMenu(
                    expanded         = destDropdownOpen,
                    onDismissRequest = { destDropdownOpen = false },
                    modifier         = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DestinationType.entries.forEach { destType ->
                        val isAvailable = destType == DestinationType.LOCAL ||
                                          destType == DestinationType.CLOUD_GDRIVE
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text       = destTypeLabel(destType),
                                        fontWeight = FontWeight.Medium,
                                        color      = if (isAvailable)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            StatusSkipped
                                    )
                                    if (!isAvailable) {
                                        Text(
                                            text     = "Coming Soon",
                                            fontSize = 11.sp,
                                            color    = StatusSkipped
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (isAvailable) {
                                    selectedDestType = destType
                                    destDropdownOpen = false
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Save Button ───────────────────────────────────────────────
            Button(
                onClick = {
                    nameError  = name.isBlank()
                    valueError = value.isBlank()
                    pathError  = targetPath.isBlank()

                    if (!nameError && !valueError && !pathError) {
                        if (isEditMode) {
                            viewModel.updateRule(
                                com.autosort.data.model.Rule(
                                    id              = ruleId,
                                    name            = name.trim(),
                                    type            = selectedType,
                                    value           = value.trim(),
                                    target          = targetPath.trim(),
                                    destinationType = selectedDestType,
                                    active          = true
                                )
                            )
                        } else {
                            viewModel.addRule(name, selectedType, value, targetPath, selectedDestType)
                        }
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text       = if (isEditMode) "Update Rule" else "Save Rule",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = Color.White
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        letterSpacing = 1.sp
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor    = Primary,
    cursorColor          = Primary
)

private fun typeHint(type: RuleType): String = when (type) {
    RuleType.CONTAINS -> "e.g. invoice  (filename contains)"
    RuleType.ENDS     -> "e.g. .pdf  (filename ends with)"
    RuleType.STARTS   -> "e.g. IMG_  (filename starts with)"
    RuleType.REGEX    -> "e.g. \\d{4}-report\\.xlsx"
}

private fun destTypeLabel(type: DestinationType): String = when (type) {
    DestinationType.LOCAL         -> "📁 Local Folder"
    DestinationType.CLOUD_GDRIVE  -> "☁️ Google Drive"
    DestinationType.CLOUD_DROPBOX -> "📦 Dropbox"
    DestinationType.CLOUD_S3      -> "🪣 AWS S3"
}

/**
 * Converts a content:// tree URI returned by ACTION_OPEN_DOCUMENT_TREE into
 * an absolute filesystem path under /storage/emulated/0/.
 * Works for primary storage (which is where Downloads lives).
 *
 * URI last path segment format: "primary:DCIM/Camera"
 */
private fun resolveUriToPath(uri: Uri): String {
    // e.g. /tree/primary:Documents/Invoices  →  last segment = "primary:Documents/Invoices"
    val lastSegment = uri.lastPathSegment ?: return uri.toString()
    return if (lastSegment.startsWith("primary:")) {
        val relativePath = lastSegment.removePrefix("primary:")
        "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
    } else {
        // Non-primary storage: return raw path as best-effort
        uri.path ?: uri.toString()
    }
}
