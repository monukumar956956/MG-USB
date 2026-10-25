package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
fun FilesScreen(
    uiState: MgAppUiState,
    onBack: () -> Unit,
    onSelectSafFolder: () -> Unit,
    onImportExternalFiles: () -> Unit,
    onOpenSubDirectory: (SafFileItem) -> Unit,
    onBreadcrumbClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameItem: (SafFileItem, String) -> Unit,
    onDeleteItemConfirmed: (SafFileItem) -> Unit,
    onStageCopyOrMove: (SafFileItem, Boolean) -> Unit,
    onPasteHere: () -> Unit,
    onClearClipboard: () -> Unit
) {
    BackHandler {
        if (uiState.breadcrumbs.size > 1) {
            onBreadcrumbClick(uiState.breadcrumbs.lastIndex - 1)
        } else {
            onBack()
        }
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var itemPendingRename by remember { mutableStateOf<SafFileItem?>(null) }
    var renameText by remember { mutableStateOf("") }

    var itemPendingDelete by remember { mutableStateOf<SafFileItem?>(null) }

    val filteredFiles = remember(uiState.currentDirectoryFiles, uiState.fileSearchQuery) {
        val q = uiState.fileSearchQuery.trim().lowercase(Locale.US)
        if (q.isEmpty()) {
            uiState.currentDirectoryFiles
        } else {
            uiState.currentDirectoryFiles.filter { it.name.lowercase(Locale.US).contains(q) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MgScreenHeader(
            title = "USB & SAF File Manager",
            subtitle = uiState.selectedTreeName ?: "Select a USB drive or folder via SAF",
            onBack = onBack
        )

        // Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSelectSafFolder,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("files_select_folder_button")
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (uiState.selectedTreeUri == null) "Select USB / Folder" else "Switch Folder")
            }

            if (uiState.selectedTreeUri != null) {
                OutlinedButton(
                    onClick = {
                        newFolderName = ""
                        showCreateFolderDialog = true
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("files_new_folder_button")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Create New Folder")
                }

                OutlinedButton(
                    onClick = onImportExternalFiles,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("files_import_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Copy external files here")
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("files_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh directory",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Clipboard Paste Banner
        if (uiState.clipboardItem != null && uiState.selectedTreeUri != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${if (uiState.isClipboardMove) "Move" else "Copy"} staged: ${uiState.clipboardItem.name}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Open target folder and tap Paste Here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Button(
                        onClick = onPasteHere,
                        modifier = Modifier.testTag("files_paste_button")
                    ) {
                        Text("Paste Here")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = onClearClipboard) {
                        Text("Cancel")
                    }
                }
            }
        }

        if (uiState.selectedTreeUri == null) {
            // Empty / Unmounted SAF State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No USB Drive or SAF Folder Selected",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MG USB Multi-Tool uses Android's Storage Access Framework (SAF) for safe, non-root access to USB-C OTG flash drives and device folders. Tap below to pick your USB drive or folder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSelectSafFolder,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Select USB Drive or Folder")
                        }
                    }
                }
            }
        } else {
            // Breadcrumbs
            if (uiState.breadcrumbs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.breadcrumbs.forEachIndexed { index, crumb ->
                        val isLast = index == uiState.breadcrumbs.lastIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLast) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.clickable { onBreadcrumbClick(index) }
                        ) {
                            Text(
                                text = crumb.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isLast) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        if (!isLast) {
                            Text(
                                text = " / ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search Input
            OutlinedTextField(
                value = uiState.fileSearchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("files_search_input"),
                placeholder = { Text("Filter files by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (uiState.isLoadingFiles || uiState.isOperationInProgress) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.operationProgressText ?: "Reading directory contents...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState.fileManagerError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = uiState.fileManagerError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (filteredFiles.isEmpty() && !uiState.isLoadingFiles) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.fileSearchQuery.isBlank()) {
                            "This folder is empty."
                        } else {
                            "No files match '${uiState.fileSearchQuery}'."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("files_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFiles, key = { it.uri.toString() }) { item ->
                        SafFileRowCard(
                            item = item,
                            onOpen = {
                                if (item.isDirectory) onOpenSubDirectory(item)
                            },
                            onCopy = { onStageCopyOrMove(item, false) },
                            onMove = { onStageCopyOrMove(item, true) },
                            onRename = {
                                itemPendingRename = item
                                renameText = item.name
                            },
                            onDelete = {
                                itemPendingDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateFolderDialog = false
                        onCreateFolder(newFolderName)
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    val targetRename = itemPendingRename
    if (targetRename != null) {
        AlertDialog(
            onDismissRequest = { itemPendingRename = null },
            title = { Text("Rename Item") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemPendingRename = null
                        onRenameItem(targetRename, renameText)
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemPendingRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    val targetDelete = itemPendingDelete
    if (targetDelete != null) {
        MgConfirmDialog(
            title = "Confirm Permanent Delete",
            message = "Are you sure you want to permanently delete '${targetDelete.name}' (${
                if (targetDelete.isDirectory) "Folder" else StorageAndCryptoUtils.formatBytes(targetDelete.sizeBytes)
            }) from the storage volume?",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                itemPendingDelete = null
                onDeleteItemConfirmed(targetDelete)
            },
            onDismiss = { itemPendingDelete = null }
        )
    }
}

@Composable
private fun SafFileRowCard(
    item: SafFileItem,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (item.category) {
        MgFileCategory.FOLDER -> Icons.Default.Folder
        MgFileCategory.APK -> Icons.Default.Android
        MgFileCategory.ZIP -> Icons.Default.FolderZip
        MgFileCategory.WINDOWS_EXE -> Icons.Default.Terminal
        MgFileCategory.AUDIO -> Icons.Default.AudioFile
        MgFileCategory.IMAGE -> Icons.Default.Image
        MgFileCategory.VIDEO -> Icons.Default.VideoFile
        MgFileCategory.DOCUMENT -> Icons.Default.Description
        MgFileCategory.OTHER -> Icons.Default.InsertDriveFile
    }
    val dateStr = if (item.lastModified > 0L) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(item.lastModified))
    } else {
        ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isDirectory, onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.category.label,
                        tint = if (item.isDirectory) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val metaText = if (item.isDirectory) {
                        "Directory • ${item.category.label}"
                    } else {
                        "${StorageAndCryptoUtils.formatBytes(item.sizeBytes)} • ${item.category.label}" +
                            if (dateStr.isNotEmpty()) " • $dateStr" else ""
                    }
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!item.isDirectory) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy ${item.name}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onMove, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = "Move ${item.name}",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = onRename, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename ${item.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${item.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
