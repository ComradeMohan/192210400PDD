package com.simats.univault

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.*

object FileUtils {
    fun getPath(context: Context, uri: Uri): String? {
        try {
            val fileName = getFileName(context, uri)
            if (fileName != null) {
                val tempFile = File(context.cacheDir, fileName)
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                return tempFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
    
    /**
     * Get the appropriate drawable resource ID for a file based on its extension
     */
    fun getFileIconResource(fileName: String): Int {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> R.drawable.ic_pdf
            "txt" -> R.drawable.ic_txt
            "doc", "docx" -> R.drawable.ic_doc
            "ppt", "pptx" -> R.drawable.ic_file
            "xls", "xlsx" -> R.drawable.ic_file
            "jpg", "jpeg", "png", "gif", "bmp" -> R.drawable.ic_file
            "mp4", "avi", "mov", "wmv" -> R.drawable.ic_file
            "mp3", "wav", "flac" -> R.drawable.ic_file
            "zip", "rar", "7z" -> R.drawable.ic_file
            else -> R.drawable.ic_file // Default file icon
        }
    }
}
