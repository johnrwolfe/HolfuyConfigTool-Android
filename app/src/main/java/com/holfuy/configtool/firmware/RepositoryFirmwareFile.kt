package com.holfuy.configtool.firmware

import androidx.documentfile.provider.DocumentFile
import java.io.InputStream

class RepositoryFirmwareFile(
    private val storage: RepositoryStorage,
    private val documentFile: DocumentFile
) : FirmwareFile
{
    override val name: String =
        documentFile.name
            ?: error("Repository file has no name.")

    override val size: Long =
        documentFile.length()

    override fun openInputStream(): InputStream
    {
        return storage.openInputStream(
            documentFile
        )
    }
}