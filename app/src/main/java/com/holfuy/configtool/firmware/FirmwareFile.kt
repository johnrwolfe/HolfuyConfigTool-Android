package com.holfuy.configtool.firmware

import java.io.InputStream

const val MAX_FIRMWARE_SIZE = 200 * 1024L
const val FIRMWARE_EXTENSION = ".bin"

interface FirmwareFile
{
    val name: String
    val size: Long

    /**
     * Returns whether the underlying file currently exists.
     *
     * name and size are immutable snapshots; exists() queries
     * the current state of the underlying storage.
     */
    fun exists(): Boolean

    fun openInputStream(): InputStream
}