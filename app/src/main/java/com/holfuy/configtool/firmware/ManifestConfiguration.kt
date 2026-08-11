package com.holfuy.configtool.firmware

import android.content.Context

class ManifestConfiguration(
    context: Context
)
{
    companion object {
        private const val PREFERENCES_NAME =
            "firmware_repository"

        private const val MANIFEST_URL_OVERRIDE =
            "manifest_url_override"
    }

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    val manifestUrl: String
        get() =
            preferences.getString(
                MANIFEST_URL_OVERRIDE,
                null
            )
                ?: "https://holfuy.com/support/firmwares/mobile_upgrader_manifest.json"

    val overrideUrl: String?
        get() =
            preferences.getString(
                MANIFEST_URL_OVERRIDE,
                null
            )

    fun setOverride(
        url: String
    )
    {
        preferences.edit()
            .putString(
                MANIFEST_URL_OVERRIDE,
                url
            )
            .apply()
    }

    fun clearOverride()
    {
        preferences.edit()
            .remove(
                MANIFEST_URL_OVERRIDE
            )
            .apply()
    }
}