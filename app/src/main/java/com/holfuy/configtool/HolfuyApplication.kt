package com.holfuy.configtool

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Build
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.device.RealHolfuyDevice
import com.holfuy.configtool.diagnostics.DiagnosticLogger
import com.holfuy.configtool.firmware.FirmwareRepository
import com.holfuy.configtool.firmware.FirmwareSelectionStore
import com.holfuy.configtool.firmware.ManifestConfiguration
import com.holfuy.configtool.firmware.RepositoryStorage
import com.holfuy.configtool.ui.viewmodel.MainViewModelFactory
import com.holfuy.configtool.usb.AndroidUsbDeviceProvider
import com.holfuy.configtool.usb.UsbDeviceProvider
import java.io.File

class HolfuyApplication : Application()
{
    private val usbManager: UsbManager by lazy {
        getSystemService(
            Context.USB_SERVICE
        ) as UsbManager
    }

    val diagnosticLogger: DiagnosticLogger by lazy {
        DiagnosticLogger(
            File(
                filesDir,
                "diagnostics.log"
            )
        )
    }

    private val usbDeviceProvider: UsbDeviceProvider by lazy {
        AndroidUsbDeviceProvider(
            usbManager
        )
    }

    private val holfuyDevice: HolfuyDevice by lazy {
        RealHolfuyDevice(
            usbManager,
            usbDeviceProvider,
            diagnosticLogger
        )
    }

    private val firmwareRepository: FirmwareRepository by lazy {
        FirmwareRepository(
            RepositoryStorage(
                applicationContext
            ),
            ManifestConfiguration(
                applicationContext
            ),
            diagnosticLogger
        )
    }

    val firmwareSelectionStore by lazy {
        FirmwareSelectionStore(this)
    }

    val mainViewModelFactory: MainViewModelFactory by lazy {
        MainViewModelFactory(
            this,
            holfuyDevice,
            usbDeviceProvider,
            firmwareRepository,
            firmwareSelectionStore,
            diagnosticLogger
        )
    }

    override fun onCreate()
    {
        super.onCreate()

        val packageInfo =
            packageManager.getPackageInfo(
                packageName,
                0
            )

        diagnosticLogger.recordApplicationStarted(
            appVersion =
                packageInfo.versionName
                    ?: "unknown",
            androidVersion =
                "${Build.VERSION.RELEASE} " +
                    "(API ${Build.VERSION.SDK_INT})",
            device =
                "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }
}