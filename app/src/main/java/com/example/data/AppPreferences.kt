package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mg_usb_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_USB_TREE_URI = stringPreferencesKey("usb_tree_uri")
        private val KEY_BACKUP_TREE_URI = stringPreferencesKey("backup_tree_uri")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: true
    }

    val usbTreeUriString: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USB_TREE_URI]
    }

    val backupTreeUriString: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKUP_TREE_URI]
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = enabled
        }
    }

    suspend fun setUsbTreeUri(uriString: String?) {
        context.dataStore.edit { prefs ->
            if (uriString == null) {
                prefs.remove(KEY_USB_TREE_URI)
            } else {
                prefs[KEY_USB_TREE_URI] = uriString
            }
        }
    }

    suspend fun setBackupTreeUri(uriString: String?) {
        context.dataStore.edit { prefs ->
            if (uriString == null) {
                prefs.remove(KEY_BACKUP_TREE_URI)
            } else {
                prefs[KEY_BACKUP_TREE_URI] = uriString
            }
        }
    }
}
