package com.holfuy.configtool.firmware

sealed class FirmwareStatus
{
    abstract val filename: String

    data class Current(
        val file: FirmwareFile,
        val modem: String
    ) : FirmwareStatus()
    {
        override val filename: String
            get() = file.name
    }

    data class Outdated(
        val file: FirmwareFile,
        val modem: String
    ) : FirmwareStatus()
    {
        override val filename: String
            get() = file.name
    }

    data class Custom(
        val file: FirmwareFile
    ) : FirmwareStatus()
    {
        override val filename: String
            get() = file.name
    }

    data class Missing(
        override val filename: String,
        val modem: String
    ) : FirmwareStatus()
}