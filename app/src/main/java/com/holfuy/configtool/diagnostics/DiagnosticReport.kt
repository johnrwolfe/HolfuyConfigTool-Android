package com.holfuy.configtool.diagnostics

import android.content.Context
import android.os.Build
import com.holfuy.configtool.device.DeviceState
import com.holfuy.configtool.firmware.RepositoryStatus
import com.holfuy.configtool.firmware.StoredFirmwareSelection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DiagnosticReport
{
    private val TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss"
        )

    fun generate(
        context: Context,
        history: String,
        deviceState: DeviceState,
        repositoryStatus: RepositoryStatus,
        firmwareSelection: StoredFirmwareSelection?
    ): String
    {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )

        val appVersion =
            packageInfo.versionName
                ?: "unknown"

        val device =
            "${Build.MANUFACTURER} ${Build.MODEL}"

        val androidVersion =
            "${Build.VERSION.RELEASE} " +
                "(API ${Build.VERSION.SDK_INT})"

        return buildString {

            appendLine(
                "Holfuy Upgrader Diagnostics"
            )

            appendLine(
                "Generated: " +
                    LocalDateTime.now()
                        .format(TIMESTAMP_FORMAT)
            )

            appendLine()

            appendLine("Application")
            appendLine(
                "  Version: $appVersion"
            )

            appendLine(
                "  Android: $androidVersion"
            )

            appendLine(
                "  Device: $device"
            )

            appendLine()

            appendLine("Current Device State")
            appendLine(
                "  USB attached: " +
                    deviceState.attached
            )

            appendLine(
                "  USB permission granted: " +
                    deviceState.permissionGranted
            )

            appendLine(
                "  Connected: " +
                    deviceState.connected
            )

            appendLine(
                "  Update in progress: " +
                    deviceState.updateInProgress
            )

            appendLine(
                "  Update progress: " +
                    "${deviceState.updateProgress}%"
            )

            appendLine()

            appendLine("Firmware Repository")
            appendLine(
                "  Configured: " +
                    repositoryStatus.configured
            )

            appendLine(
                "  Location: " +
                    (repositoryStatus.displayName
                        ?: "unknown")
            )

            appendLine(
                "  Refreshing: " +
                    repositoryStatus.refreshing
            )

            appendLine(
                "  Firmware entries: " +
                    repositoryStatus.firmware.size
            )

            appendLine(
                "  Last successful check: " +
                    (
                        repositoryStatus
                            .lastSuccessfullyChecked
                            ?.toString()
                            ?: "none"
                    )
            )

            appendLine(
                "  Last failed check: " +
                    (
                        repositoryStatus
                            .lastCheckFailed
                            ?.toString()
                            ?: "none"
                    )
            )

            appendLine()

            appendLine("Selected Firmware")

            if (firmwareSelection == null) {

                appendLine(
                    "  None"
                )

            } else {

                appendLine(
                    "  Filename: " +
                        firmwareSelection.name
                )

                appendLine(
                    "  Size: " +
                        "${firmwareSelection.size} bytes"
                )

                appendLine(
                    "  Source: " +
                        firmwareSelection.source
                )

                firmwareSelection.modem?.let {
                    appendLine(
                        "  Modem: $it"
                    )
                }

                appendLine(
                    "  Available: " +
                        "not determined in report"
                )
            }

            appendLine()

            appendLine("Recent Diagnostic History")

            if (history.isBlank()) {

                appendLine(
                    "  No diagnostic events have been recorded."
                )

            } else {

                append(history.trimEnd())
                appendLine()
            }
        }
    }
}