package com.holfuy.configtool.firmware

import android.content.Context
import android.net.Uri

class FirmwareRepository(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "firmware_repository"
        private const val KEY_FOLDER_URI = "folder_uri"
    }

    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    var folderUri: Uri?
        get() =
            prefs.getString(
                KEY_FOLDER_URI,
                null
            )?.let(Uri::parse)

        set(value) {
            prefs.edit()
                .putString(
                    KEY_FOLDER_URI,
                    value?.toString()
                )
                .apply()
        }

    val isConfigured: Boolean
        get() = folderUri != null
}