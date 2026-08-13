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
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.holfuy.configtool.BuildConfig
import com.holfuy.configtool.device.DeviceState
import com.holfuy.configtool.firmware.FirmwareFile
import com.holfuy.configtool.firmware.FirmwareStatus
import com.holfuy.configtool.firmware.RepositoryStatus
import com.holfuy.configtool.ui.state.MainUiState

private val SECTION_SPACING = 24.dp

@Composable
fun MainScreen(
    uiState: MainUiState,
    deviceState: DeviceState,
    onConnectClick: () -> Unit,
    onSelectFirmwareClick: () -> Unit,
    onUpdateFirmwareClick: () -> Unit,
    onHelpClick: () -> Unit,
    repositoryStatus: RepositoryStatus
)
{
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
            text = "Holfuy Upgrader",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onHelpClick
        ) {
            Text("Help")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Firmware Repository")

                Text(
                    repositoryStatus.displayName
                        ?: "Not configured"
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (repositoryStatus.refreshing) {

                    Text("Refreshing...")

                } else {

                    repositoryStatus.lastSuccessfullyChecked
                        ?.let(::formatInstant)
                        ?.let {

                            Text(
                                "Last successfully checked: $it",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                    val lastCheckFailed =
                        repositoryStatus.lastCheckFailed

                    val lastSuccessfullyChecked =
                        repositoryStatus.lastSuccessfullyChecked

                    if (
                        lastCheckFailed != null &&
                        (
                            lastSuccessfullyChecked == null ||
                            lastCheckFailed > lastSuccessfullyChecked
                        )
                    ) {

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            "Unable to check for firmware at " +
                                formatInstant(lastCheckFailed),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (repositoryStatus.firmware.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    repositoryStatus.firmware.forEach { firmware ->

                        when (firmware) {

                            is FirmwareStatus.Current -> {

                                Text(
                                    "${firmware.file.name}: Current"
                                )
                            }

                            is FirmwareStatus.Outdated -> {

                                Text(
                                    "${firmware.file.name}: Outdated"
                                )
                            }

                            is FirmwareStatus.Missing -> {

                                Text(
                                    "${firmware.filename}: Missing"
                                )
                            }

                            is FirmwareStatus.Unknown -> {

                                Text(
                                    "${firmware.file.name}: Unknown"
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SECTION_SPACING))

        Button(
            enabled = !deviceState.updateInProgress,
            onClick = onSelectFirmwareClick
        ) {
            Text("Select Firmware")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Selected Firmware")

                Text(
                    uiState.selectedFirmware?.name
                        ?: "No file selected"
                )

                uiState.selectedFirmware?.let { firmware ->
                    Text("Size: ${firmware.size} bytes")
                }
            }
        }

        Spacer(modifier = Modifier.height(SECTION_SPACING))

        Button(
            enabled =
                deviceState.attached &&
                !deviceState.connected &&
                !deviceState.updateInProgress,
            onClick = onConnectClick
        ) {
            Text(
                if (uiState.connecting)
                    "Connecting..."
                else
                    "Connect"
            )
        }

        Spacer(modifier = Modifier.height(SECTION_SPACING))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Connection Status")

                Text(
                    if (deviceState.connected)
                        "Connected"
                    else
                        "Disconnected"
                )
            }
        }

        Spacer(modifier = Modifier.height(SECTION_SPACING))

        Button(
            enabled =
                deviceState.connected &&
                !deviceState.updateInProgress &&
                uiState.selectedFirmware != null,
            onClick = onUpdateFirmwareClick
        ) {
            Text("Update Firmware")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Update Status")

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    deviceState.updateInProgress -> {

                        LinearProgressIndicator(
                            progress = {
                                deviceState.updateProgress / 100f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("${deviceState.updateProgress}%")
                    }

                    uiState.updateCompleted -> {

                        Text("Firmware update complete")
                    }

                    else -> {

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SECTION_SPACING))

        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatInstant(
    instant: Instant
): String
{
    return DateTimeFormatter
        .ofLocalizedDateTime(
            FormatStyle.MEDIUM,
            FormatStyle.SHORT
        )
        .withZone(ZoneId.systemDefault())
        .format(instant)
}