package com.holfuy.configtool.firmware

sealed class FirmwareStatus
{
    data class Current(
        val file: FirmwareFile,
        val modem: String
    ) : FirmwareStatus()

    data class Outdated(
        val file: FirmwareFile,
        val modem: String
    ) : FirmwareStatus()

    data class Custom(
        val file: FirmwareFile
    ) : FirmwareStatus()

    data class Missing(
        val filename: String,
        val modem: String
    ) : FirmwareStatus()
}