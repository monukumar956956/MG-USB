package com.example.util

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.system.Os
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

enum class MgFileCategory(val label: String) {
    FOLDER("Folder"),
    APK("Android APK"),
    ZIP("ZIP Archive"),
    WINDOWS_EXE("Windows PC Tool"),
    AUDIO("Audio / Music"),
    IMAGE("Image"),
    VIDEO("Video"),
    DOCUMENT("Document / Text"),
    OTHER("Binary / File")
}

data class StorageStatsInfo(
    val label: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val isReliable: Boolean,
    val sourceNote: String
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) {
            (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}

data class UsbHardwareDeviceInfo(
    val deviceName: String,
    val productName: String,
    val manufacturerName: String,
    val vendorId: Int,
    val productId: Int,
    val isMassStorage: Boolean
)

data class VerificationResult(
    val isMatch: Boolean,
    val sizeMatches: Boolean,
    val hashMatches: Boolean,
    val sourceSize: Long,
    val targetSize: Long,
    val sourceSha256: String,
    val targetSha256: String,
    val summary: String
)

object StorageAndCryptoUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "Unknown"
        if (bytes < 1024L) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB", "PB")
        var value = bytes.toDouble()
        var unitIndex = -1
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        return String.format(Locale.US, "%.2f %s", value, units[unitIndex])
    }

    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (bytesRead > 0) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun computeSha256Bytes(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyFileIntegrity(
        sourceSize: Long,
        targetSize: Long,
        sourceSha256: String,
        targetSha256: String
    ): VerificationResult {
        val cleanSourceHash = sourceSha256.trim().lowercase(Locale.US)
        val cleanTargetHash = targetSha256.trim().lowercase(Locale.US)
        val sizeMatches = sourceSize >= 0L && sourceSize == targetSize
        val hashMatches = cleanSourceHash.isNotEmpty() &&
            cleanSourceHash.length == 64 &&
            cleanSourceHash == cleanTargetHash
        val isMatch = sizeMatches && hashMatches
        val summary = when {
            isMatch -> "VERIFIED: Exact byte size (${formatBytes(sourceSize)}) and SHA-256 checksum match."
            !sizeMatches && !hashMatches -> "MISMATCH: Both file size and SHA-256 digest differ!"
            !sizeMatches -> "SIZE MISMATCH: Source is ${formatBytes(sourceSize)}, target is ${formatBytes(targetSize)}."
            else -> "HASH MISMATCH: File sizes match (${formatBytes(sourceSize)}), but SHA-256 digests differ!"
        }
        return VerificationResult(
            isMatch = isMatch,
            sizeMatches = sizeMatches,
            hashMatches = hashMatches,
            sourceSize = sourceSize,
            targetSize = targetSize,
            sourceSha256 = cleanSourceHash,
            targetSha256 = cleanTargetHash,
            summary = summary
        )
    }

    /**
     * Validates a file or folder name for safe creation/renaming.
     * Rejects blank names, path separators, traversal tokens, and control characters.
     */
    fun validateFileName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 255) return false
        if (trimmed == "." || trimmed == "..") return false
        val illegalChars = charArrayOf('/', '\\', '\u0000', ':', '*', '?', '"', '<', '>', '|')
        if (trimmed.any { it in illegalChars || it.code < 32 }) return false
        return true
    }

    /**
     * Protects ZIP extraction against Zip-Slip path traversal vulnerabilities.
     */
    fun isZipEntryPathSafe(destinationDir: File, entryName: String): Boolean {
        if (entryName.isBlank() || entryName.contains("\u0000")) return false
        val normalizedEntry = entryName.replace('\\', '/')
        if (normalizedEntry.startsWith("/")) return false
        return try {
            val destCanonicalPath = destinationDir.canonicalPath
            val targetFile = File(destinationDir, normalizedEntry)
            val targetCanonicalPath = targetFile.canonicalPath
            targetCanonicalPath == destCanonicalPath ||
                targetCanonicalPath.startsWith(destCanonicalPath + File.separator)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Overload for SAF segment validation where extraction traverses DocumentFile trees.
     */
    fun isZipEntryNameSafeForSaf(entryName: String): Boolean {
        if (entryName.isBlank() || entryName.contains("\u0000")) return false
        val normalized = entryName.replace('\\', '/')
        if (normalized.startsWith("/")) return false
        val segments = normalized.split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return false
        return segments.all { segment ->
            segment != "." && segment != ".." && validateFileName(segment)
        }
    }

    fun classifyFileType(fileName: String, mimeType: String?, isDirectory: Boolean = false): MgFileCategory {
        if (isDirectory || mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            return MgFileCategory.FOLDER
        }
        val lower = fileName.lowercase(Locale.US)
        return when {
            lower.endsWith(".apk") || lower.endsWith(".apks") || lower.endsWith(".xapk") ||
                mimeType == "application/vnd.android.package-archive" -> MgFileCategory.APK
            lower.endsWith(".zip") || mimeType == "application/zip" ||
                mimeType == "application/x-zip-compressed" -> MgFileCategory.ZIP
            lower.endsWith(".exe") || lower.endsWith(".msi") || lower.endsWith(".bat") ||
                lower.endsWith(".cmd") || lower.endsWith(".ps1") || lower.endsWith(".iso") ||
                lower.endsWith(".img") -> MgFileCategory.WINDOWS_EXE
            lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac") ||
                lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".aac") ||
                lower.endsWith(".opus") || (mimeType?.startsWith("audio/") == true) -> MgFileCategory.AUDIO
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".gif") ||
                (mimeType?.startsWith("image/") == true) -> MgFileCategory.IMAGE
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
                lower.endsWith(".mov") || (mimeType?.startsWith("video/") == true) -> MgFileCategory.VIDEO
            lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".pdf") ||
                lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".log") ||
                lower.endsWith(".ini") || (mimeType?.startsWith("text/") == true) -> MgFileCategory.DOCUMENT
            else -> MgFileCategory.OTHER
        }
    }

    fun getPhoneStorageStats(): StorageStatsInfo {
        return try {
            val dataDir = Environment.getDataDirectory()
            val statFs = StatFs(dataDir.path)
            val total = statFs.totalBytes
            val free = statFs.availableBytes
            val used = (total - free).coerceAtLeast(0L)
            StorageStatsInfo(
                label = "Phone Internal Storage",
                totalBytes = total,
                freeBytes = free,
                usedBytes = used,
                isReliable = total > 0L,
                sourceNote = "Verified via Android StatFs (${dataDir.path})"
            )
        } catch (e: Exception) {
            StorageStatsInfo(
                label = "Phone Internal Storage",
                totalBytes = 0L,
                freeBytes = 0L,
                usedBytes = 0L,
                isReliable = false,
                sourceNote = "Unavailable: ${e.localizedMessage ?: "StatFs error"}"
            )
        }
    }

    /**
     * Queries capacity and free space of a selected SAF tree URI only when reliably available
     * through FileDescriptor fstatvfs or DocumentsContract root summary.
     */
    fun getSafVolumeStorageStats(context: Context, treeUri: Uri): StorageStatsInfo? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            context.contentResolver.openFileDescriptor(docUri, "r")?.use { pfd ->
                val statVfs = Os.fstatvfs(pfd.fileDescriptor)
                val blockSize = statVfs.f_frsize.takeIf { it > 0L } ?: statVfs.f_bsize
                val totalBytes = statVfs.f_blocks * blockSize
                val freeBytes = statVfs.f_bavail * blockSize
                if (totalBytes > 0L && freeBytes >= 0L && freeBytes <= totalBytes) {
                    val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
                    return StorageStatsInfo(
                        label = "Selected SAF / USB Volume ($docId)",
                        totalBytes = totalBytes,
                        freeBytes = freeBytes,
                        usedBytes = usedBytes,
                        isReliable = true,
                        sourceNote = "Verified via OS fstatvfs on SAF volume descriptor"
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Inspects actual USB hardware connected via Android UsbManager.
     * Never claims a USB drive is connected unless verified by UsbManager.
     */
    fun detectConnectedUsbDevices(context: Context): List<UsbHardwareDeviceInfo> {
        return try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                ?: return emptyList()
            val deviceMap: HashMap<String, UsbDevice> = usbManager.deviceList ?: return emptyList()
            deviceMap.values.map { device ->
                var hasMassStorageInterface = device.deviceClass == UsbConstants.USB_CLASS_MASS_STORAGE
                for (i in 0 until device.interfaceCount) {
                    val usbInterface = device.getInterface(i)
                    if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) {
                        hasMassStorageInterface = true
                        break
                    }
                }
                UsbHardwareDeviceInfo(
                    deviceName = device.deviceName ?: "USB Device",
                    productName = device.productName?.takeIf { it.isNotBlank() } ?: "USB OTG Peripheral",
                    manufacturerName = device.manufacturerName?.takeIf { it.isNotBlank() } ?: "Generic USB Vendor",
                    vendorId = device.vendorId,
                    productId = device.productId,
                    isMassStorage = hasMassStorageInterface
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
