package com.holfuy.configtool

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.device.RealHolfuyDevice
import com.holfuy.configtool.firmware.FirmwareRepository
import com.holfuy.configtool.firmware.ManifestConfiguration
import com.holfuy.configtool.firmware.RepositoryStorage
import com.holfuy.configtool.ui.viewmodel.MainViewModelFactory
import com.holfuy.configtool.usb.AndroidUsbDeviceProvider
import com.holfuy.configtool.usb.UsbDeviceProvider

class HolfuyApplication : Application()
{
    private val usbManager: UsbManager by lazy {
        getSystemService(
            Context.USB_SERVICE
        ) as UsbManager
    }

    private val usbDeviceProvider: UsbDeviceProvider by lazy {
        AndroidUsbDeviceProvider(
            usbManager
        )
    }

    private val holfuyDevice: HolfuyDevice by lazy {
        RealHolfuyDevice(
            usbManager,
            usbDeviceProvider
        )
    }

    private val firmwareRepository: FirmwareRepository by lazy {
        FirmwareRepository(
            RepositoryStorage(
                applicationContext
            ),
            ManifestConfiguration(
                applicationContext
            )
        )
    }

    val mainViewModelFactory: MainViewModelFactory by lazy {
        MainViewModelFactory(
            holfuyDevice,
            usbDeviceProvider,
            firmwareRepository
        )
    }
}