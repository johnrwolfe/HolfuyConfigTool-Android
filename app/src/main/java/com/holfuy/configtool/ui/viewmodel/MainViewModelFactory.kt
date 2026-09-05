package com.holfuy.configtool.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.firmware.FirmwareRepository

class MainViewModelFactory(
    private val holfuyDevice: HolfuyDevice,
    private val firmwareRepository: FirmwareRepository
) : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T
    {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            holfuyDevice,
            firmwareRepository
        ) as T
    }
}