package com.holfuy.configtool.diagnostics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DiagnosticLogger(
    private val logFile: File
)
{
    companion object
    {
        private const val TAG = "HolfuyUSB-DIAG"

        private const val MAX_LOG_SIZE = 100 * 1024

        private val TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm:ss"
            )
    }

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor {
            runnable ->
                Thread(
                    runnable,
                    "HolfuyDiagnostics"
                ).apply {
                    isDaemon = true
                }
        }

    fun recordApplicationStarted(
        appVersion: String,
        androidVersion: String,
        device: String
    )
    {
        record(
            "Application started: " +
                "version=$appVersion, " +
                "Android=$androidVersion, " +
                "device=$device"
        )
    }

    fun recordUsbAttached()
    {
        record(
            "USB device attached: Holfuy station"
        )
    }

    fun recordUsbPermissionRequested()
    {
        record(
            "USB permission requested"
        )
    }

    fun recordUsbPermissionGranted()
    {
        record(
            "USB permission granted"
        )
    }

    fun recordUsbPermissionDenied()
    {
        record(
            "USB permission denied"
        )
    }

    fun recordUsbDetached()
    {
        record(
            "USB device detached: Holfuy station"
        )
    }

    fun recordConnectRequested()
    {
        record(
            "Connection requested"
        )
    }

    fun recordUsbSessionOpened()
    {
        record(
            "USB session opened"
        )
    }

    fun recordIspConnected()
    {
        record(
            "ISP connection succeeded"
        )
    }

    fun recordIspConnectionFailed(
        reason: String
    )
    {
        record(
            "ISP connection failed: ${sanitize(reason)}"
        )
    }

    fun recordFirmwareSelected(
        filename: String,
        size: Long,
        source: String
    )
    {
        record(
            "Firmware selected: " +
                "${sanitize(filename)} " +
                "(${size} bytes, source=$source)"
        )
    }

    fun recordFirmwareUnavailable(
        filename: String
    )
    {
        record(
            "Selected firmware unavailable: " +
                sanitize(filename)
        )
    }

    fun recordRepositoryConfigurationStarted()
    {
        record(
            "Firmware repository configuration started"
        )
    }

    fun recordRepositoryConfigured()
    {
        record(
            "Firmware repository configured"
        )
    }

    fun recordRepositoryRefreshStarted()
    {
        record(
            "Firmware repository refresh started"
        )
    }

    fun recordRepositoryRefreshCompleted()
    {
        record(
            "Firmware repository refresh completed"
        )
    }

    fun recordRepositoryRefreshFailed(
        reason: String
    )
    {
        record(
            "Firmware repository refresh failed: " +
                sanitize(reason)
        )
    }

    fun recordFirmwareDownloadFailed(
        filename: String
    )
    {
        record(
            "Firmware download failed: " +
                sanitize(filename)
        )
    }

    fun recordFirmwareUpdateRequested(
        filename: String
    )
    {
        record(
            "Firmware update requested: " +
                sanitize(filename)
        )
    }

    fun recordFirmwareFileOpened(
        filename: String,
        size: Int
    )
    {
        record(
            "Firmware file opened: " +
                "${sanitize(filename)} ($size bytes)"
        )
    }

    fun recordFirmwareFileOpenFailed(
        filename: String
    )
    {
        record(
            "Unable to open firmware file: " +
                sanitize(filename)
        )
    }

    fun recordFirmwareUpdateStarted(
        size: Int
    )
    {
        record(
            "Firmware update started: $size bytes"
        )
    }

    fun recordFirmwareUpdateProgress(
        progress: Int
    )
    {
        record(
            "Firmware update progress: $progress%"
        )
    }

    fun recordFirmwareUpdateCompleted()
    {
        record(
            "Firmware update completed successfully"
        )
    }

    fun recordFirmwareUpdateFailed(
        reason: String
    )
    {
        record(
            "Firmware update failed: " +
                sanitize(reason)
        )
    }

    fun recordFirmwareUpdateInterrupted()
    {
        record(
            "Firmware update interrupted: USB device detached"
        )
    }

    fun recordUnexpectedException(
        operation: String,
        exception: Exception
    )
    {
        val detail =
            exception.message
                ?.takeIf { it.isNotBlank() }
                ?.let { ": ${sanitize(it)}" }
                ?: ""

        record(
            "Unexpected exception during " +
                "${sanitize(operation)}$detail"
        )
    }

    fun record(
        message: String
    )
    {
        val timestamp =
            LocalDateTime.now()
                .format(TIMESTAMP_FORMAT)

        executor.execute {
            try {
                append(
                    "$timestamp  $message"
                )
            }
            catch (e: Exception) {
                /*
                 * Diagnostics must never interfere with application
                 * operation. Log the failure for development purposes,
                 * but do not propagate it to the caller.
                 */
                Log.e(
                    TAG,
                    "Unable to write diagnostic event",
                    e
                )
            }
        }
    }

    suspend fun snapshot(): String
    {
        return withContext(Dispatchers.IO) {
            executor.submit<String> {
                try {
                    if (!logFile.exists()) {
                        return@submit ""
                    }

                    logFile.readText()
                }
                catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Unable to read diagnostic history",
                        e
                    )

                    ""
                }
            }.get()
        }
    }

    private fun append(
        line: String
    )
    {
        logFile.parentFile?.mkdirs()

        logFile.appendText(
            line + "\n"
        )

        trimToLimit()
    }

    private fun trimToLimit()
    {
        if (!logFile.exists() ||
            logFile.length() <= MAX_LOG_SIZE
        ) {
            return
        }

        val contents =
            logFile.readLines()

        var start =
            contents.size

        var size = 0

        while (start > 0) {
            val candidate =
                contents[start - 1] + "\n"

            if (
                size + candidate
                    .toByteArray()
                    .size >
                MAX_LOG_SIZE
            ) {
                break
            }

            size +=
                candidate
                    .toByteArray()
                    .size

            start--
        }

        logFile.writeText(
            contents
                .subList(
                    start,
                    contents.size
                )
                .joinToString("\n") +
                "\n"
        )
    }

    private fun sanitize(
        value: String
    ): String
    {
        return value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
    }
}