package com.holfuy.configtool.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.holfuy.configtool.usb.HolfuyUsb

class AndroidUsbDeviceProvider(
    private val usbManager: UsbManager
) : UsbDeviceProvider
{
    override fun findDevice(): UsbDevice?
    {
        return usbManager.deviceList
            .values
            .firstOrNull(HolfuyUsb::isSupported)
    }

    override fun hasPermission(
        device: UsbDevice
    ): Boolean
    {
        return usbManager.hasPermission(device)
    }
}