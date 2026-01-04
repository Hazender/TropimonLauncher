package com.hazender.tropimonlauncher.game.download.game.tropimon

import com.hazender.tropimonlauncher.utils.logging.Logger.lError
import com.hazender.tropimonlauncher.utils.network.downloadFromMirrorList
import kotlinx.coroutines.runInterruptible
import java.io.File

class TropimonDownloadTask(
    val url: String,
    val targetFile: File,
    val sha1: String?,
    val fileSize: Long,
    private val onFileDownloadedSize: (Long) -> Unit = {},
    private val onFileDownloadStarted: () -> Unit = {},
    private val onFileDownloaded: () -> Unit = {}
) {
    suspend fun download() {
        onFileDownloadStarted()

        runCatching {
            runInterruptible {
                downloadFromMirrorList(
                    urls = listOf(url),
                    sha1 = sha1,
                    outputFile = targetFile,
                    bufferSize = 32768
                ) { downloadedBytes ->
                    onFileDownloadedSize(downloadedBytes)
                }
            }
            onFileDownloaded()
        }.onFailure { e ->
            lError("Failed to download ${targetFile.name}: ${e.message}")
            throw e
        }
    }
}