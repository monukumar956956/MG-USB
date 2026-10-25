package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.HistoryEntity
import com.example.domain.SafFileItem
import com.example.ui.MgAppUiState
import com.example.ui.components.MgConfirmDialog
import com.example.ui.components.MgScreenHeader
import com.example.util.MgFileCategory
import com.example.util.StorageAndCryptoUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    uiState: MgAppUiState,
    onBack: () -> Unit,
    onSelectBackupFolder: () -> Unit,
    onPickFilesToBackup: () -> Unit,
    onExecuteBackup: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        MgScreenHeader(
            title = "USB Backup Utility",
            subtitle = "Copy selected files to your chosen backup folder locally",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("backup_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "1. Destination Backup Folder",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = uiState.backupTreeName
                                        ?: uiState.selectedTreeName
                                        ?: "No backup folder selected yet",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onSelectBackupFolder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("select_backup_folder_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Backup Destination Folder")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "2. Select Source Files (${uiState.pendingBackupFiles.size} staged)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All backups run 100% locally between your phone and USB drive. Files are never uploaded to any remote server.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onPickFilesToBackup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_backup_files_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Files to Back Up")
                        }

                        if (uiState.pendingBackupFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            uiState.pendingBackupFiles.forEach { (_, name) ->
                                Text(
                                    text = "• $name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onExecuteBackup,
                            enabled = uiState.pendingBackupFiles.isNotEmpty() && !uiState.isBackingUp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("execute_backup_button")
                        ) {
                            Text(if (uiState.isBackingUp) "Backing Up..." else "Start Local Backup Copy")
                        }

                        if (uiState.isBackingUp || uiState.backupProgressFraction > 0f) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { uiState.backupProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }

                        if (uiState.backupResultSummary != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.backupResultSummary,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    historyItems: List<HistoryEntity>,
    onBack: () -> Unit,
    onClearHistoryConfirmed: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        MgScreenHeader(
            title = "Local Action History",
            subtitle = "On-device audit log with timestamps (Never uploaded)",
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${historyItems.size} logged event(s)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(
                onClick = { showClearConfirm = true },
                enabled = historyItems.isNotEmpty(),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("clear_history_button")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear History")
            }
        }

        if (historyItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No completed actions logged yet.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.actionType,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.status == "FAILED") {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                Text(
                                    text = dateFormat.format(Date(item.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        MgConfirmDialog(
            title = stringResource(R.string.clear_history_confirm_title),
            message = stringResource(R.string.clear_history_confirm_body),
            confirmText = "Clear All",
            isDestructive = true,
            onConfirm = {
                showClearConfirm = false
                onClearHistoryConfirmed()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
fun AndroidToolsScreen(
    uiState: MgAppUiState,
    onBack: () -> Unit,
    onSelectSafFolder: () -> Unit,
    onPickZipFile: () -> Unit,
    onOpenApkInstaller: (SafFileItem) -> Unit,
    onInspectZipItem: (SafFileItem) -> Unit,
    onExtractInspectedZip: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    val apkFiles = remember(uiState.currentDirectoryFiles) {
        uiState.currentDirectoryFiles.filter { it.category == MgFileCategory.APK }
    }
    val zipFiles = remember(uiState.currentDirectoryFiles) {
        uiState.currentDirectoryFiles.filter { it.category == MgFileCategory.ZIP }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MgScreenHeader(
            title = "Android APK & ZIP Tools",
            subtitle = "System APK Hand-off & Zip-Slip Protected Archive Manager",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("android_tools_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APK Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "APK Packages in Selected Folder (${apkFiles.size})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Opens with Android's official system installer — never installs silently",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        if (apkFiles.isEmpty()) {
                            Text(
                                text = if (uiState.selectedTreeUri == null) {
                                    "Select a USB drive or folder to scan for .apk packages."
                                } else {
                                    "No .apk files found in '${uiState.selectedTreeName}'. Navigate to a folder containing APKs in the File Manager."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = onSelectSafFolder) {
                                Text("Select Folder with APKs")
                            }
                        } else {
                            apkFiles.forEach { apk ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = apk.name,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = StorageAndCryptoUtils.formatBytes(apk.sizeBytes),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Button(onClick = { onOpenApkInstaller(apk) }) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("System Installer")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ZIP Inspector & Safe Extraction Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ZIP Archive Inspector & Safe Extractor",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Enforces strict Zip-Slip path-traversal protection ('../' blocked)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onPickZipFile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_zip_archive_button")
                        ) {
                            Text("Select Any .ZIP File to Inspect / Extract")
                        }

                        if (zipFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ZIP Archives in Current Folder:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            zipFiles.forEach { zipItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${zipItem.name} (${StorageAndCryptoUtils.formatBytes(zipItem.sizeBytes)})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(onClick = { onInspectZipItem(zipItem) }) {
                                        Text("Inspect")
                                    }
                                }
                            }
                        }

                        if (uiState.inspectedZipName != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Archive: ${uiState.inspectedZipName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (uiState.zipOperationMessage != null) {
                                Text(
                                    text = uiState.zipOperationMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.inspectedZipEntries.take(15).forEach { entry ->
                                Text(
                                    text = "${if (entry.isPathSafe) "[SAFE]" else "[BLOCKED]"} ${entry.name} (${StorageAndCryptoUtils.formatBytes(entry.uncompressedSize)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.isPathSafe) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            if (uiState.inspectedZipEntries.size > 15) {
                                Text(
                                    text = "...and ${uiState.inspectedZipEntries.size - 15} more entries",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onExtractInspectedZip,
                                enabled = uiState.selectedTreeUri != null && !uiState.isOperationInProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("extract_zip_button")
                            ) {
                                Text(
                                    if (uiState.selectedTreeUri == null) {
                                        "Select SAF Destination Folder First"
                                    } else {
                                        "Extract Safely to Current Folder"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PcToolsScreen(
    uiState: MgAppUiState,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val detectedPcFiles = remember(uiState.currentDirectoryFiles) {
        uiState.currentDirectoryFiles.filter { it.category == MgFileCategory.WINDOWS_EXE }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MgScreenHeader(
            title = "Windows PC Portable Tools",
            subtitle = "Manage PC utilities & documentation stored on your OTG USB drive",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("pc_tools_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Platform Architecture Notice
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Windows EXE Architecture Note",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Important Platform Notice (x86/x64 Windows vs. Android)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Windows executable (.EXE / .MSI) applications cannot run directly on Android. MG USB Multi-Tool helps you organize, verify (via SHA-256), and carry portable Windows utilities and bootable ISOs on your USB-C OTG drive for use when plugged into a Windows PC. This app does not claim to repair PC hardware or remove PC malware from Android.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Detected PC Executables / ISOs in Current USB Folder
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Detected PC Files on Current Folder (${detectedPcFiles.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (detectedPcFiles.isEmpty()) {
                            Text(
                                text = "No .exe, .msi, .iso, or .bat files detected in the currently open folder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            detectedPcFiles.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = StorageAndCryptoUtils.formatBytes(file.sizeBytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reference Documentation for USB Portable Toolkit
            item {
                Text(
                    text = "RECOMMENDED USB PORTABLE PC TOOLKIT DOCUMENTATION",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(uiState.pcToolsReference) { guide ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Computer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = guide.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = guide.typicalExecutable,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = guide.purpose,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PC Workflow: ${guide.usageOnPcInstructions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: MgAppUiState,
    onBack: () -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onDisconnectSafFolder: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        MgScreenHeader(
            title = "Settings, Security & About",
            subtitle = "Theme Controls, SAF Permission Status & Architecture",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dark Gaming Theme (Neon Green)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (uiState.isDarkTheme) {
                                    "Active: Obsidian Dark with Neon Green & Cyan highlights"
                                } else {
                                    "Active: High-Contrast Light Technical Theme"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isDarkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            modifier = Modifier.testTag("theme_toggle_switch")
                        )
                    }
                }
            }

            // Permission & SAF Access Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Permissions & Storage Access Status",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• Root Access Required: NO (100% Standard User-Space APIs)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Broad Storage Permission: NONE (Uses scoped Android SAF Document Trees)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Active SAF Folder: ${uiState.selectedTreeName ?: "None mounted"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Backup SAF Folder: ${uiState.backupTreeName ?: "Not configured"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Physical USB OTG Devices Detected: ${uiState.connectedUsbDevices.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (uiState.selectedTreeUri != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showDisconnectConfirm = true },
                                modifier = Modifier.testTag("disconnect_saf_button")
                            ) {
                                Text("Disconnect Active SAF Folder Session")
                            }
                        }
                    }
                }
            }

            // About & Privacy Guarantee
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "About MG USB Multi-Tool",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "App Name: MG USB Multi-Tool\nApplication ID: com.monugamer.mgusb\nVersion: 1.0.0 (Min SDK 26 / Android 8.0+)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Privacy & Safety Guarantee: No ads, no hidden analytics, no background servers, and no automatic drive formatting. All destructive file operations and physical write tests require explicit user confirmation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showDisconnectConfirm) {
        MgConfirmDialog(
            title = "Disconnect SAF Folder Session?",
            message = "This clears the saved folder reference in MG USB Multi-Tool. Your files on the USB drive or phone remain untouched.",
            confirmText = "Disconnect",
            isDestructive = false,
            onConfirm = {
                showDisconnectConfirm = false
                onDisconnectSafFolder()
            },
            onDismiss = { showDisconnectConfirm = false }
        )
    }
}
