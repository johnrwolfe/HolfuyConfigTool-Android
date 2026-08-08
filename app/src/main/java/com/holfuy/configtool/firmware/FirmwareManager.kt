package com.holfuy.configtool.firmware

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import com.holfuy.configtool.firmware.FirmwareRepository
import com.holfuy.configtool.firmware.RefreshResult

private val json = Json {ignoreUnknownKeys = true}
private val client = OkHttpClient()
private const val MANIFEST_URL = "https://holfuy.com/support/firmwares/mobile_upgrader_manifest.json"

class FirmwareManager(
    private val firmwareRepository: FirmwareRepository
)
{
    companion object {
        private const val TAG = "HolfuyUSB-FW"
        private const val MAX_DOWNLOAD_ATTEMPTS = 5
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
    
    private suspend fun downloadWithRetry(
        descriptor: FirmwareDescriptor
    )
    {
        var lastException: Exception? = null        
        val tempFilename = "${descriptor.filename}.part"
    
        repeat(MAX_DOWNLOAD_ATTEMPTS) { attempt ->
    
            try {
                Log.i(
                    TAG,
                    "Downloading ${descriptor.filename} (attempt ${attempt + 1})"
                )
                
                download(descriptor)
    
                if (attempt > 0) {
    
                    Log.i(
                        TAG,
                        "Succeeded downloading ${descriptor.filename} on attempt ${attempt + 1}."
                    )
                }
    
                return
    
            } catch (e: Exception) {
    
                lastException = e
                firmwareRepository.deleteIfPresent(tempFilename)
    
                Log.w(
                    TAG,
                    "Attempt ${attempt + 1} of $MAX_DOWNLOAD_ATTEMPTS failed for ${descriptor.filename}.",
                    e
                )
                
                if ((attempt + 1) < MAX_DOWNLOAD_ATTEMPTS) {                
                    delay(1000)
                }
            }
        }
    
        throw checkNotNull(lastException)
    }
    
    private suspend fun synchronizeRepository(
        manifest: FirmwareManifest
    ): RefreshResult
    {
    
        val updated = mutableListOf<String>()
        val stale = mutableListOf<String>()
        val unavailable = mutableListOf<String>()
        
        manifest.firmwares.forEach { descriptor ->
        
            try {
        
                downloadWithRetry(descriptor)
        
                firmwareRepository.promote(
                    "${descriptor.filename}.part",
                    descriptor.filename
                )
        
                val file =
                    firmwareRepository.find(
                        descriptor.filename
                    )
                        ?: error(
                            "'${descriptor.filename}' not found after promotion."
                        )
        
                Log.i(
                    TAG,
                    "Updated ${descriptor.filename} (${file.length()} bytes)"
                )
        
                updated += descriptor.filename
        
            } catch (e: Exception) {
        
                if (
                    firmwareRepository.find(descriptor.filename) != null
                ) {
        
                    stale += descriptor.filename
        
                } else {
        
                    unavailable += descriptor.filename
                }
            }
        }
        return RefreshResult(
            updated = updated,
            stale = stale,
            unavailable = unavailable,
            verifiedAt = Instant.now()
        )
    }

    private suspend fun download(
        descriptor: FirmwareDescriptor
    )
    {
        val request =
            Request.Builder()
                .url(descriptor.path)
                .build()
                
        val tempFilename =
            "${descriptor.filename}.part"
    
        client.newCall(request)
            .execute()
            .use { response ->
    
                check(response.isSuccessful) {
                    "HTTP ${response.code}"
                }
    
                val file =
                    firmwareRepository.createOrReplace(
                        tempFilename
                    )
    
                firmwareRepository
                    .openOutputStream(file)
                    .use { output ->
    
                        response.body!!
                            .byteStream()
                            .copyTo(output)
    
                    }
                
                val checksum = firmwareRepository.sha256(file)
                
                check(
                    checksum == descriptor.sha256
                ) {
                    "SHA-256 verification failed for '${descriptor.filename}'."
                }
                
                Log.i(
                    TAG,
                    "Verified ${descriptor.filename}"
                )
            }
    }
    
    suspend fun refresh(): RefreshResult
    {
        return withContext(Dispatchers.IO) {
    
            val manifest = loadManifest()
    
            try {
    
                val result =
                    synchronizeRepository(manifest)
                
                Log.i(
                    TAG,
                    "Refresh: updated=${result.updated}, " +
                    "stale=${result.stale}, " +
                    "unavailable=${result.unavailable}"
                )
                
                result
    
            } catch (e: Exception) {
    
                Log.w(
                    TAG,
                    "Unable to refresh firmware library.",
                    e
                )
    
                RefreshResult(
                    updated = emptyList(),
                    stale = emptyList(),
                    unavailable = manifest.firmwares.map { it.filename },
                    verifiedAt = null
                )
            }
        }
    }
}