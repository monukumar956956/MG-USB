package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MgDestination
import com.example.ui.MgUsbViewModel
import com.example.ui.components.MgStatusBanner
import com.example.ui.screens.AndroidToolsScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.PcToolsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageTestScreen
import com.example.ui.theme.MGUsbTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MgUsbMultiToolApp()
        }
    }
}

@Composable
fun MgUsbMultiToolApp(
    viewModel: MgUsbViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val musicState by viewModel.musicUiState.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // SAF Document Tree Launcher for main USB / Folder browser
    val safTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onSafFolderSelected(uri)
        }
    }

    // SAF Document Tree Launcher for Backup Destination Folder
    val backupTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onBackupFolderSelected(uri)
        }
    }

    // External File Import into Current SAF Folder
    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importExternalFilesIntoCurrentFolder(uris)
        }
    }

    // Single File SHA-256 Picker
    val singleSha256Picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.calculateSingleFileSha256(uri)
        }
    }

    // Two-File Copy Verification Pickers
    val verifySourcePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectVerificationFile(uri, isSource = true)
        }
    }

    val verifyTargetPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectVerificationFile(uri, isSource = false)
        }
    }

    // Audio Files Picker for Media3 Music Player
    val audioFilesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onAudioFilesPicked(uris)
        }
    }

    // Backup Source Files Picker
    val backupSourceFilesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onFilesSelectedForBackup(uris)
        }
    }

    // ZIP Archive Picker
    val zipArchivePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectZipArchive(uri)
        }
    }

    MGUsbTheme(darkTheme = uiState.isDarkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                MgBottomNavigationBar(
                    currentDestination = uiState.currentDestination,
                    onSelectDestination = { viewModel.navigateTo(it) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val bannerMsg = uiState.statusBannerMessage
                if (bannerMsg != null) {
                    MgStatusBanner(
                        message = bannerMsg,
                        onDismiss = { viewModel.dismissBanner() }
                    )
                }

                AnimatedContent(
                    targetState = uiState.currentDestination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition",
                    modifier = Modifier.weight(1f)
                ) { destination ->
                    when (destination) {
                        MgDestination.HOME -> HomeScreen(
                            uiState = uiState,
                            recentHistory = historyItems,
                            onNavigate = { viewModel.navigateTo(it) },
                            onSelectSafFolder = { safTreeLauncher.launch(null) },
                            onRefreshStatus = { viewModel.refreshHardwareAndStorageStatus() }
                        )

                        MgDestination.FILES -> FilesScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onSelectSafFolder = { safTreeLauncher.launch(null) },
                            onImportExternalFiles = { importFilesLauncher.launch(arrayOf("*/*")) },
                            onOpenSubDirectory = { viewModel.openSubDirectory(it) },
                            onBreadcrumbClick = { viewModel.navigateToBreadcrumb(it) },
                            onRefresh = { viewModel.refreshCurrentDirectory() },
                            onSearchQueryChange = { viewModel.setFileSearchQuery(it) },
                            onCreateFolder = { viewModel.createNewFolder(it) },
                            onRenameItem = { item, newName -> viewModel.renameItem(item, newName) },
                            onDeleteItemConfirmed = { viewModel.deleteItemConfirmed(it) },
                            onStageCopyOrMove = { item, isMove -> viewModel.stageForCopyOrMove(item, isMove) },
                            onPasteHere = { viewModel.pasteClipboardIntoCurrentFolder() },
                            onClearClipboard = { viewModel.clearClipboard() }
                        )

                        MgDestination.STORAGE_TEST -> StorageTestScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onPickSingleHashFile = { singleSha256Picker.launch(arrayOf("*/*")) },
                            onPickVerifySourceFile = { verifySourcePicker.launch(arrayOf("*/*")) },
                            onPickVerifyTargetFile = { verifyTargetPicker.launch(arrayOf("*/*")) },
                            onRunTwoFileVerification = { viewModel.runTwoFileCopyVerification() },
                            onRunConfirmedHardwareBenchmark = { viewModel.runConfirmedHardwareBenchmark() },
                            onSelectSafFolder = { safTreeLauncher.launch(null) }
                        )

                        MgDestination.MUSIC -> MusicScreen(
                            musicState = musicState,
                            onBack = { viewModel.navigateBack() },
                            onPickAudioFiles = { audioFilesPicker.launch(arrayOf("audio/*")) },
                            onSelectSafFolder = { safTreeLauncher.launch(null) },
                            onPlayTrackAt = { viewModel.musicController.playTrackAt(it) },
                            onTogglePlayPause = { viewModel.musicController.togglePlayPause() },
                            onSkipNext = { viewModel.musicController.skipNext() },
                            onSkipPrevious = { viewModel.musicController.skipPrevious() },
                            onSeekTo = { viewModel.musicController.seekTo(it) },
                            onVolumeChange = { viewModel.musicController.setVolume(it) }
                        )

                        MgDestination.BACKUP -> BackupScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onSelectBackupFolder = { backupTreeLauncher.launch(null) },
                            onPickFilesToBackup = { backupSourceFilesPicker.launch(arrayOf("*/*")) },
                            onExecuteBackup = { viewModel.executeBackupJob() }
                        )

                        MgDestination.ANDROID_TOOLS -> AndroidToolsScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onSelectSafFolder = { safTreeLauncher.launch(null) },
                            onPickZipFile = { zipArchivePicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                            onOpenApkInstaller = { apk -> viewModel.launchSystemApkInstaller(context, apk.uri, apk.name) },
                            onInspectZipItem = { zipItem -> viewModel.inspectZipArchive(zipItem.uri, zipItem.name) },
                            onExtractInspectedZip = { viewModel.extractInspectedZipToCurrentFolder() }
                        )

                        MgDestination.PC_TOOLS -> PcToolsScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() }
                        )

                        MgDestination.HISTORY -> HistoryScreen(
                            historyItems = historyItems,
                            onBack = { viewModel.navigateBack() },
                            onClearHistoryConfirmed = { viewModel.clearAllHistoryConfirmed() }
                        )

                        MgDestination.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onToggleDarkTheme = { viewModel.toggleDarkTheme(it) },
                            onDisconnectSafFolder = { viewModel.clearSelectedSafFolder() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MgBottomNavigationBar(
    currentDestination: MgDestination,
    onSelectDestination: (MgDestination) -> Unit
) {
    val navItems = listOf(
        Triple(MgDestination.HOME, "Home", Icons.Default.Home),
        Triple(MgDestination.FILES, "Files", Icons.Default.Folder),
        Triple(MgDestination.STORAGE_TEST, "Storage", Icons.Default.Speed),
        Triple(MgDestination.MUSIC, "Music", Icons.Default.LibraryMusic),
        Triple(MgDestination.ANDROID_TOOLS, "Tools", Icons.Default.Build)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        navItems.forEach { (dest, label, icon) ->
            val selected = currentDestination == dest ||
                (dest == MgDestination.ANDROID_TOOLS && currentDestination in listOf(
                    MgDestination.ANDROID_TOOLS,
                    MgDestination.PC_TOOLS,
                    MgDestination.BACKUP,
                    MgDestination.HISTORY,
                    MgDestination.SETTINGS
                ))
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectDestination(dest) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("bottom_nav_${dest.name.lowercase()}")
            )
        }
    }
}
