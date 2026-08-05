package com.holfuy.configtool.firmware

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile


class FirmwareRepository(
    private val context: Context
)
{
    companion object
    {
        private const val PREFS_NAME = "firmware_repository"
        private const val KEY_ROOT_URI = "root_uri"
    }

    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    val isConfigured: Boolean
        get() = getRoot() != null
        
    fun getDisplayName(): String?
    {
        val root = getRoot()
            ?: return null
    
        val treeId =
            DocumentsContract.getTreeDocumentId(root.uri)
    
        return treeId.substringAfter(':')
    }

    fun configure(
        rootUri: Uri
    )
    {
        prefs.edit()
            .putString(
                KEY_ROOT_URI,
                rootUri.toString()
            )
            .apply()
    }

    fun getRoot(): DocumentFile?
    {
        val rootUri =
            prefs.getString(
                KEY_ROOT_URI,
                null
            )?.let(Uri::parse)
                ?: return null

        return DocumentFile.fromTreeUri(
            context,
            rootUri
        )
    }

    fun listFiles(): List<DocumentFile>
    {
        return getRoot()
            ?.listFiles()
            ?.filter {
                it.isFile
            }
            ?.toList()
            ?: emptyList()
    }

    fun find(
        filename: String
    ): DocumentFile?
    {
        return listFiles()
            .firstOrNull {
                it.name == filename
            }
    }
    
    fun createOrReplace(
        filename: String
    ): DocumentFile
    {
        val root =
            getRoot()
                ?: error(
                    "Firmware repository is not configured."
                )
    
        find(filename)
            ?.delete()
    
        return root.createFile(
            "application/octet-stream",
            filename
        )
            ?: error(
                "Unable to create '$filename'."
            )
    }
}