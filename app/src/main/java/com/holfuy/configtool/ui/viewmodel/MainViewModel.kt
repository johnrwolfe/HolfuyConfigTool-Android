package com.holfuy.configtool.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.holfuy.configtool.device.DeviceRepository
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.firmware.FirmwareRepository
import com.holfuy.configtool.ui.state.MainUiState

class MainViewModel(
    private val holfuyDevice: HolfuyDevice,
    val firmwareRepository: FirmwareRepository
) : ViewModel()
{
    companion object {
        private const val TAG = "HolfuyUSB-VM"
    }
    
    var uiState by mutableStateOf(MainUiState())
        private set
        
    private var firmwareBytes: ByteArray? = null
    val deviceStateFlow = DeviceRepository.stateFlow
      
    fun onResume()
    {
        if (!firmwareRepository.configured) {
    
            firmwareRepository.beginConfiguration()
    
        } else {
    
            viewModelScope.launch {
    
                firmwareRepository.refresh()
            }
            
            Log.i(
                TAG,
                "Firmware repository: ${firmwareRepository.displayName}"
            )
        }
    }
    
    fun setFirmware(
        fileName: String,
        bytes: ByteArray
    )
    {
        firmwareBytes = bytes
    
        uiState = uiState.copy(
            firmwareFileName = fileName,
            firmwareSize = bytes.size,
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
        val bytes = firmwareBytes ?: return
    
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
                    updateCompleted = false
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
                    errorMessage = e.message
                )
            }
            finally {
    
                DeviceRepository.setUpdateInProgress(
                    false
                )
    
                Log.d(
                    TAG,
                    "DeviceRepository state=${DeviceRepository.state}"
                )
            }
        }
    }
    
    fun clearTransientStatus()
    {
        uiState = uiState.copy(
            updateCompleted = false
        )
    }
}