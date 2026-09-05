package com.holfuy.configtool.usb

import android.hardware.usb.UsbDevice

interface UsbDeviceProvider
{
    fun findDevice(): UsbDevice?

    fun hasPermission(
        device: UsbDevice
    ): Boolean
}