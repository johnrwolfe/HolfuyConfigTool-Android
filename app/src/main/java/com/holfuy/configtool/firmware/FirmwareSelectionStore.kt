package com.holfuy.configtool.firmware

import android.content.Context
import android.net.Uri
import com.holfuy.configtool.ui.state.FirmwareSelectionSource 

data class StoredFirmwareSelection(
    val source: FirmwareSelectionSource,
    val name: String,
    val size: Long,
    val modem: String? = null,
    val uri: Uri? = null
)

class FirmwareSelectionStore(
    context: Context
)
{
    companion object
    {
        private const val PREFERENCES_NAME =
            "firmware_selection"

        private const val KEY_SOURCE =
            "source"

        private const val KEY_NAME =
            "name"

        private const val KEY_SIZE =
            "size"

        private const val KEY_MODEM =
            "modem"

        private const val KEY_URI =
            "uri"
    }

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    fun getSelection(): StoredFirmwareSelection?
    {
        val sourceName =
            preferences.getString(
                KEY_SOURCE,
                null
            )
                ?: return null

        val source =
            try
            {
                FirmwareSelectionSource.valueOf(
                    sourceName
                )
            }
            catch (_: IllegalArgumentException)
            {
                return null
            }

        val name =
            preferences.getString(
                KEY_NAME,
                null
            )
                ?: return null

        val size =
            preferences.getLong(
                KEY_SIZE,
                -1L
            )

        val modem =
            preferences.getString(
                KEY_MODEM,
                null
            )

        val uri =
            preferences.getString(
                KEY_URI,
                null
            )?.let(Uri::parse)

        return StoredFirmwareSelection(
            source = source,
            name = name,
            size = size,
            modem = modem,
            uri = uri
        )
    }

    fun setSelection(
        selection: StoredFirmwareSelection
    )
    {
        preferences.edit()
            .clear()
            .putString(
                KEY_SOURCE,
                selection.source.name
            )
            .putString(
                KEY_NAME,
                selection.name
            )
            .putLong(
                KEY_SIZE,
                selection.size
            )
            .apply {
                selection.modem?.let {
                    putString(
                        KEY_MODEM,
                        it
                    )
                }

                selection.uri?.let {
                    putString(
                        KEY_URI,
                        it.toString()
                    )
                }
            }
            .apply()
    }

    fun clearSelection()
    {
        preferences.edit()
            .clear()
            .apply()
    }
}