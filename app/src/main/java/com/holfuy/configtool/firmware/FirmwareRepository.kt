package com.holfuy.configtool.firmware

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

private val json = Json { ignoreUnknownKeys = true }
private val client = OkHttpClient()

class FirmwareRepository(
    private val storage: RepositoryStorage,
    private val manifestConfiguration: ManifestConfiguration
)
{
    companion object
    {
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
        val tempFilename =
            "${descriptor.filename}.part"

        repeat(MAX_DOWNLOAD_ATTEMPTS) { attempt ->

            try {

                Log.i(
                    TAG,
                    "Downloading ${descriptor.filename} " +
                        "(attempt ${attempt + 1})"
                )

                download(descriptor)

                if (attempt > 0) {

                    Log.i(
                        TAG,
                        "Succeeded downloading " +
                            "${descriptor.filename} " +
                            "on attempt ${attempt + 1}."
                    )
                }

                return

            } catch (e: Exception) {

                lastException = e

                storage.deleteIfPresent(
                    tempFilename
                )

                Log.w(
                    TAG,
                    "Attempt ${attempt + 1} of " +
                        "$MAX_DOWNLOAD_ATTEMPTS failed for " +
                        "${descriptor.filename}.",
                    e
                )

                if (
                    (attempt + 1) <
                    MAX_DOWNLOAD_ATTEMPTS
                ) {
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
                                "'${descriptor.filename}' " +
                                    "not found after promotion."
                            )

                    Log.i(
                        TAG,
                        "Downloaded ${descriptor.filename} " +
                            "(${file.length()} bytes)"
                    )

                    firmwareStatus +=
                        FirmwareStatus.Current(
                            file =
                                storage.firmwareFile(file),
                            modem =
                                descriptor.modem
                        )

                } catch (e: Exception) {

                    Log.w(
                        TAG,
                        "Unable to obtain " +
                            "${descriptor.filename}.",
                        e
                    )

                    firmwareStatus +=
                        FirmwareStatus.Missing(
                            filename =
                                descriptor.filename,
                            modem =
                                descriptor.modem
                        )
                }

            } else {

                try {

                    val checksum =
                        storage.sha256(
                            existingFile
                        )

                    if (
                        checksum ==
                        descriptor.sha256
                    ) {

                        Log.i(
                            TAG,
                            "Verified existing " +
                                "${descriptor.filename}"
                        )

                        firmwareStatus +=
                            FirmwareStatus.Current(
                                file =
                                    storage.firmwareFile(
                                        existingFile
                                    ),
                                modem =
                                    descriptor.modem
                            )

                    } else {

                        Log.i(
                            TAG,
                            "${descriptor.filename} " +
                                "is outdated; downloading replacement."
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
                                        "'${descriptor.filename}' " +
                                            "not found after promotion."
                                    )

                            Log.i(
                                TAG,
                                "Updated ${descriptor.filename} " +
                                    "(${file.length()} bytes)"
                            )

                            firmwareStatus +=
                                FirmwareStatus.Current(
                                    file =
                                        storage.firmwareFile(
                                            file
                                        ),
                                    modem =
                                        descriptor.modem
                                )

                        } catch (e: Exception) {

                            Log.w(
                                TAG,
                                "Unable to replace outdated " +
                                    "${descriptor.filename}.",
                                e
                            )

                            firmwareStatus +=
                                FirmwareStatus.Outdated(
                                    file =
                                        storage.firmwareFile(
                                            existingFile
                                        ),
                                    modem =
                                        descriptor.modem
                                )
                        }
                    }

                } catch (e: Exception) {

                    Log.w(
                        TAG,
                        "Unable to verify existing " +
                            "${descriptor.filename}; " +
                            "will download replacement.",
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
                                    "'${descriptor.filename}' " +
                                        "not found after promotion."
                                )

                        Log.i(
                            TAG,
                            "Replaced unreadable " +
                                "${descriptor.filename} " +
                                "(${file.length()} bytes)"
                        )

                        firmwareStatus +=
                            FirmwareStatus.Current(
                                file =
                                    storage.firmwareFile(file),
                                modem =
                                    descriptor.modem
                            )

                    } catch (
                        downloadException: Exception
                    ) {

                        Log.w(
                            TAG,
                            "Unable to replace unreadable " +
                                "${descriptor.filename}.",
                            downloadException
                        )

                        firmwareStatus +=
                            FirmwareStatus.Outdated(
                                file =
                                    storage.firmwareFile(
                                        existingFile
                                    ),
                                modem =
                                    descriptor.modem
                            )
                    }
                }
            }
        }

        /*
         * Files in the repository that are not mentioned in the manifest
         * are reported as CUSTOM.
         */
        storage.listFiles()
            .filter { file ->
                firmwareStatus.none {
                    it.filename == file.name
                }
            }
            .forEach { file ->

                firmwareStatus +=
                    FirmwareStatus.Custom(
                        storage.firmwareFile(file)
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

                val checksum =
                    storage.sha256(file)

                check(
                    checksum ==
                        descriptor.sha256
                ) {
                    "SHA-256 verification failed for " +
                        "'${descriptor.filename}'."
                }

                Log.i(
                    TAG,
                    "Verified ${descriptor.filename}"
                )
            }
    }

    /*
     * Reconcile a previously classified snapshot with the files that
     * physically exist in the repository.
     *
     * Existing classifications are preserved.
     *
     * Files that have disappeared are removed from the snapshot.
     *
     * Files that exist physically but were not present in the previous
     * snapshot are newly discovered here and are treated as CUSTOM.
     *
     * The latter is primarily a fallback for a refresh where the manifest
     * cannot be obtained. A successful manifest refresh subsequently
     * replaces the entire snapshot with authoritative classifications.
     */
    private fun reconcileSnapshotWithStorage(
        snapshot: List<FirmwareStatus>
    ): List<FirmwareStatus>
    {
        val existingFiles =
            storage.listFiles()

        val existingByName =
            existingFiles
                .mapNotNull { file ->
                    file.name?.let { name ->
                        name to file
                    }
                }
                .toMap()

        val reconciled =
            snapshot
                .filter {
                    it.filename in existingByName
                }
                .toMutableList()

        val knownNames =
            reconciled
                .map {
                    it.filename
                }
                .toSet()

        existingFiles
            .filter { file ->
                val name = file.name

                name != null &&
                    name !in knownNames
            }
            .forEach { file ->

                reconciled +=
                    FirmwareStatus.Custom(
                        storage.firmwareFile(file)
                    )
            }

        return reconciled
    }
    
    fun firmwareFile(
        name: String,
        size: Long
    ): FirmwareFile
    {
        return storage.firmwareFile(
            name = name,
            size = size
        )
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

                /*
                 * Immediately reconcile the previous snapshot against the
                 * actual repository contents.
                 *
                 * This both removes externally deleted files and discovers
                 * files that appeared since the previous snapshot.
                 */
                status = status.copy(
                    refreshing = true,
                    firmware =
                        reconcileSnapshotWithStorage(
                            status.firmware
                        )
                )

                val manifest =
                    try {

                        loadManifest()

                    } catch (e: Exception) {

                        Log.w(
                            TAG,
                            "Unable to download firmware manifest.",
                            e
                        )

                        /*
                         * Reconcile once more because repository contents
                         * may have changed while the manifest request was
                         * in progress.
                         *
                         * Newly discovered files are treated as CUSTOM.
                         */
                        status = status.copy(
                            refreshing = false,
                            lastCheckFailed =
                                Instant.now(),
                            firmware =
                                reconcileSnapshotWithStorage(
                                    status.firmware
                                )
                        )

                        return@withContext
                    }

                /*
                 * Build the complete manifest-derived snapshot privately.
                 * Nothing becomes visible to Compose until synchronization
                 * has completed.
                 */
                val firmwareStatus =
                    synchronizeRepository(
                        manifest
                    )

                /*
                 * Atomically replace the temporary snapshot with the
                 * complete, authoritative repository state.
                 */
                status = status.copy(
                    refreshing = false,
                    lastSuccessfullyChecked =
                        Instant.now(),
                    lastCheckFailed = null,
                    firmware = firmwareStatus
                )

                Log.i(
                    TAG,
                    "Refresh completed successfully."
                )
            }

        } finally {

            refreshMutex.unlock()
        }
    }
}