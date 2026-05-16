package com.autosort.ui.screens

import android.app.Activity
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autosort.ui.theme.Primary
import com.autosort.ui.theme.StatusSuccess
import com.autosort.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val sourceFolder   by viewModel.sourceFolder.collectAsState()
    val googleEmail    by viewModel.googleEmail.collectAsState()
    val isGoogleLinked by viewModel.isGoogleLinked.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = resolveTreeUriToPath(uri)
            viewModel.updateSourceFolder(path)
        }
    }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // V-03: Always pass result — handleSignInResult has try-catch for error codes
        viewModel.handleGoogleSignInResult(result.data)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Settings",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Monitored Folder ──────────────────────────────────────────
            Text(
                text          = "MONITORED FOLDER",
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.SemiBold,
                color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { folderPickerLauncher.launch(null) },
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Folder,
                            contentDescription = null,
                            tint               = Primary,
                            modifier           = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Source Folder",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text     = sourceFolder,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Text(
                text  = "Tap to change the folder that AutoSort monitors. The service will restart automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Spacer(Modifier.height(8.dp))

            // ── Google Drive ──────────────────────────────────────────────
            Text(
                text          = "GOOGLE DRIVE",
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.SemiBold,
                color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isGoogleLinked) StatusSuccess.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = if (isGoogleLinked) Icons.Default.Cloud
                                                 else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint               = if (isGoogleLinked) StatusSuccess
                                                 else MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = if (isGoogleLinked) "Connected" else "Not Connected",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text     = if (isGoogleLinked) googleEmail
                                       else "Sign in to upload files to Google Drive",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Sign In / Sign Out button
                if (isGoogleLinked) {
                    OutlinedButton(
                        onClick  = { viewModel.signOutGoogle() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Button(
                        onClick  = {
                            val intent = viewModel.getGoogleSignInIntent()
                            googleSignInLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Sign in with Google", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Status Info ───────────────────────────────────────────────
            Text(
                text          = "STATUS",
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.SemiBold,
                color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Info,
                            contentDescription = null,
                            tint               = StatusSuccess,
                            modifier           = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Service Status",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text     = "Active — watching for new files",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = StatusSuccess,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Converts a content:// tree URI to an absolute filesystem path.
 */
private fun resolveTreeUriToPath(uri: Uri): String {
    val lastSegment = uri.lastPathSegment ?: return uri.toString()
    return if (lastSegment.startsWith("primary:")) {
        val relativePath = lastSegment.removePrefix("primary:")
        "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
    } else {
        uri.path ?: uri.toString()
    }
}
