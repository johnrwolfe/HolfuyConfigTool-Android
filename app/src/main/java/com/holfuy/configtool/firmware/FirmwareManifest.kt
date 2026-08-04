package com.holfuy.configtool.firmware

data class FirmwareManifest(
    val firmwares: List<FirmwareDescriptor>
)

data class FirmwareDescriptor(
    val path: String,
    val modem: String
)
{
    val filename: String
        get() = path.substringAfterLast('/')
}