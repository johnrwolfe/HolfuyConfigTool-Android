package com.holfuy.configtool.firmware

import java.io.InputStream

const val MAX_FIRMWARE_SIZE = 200 * 1024L
const val FIRMWARE_EXTENSION = ".bin"

interface FirmwareFile
{
    val name: String
    val size: Long

    fun openInputStream(): InputStream
}