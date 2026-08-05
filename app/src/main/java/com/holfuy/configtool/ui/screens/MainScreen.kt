package com.holfuy.configtool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.holfuy.configtool.BuildConfig
import com.holfuy.configtool.device.DeviceState
import com.holfuy.configtool.ui.state.MainUiState

private val SECTION_SPACING = 24.dp

@Composable
fun MainScreen(
    uiState: MainUiState,
    deviceState: DeviceState,
    onConnectClick: () -> Unit,
    onSelectFirmwareClick: () -> Unit,
    onUpdateFirmwareClick: () -> Unit,
    onHelpClick: () -> Unit
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
                    uiState.firmwareFileName
                        ?: "No file selected"
                )

                uiState.firmwareSize?.let {
                    Text("Size: $it bytes")
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
                uiState.firmwareFileName != null,
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