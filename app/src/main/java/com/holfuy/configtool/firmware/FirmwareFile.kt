package com.holfuy.configtool.firmware

import java.io.InputStream

const val MIN_FIRMWARE_SIZE = 48L
const val MAX_FIRMWARE_SIZE = 200 * 1024L
const val FIRMWARE_EXTENSION = ".bin"

interface FirmwareFile
{
    val name: String
    val size: Long

    /**
     * Returns whether the underlying file currently exists.
     *
     * name and size are immutable snapshots because the UI must display
     * them even when the file itself does not currently exist on the device;
     * exists() queries the current state of the underlying storage.
     */
    fun exists(): Boolean

    fun openInputStream(): InputStream
}