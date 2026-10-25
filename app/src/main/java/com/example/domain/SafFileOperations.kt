package com.example.domain

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.util.MgFileCategory
import com.example.util.StorageAndCryptoUtils
import com.example.util.VerificationResult
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.Random
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SafFileItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val mimeType: String?,
    val category: MgFileCategory
)

data class ZipEntryInfo(
    val name: String,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val isDirectory: Boolean,
    val isPathSafe: Boolean
)

data class HardwareBenchmarkResult(
    val success: Boolean,
    val bytesTested: Long,
    val writeSpeedMbps: Double,
    val readSpeedMbps: Double,
    val expectedSha256: String,
    val actualSha256: String,
    val tempFileCleanedUp: Boolean,
    val message: String
)

data class PcToolGuideItem(
    val name: String,
    val category: String,
    val typicalExecutable: String,
    val purpose: String,
    val usageOnPcInstructions: String
)

object SafFileOperations {

    val builtInPcToolsReference: List<PcToolGuideItem> = listOf(
        PcToolGuideItem(
            name = "Ventoy Bootable USB Utility",
            category = "Bootable ISO Manager",
            typicalExecutable = "Ventoy2Disk.exe",
            purpose = "Creates a multi-boot USB drive where you can copy .ISO/.WIM/.IMG files directly without reformatting.",
            usageOnPcInstructions = "Store your .ISO images on the USB drive from Android, then connect the drive to a Windows/Linux PC to boot or run Ventoy2Disk.exe."
        ),
        PcToolGuideItem(
            name = "Rufus Portable",
            category = "USB OS Flasher",
            typicalExecutable = "rufus-portable.exe",
            purpose = "Formats and creates bootable USB flash drives from Windows or Linux installation ISOs on a Windows PC.",
            usageOnPcInstructions = "Keep rufus-portable.exe in your USB 'PC_Tools' folder for emergency OS recovery on Windows PCs. Cannot execute directly on Android."
        ),
        PcToolGuideItem(
            name = "H2testw / FakeFlashTest",
            category = "Desktop Flash Drive Capacity Tester",
            typicalExecutable = "h2testw.exe",
            purpose = "Windows utility for full-surface flash media testing to detect counterfeit capacity drives.",
            usageOnPcInstructions = "Run h2testw.exe on Windows, or use MG USB Multi-Tool's built-in 'Storage Test -> Hardware Read/Write Test' directly on Android."
        ),
        PcToolGuideItem(
            name = "CrystalDiskInfo Portable",
            category = "S.M.A.R.T. Drive Health Viewer",
            typicalExecutable = "DiskInfo64.exe",
            purpose = "Reads S.M.A.R.T. telemetry, temperature, and power-on hours for NVMe/SATA drives on Windows.",
            usageOnPcInstructions = "Extract the portable ZIP onto your USB drive using Android Tools -> ZIP Extractor, then launch DiskInfo64.exe on any Windows PC."
        ),
        PcToolGuideItem(
            name = "7-Zip Portable",
            category = "Archive Manager",
            typicalExecutable = "7zFM.exe",
            purpose = "High-ratio archiver for .7z, .zip, .rar, and .tar archives on Windows systems.",
            usageOnPcInstructions = "Carry alongside compressed driver packs on your OTG drive. For .zip archives on Android, use MG USB Multi-Tool's Android Tools tab."
        )
    )

    suspend fun listDirectory(context: Context, directoryDoc: DocumentFile): Result<List<SafFileItem>> =
        withContext(Dispatchers.IO) {
            try {
                if (!directoryDoc.exists() || !directoryDoc.canRead()) {
                    return@withContext Result.failure(
                        IllegalStateException("Selected folder is inaccessible or USB drive was disconnected.")
                    )
                }
                val children = directoryDoc.listFiles()
                val items = children.map { doc ->
                    val name = doc.name ?: "Unnamed"
                    val isDir = doc.isDirectory
                    val mime = doc.type
                    SafFileItem(
                        uri = doc.uri,
                        name = name,
                        sizeBytes = if (isDir) 0L else doc.length(),
                        lastModified = doc.lastModified(),
                        isDirectory = isDir,
                        mimeType = mime,
                        category = StorageAndCryptoUtils.classifyFileType(name, mime, isDir)
                    )
                }.sortedWith(
                    compareByDescending<SafFileItem> { it.isDirectory }
                        .thenBy { it.name.lowercase(Locale.US) }
                )
                Result.success(items)
            } catch (e: SecurityException) {
                Result.failure(IllegalStateException("SAF permission denied. Please re-select the USB folder."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createFolder(parentDoc: DocumentFile, folderName: String): Result<DocumentFile> =
        withContext(Dispatchers.IO) {
            val clean = folderName.trim()
            if (!StorageAndCryptoUtils.validateFileName(clean)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid folder name. Avoid special characters (/ \\ : * ? \" < > |).")
                )
            }
            if (parentDoc.findFile(clean) != null) {
                return@withContext Result.failure(
                    IllegalStateException("A file or folder named '$clean' already exists.")
                )
            }
            val created = parentDoc.createDirectory(clean)
                ?: return@withContext Result.failure(
                    IllegalStateException("Unable to create folder '$clean'. Check write permissions.")
                )
            Result.success(created)
        }

    suspend fun renameDocument(targetDoc: DocumentFile, newName: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            val clean = newName.trim()
            if (!StorageAndCryptoUtils.validateFileName(clean)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid file name. Avoid path separators or reserved characters.")
                )
            }
            val ok = targetDoc.renameTo(clean)
            if (ok) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Rename failed. The USB provider may not allow renaming this item."))
            }
        }

    suspend fun deleteDocument(targetDoc: DocumentFile): Result<Boolean> =
        withContext(Dispatchers.IO) {
            val ok = targetDoc.delete()
            if (ok) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Delete failed. Check write access or USB connection."))
            }
        }

    suspend fun copyUriToDirectory(
        context: Context,
        sourceUri: Uri,
        targetDir: DocumentFile,
        deleteSourceAfterCopy: Boolean = false,
        sourceDocForDelete: DocumentFile? = null,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Result<SafFileItem> = withContext(Dispatchers.IO) {
        try {
            val meta = queryUriDisplayNameAndSize(context, sourceUri)
            val baseName = meta.first.ifBlank { "copied_file_${System.currentTimeMillis()}" }
            val totalBytes = meta.second
            val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"

            val uniqueName = resolveUniqueName(targetDir, baseName)
            val destDoc = targetDir.createFile(mimeType, uniqueName)
                ?: return@withContext Result.failure(
                    IllegalStateException("Could not create destination file '$uniqueName' in target folder.")
                )

            var copiedBytes = 0L
            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                context.contentResolver.openOutputStream(destDoc.uri)?.use { rawOut ->
                    val input = BufferedInputStream(rawIn)
                    val output = BufferedOutputStream(rawOut)
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        copiedBytes += read
                        onProgress(copiedBytes, totalBytes)
                    }
                    output.flush()
                } ?: return@withContext Result.failure(IllegalStateException("Cannot open output stream on destination."))
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream on source file."))

            if (deleteSourceAfterCopy && sourceDocForDelete != null) {
                sourceDocForDelete.delete()
            }

            val finalName = destDoc.name ?: uniqueName
            Result.success(
                SafFileItem(
                    uri = destDoc.uri,
                    name = finalName,
                    sizeBytes = destDoc.length(),
                    lastModified = destDoc.lastModified(),
                    isDirectory = false,
                    mimeType = destDoc.type,
                    category = StorageAndCryptoUtils.classifyFileType(finalName, destDoc.type, false)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun computeUriSha256(context: Context, uri: Uri): Result<Pair<String, Long>> =
        withContext(Dispatchers.IO) {
            try {
                val meta = queryUriDisplayNameAndSize(context, uri)
                val hash = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    StorageAndCryptoUtils.computeSha256(inputStream)
                } ?: return@withContext Result.failure(IllegalStateException("Could not read file stream."))
                Result.success(hash to meta.second)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun verifyTwoFilesBySha256(
        context: Context,
        sourceUri: Uri,
        targetUri: Uri
    ): Result<VerificationResult> = withContext(Dispatchers.IO) {
        try {
            val (_, sourceSize) = queryUriDisplayNameAndSize(context, sourceUri)
            val (_, targetSize) = queryUriDisplayNameAndSize(context, targetUri)
            val sourceHash = context.contentResolver.openInputStream(sourceUri)?.use {
                StorageAndCryptoUtils.computeSha256(it)
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read source file."))
            val targetHash = context.contentResolver.openInputStream(targetUri)?.use {
                StorageAndCryptoUtils.computeSha256(it)
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read target file."))

            Result.success(
                StorageAndCryptoUtils.verifyFileIntegrity(
                    sourceSize = sourceSize,
                    targetSize = targetSize,
                    sourceSha256 = sourceHash,
                    targetSha256 = targetHash
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Runs a dedicated, non-destructive Read/Write hardware verification test on the selected SAF folder.
     * Creates 'mg_usb_benchmark_temp.bin' (4 MB), writes deterministic random blocks while computing SHA-256,
     * reads back the file from storage to verify SHA-256 and read speed, and deletes the temporary test file.
     */
    suspend fun runDedicatedReadWriteTest(
        context: Context,
        targetDir: DocumentFile,
        testSizeBytes: Int = 4 * 1024 * 1024, // 4 MB dedicated test file
        onStageUpdate: (String) -> Unit = {}
    ): HardwareBenchmarkResult = withContext(Dispatchers.IO) {
        val tempFileName = "mg_usb_benchmark_temp.bin"
        var tempDoc: DocumentFile? = null
        try {
            if (!targetDir.exists() || !targetDir.canWrite()) {
                return@withContext HardwareBenchmarkResult(
                    success = false,
                    bytesTested = 0L,
                    writeSpeedMbps = 0.0,
                    readSpeedMbps = 0.0,
                    expectedSha256 = "",
                    actualSha256 = "",
                    tempFileCleanedUp = true,
                    message = "Selected folder does not allow write access or USB drive is disconnected."
                )
            }

            // Remove any leftover temp benchmark file if present
            targetDir.findFile(tempFileName)?.delete()

            onStageUpdate("Allocating $tempFileName (${StorageAndCryptoUtils.formatBytes(testSizeBytes.toLong())})...")
            tempDoc = targetDir.createFile("application/octet-stream", tempFileName)
                ?: return@withContext HardwareBenchmarkResult(
                    success = false,
                    bytesTested = 0L,
                    writeSpeedMbps = 0.0,
                    readSpeedMbps = 0.0,
                    expectedSha256 = "",
                    actualSha256 = "",
                    tempFileCleanedUp = true,
                    message = "Could not create temporary test file in the selected folder."
                )

            val blockSize = 64 * 1024
            val blocksCount = testSizeBytes / blockSize
            val random = Random(0x4D47555342L)
            val writeDigest = MessageDigest.getInstance("SHA-256")
            val blockBuffer = ByteArray(blockSize)

            onStageUpdate("Writing ${StorageAndCryptoUtils.formatBytes(testSizeBytes.toLong())} test payload...")
            val writeStartNs = System.nanoTime()
            context.contentResolver.openOutputStream(tempDoc.uri)?.use { rawOut ->
                val out = BufferedOutputStream(rawOut, blockSize)
                for (i in 0 until blocksCount) {
                    random.nextBytes(blockBuffer)
                    writeDigest.update(blockBuffer)
                    out.write(blockBuffer)
                }
                out.flush()
            } ?: return@withContext HardwareBenchmarkResult(
                success = false,
                bytesTested = 0L,
                writeSpeedMbps = 0.0,
                readSpeedMbps = 0.0,
                expectedSha256 = "",
                actualSha256 = "",
                tempFileCleanedUp = false,
                message = "Failed to open output stream for benchmark file."
            )
            val writeElapsedSec = ((System.nanoTime() - writeStartNs).coerceAtLeast(1L)) / 1_000_000_000.0
            val expectedHash = writeDigest.digest().joinToString("") { "%02x".format(it) }
            val writeMbps = (testSizeBytes / (1024.0 * 1024.0)) / writeElapsedSec

            onStageUpdate("Reading back & verifying SHA-256 integrity...")
            val readDigest = MessageDigest.getInstance("SHA-256")
            var totalReadBytes = 0L
            val readStartNs = System.nanoTime()
            context.contentResolver.openInputStream(tempDoc.uri)?.use { rawIn ->
                val input = BufferedInputStream(rawIn, blockSize)
                var read: Int
                while (input.read(blockBuffer).also { read = it } != -1) {
                    if (read > 0) {
                        readDigest.update(blockBuffer, 0, read)
                        totalReadBytes += read
                    }
                }
            } ?: return@withContext HardwareBenchmarkResult(
                success = false,
                bytesTested = 0L,
                writeSpeedMbps = writeMbps,
                readSpeedMbps = 0.0,
                expectedSha256 = expectedHash,
                actualSha256 = "",
                tempFileCleanedUp = false,
                message = "Failed to open input stream during read verification."
            )
            val readElapsedSec = ((System.nanoTime() - readStartNs).coerceAtLeast(1L)) / 1_000_000_000.0
            val actualHash = readDigest.digest().joinToString("") { "%02x".format(it) }
            val readMbps = (totalReadBytes / (1024.0 * 1024.0)) / readElapsedSec

            val cleaned = tempDoc.delete()
            tempDoc = null

            val passed = totalReadBytes == testSizeBytes.toLong() && expectedHash == actualHash
            val summaryMsg = if (passed) {
                "PASSED: 4.00 MB written (${String.format(Locale.US, "%.2f", writeMbps)} MB/s) & read back (${String.format(Locale.US, "%.2f", readMbps)} MB/s) with 100% SHA-256 match. Temporary test file removed."
            } else {
                "FAILED: Data corruption detected during read-back verification! Expected SHA-256 $expectedHash but read $actualHash."
            }

            HardwareBenchmarkResult(
                success = passed,
                bytesTested = totalReadBytes,
                writeSpeedMbps = writeMbps,
                readSpeedMbps = readMbps,
                expectedSha256 = expectedHash,
                actualSha256 = actualHash,
                tempFileCleanedUp = cleaned,
                message = summaryMsg
            )
        } catch (e: Exception) {
            val cleaned = try {
                tempDoc?.delete() ?: true
            } catch (_: Exception) {
                false
            }
            HardwareBenchmarkResult(
                success = false,
                bytesTested = 0L,
                writeSpeedMbps = 0.0,
                readSpeedMbps = 0.0,
                expectedSha256 = "",
                actualSha256 = "",
                tempFileCleanedUp = cleaned,
                message = "Benchmark error: ${e.localizedMessage ?: "I/O failure"}"
            )
        }
    }

    suspend fun listZipEntries(context: Context, zipUri: Uri): Result<List<ZipEntryInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val entries = mutableListOf<ZipEntryInfo>()
                context.contentResolver.openInputStream(zipUri)?.use { rawIn ->
                    ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val safe = StorageAndCryptoUtils.isZipEntryNameSafeForSaf(entry.name)
                            entries.add(
                                ZipEntryInfo(
                                    name = entry.name,
                                    compressedSize = entry.compressedSize.coerceAtLeast(0L),
                                    uncompressedSize = entry.size.coerceAtLeast(0L),
                                    isDirectory = entry.isDirectory,
                                    isPathSafe = safe
                                )
                            )
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Unable to open ZIP archive."))
                Result.success(entries)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Safely extracts a ZIP archive into a target SAF directory while enforcing strict
     * Zip-Slip path-traversal checks on every entry.
     */
    suspend fun extractZipSafely(
        context: Context,
        zipUri: Uri,
        targetParentDir: DocumentFile,
        subfolderName: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cleanSubfolder = subfolderName.trim().ifEmpty { "Extracted_Archive" }
            if (!StorageAndCryptoUtils.validateFileName(cleanSubfolder)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid destination folder name."))
            }
            val destRoot = targetParentDir.findFile(cleanSubfolder)?.takeIf { it.isDirectory }
                ?: targetParentDir.createDirectory(cleanSubfolder)
                ?: return@withContext Result.failure(IllegalStateException("Could not create extraction folder."))

            var extractedFilesCount = 0
            context.contentResolver.openInputStream(zipUri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
                    var entry = zis.nextEntry
                    val buffer = ByteArray(16 * 1024)
                    while (entry != null) {
                        val rawName = entry.name
                        if (!StorageAndCryptoUtils.isZipEntryNameSafeForSaf(rawName)) {
                            zis.closeEntry()
                            return@withContext Result.failure(
                                SecurityException("Blocked unsafe path traversal entry in ZIP: '$rawName'")
                            )
                        }
                        val segments = rawName.replace('\\', '/').split('/').filter { it.isNotEmpty() }
                        if (entry.isDirectory) {
                            ensureNestedDirectories(destRoot, segments)
                        } else if (segments.isNotEmpty()) {
                            val parentSegments = segments.dropLast(1)
                            val fileName = segments.last()
                            val parentDir = ensureNestedDirectories(destRoot, parentSegments)
                            val targetName = resolveUniqueName(parentDir, fileName)
                            val outDoc = parentDir.createFile("application/octet-stream", targetName)
                                ?: return@withContext Result.failure(
                                    IllegalStateException("Could not create extracted file '$targetName'.")
                                )
                            context.contentResolver.openOutputStream(outDoc.uri)?.use { outStream ->
                                var count: Int
                                while (zis.read(buffer).also { count = it } != -1) {
                                    outStream.write(buffer, 0, count)
                                }
                                outStream.flush()
                            }
                            extractedFilesCount++
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read ZIP file stream."))

            Result.success(extractedFilesCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ensureNestedDirectories(root: DocumentFile, segments: List<String>): DocumentFile {
        var current = root
        for (segment in segments) {
            val next = current.findFile(segment)?.takeIf { it.isDirectory }
                ?: current.createDirectory(segment)
                ?: throw IllegalStateException("Failed to create directory segment '$segment'")
            current = next
        }
        return current
    }

    fun queryUriDisplayNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        var size = -1L
        val cursor: Cursor? = try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )
        } catch (_: Exception) {
            null
        }
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx != -1 && !it.isNull(nameIdx)) {
                    name = it.getString(nameIdx)
                }
                if (sizeIdx != -1 && !it.isNull(sizeIdx)) {
                    size = it.getLong(sizeIdx)
                }
            }
        }
        return name to size
    }

    private fun resolveUniqueName(dir: DocumentFile, desiredName: String): String {
        if (dir.findFile(desiredName) == null) return desiredName
        val dotIndex = desiredName.lastIndexOf('.')
        val base = if (dotIndex > 0) desiredName.substring(0, dotIndex) else desiredName
        val ext = if (dotIndex > 0) desiredName.substring(dotIndex) else ""
        var counter = 1
        while (counter < 500) {
            val candidate = "${base}_($counter)$ext"
            if (dir.findFile(candidate) == null) return candidate
            counter++
        }
        return "${base}_${System.currentTimeMillis()}$ext"
    }
}
