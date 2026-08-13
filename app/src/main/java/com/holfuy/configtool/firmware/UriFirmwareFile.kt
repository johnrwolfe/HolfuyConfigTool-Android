package com.holfuy.configtool.firmware

import android.content.Context
import android.net.Uri
import java.io.InputStream

class UriFirmwareFile(
    private val context: Context,
    private val uri: Uri,
    override val name: String,
    override val size: Long
) : FirmwareFile
{
    override fun openInputStream(): InputStream
    {
        return context.contentResolver
            .openInputStream(uri)
            ?: error(
                "Unable to open '$name'."
            )
    }
}