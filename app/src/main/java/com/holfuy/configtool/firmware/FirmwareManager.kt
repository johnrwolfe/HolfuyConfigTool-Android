package com.holfuy.configtool.firmware

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest
import kotlinx.coroutines.delay
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
    )
    {
        manifest.firmwares.forEach { descriptor ->
    
            val tempFilename =
                "${descriptor.filename}.part"
            
            downloadWithRetry(descriptor)
            
            firmwareRepository.promote(
                tempFilename,
                descriptor.filename
            )
    
            val file =
                firmwareRepository.find(
                    descriptor.filename
                )
            
            Log.i(
                TAG,
                "Downloaded ${descriptor.filename} (${file?.length()} bytes)"
            )
        }
    }

    private fun sha256(
        file: DocumentFile
    ): String
    {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )
    
        context.contentResolver
            .openInputStream(file.uri)
            .use { input ->
    
                check(input != null) {
                    "Unable to open '${file.name}'."
                }
    
                val buffer =
                    ByteArray(8192)
    
                while (true) {
    
                    val count =
                        input.read(buffer)
    
                    if (count < 0)
                        break
    
                    digest.update(
                        buffer,
                        0,
                        count
                    )
                }
            }
    
        return digest.digest()
            .joinToString("") {
                "%02x".format(it)
            }
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
    
                context.contentResolver
                    .openOutputStream(file.uri)
                    .use { output ->
    
                        check(output != null) {
                            "Unable to open '${descriptor.filename}'."
                        }
    
                        Log.i(
                            TAG,
                            "Copying ${descriptor.filename}"
                        )
    
                        response.body!!
                            .byteStream()
                            .copyTo(output)
    
                        Log.i(
                            TAG,
                            "Finished copying ${descriptor.filename}"
                        )
                    }
    
                Log.i(
                    TAG,
                    "Closed output ${descriptor.filename}"
                )
                
                val checksum = sha256(file)
                
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
    
        Log.i(
            TAG,
            "Finished download ${descriptor.filename}"
        )
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