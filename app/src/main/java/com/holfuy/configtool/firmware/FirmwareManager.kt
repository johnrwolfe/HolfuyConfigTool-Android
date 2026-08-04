package com.holfuy.configtool.firmware

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    
    private fun loadManifest(): FirmwareManifest
    {
        return json.decodeFromString(
            downloadManifest()
        )
    }
    
    private suspend fun synchronizeRepository(
        manifest: FirmwareManifest
    )
    {
        manifest.firmwares.forEach { descriptor ->
    
            firmwareRepository.createOrReplace(
                descriptor.filename
            )
    
            Log.i(
                TAG,
                "Created ${descriptor.filename}"
            )
        }
    }
    
    private suspend fun download(
        descriptor: FirmwareDescriptor
    )
    {
        TODO()
    }
    
    suspend fun refresh()
    {
        withContext(Dispatchers.IO) {
    
            try {
    
                val manifest = loadManifest()

                synchronizeRepository(manifest)
      
            } catch (e: Exception) {
    
                Log.w(
                    TAG,
                    "Unable to refresh firmware library.",
                    e
                )
            }
        }
    }
}