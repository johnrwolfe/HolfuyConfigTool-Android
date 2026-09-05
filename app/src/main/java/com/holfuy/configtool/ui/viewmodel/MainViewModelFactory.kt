package com.holfuy.configtool.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.firmware.FirmwareRepository
import com.holfuy.configtool.usb.UsbDeviceProvider

class MainViewModelFactory(
    private val holfuyDevice: HolfuyDevice,
    private val usbDeviceProvider: UsbDeviceProvider,
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
            usbDeviceProvider,
            firmwareRepository
        ) as T
    }
}