package com.holfuy.configtool.firmware

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private val json = Json {ignoreUnknownKeys = true}
private val client = OkHttpClient()
private const val MANIFEST_URL = "https://holfuy.com/support/firmwares/mobile_upgrader_manifest.json"

class FirmwareManager(
    private val context: Context,
    private val firmwareRepository: FirmwareRepository
)
{
    companion object {
        private const val TAG = "HolfuyUSB-FW"
    }

    private fun downloadManifest(): String
    {
        val request =
            Request.Builder()
                .url(MANIFEST_URL)
                .build()
    
        client.newCall(request)
            .execute()
            .use { response ->
    
                check(response.isSuccessful) {
                    "HTTP ${response.code}"
                }
    
                return response.body!!.string()
            }
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