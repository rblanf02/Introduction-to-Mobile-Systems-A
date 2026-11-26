package com.steppy.app.platform.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.steppy.app.data.backup.BackupCodec
import com.steppy.app.ui.SteppyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

/** Opens the system file picker to save or pick a backup file. */
class BackupFileActions(
    val saveBackup: () -> Unit,
    val pickRestoreFile: () -> Unit
)

/**
 * Uses the Storage Access Framework, so the user picks where the file goes (for example
 * Google Drive) and the app needs no storage permission. Nothing is uploaded by the app.
 */
@Composable
fun rememberBackupFileActions(viewModel: SteppyViewModel): BackupFileActions {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri ->
        // Null when the user closed the picker
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = try {
                val json = viewModel.createBackupJson()
                withContext(Dispatchers.IO) { writeText(context, uri, json) }
                true
            } catch (e: IOException) {
                false
            } catch (e: SecurityException) {
                false
            }
            viewModel.onBackupSaved(saved)
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { readTextOrNull(context, uri) }
            viewModel.onRestoreFileRead(text)
        }
    }

    return BackupFileActions(
        saveBackup = { saveLauncher.launch("steppy-backup-${LocalDate.now()}.json") },
        // Cloud storage apps don't always report JSON files as application/json
        pickRestoreFile = { openLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "text/plain", "application/octet-stream")) }
    )
}

private const val BACKUP_MIME_TYPE = "application/json"

private fun writeText(context: Context, uri: Uri, text: String) {
    // "wt" truncates, so an existing file is replaced completely
    val stream = context.contentResolver.openOutputStream(uri, "wt")
        ?: throw IOException("Can't open $uri")
    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}

/**
 * The file's text, or null if it can't be read. Reading stops just past
 * [BackupCodec.MAX_FILE_BYTES], so a huge file is never fully loaded; the codec refuses it.
 */
private fun readTextOrNull(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        input.readNBytesCompat(BackupCodec.MAX_FILE_BYTES + 1).toString(Charsets.UTF_8)
    }
} catch (e: IOException) {
    null
} catch (e: SecurityException) {
    null
}

/** InputStream.readNBytes needs API 33; min SDK is 26. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (out.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}
