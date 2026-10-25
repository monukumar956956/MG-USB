package com.example.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.HistoryEntity
import com.example.data.HistoryRepository
import com.example.domain.HardwareBenchmarkResult
import com.example.domain.PcToolGuideItem
import com.example.domain.SafFileItem
import com.example.domain.SafFileOperations
import com.example.domain.ZipEntryInfo
import com.example.media.AudioTrackItem
import com.example.media.MusicPlayerController
import com.example.media.MusicPlayerUiState
import com.example.util.MgFileCategory
import com.example.util.StorageAndCryptoUtils
import com.example.util.StorageStatsInfo
import com.example.util.UsbHardwareDeviceInfo
import com.example.util.VerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MgDestination(val title: String) {
    HOME("Dashboard"),
    FILES("USB File Manager"),
    STORAGE_TEST("Storage & SHA-256 Test"),
    MUSIC("Media3 Music Player"),
    BACKUP("Backup Utility"),
    ANDROID_TOOLS("Android APK & ZIP Tools"),
    PC_TOOLS("Windows PC Tools"),
    HISTORY("Action History"),
    SETTINGS("Settings & Security")
}

data class BreadcrumbNode(
    val name: String,
    val uri: Uri
)

data class SingleHashResult(
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String
)

data class MgAppUiState(
    val currentDestination: MgDestination = MgDestination.HOME,
    val backStack: List<MgDestination> = emptyList(),
    val isDarkTheme: Boolean = true,
    val phoneStorage: StorageStatsInfo = StorageAndCryptoUtils.getPhoneStorageStats(),
    val usbVolumeStorage: StorageStatsInfo? = null,
    val connectedUsbDevices: List<UsbHardwareDeviceInfo> = emptyList(),
    val selectedTreeUri: Uri? = null,
    val selectedTreeName: String? = null,
    val breadcrumbs: List<BreadcrumbNode> = emptyList(),
    val currentDirectoryFiles: List<SafFileItem> = emptyList(),
    val isLoadingFiles: Boolean = false,
    val fileManagerError: String? = null,
    val fileSearchQuery: String = "",
    val clipboardItem: SafFileItem? = null,
    val isClipboardMove: Boolean = false,
    val statusBannerMessage: String? = null,
    val isOperationInProgress: Boolean = false,
    val operationProgressText: String? = null,
    // Storage Test & SHA-256
    val singleHashResult: SingleHashResult? = null,
    val verifySourceUri: Uri? = null,
    val verifySourceName: String? = null,
    val verifyTargetUri: Uri? = null,
    val verifyTargetName: String? = null,
    val copyVerificationResult: VerificationResult? = null,
    val isRunningBenchmark: Boolean = false,
    val benchmarkStageText: String = "",
    val benchmarkResult: HardwareBenchmarkResult? = null,
    // Backup
    val backupTreeUri: Uri? = null,
    val backupTreeName: String? = null,
    val pendingBackupFiles: List<Pair<Uri, String>> = emptyList(),
    val isBackingUp: Boolean = false,
    val backupProgressFraction: Float = 0f,
    val backupResultSummary: String? = null,
    // Android Tools (ZIP Inspector)
    val inspectedZipName: String? = null,
    val inspectedZipUri: Uri? = null,
    val inspectedZipEntries: List<ZipEntryInfo> = emptyList(),
    val zipOperationMessage: String? = null,
    // Windows PC Tools
    val pcToolsReference: List<PcToolGuideItem> = SafFileOperations.builtInPcToolsReference
)

class MgUsbViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext
    private val preferences = AppPreferences(appContext)
    private val historyRepository = HistoryRepository(AppDatabase.getInstance(appContext).historyDao())
    val musicController = MusicPlayerController(appContext, viewModelScope)

    private val _uiState = MutableStateFlow(MgAppUiState())
    val uiState: StateFlow<MgAppUiState> = _uiState.asStateFlow()

    val musicUiState: StateFlow<MusicPlayerUiState> = musicController.uiState

    val historyItems: StateFlow<List<HistoryEntity>> = historyRepository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshHardwareAndStorageStatus()
        }
    }

    init {
        registerUsbReceiver()
        refreshHardwareAndStorageStatus()
        observePreferences()
    }

    private fun registerUsbReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            ContextCompat.registerReceiver(
                appContext,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {
            // Safe fallback in unit test environments
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.isDarkTheme.collect { dark ->
                _uiState.update { it.copy(isDarkTheme = dark) }
            }
        }
        viewModelScope.launch {
            val savedUsbUriStr = preferences.usbTreeUriString.firstOrNull()
            if (!savedUsbUriStr.isNullOrBlank()) {
                val uri = Uri.parse(savedUsbUriStr)
                val doc = DocumentFile.fromTreeUri(appContext, uri)
                if (doc != null && doc.exists() && doc.canRead()) {
                    onSafFolderSelected(uri, logToHistory = false)
                }
            }
            val savedBackupUriStr = preferences.backupTreeUriString.firstOrNull()
            if (!savedBackupUriStr.isNullOrBlank()) {
                val uri = Uri.parse(savedBackupUriStr)
                val doc = DocumentFile.fromTreeUri(appContext, uri)
                if (doc != null && doc.exists() && doc.canWrite()) {
                    _uiState.update {
                        it.copy(
                            backupTreeUri = uri,
                            backupTreeName = doc.name ?: "Backup Folder"
                        )
                    }
                }
            }
        }
    }

    fun navigateTo(destination: MgDestination) {
        _uiState.update { current ->
            if (current.currentDestination == destination) return@update current
            current.copy(
                currentDestination = destination,
                backStack = current.backStack + current.currentDestination,
                statusBannerMessage = null
            )
        }
    }

    fun navigateBack(): Boolean {
        val current = _uiState.value
        if (current.backStack.isEmpty()) {
            if (current.currentDestination != MgDestination.HOME) {
                _uiState.update { it.copy(currentDestination = MgDestination.HOME) }
                return true
            }
            return false
        }
        val previous = current.backStack.last()
        _uiState.update {
            it.copy(
                currentDestination = previous,
                backStack = current.backStack.dropLast(1)
            )
        }
        return true
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDarkTheme(enabled)
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun refreshHardwareAndStorageStatus() {
        val phoneStats = StorageAndCryptoUtils.getPhoneStorageStats()
        val usbDevices = StorageAndCryptoUtils.detectConnectedUsbDevices(appContext)
        val treeUri = _uiState.value.selectedTreeUri
        val usbVolStats = treeUri?.let { StorageAndCryptoUtils.getSafVolumeStorageStats(appContext, it) }

        _uiState.update {
            it.copy(
                phoneStorage = phoneStats,
                connectedUsbDevices = usbDevices,
                usbVolumeStorage = usbVolStats
            )
        }
    }

    fun onSafFolderSelected(treeUri: Uri, logToHistory: Boolean = true) {
        viewModelScope.launch {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    appContext.contentResolver.takePersistableUriPermission(treeUri, flags)
                } catch (_: SecurityException) {
                    // Persistable permission may not be supported by all custom providers
                }
                preferences.setUsbTreeUri(treeUri.toString())
                val rootDoc = DocumentFile.fromTreeUri(appContext, treeUri)
                if (rootDoc == null || !rootDoc.exists() || !rootDoc.canRead()) {
                    _uiState.update {
                        it.copy(
                            fileManagerError = "Selected USB folder is inaccessible or permission was denied."
                        )
                    }
                    return@launch
                }
                val rootName = rootDoc.name ?: "USB / Selected Folder"
                val volStats = StorageAndCryptoUtils.getSafVolumeStorageStats(appContext, treeUri)
                _uiState.update {
                    it.copy(
                        selectedTreeUri = treeUri,
                        selectedTreeName = rootName,
                        usbVolumeStorage = volStats,
                        breadcrumbs = listOf(BreadcrumbNode(rootName, rootDoc.uri)),
                        fileManagerError = null
                    )
                }
                loadDirectoryAtUri(rootDoc.uri)
                if (logToHistory) {
                    historyRepository.logAction(
                        actionType = "MOUNT_SAF",
                        title = "Selected Folder: $rootName",
                        details = "URI: $treeUri",
                        status = "SUCCESS"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(fileManagerError = "Error mounting SAF folder: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearSelectedSafFolder() {
        viewModelScope.launch {
            preferences.setUsbTreeUri(null)
            _uiState.update {
                it.copy(
                    selectedTreeUri = null,
                    selectedTreeName = null,
                    usbVolumeStorage = null,
                    breadcrumbs = emptyList(),
                    currentDirectoryFiles = emptyList(),
                    fileManagerError = null,
                    statusBannerMessage = "Disconnected SAF folder session."
                )
            }
        }
    }

    fun openSubDirectory(item: SafFileItem) {
        if (!item.isDirectory) return
        _uiState.update { state ->
            state.copy(
                breadcrumbs = state.breadcrumbs + BreadcrumbNode(item.name, item.uri)
            )
        }
        loadDirectoryAtUri(item.uri)
    }

    fun navigateToBreadcrumb(index: Int) {
        val currentCrumbs = _uiState.value.breadcrumbs
        if (index !in currentCrumbs.indices) return
        val targetCrumb = currentCrumbs[index]
        _uiState.update {
            it.copy(breadcrumbs = currentCrumbs.take(index + 1))
        }
        loadDirectoryAtUri(targetCrumb.uri)
    }

    fun refreshCurrentDirectory() {
        val currentCrumb = _uiState.value.breadcrumbs.lastOrNull()
        if (currentCrumb != null) {
            loadDirectoryAtUri(currentCrumb.uri)
        }
        refreshHardwareAndStorageStatus()
    }

    private fun resolveCurrentDirectoryDoc(): DocumentFile? {
        val rootTreeUri = _uiState.value.selectedTreeUri ?: return null
        val currentCrumbUri = _uiState.value.breadcrumbs.lastOrNull()?.uri ?: rootTreeUri
        return DocumentFile.fromTreeUri(appContext, currentCrumbUri)
    }

    private fun loadDirectoryAtUri(dirUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFiles = true, fileManagerError = null) }
            val dirDoc = DocumentFile.fromTreeUri(appContext, dirUri)
            if (dirDoc == null) {
                _uiState.update {
                    it.copy(
                        isLoadingFiles = false,
                        fileManagerError = "Unable to resolve folder URI."
                    )
                }
                return@launch
            }
            val result = SafFileOperations.listDirectory(appContext, dirDoc)
            result.fold(
                onSuccess = { files ->
                    _uiState.update {
                        it.copy(
                            isLoadingFiles = false,
                            currentDirectoryFiles = files,
                            fileManagerError = null
                        )
                    }
                    syncAudioFilesFromDirectory(files)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingFiles = false,
                            currentDirectoryFiles = emptyList(),
                            fileManagerError = err.localizedMessage ?: "Failed to list directory."
                        )
                    }
                }
            )
        }
    }

    private fun syncAudioFilesFromDirectory(files: List<SafFileItem>) {
        val audioTracks = files
            .filter { it.category == MgFileCategory.AUDIO && !it.isDirectory }
            .map {
                AudioTrackItem(
                    uri = it.uri,
                    title = it.name,
                    sizeBytes = it.sizeBytes,
                    mimeType = it.mimeType
                )
            }
        if (audioTracks.isNotEmpty() && musicUiState.value.playlist.isEmpty()) {
            musicController.setPlaylist(audioTracks, autoPlayIndex = -1)
        }
    }

    fun onAudioFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val newTracks = uris.map { uri ->
            val (name, size) = SafFileOperations.queryUriDisplayNameAndSize(appContext, uri)
            AudioTrackItem(
                uri = uri,
                title = name,
                sizeBytes = size.coerceAtLeast(0L),
                mimeType = appContext.contentResolver.getType(uri)
            )
        }
        musicController.setPlaylist(newTracks, autoPlayIndex = 0)
        viewModelScope.launch {
            historyRepository.logAction(
                actionType = "MUSIC_LOAD",
                title = "Loaded ${newTracks.size} Audio Track(s)",
                details = newTracks.joinToString(", ") { it.title },
                status = "SUCCESS"
            )
        }
    }

    fun setFileSearchQuery(query: String) {
        _uiState.update { it.copy(fileSearchQuery = query) }
    }

    fun createNewFolder(folderName: String) {
        val currentDir = resolveCurrentDirectoryDoc() ?: return
        viewModelScope.launch {
            val result = SafFileOperations.createFolder(currentDir, folderName)
            result.fold(
                onSuccess = { created ->
                    val name = created.name ?: folderName
                    _uiState.update { it.copy(statusBannerMessage = "Created folder '$name'") }
                    historyRepository.logAction("CREATE_FOLDER", "Created Folder: $name", "Location: ${currentDir.name ?: "Root"}")
                    refreshCurrentDirectory()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(statusBannerMessage = "Error: ${err.localizedMessage}") }
                }
            )
        }
    }

    fun renameItem(item: SafFileItem, newName: String) {
        val rootUri = _uiState.value.selectedTreeUri ?: return
        val doc = DocumentFile.fromTreeUri(appContext, item.uri) ?: DocumentFile.fromSingleUri(appContext, item.uri) ?: return
        viewModelScope.launch {
            val result = SafFileOperations.renameDocument(doc, newName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(statusBannerMessage = "Renamed '${item.name}' to '$newName'") }
                    historyRepository.logAction(
                        actionType = "RENAME",
                        title = "Renamed '${item.name}' -> '$newName'",
                        details = "Tree: $rootUri"
                    )
                    refreshCurrentDirectory()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(statusBannerMessage = "Rename failed: ${err.localizedMessage}") }
                }
            )
        }
    }

    fun deleteItemConfirmed(item: SafFileItem) {
        val doc = DocumentFile.fromTreeUri(appContext, item.uri) ?: DocumentFile.fromSingleUri(appContext, item.uri) ?: return
        viewModelScope.launch {
            val result = SafFileOperations.deleteDocument(doc)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(statusBannerMessage = "Deleted '${item.name}'") }
                    historyRepository.logAction(
                        actionType = "DELETE",
                        title = "Deleted ${if (item.isDirectory) "Folder" else "File"}: ${item.name}",
                        details = "Size: ${StorageAndCryptoUtils.formatBytes(item.sizeBytes)}"
                    )
                    refreshCurrentDirectory()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(statusBannerMessage = "Delete failed: ${err.localizedMessage}") }
                }
            )
        }
    }

    fun stageForCopyOrMove(item: SafFileItem, isMove: Boolean) {
        if (item.isDirectory) {
            _uiState.update { it.copy(statusBannerMessage = "Select individual files to copy or move across folders.") }
            return
        }
        _uiState.update {
            it.copy(
                clipboardItem = item,
                isClipboardMove = isMove,
                statusBannerMessage = "${if (isMove) "Ready to move" else "Copied to clipboard"}: ${item.name}. Navigate to destination folder and tap Paste."
            )
        }
    }

    fun clearClipboard() {
        _uiState.update { it.copy(clipboardItem = null, isClipboardMove = false) }
    }

    fun pasteClipboardIntoCurrentFolder() {
        val clip = _uiState.value.clipboardItem ?: return
        val isMove = _uiState.value.isClipboardMove
        val targetDir = resolveCurrentDirectoryDoc() ?: return
        val sourceDoc = DocumentFile.fromTreeUri(appContext, clip.uri)
            ?: DocumentFile.fromSingleUri(appContext, clip.uri)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationInProgress = true,
                    operationProgressText = "${if (isMove) "Moving" else "Copying"} ${clip.name}..."
                )
            }
            val result = SafFileOperations.copyUriToDirectory(
                context = appContext,
                sourceUri = clip.uri,
                targetDir = targetDir,
                deleteSourceAfterCopy = isMove,
                sourceDocForDelete = sourceDoc,
                onProgress = { copied, total ->
                    val pct = if (total > 0L) ((copied * 100) / total).toInt() else 0
                    _uiState.update {
                        it.copy(operationProgressText = "${if (isMove) "Moving" else "Copying"} ${clip.name} ($pct%)")
                    }
                }
            )
            result.fold(
                onSuccess = { destItem ->
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgressText = null,
                            clipboardItem = null,
                            isClipboardMove = false,
                            statusBannerMessage = "${if (isMove) "Moved" else "Copied"} '${destItem.name}' (${StorageAndCryptoUtils.formatBytes(destItem.sizeBytes)})"
                        )
                    }
                    historyRepository.logAction(
                        actionType = if (isMove) "MOVE" else "COPY",
                        title = "${if (isMove) "Moved" else "Copied"} ${destItem.name}",
                        details = "Destination: ${targetDir.name ?: "Folder"} (${StorageAndCryptoUtils.formatBytes(destItem.sizeBytes)})"
                    )
                    refreshCurrentDirectory()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgressText = null,
                            statusBannerMessage = "${if (isMove) "Move" else "Copy"} failed: ${err.localizedMessage}"
                        )
                    }
                    historyRepository.logAction(
                        actionType = if (isMove) "MOVE" else "COPY",
                        title = "Failed to ${if (isMove) "move" else "copy"} ${clip.name}",
                        details = err.localizedMessage ?: "I/O Error",
                        status = "FAILED"
                    )
                }
            )
        }
    }

    fun importExternalFilesIntoCurrentFolder(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val targetDir = resolveCurrentDirectoryDoc()
        if (targetDir == null) {
            _uiState.update { it.copy(statusBannerMessage = "Please select a USB/SAF folder first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationInProgress = true,
                    operationProgressText = "Importing ${uris.size} file(s)..."
                )
            }
            var successCount = 0
            uris.forEachIndexed { idx, uri ->
                _uiState.update {
                    it.copy(operationProgressText = "Copying file ${idx + 1} of ${uris.size}...")
                }
                val res = SafFileOperations.copyUriToDirectory(
                    context = appContext,
                    sourceUri = uri,
                    targetDir = targetDir
                )
                if (res.isSuccess) successCount++
            }
            _uiState.update {
                it.copy(
                    isOperationInProgress = false,
                    operationProgressText = null,
                    statusBannerMessage = "Imported $successCount / ${uris.size} file(s) into ${targetDir.name ?: "folder"}."
                )
            }
            historyRepository.logAction(
                actionType = "IMPORT_COPY",
                title = "Imported $successCount/${uris.size} file(s)",
                details = "Target folder: ${targetDir.name ?: "Selected Folder"}",
                status = if (successCount == uris.size) "SUCCESS" else "PARTIAL"
            )
            refreshCurrentDirectory()
        }
    }

    // --- Storage Test & SHA-256 Verification ---

    fun calculateSingleFileSha256(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationInProgress = true,
                    operationProgressText = "Calculating SHA-256 digest..."
                )
            }
            val (name, _) = SafFileOperations.queryUriDisplayNameAndSize(appContext, uri)
            val res = SafFileOperations.computeUriSha256(appContext, uri)
            res.fold(
                onSuccess = { (hash, size) ->
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgressText = null,
                            singleHashResult = SingleHashResult(name, size, hash)
                        )
                    }
                    historyRepository.logAction(
                        actionType = "CHECKSUM",
                        title = "SHA-256 Computed: $name",
                        details = "SHA-256: $hash (${StorageAndCryptoUtils.formatBytes(size)})"
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgressText = null,
                            statusBannerMessage = "Checksum error: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun selectVerificationFile(uri: Uri, isSource: Boolean) {
        val (name, _) = SafFileOperations.queryUriDisplayNameAndSize(appContext, uri)
        _uiState.update {
            if (isSource) {
                it.copy(verifySourceUri = uri, verifySourceName = name, copyVerificationResult = null)
            } else {
                it.copy(verifyTargetUri = uri, verifyTargetName = name, copyVerificationResult = null)
            }
        }
    }

    fun runTwoFileCopyVerification() {
        val src = _uiState.value.verifySourceUri ?: return
        val dst = _uiState.value.verifyTargetUri ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationInProgress = true,
                    operationProgressText = "Comparing file sizes & SHA-256 checksums..."
                )
            }
            val result = SafFileOperations.verifyTwoFilesBySha256(appContext, src, dst)
            result.fold(
                onSuccess = { verification ->
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgressText = null,
                            copyVerificationResult = verification
                        )
                    }
                    historyRepository.logAction(
                        actionType = "VERIFY_COPY",
                        title = "Copy Verification: ${if (verification.isMatch) "MATCH" else "MISMATCH"}",
                        details = verification.summary,
                        status = if (verification.isMatch) "SUCCESS" else "FAILED"
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgressText = null,
                            statusBannerMessage = "Verification failed: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun runConfirmedHardwareBenchmark() {
        val targetDir = resolveCurrentDirectoryDoc()
        if (targetDir == null) {
            _uiState.update {
                it.copy(statusBannerMessage = "Select a writable USB/SAF folder before running the hardware R/W test.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningBenchmark = true,
                    benchmarkStageText = "Starting hardware read/write test...",
                    benchmarkResult = null
                )
            }
            val result = SafFileOperations.runDedicatedReadWriteTest(
                context = appContext,
                targetDir = targetDir,
                onStageUpdate = { stage ->
                    _uiState.update { it.copy(benchmarkStageText = stage) }
                }
            )
            _uiState.update {
                it.copy(
                    isRunningBenchmark = false,
                    benchmarkStageText = "",
                    benchmarkResult = result
                )
            }
            historyRepository.logAction(
                actionType = "BENCHMARK",
                title = "Hardware R/W Test: ${if (result.success) "PASSED" else "FAILED"}",
                details = result.message,
                status = if (result.success) "SUCCESS" else "FAILED"
            )
            refreshCurrentDirectory()
        }
    }

    // --- Backup & History ---

    fun onBackupFolderSelected(treeUri: Uri) {
        viewModelScope.launch {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    appContext.contentResolver.takePersistableUriPermission(treeUri, flags)
                } catch (_: SecurityException) {}
                preferences.setBackupTreeUri(treeUri.toString())
                val doc = DocumentFile.fromTreeUri(appContext, treeUri)
                val name = doc?.name ?: "Backup Destination"
                _uiState.update {
                    it.copy(
                        backupTreeUri = treeUri,
                        backupTreeName = name,
                        backupResultSummary = "Destination folder set to '$name'."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(statusBannerMessage = "Could not set backup folder: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onFilesSelectedForBackup(uris: List<Uri>) {
        val items = uris.map { uri ->
            val (name, _) = SafFileOperations.queryUriDisplayNameAndSize(appContext, uri)
            uri to name
        }
        _uiState.update {
            it.copy(
                pendingBackupFiles = items,
                backupResultSummary = null
            )
        }
    }

    fun executeBackupJob() {
        val destTreeUri = _uiState.value.backupTreeUri ?: _uiState.value.selectedTreeUri
        if (destTreeUri == null) {
            _uiState.update { it.copy(statusBannerMessage = "Please select a backup destination folder first.") }
            return
        }
        val destDoc = DocumentFile.fromTreeUri(appContext, destTreeUri)
        if (destDoc == null || !destDoc.canWrite()) {
            _uiState.update { it.copy(statusBannerMessage = "Backup destination folder is not writable.") }
            return
        }
        val filesToBackup = _uiState.value.pendingBackupFiles
        if (filesToBackup.isEmpty()) {
            _uiState.update { it.copy(statusBannerMessage = "Select at least one file to back up.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBackingUp = true,
                    backupProgressFraction = 0f,
                    backupResultSummary = "Backing up ${filesToBackup.size} file(s)..."
                )
            }
            var succeeded = 0
            var failed = 0
            filesToBackup.forEachIndexed { index, (uri, _) ->
                val res = SafFileOperations.copyUriToDirectory(appContext, uri, destDoc)
                if (res.isSuccess) succeeded++ else failed++
                _uiState.update {
                    it.copy(backupProgressFraction = (index + 1).toFloat() / filesToBackup.size.toFloat())
                }
            }
            val summary = "Backup complete: $succeeded succeeded, $failed failed (Folder: ${destDoc.name ?: "Backup"})."
            _uiState.update {
                it.copy(
                    isBackingUp = false,
                    backupProgressFraction = 1f,
                    backupResultSummary = summary
                )
            }
            historyRepository.logAction(
                actionType = "BACKUP",
                title = "Backup Job ($succeeded/${filesToBackup.size} files)",
                details = summary,
                status = if (failed == 0) "SUCCESS" else "PARTIAL"
            )
            refreshCurrentDirectory()
        }
    }

    fun clearAllHistoryConfirmed() {
        viewModelScope.launch {
            historyRepository.clearAll()
            _uiState.update { it.copy(statusBannerMessage = "Action history cleared.") }
        }
    }

    // --- Android Tools (APK Installer & Safe ZIP Extractor) ---

    fun launchSystemApkInstaller(context: Context, apkUri: Uri, apkName: String) {
        try {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            viewModelScope.launch {
                historyRepository.logAction(
                    actionType = "APK_OPEN",
                    title = "Opened APK in System Installer: $apkName",
                    details = "Delegated to Android Package Installer with user permission."
                )
            }
        } catch (e: ActivityNotFoundException) {
            _uiState.update {
                it.copy(statusBannerMessage = "No system package installer available to open '$apkName'.")
            }
        } catch (e: SecurityException) {
            _uiState.update {
                it.copy(statusBannerMessage = "Android security blocked opening APK directly: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(statusBannerMessage = "Could not open APK: ${e.localizedMessage}")
            }
        }
    }

    fun inspectZipArchive(zipUri: Uri, zipNameOverride: String? = null) {
        viewModelScope.launch {
            val (queriedName, _) = SafFileOperations.queryUriDisplayNameAndSize(appContext, zipUri)
            val displayName = zipNameOverride ?: queriedName
            _uiState.update {
                it.copy(
                    inspectedZipName = displayName,
                    inspectedZipUri = zipUri,
                    zipOperationMessage = "Reading ZIP entries..."
                )
            }
            val res = SafFileOperations.listZipEntries(appContext, zipUri)
            res.fold(
                onSuccess = { entries ->
                    val unsafeCount = entries.count { !it.isPathSafe }
                    val msg = if (unsafeCount > 0) {
                        "Warning: Detected $unsafeCount unsafe path-traversal entry(s)! Extraction will block unsafe paths."
                    } else {
                        "Verified ${entries.size} entry(s) — All paths pass Zip-Slip security check."
                    }
                    _uiState.update {
                        it.copy(
                            inspectedZipEntries = entries,
                            zipOperationMessage = msg
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            inspectedZipEntries = emptyList(),
                            zipOperationMessage = "Failed to inspect ZIP: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun extractInspectedZipToCurrentFolder() {
        val zipUri = _uiState.value.inspectedZipUri ?: return
        val zipName = _uiState.value.inspectedZipName ?: "Archive.zip"
        val targetDir = resolveCurrentDirectoryDoc()
        if (targetDir == null) {
            _uiState.update {
                it.copy(statusBannerMessage = "Please select a destination USB/SAF folder before extracting.")
            }
            return
        }
        val subfolder = zipName.removeSuffix(".zip").removeSuffix(".ZIP") + "_extracted"
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationInProgress = true,
                    operationProgressText = "Extracting $zipName safely..."
                )
            }
            val res = SafFileOperations.extractZipSafely(appContext, zipUri, targetDir, subfolder)
            res.fold(
                onSuccess = { count ->
                    val msg = "Extracted $count file(s) into '$subfolder' with path-traversal protection."
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgressText = null,
                            zipOperationMessage = msg,
                            statusBannerMessage = msg
                        )
                    }
                    historyRepository.logAction(
                        actionType = "ZIP_EXTRACT",
                        title = "Extracted ZIP: $zipName",
                        details = msg
                    )
                    refreshCurrentDirectory()
                },
                onFailure = { err ->
                    val msg = "ZIP extraction blocked/failed: ${err.localizedMessage}"
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            operationProgressText = null,
                            zipOperationMessage = msg,
                            statusBannerMessage = msg
                        )
                    }
                    historyRepository.logAction(
                        actionType = "ZIP_EXTRACT",
                        title = "ZIP Extraction Failed: $zipName",
                        details = msg,
                        status = "FAILED"
                    )
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            appContext.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
        musicController.release()
    }
}
