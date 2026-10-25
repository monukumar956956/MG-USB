package com.example

import com.example.util.MgFileCategory
import com.example.util.StorageAndCryptoUtils
import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun sha256Checksum_matchesKnownVector() {
        val payload = "MG_USB_MULTI_TOOL_TEST_VECTOR".toByteArray(Charsets.UTF_8)
        val streamHash = StorageAndCryptoUtils.computeSha256(ByteArrayInputStream(payload))
        val bytesHash = StorageAndCryptoUtils.computeSha256Bytes(payload)
        assertEquals(64, streamHash.length)
        assertEquals(streamHash, bytesHash)

        // Known SHA-256 of "abc"
        val abcHash = StorageAndCryptoUtils.computeSha256Bytes("abc".toByteArray(Charsets.UTF_8))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            abcHash
        )
    }

    @Test
    fun verifyFileIntegrity_detectsMatchAndMismatch() {
        val validHash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val differentHash = "000000000000cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

        val matched = StorageAndCryptoUtils.verifyFileIntegrity(
            sourceSize = 4096L,
            targetSize = 4096L,
            sourceSha256 = validHash,
            targetSha256 = validHash.uppercase()
        )
        assertTrue(matched.isMatch)
        assertTrue(matched.sizeMatches)
        assertTrue(matched.hashMatches)

        val sizeMismatch = StorageAndCryptoUtils.verifyFileIntegrity(
            sourceSize = 4096L,
            targetSize = 2048L,
            sourceSha256 = validHash,
            targetSha256 = validHash
        )
        assertFalse(sizeMismatch.isMatch)
        assertFalse(sizeMismatch.sizeMatches)

        val hashMismatch = StorageAndCryptoUtils.verifyFileIntegrity(
            sourceSize = 4096L,
            targetSize = 4096L,
            sourceSha256 = validHash,
            targetSha256 = differentHash
        )
        assertFalse(hashMismatch.isMatch)
        assertTrue(hashMismatch.sizeMatches)
        assertFalse(hashMismatch.hashMatches)
    }

    @Test
    fun validateFileName_rejectsUnsafeNames() {
        assertTrue(StorageAndCryptoUtils.validateFileName("backup_2026.zip"))
        assertTrue(StorageAndCryptoUtils.validateFileName("My Music Track.mp3"))
        assertFalse(StorageAndCryptoUtils.validateFileName(""))
        assertFalse(StorageAndCryptoUtils.validateFileName("   "))
        assertFalse(StorageAndCryptoUtils.validateFileName(".."))
        assertFalse(StorageAndCryptoUtils.validateFileName("."))
        assertFalse(StorageAndCryptoUtils.validateFileName("folder/hack.apk"))
        assertFalse(StorageAndCryptoUtils.validateFileName("folder\\hack.exe"))
        assertFalse(StorageAndCryptoUtils.validateFileName("bad:name.txt"))
    }

    @Test
    fun zipSlipProtection_blocksPathTraversal() {
        val tempRoot = File(System.getProperty("java.io.tmpdir"), "mg_zip_test_root")
        assertTrue(StorageAndCryptoUtils.isZipEntryPathSafe(tempRoot, "tools/readme.txt"))
        assertTrue(StorageAndCryptoUtils.isZipEntryNameSafeForSaf("tools/portable/app.apk"))

        assertFalse(StorageAndCryptoUtils.isZipEntryPathSafe(tempRoot, "../../etc/passwd"))
        assertFalse(StorageAndCryptoUtils.isZipEntryPathSafe(tempRoot, "/absolute/path.txt"))
        assertFalse(StorageAndCryptoUtils.isZipEntryNameSafeForSaf("../outside.sh"))
        assertFalse(StorageAndCryptoUtils.isZipEntryNameSafeForSaf("sub/../../escape.apk"))
        assertFalse(StorageAndCryptoUtils.isZipEntryNameSafeForSaf("/root/escape.txt"))
    }

    @Test
    fun formatBytesAndClassifyFileType_workCorrectly() {
        assertEquals("512 B", StorageAndCryptoUtils.formatBytes(512L))
        assertEquals("1.00 KB", StorageAndCryptoUtils.formatBytes(1024L))
        assertEquals("4.00 MB", StorageAndCryptoUtils.formatBytes(4L * 1024L * 1024L))

        assertEquals(MgFileCategory.APK, StorageAndCryptoUtils.classifyFileType("app_release.apk", null))
        assertEquals(MgFileCategory.ZIP, StorageAndCryptoUtils.classifyFileType("portable_tools.zip", null))
        assertEquals(MgFileCategory.WINDOWS_EXE, StorageAndCryptoUtils.classifyFileType("rufus-portable.exe", null))
        assertEquals(MgFileCategory.AUDIO, StorageAndCryptoUtils.classifyFileType("theme_song.flac", null))
        assertEquals(MgFileCategory.FOLDER, StorageAndCryptoUtils.classifyFileType("Backups", null, isDirectory = true))
    }
}
