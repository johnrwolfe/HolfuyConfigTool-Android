package com.holfuy.configtool.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holfuy.configtool.device.DeviceRepository
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.firmware.FirmwareFile
import com.holfuy.configtool.firmware.FirmwareRepository
import com.holfuy.configtool.firmware.RepositoryStatus
import com.holfuy.configtool.ui.state.FirmwareSelectionSource
import com.holfuy.configtool.ui.state.MainUiState
import com.holfuy.configtool.ui.state.SelectedFirmware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val holfuyDevice: HolfuyDevice,
    private val firmwareRepository: FirmwareRepository
) : ViewModel()
{
    companion object {
        private const val TAG = "HolfuyUSB-VM"
    }

    var uiState by mutableStateOf(MainUiState())
        private set

    val deviceStateFlow = DeviceRepository.stateFlow

    val repositoryStatus: RepositoryStatus
        get() = firmwareRepository.status

    fun configureRepository(
        rootUri: Uri
    )
    {
        firmwareRepository.configure(
            rootUri
        )
    }

    fun endRepositoryConfiguration()
    {
        firmwareRepository.endConfiguration()
    }

    fun onResume()
    {
        if (!firmwareRepository.status.configured) {

            firmwareRepository.beginConfiguration()

        } else {

            viewModelScope.launch {

                firmwareRepository.refresh()
            }

            Log.i(
                TAG,
                "Firmware repository: " +
                    firmwareRepository.status.displayName
            )
        }

        refreshSelectedFirmwareAvailability()
    }

    private fun refreshSelectedFirmwareAvailability()
    {
        val selected =
            uiState.selectedFirmware
                ?: return

        uiState = uiState.copy(
            selectedFirmwareAvailable =
                selected.file.exists()
        )
    }

    fun setFirmwareSelectionError(
        message: String
    )
    {
        uiState = uiState.copy(
            firmwareSelectionError = message
        )
    }

    fun clearFirmwareSelectionError()
    {
        uiState = uiState.copy(
            firmwareSelectionError = null
        )
    }

    fun setFirmware(
        file: FirmwareFile,
        source: FirmwareSelectionSource,
        modem: String? = null
    )
    {
        uiState = uiState.copy(
            selectedFirmware =
                SelectedFirmware(
                    file = file,
                    source = source,
                    modem = modem
                ),
            selectedFirmwareAvailable =
                file.exists()
        )
    }

    fun connect()
    {
        Log.d(TAG, "connect() called")

        viewModelScope.launch {

            uiState = uiState.copy(
                connecting = true,
                errorMessage = null
            )

            try {

                if (!holfuyDevice.connect()) {

                    uiState = uiState.copy(
                        errorMessage = "Connection failed"
                    )
                }
            }
            catch (e: Exception) {

                Log.e(
                    TAG,
                    "Connect failed",
                    e
                )

                uiState = uiState.copy(
                    errorMessage = e.message
                )
            }
            finally {

                uiState = uiState.copy(
                    connecting = false
                )
            }
        }
    }

    fun updateFirmware()
    {
        val selectedFirmware =
            uiState.selectedFirmware
                ?: return

        val firmware =
            selectedFirmware.file

        viewModelScope.launch(Dispatchers.IO) {

            try {

                Log.d(
                    TAG,
                    "updateFirmware() called"
                )

                DeviceRepository.setUpdateInProgress(
                    true
                )

                DeviceRepository.setUpdateProgress(
                    0
                )

                uiState = uiState.copy(
                    updateCompleted = false,
                    firmwareUpdateInterrupted = false,
                    firmwareUpdateError = null
                )

                val bytes =
                    try {

                        firmware
                            .openInputStream()
                            .use { input ->
                                input.readBytes()
                            }

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "Unable to load firmware: " +
                                firmware.name,
                            e
                        )

                        uiState = uiState.copy(
                            firmwareUpdateError =
                                "Unable to open the selected " +
                                    "firmware file."
                        )

                        return@launch
                    }

                Log.i(
                    TAG,
                    "Loaded firmware: ${firmware.name} " +
                        "(${bytes.size} bytes)"
                )

                val success =
                    holfuyDevice.updateFirmware(
                        bytes
                    ) { progress ->

                        DeviceRepository.setUpdateProgress(
                            progress
                        )
                    }

                uiState = uiState.copy(
                    updateCompleted = success
                )

                Log.i(
                    TAG,
                    "updateFirmware success=$success"
                )
            }
            catch (e: Exception) {

                Log.e(
                    TAG,
                    "Firmware update failed",
                    e
                )

                uiState = uiState.copy(
                    firmwareUpdateError =
                        "Firmware update failed. " +
                        "The station's application firmware " +
                        "may be corrupted. " +
                        "See the User Guide for recovery instructions."
                )
            }
            finally {

                DeviceRepository.setUpdateInProgress(
                    false
                )

                Log.d(
                    TAG,
                    "DeviceRepository state=" +
                        DeviceRepository.state
                )
            }
        }
    }

    fun firmwareUpdateInterrupted()
    {
        uiState = uiState.copy(
            firmwareUpdateInterrupted = true,
            updateCompleted = false
        )
    }

    fun clearFirmwareUpdateInterrupted()
    {
        uiState = uiState.copy(
            firmwareUpdateInterrupted = false
        )
    }

    fun clearTransientStatus()
    {
        uiState = uiState.copy(
            updateCompleted = false,
            firmwareUpdateInterrupted = false
        )
    }
}