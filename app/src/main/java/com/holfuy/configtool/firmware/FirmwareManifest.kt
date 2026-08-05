package com.holfuy.configtool.firmware

import kotlinx.serialization.Serializable

@Serializable
data class FirmwareManifest(
    val firmwares: List<FirmwareDescriptor>
)

@Serializable
data class FirmwareDescriptor(
    val path: String,
    val modem: String,
    val sha256: String
)
{
    val filename: String
        get() = path.substringAfterLast('/')
}