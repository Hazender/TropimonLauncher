/*
 * Tropimon Launcher
 * Copyright (C) 2025 Hazender
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */
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