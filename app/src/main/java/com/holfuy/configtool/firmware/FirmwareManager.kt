package com.holfuy.configtool.firmware

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.json.Json

private val json = Json {ignoreUnknownKeys = true}

class FirmwareManager(
    private val context: Context,
    private val firmwareRepository: FirmwareRepository
)
{
    companion object {
        private const val TAG = "HolfuyUSB-FW"
    }

    private fun parseManifest(
        text: String
    ): FirmwareManifest = json.decodeFromString(text)
        
    fun listFirmwareFiles()
    {
        val folderUri: Uri =
            firmwareRepository.folderUri
                ?: run {

                    Log.i(
                        TAG,
                        "Firmware repository not configured."
                    )

                    return
                }

        val folder =
            DocumentFile.fromTreeUri(
                context,
                folderUri
            )

        if (folder == null) {

            Log.w(
                TAG,
                "Unable to open firmware repository."
            )

            return
        }

        Log.i(
            TAG,
            "Firmware repository:"
        )

        folder.listFiles()
            .sortedBy {
                it.name ?: ""
            }
            .forEach { file ->

                Log.i(
                    TAG,
                    "${file.name}  ${file.length()} bytes"
                )
            }
    }
}