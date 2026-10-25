package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.StorageAndCryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string and verify phone storage stats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MG USB Multi-Tool", appName)

        val phoneStats = StorageAndCryptoUtils.getPhoneStorageStats()
        assertTrue(phoneStats.totalBytes >= 0L)
        assertTrue(phoneStats.freeBytes >= 0L)
    }
}
