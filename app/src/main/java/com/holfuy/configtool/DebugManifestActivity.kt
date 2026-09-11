package com.holfuy.configtool

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.holfuy.configtool.firmware.ManifestConfiguration

class DebugManifestActivity : Activity()
{
    companion object {
        const val ACTION_SET =
            "com.holfuy.configtool.debug.SET_MANIFEST_URL"

        const val ACTION_CLEAR =
            "com.holfuy.configtool.debug.CLEAR_MANIFEST_URL"

        const val ACTION_SHOW =
            "com.holfuy.configtool.debug.SHOW_MANIFEST_URL"

        const val EXTRA_URL =
            "url"
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    )
    {
        super.onCreate(savedInstanceState)

        val configuration =
            ManifestConfiguration(this)

        when (intent.action) {

            ACTION_SET -> {

                val url =
                    intent.getStringExtra(
                        EXTRA_URL
                    )
                        ?: error(
                            "Missing manifest URL."
                        )

                configuration.setOverride(
                    url
                )

                showResult(
                    "Manifest URL override set to:\n$url"
                )
            }

            ACTION_CLEAR -> {

                configuration.clearOverride()

                showResult(
                    "Manifest URL override cleared."
                )
            }

            ACTION_SHOW -> {

                val override =
                    configuration.overrideUrl

                showResult(
                    if (override == null)
                        "Using production manifest URL."
                    else
                        "Manifest URL override:\n$override"
                )
            }

            else -> {

                showResult(
                    "Unknown debug manifest command."
                )
            }
        }
    }

    private fun showResult(
        message: String
    )
    {
        setContentView(
            TextView(this).apply {
                text = message
                setPadding(
                    32,
                    32,
                    32,
                    32
                )
            }
        )
    }
}