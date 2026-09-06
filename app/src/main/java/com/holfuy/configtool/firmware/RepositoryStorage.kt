package com.holfuy.configtool.firmware

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest


class RepositoryStorage(
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

    val configured: Boolean
        get() = getRoot() != null
        
    val displayName: String?
        get()
        {
            val root = getRoot()
                ?: return null
    
            val treeId =
                DocumentsContract.getTreeDocumentId(root.uri)
    
            return treeId.substringAfter(':')
        }
        
    fun setRepositoryRoot(
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
    
    fun openInputStream(
        file: DocumentFile
    ): InputStream
    {
        return context.contentResolver
            .openInputStream(file.uri)
            ?: error(
                "Unable to open '${file.name}'."
            )
    }
    
    fun firmwareFile(
        file: DocumentFile
    ): FirmwareFile
    {
        val name =
            file.name
                ?: error(
                    "Repository file has no name."
                )
    
        return RepositoryFirmwareFile(
            storage = this,
            name = name,
            size = file.length()
        )
    }
    
    fun firmwareFile(
        name: String,
        size: Long
    ): FirmwareFile
    {
        return RepositoryFirmwareFile(
            storage = this,
            name = name,
            size = size
        )
    }
    
    fun openOutputStream(
        file: DocumentFile
    ): OutputStream
    {
        return context.contentResolver
            .openOutputStream(file.uri)
            ?: error(
                "Unable to open '${file.name}'."
            )
    }
    
    fun sha256(
        file: DocumentFile
    ): String
    {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )
    
        openInputStream(file).use { input ->
    
            val buffer = ByteArray(8192)
    
            while (true) {
    
                val count =
                    input.read(buffer)
    
                if (count <= 0)
                    break
    
                digest.update(
                    buffer,
                    0,
                    count
                )
            }
        }
    
        return digest.digest()
            .joinToString("") {
                "%02x".format(it)
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
    
    fun promote(
        tempFilename: String,
        filename: String
    )
    {
        val temp =
            find(tempFilename)
                ?: error(
                    "'$tempFilename' does not exist."
                )
    
        find(filename)
            ?.delete()
    
        val renamedUri =
            DocumentsContract.renameDocument(
                context.contentResolver,
                temp.uri,
                filename
            )
                ?: error(
                    "Unable to rename '$tempFilename'."
                )
    
        check(
            renamedUri == temp.uri || find(filename) != null
        ) {
            "Unable to promote '$filename'."
        }
    }
    
    fun deleteIfPresent(
        filename: String
    )
    {
        find(filename)
            ?.delete()
    }
}