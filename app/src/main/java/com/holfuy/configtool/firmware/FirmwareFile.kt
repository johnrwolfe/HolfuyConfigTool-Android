package com.holfuy.configtool.firmware

import java.io.InputStream

interface FirmwareFile
{
    val name: String
    val size: Long

    fun openInputStream(): InputStream
}