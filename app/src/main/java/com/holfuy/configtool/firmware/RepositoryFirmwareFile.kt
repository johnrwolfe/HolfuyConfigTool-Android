package com.holfuy.configtool.firmware

import java.io.InputStream

class RepositoryFirmwareFile(
    private val storage: RepositoryStorage,
    override val name: String,
    override val size: Long
) : FirmwareFile
{
    override fun exists(): Boolean
    {
        return storage.find(name) != null
    }

    override fun openInputStream(): InputStream
    {
        val currentFile =
            storage.find(name)
                ?: error(
                    "Repository file '$name' does not exist."
                )

        return storage.openInputStream(
            currentFile
        )
    }
}