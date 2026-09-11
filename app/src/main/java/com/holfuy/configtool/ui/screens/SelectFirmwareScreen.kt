package com.holfuy.configtool.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.holfuy.configtool.firmware.FIRMWARE_EXTENSION
import com.holfuy.configtool.firmware.FirmwareFile
import com.holfuy.configtool.firmware.FirmwareStatus
import com.holfuy.configtool.firmware.MAX_FIRMWARE_SIZE
import com.holfuy.configtool.firmware.MIN_FIRMWARE_SIZE
import com.holfuy.configtool.firmware.RepositoryStatus
import com.holfuy.configtool.ui.state.FirmwareSelectionSource

private fun isSelectable(
    firmware: FirmwareStatus
): Boolean
{
    val file =
        when (firmware) {
            is FirmwareStatus.Current -> firmware.file
            is FirmwareStatus.Outdated -> firmware.file
            is FirmwareStatus.Custom -> firmware.file
            is FirmwareStatus.Missing -> return false
        }

    return file.size >= MIN_FIRMWARE_SIZE &&
        file.size <= MAX_FIRMWARE_SIZE &&
        file.name.endsWith(
            FIRMWARE_EXTENSION,
            ignoreCase = true
        )
}

@Composable
fun SelectFirmwareScreen(
    repositoryStatus: RepositoryStatus,
    selectedFirmware: FirmwareFile?,
    firmwareSelectionError: String?,
    selectedFirmwareSource: FirmwareSelectionSource?,
    onSelect: (FirmwareFile, String?) -> Unit,
    onBrowse: () -> Unit,
    onBack: () -> Unit
)
{
    BackHandler(
        onBack = onBack
    )

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    )
    {
        Text(
            text = "Select Firmware",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        firmwareSelectionError?.let { message ->

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        val selectableFirmware =
            repositoryStatus.firmware.filter(::isSelectable)

        if (repositoryStatus.refreshing) {

            Text(
                "Refreshing firmware repository..."
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
        else if (
            repositoryStatus.lastCheckFailed != null &&
            (
                repositoryStatus.lastSuccessfullyChecked == null ||
                    repositoryStatus.lastCheckFailed >
                    repositoryStatus.lastSuccessfullyChecked
            )
        ) {

            Text(
                "Unable to check for firmware updates.",
                color = MaterialTheme.colorScheme.error
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        if (selectableFirmware.isEmpty()) {

            Text(
                "No firmware files are available in the repository."
            )

        } else {

            selectableFirmware.forEach { firmware ->

                when (firmware) {

                    is FirmwareStatus.Current -> {

                        FirmwareSelectionRow(
                            file = firmware.file,
                            disposition = "Current",
                            modem = firmware.modem,
                            enabled = true,
                            selected =
                                selectedFirmwareSource ==
                                    FirmwareSelectionSource.REPOSITORY &&
                                selectedFirmware?.name ==
                                    firmware.file.name,
                            onClick = {

                                onSelect(
                                    firmware.file,
                                    firmware.modem
                                )
                            }
                        )
                    }

                    is FirmwareStatus.Outdated -> {

                        FirmwareSelectionRow(
                            file = firmware.file,
                            disposition = "Outdated",
                            modem = firmware.modem,
                            enabled = true,
                            selected =
                                selectedFirmwareSource ==
                                    FirmwareSelectionSource.REPOSITORY &&
                                selectedFirmware?.name ==
                                    firmware.file.name,
                            onClick = {

                                onSelect(
                                    firmware.file,
                                    firmware.modem
                                )
                            }
                        )
                    }

                    is FirmwareStatus.Custom -> {

                        FirmwareSelectionRow(
                            file = firmware.file,
                            disposition = "Custom",
                            modem = null,
                            enabled = true,
                            selected =
                                selectedFirmwareSource ==
                                    FirmwareSelectionSource.REPOSITORY &&
                                selectedFirmware?.name ==
                                    firmware.file.name,
                            onClick = {

                                onSelect(
                                    firmware.file,
                                    null
                                )
                            }
                        )
                    }

                    is FirmwareStatus.Missing -> {
                        // Missing files are not selectable.
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = onBrowse
        ) {
            Text("Browse…")
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = onBack
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun FirmwareSelectionRow(
    file: FirmwareFile? = null,
    filename: String? = null,
    disposition: String,
    modem: String? = null,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit
)
{
    val displayName =
        file?.name
            ?: filename
            ?: return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(
                    vertical = 12.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    )
    {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Column {

            modem?.let {
                Text(
                    text = "Modem: $it"
                )
            }

            Text(
                text = "Filename: $displayName"
            )

            file?.let {
                Text(
                    text = "Size: ${it.size} bytes"
                )
            }

            Text(
                text = disposition
            )
        }
    }
}