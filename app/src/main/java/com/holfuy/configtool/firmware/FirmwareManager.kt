package com.holfuy.configtool.firmware

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile

class FirmwareManager(
    private val context: Context,
    private val firmwareLibrary: FirmwareLibrary
)
{
    companion object {
        private const val TAG = "HolfuyUSB-FW"
    }

    fun listFirmwareFiles()
    {
        val folderUri: Uri =
            firmwareLibrary.folderUri
                ?: run {

                    Log.i(
                        TAG,
                        "Firmware library not configured."
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
                "Unable to open firmware library."
            )

            return
        }

        Log.i(
            TAG,
            "Firmware library:"
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