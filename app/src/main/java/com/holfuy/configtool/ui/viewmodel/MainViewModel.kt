package com.holfuy.configtool.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holfuy.configtool.device.DeviceRepository
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.diagnostics.DiagnosticLogger
import com.holfuy.configtool.firmware.FirmwareFile
import com.holfuy.configtool.firmware.FirmwareRepository
import com.holfuy.configtool.firmware.FirmwareSelectionStore
import com.holfuy.configtool.firmware.RepositoryStatus
import com.holfuy.configtool.firmware.StoredFirmwareSelection
import com.holfuy.configtool.firmware.UriFirmwareFile
import com.holfuy.configtool.ui.state.FirmwareSelectionSource
import com.holfuy.configtool.ui.state.MainUiState
import com.holfuy.configtool.ui.state.SelectedFirmware
import com.holfuy.configtool.usb.UsbDeviceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val holfuyDevice: HolfuyDevice,
    private val usbDeviceProvider: UsbDeviceProvider,
    private val firmwareRepository: FirmwareRepository,
    private val firmwareSelectionStore: FirmwareSelectionStore,
    private val diagnosticLogger: DiagnosticLogger
) : AndroidViewModel(application)
{
    companion object {
        private const val TAG = "HolfuyUSB-VM"

        private const val FIRMWARE_UPDATE_FAILURE_MESSAGE =
            "Device disconnected. Firmware update interrupted. " +
            "Please restart the firmware upgrade process. " +
            "If it won't work after several tries please contact " +
            "Holfuy Support."
    }

    private val _uiState =
        MutableStateFlow(MainUiState())

    val uiState =
        _uiState.asStateFlow()

    val deviceStateFlow = DeviceRepository.stateFlow

    val repositoryStatus: RepositoryStatus
        get() = firmwareRepository.status

    init
    {
        restoreFirmwareSelection()
    }

    private fun restoreFirmwareSelection()
    {
        val storedSelection =
            firmwareSelectionStore.getSelection()
                ?: return

        val file =
            when (storedSelection.source)
            {
                FirmwareSelectionSource.REPOSITORY ->
                    firmwareRepository.firmwareFile(
                        name = storedSelection.name,
                        size = storedSelection.size
                    )

                FirmwareSelectionSource.BROWSE ->
                    storedSelection.uri?.let { uri ->
                        UriFirmwareFile(
                            context = getApplication<Application>(),
                            uri = uri,
                            name = storedSelection.name,
                            size = storedSelection.size
                        )
                    }
            }
                ?: return

        _uiState.update {
            it.copy(
                selectedFirmware =
                    SelectedFirmware(
                        file = file,
                        source = storedSelection.source,
                        modem = storedSelection.modem
                    ),
                selectedFirmwareAvailable =
                    file.exists()
            )
        }
    }

    fun configureRepository(
        rootUri: Uri
    )
    {
        diagnosticLogger.recordRepositoryConfigured()

        firmwareRepository.configure(
            rootUri
        )

        refreshSelectedFirmwareAvailability()
    }

    fun endRepositoryConfiguration()
    {
        firmwareRepository.endConfiguration()
    }

    fun onResume()
    {
        if (!firmwareRepository.status.configured) {

            diagnosticLogger.recordRepositoryConfigurationStarted()

            firmwareRepository.beginConfiguration()

        } else {

            viewModelScope.launch {
                if (!DeviceRepository.state.updateInProgress) {
                    firmwareRepository.refresh()
                }
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
            _uiState.value.selectedFirmware
                ?: return

        _uiState.update {
            it.copy(
                selectedFirmwareAvailable =
                    selected.file.exists()
            )
        }
    }

    fun setFirmwareSelectionError(
        message: String
    )
    {
        _uiState.update {
            it.copy(
                firmwareSelectionError = message
            )
        }
    }

    fun clearFirmwareSelectionError()
    {
        _uiState.update {
            it.copy(
                firmwareSelectionError = null
            )
        }
    }

    fun setFirmware(
        file: FirmwareFile,
        source: FirmwareSelectionSource,
        modem: String? = null,
        uri: Uri? = null
    )
    {
        diagnosticLogger.recordFirmwareSelected(
            filename = file.name,
            size = file.size,
            source = source.name
        )
    
        firmwareSelectionStore.setSelection(
            StoredFirmwareSelection(
                source = source,
                name = file.name,
                size = file.size,
                modem = modem,
                uri = uri
            )
        )
    
        _uiState.update {
            it.copy(
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
    }

    fun refreshUsbState()
    {
        val usbDevice =
            usbDeviceProvider.findDevice()
    
        val permissionGranted =
            usbDevice?.let {
                usbDeviceProvider.hasPermission(it)
            } ?: false
    
        DeviceRepository.setAttached(
            usbDevice != null
        )
    
        DeviceRepository.setPermissionGranted(
            permissionGranted
        )
    
        if (usbDevice == null) {
            DeviceRepository.clearConnectionState()
        }
    }
    
    fun setUsbPermissionGranted(
        granted: Boolean
    )
    {
        DeviceRepository.setPermissionGranted(
            granted
        )
    }

    fun onUsbDetached()
    {
        Log.i(
            TAG,
            "onUsbDetached()"
        )
    
        val updateInProgress =
            DeviceRepository.state.updateInProgress
    
        holfuyDevice.onUsbDetached()
    
        DeviceRepository.clearConnectionState()
    
        if (updateInProgress) {
    
            diagnosticLogger.recordFirmwareUpdateInterrupted()
    
            DeviceRepository.setUpdateCompleted(
                false
            )
    
            DeviceRepository.setFirmwareUpdateError(
                FIRMWARE_UPDATE_FAILURE_MESSAGE
            )
    
        } else {
            clearTransientStatus()
        }
    }

    fun connect()
    {
        diagnosticLogger.recordConnectRequested()

        viewModelScope.launch(Dispatchers.IO) {

            _uiState.update {
                it.copy(
                    connecting = true,
                    connectionError = null
                )
            }

            try {

                val success =
                    holfuyDevice.connect()

                if (!success) {
                    _uiState.update {
                        it.copy(
                            connectionError =
                                "Unable to connect to Holfuy station."
                        )
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "connect(): exception from holfuyDevice.connect()",
                    e
                )

                _uiState.update {
                    it.copy(
                        connectionError =
                            e.message
                                ?: "Unable to connect to Holfuy station."
                    )
                }

            } finally {

                _uiState.update {
                    it.copy(
                        connecting = false
                    )
                }
            }
        }
    }

    fun updateFirmware()
    {
        val selectedFirmware =
            _uiState.value.selectedFirmware
                ?: return
    
        val firmware =
            selectedFirmware.file
    
        diagnosticLogger.recordFirmwareUpdateRequested(
            firmware.name
        )
    
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
    
                DeviceRepository.setUpdateCompleted(
                    false
                )
    
                DeviceRepository.setFirmwareUpdateError(
                    null
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
    
                        diagnosticLogger.recordFirmwareFileOpenFailed(
                            firmware.name
                        )
    
                        diagnosticLogger.recordFirmwareUnavailable(
                            firmware.name
                        )
    
                        DeviceRepository.setFirmwareUpdateError(
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
    
                diagnosticLogger.recordFirmwareFileOpened(
                    firmware.name,
                    bytes.size
                )
    
                val success =
                    holfuyDevice.updateFirmware(
                        bytes
                    ) { progress ->
    
                        DeviceRepository.setUpdateProgress(
                            progress
                        )
                    }
    
                DeviceRepository.setUpdateCompleted(
                    success
                )
    
                DeviceRepository.setFirmwareUpdateError(
                    if (success)
                        null
                    else
                        FIRMWARE_UPDATE_FAILURE_MESSAGE
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
    
                diagnosticLogger.recordUnexpectedException(
                    "firmware update",
                    e
                )
    
                DeviceRepository.setUpdateCompleted(
                    false
                )
    
                DeviceRepository.setFirmwareUpdateError(
                    FIRMWARE_UPDATE_FAILURE_MESSAGE
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

    fun clearTransientStatus()
    {
        _uiState.update {
            it.copy(
                firmwareSelectionError = null
            )
        }
    }

}