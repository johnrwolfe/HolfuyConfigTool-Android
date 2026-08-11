package com.holfuy.configtool.firmware

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private val json = Json {ignoreUnknownKeys = true}
private val client = OkHttpClient()

class FirmwareRepository(
    private val storage: RepositoryStorage,
    private val manifestConfiguration: ManifestConfiguration
)
{
    companion object {
        private const val TAG = "HolfuyUSB-FW"
        private const val MAX_DOWNLOAD_ATTEMPTS = 5
    }
    
    private val refreshMutex = Mutex()
    
    var status by mutableStateOf(
        RepositoryStatus(
            configured = storage.configured,
            displayName = storage.displayName
        )
    )
        private set
        
    fun beginConfiguration()
    {
        status = status.copy(
            configuring = true
        )
    }
    
    fun endConfiguration()
    {
        status = status.copy(
            configuring = false
        )
    }
    
    fun configure(
        rootUri: Uri
    )
    {
        storage.setRepositoryRoot(
            rootUri
        )
    
        status = status.copy(
            configured = storage.configured,
            displayName = storage.displayName
        )
    }
    
    private fun downloadManifest(): String
    {
        val request =
            Request.Builder()
                .url(manifestConfiguration.manifestUrl)
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
                storage.deleteIfPresent(tempFilename)
    
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
    ): List<FirmwareStatus>
    {
        val firmwareStatus =
            mutableListOf<FirmwareStatus>()
    
        manifest.firmwares.forEach { descriptor ->
    
            val existingFile =
                storage.find(
                    descriptor.filename
                )
    
            if (existingFile == null) {
    
                try {
    
                    downloadWithRetry(
                        descriptor
                    )
    
                    storage.promote(
                        "${descriptor.filename}.part",
                        descriptor.filename
                    )
    
                    val file =
                        storage.find(
                            descriptor.filename
                        )
                            ?: error(
                                "'${descriptor.filename}' not found after promotion."
                            )
    
                    Log.i(
                        TAG,
                        "Downloaded ${descriptor.filename} (${file.length()} bytes)"
                    )
    
                    firmwareStatus +=
                        FirmwareStatus(
                            descriptor.filename,
                            FirmwareDisposition.CURRENT
                        )
    
                } catch (e: Exception) {
    
                    Log.w(
                        TAG,
                        "Unable to obtain ${descriptor.filename}.",
                        e
                    )
    
                    firmwareStatus +=
                        FirmwareStatus(
                            descriptor.filename,
                            FirmwareDisposition.MISSING
                        )
                }
    
            } else {
    
                try {
    
                    val checksum =
                        storage.sha256(
                            existingFile
                        )
    
                    if (checksum == descriptor.sha256) {
    
                        Log.i(
                            TAG,
                            "Verified existing ${descriptor.filename}"
                        )
    
                        firmwareStatus +=
                            FirmwareStatus(
                                descriptor.filename,
                                FirmwareDisposition.CURRENT
                            )
    
                    } else {
    
                        Log.i(
                            TAG,
                            "${descriptor.filename} is outdated; downloading replacement."
                        )
    
                        try {
    
                            downloadWithRetry(
                                descriptor
                            )
    
                            storage.promote(
                                "${descriptor.filename}.part",
                                descriptor.filename
                            )
    
                            val file =
                                storage.find(
                                    descriptor.filename
                                )
                                    ?: error(
                                        "'${descriptor.filename}' not found after promotion."
                                    )
    
                            Log.i(
                                TAG,
                                "Updated ${descriptor.filename} (${file.length()} bytes)"
                            )
    
                            firmwareStatus +=
                                FirmwareStatus(
                                    descriptor.filename,
                                    FirmwareDisposition.CURRENT
                                )
    
                        } catch (e: Exception) {
    
                            Log.w(
                                TAG,
                                "Unable to replace outdated ${descriptor.filename}.",
                                e
                            )
    
                            firmwareStatus +=
                                FirmwareStatus(
                                    descriptor.filename,
                                    FirmwareDisposition.OUTDATED
                                )
                        }
                    }
    
                } catch (e: Exception) {
    
                    Log.w(
                        TAG,
                        "Unable to verify existing ${descriptor.filename}; will download replacement.",
                        e
                    )
    
                    try {
    
                        downloadWithRetry(
                            descriptor
                        )
    
                        storage.promote(
                            "${descriptor.filename}.part",
                            descriptor.filename
                        )
    
                        val file =
                            storage.find(
                                descriptor.filename
                            )
                                ?: error(
                                    "'${descriptor.filename}' not found after promotion."
                                )
    
                        Log.i(
                            TAG,
                            "Replaced unreadable ${descriptor.filename} (${file.length()} bytes)"
                        )
    
                        firmwareStatus +=
                            FirmwareStatus(
                                descriptor.filename,
                                FirmwareDisposition.CURRENT
                            )
    
                    } catch (downloadException: Exception) {
    
                        Log.w(
                            TAG,
                            "Unable to replace unreadable ${descriptor.filename}.",
                            downloadException
                        )
    
                        /*
                         * The file exists, but we were unable to establish
                         * whether its contents match the manifest. Per our
                         * agreed semantics, treat this as an unusable local
                         * copy for purposes of refresh.
                         */
                        firmwareStatus +=
                            FirmwareStatus(
                                descriptor.filename,
                                FirmwareDisposition.OUTDATED
                            )
                    }
                }
            }
        }
    
        /*
         * Files in the repository that are not mentioned in the manifest
         * are deliberately left untouched and are reported as UNKNOWN.
         */
        storage.listFiles()
            .filter { file ->
                firmwareStatus.none {
                    it.filename == file.name
                }
            }
            .forEach { file ->
    
                firmwareStatus +=
                    FirmwareStatus(
                        file.name!!,
                        FirmwareDisposition.UNKNOWN
                    )
            }
    
        return firmwareStatus
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
                    storage.createOrReplace(
                        tempFilename
                    )
    
                storage
                    .openOutputStream(file)
                    .use { output ->
    
                        response.body!!
                            .byteStream()
                            .copyTo(output)
    
                    }
                
                val checksum = storage.sha256(file)
                
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
    
    suspend fun refresh()
    {
        if (!refreshMutex.tryLock()) {
    
            Log.i(
                TAG,
                "Refresh already in progress; ignoring request."
            )
    
            return
        }
    
        try {
    
            withContext(Dispatchers.IO) {
    
                status = status.copy(
                    refreshing = true
                )
    
                try {
    
                    val manifest =
                        loadManifest()
    
                    val firmwareStatus =
                        synchronizeRepository(
                            manifest
                        )
    
                    status = status.copy(
                        refreshing = false,
                        lastSuccessfullyChecked = Instant.now(),
                        lastCheckFailed = null,
                        firmware = firmwareStatus
                    )
    
                    Log.i(
                        TAG,
                        "Refresh completed successfully."
    
                    )
    
                } catch (e: Exception) {
    
                    Log.w(
                        TAG,
                        "Unable to check firmware repository.",
                        e
                    )
    
                    status = status.copy(
                        refreshing = false,
                        lastCheckFailed = Instant.now()
                    )
                }
            }
    
        } finally {
    
            refreshMutex.unlock()
        }
    }
}