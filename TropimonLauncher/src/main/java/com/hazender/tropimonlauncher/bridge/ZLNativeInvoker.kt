/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
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

package com.hazender.tropimonlauncher.bridge

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import com.hazender.tropimonlauncher.context.GlobalContext
import com.hazender.tropimonlauncher.game.launch.Launcher
import com.hazender.tropimonlauncher.info.InfoDistributor
import com.hazender.tropimonlauncher.utils.file.shareFile
import com.hazender.tropimonlauncher.utils.killProgress
import com.hazender.tropimonlauncher.utils.logging.Logger.lInfo
import com.hazender.tropimonlauncher.utils.network.openLink
import java.io.File

object ZLNativeInvoker {
    @JvmStatic
    var staticLauncher: Launcher? = null

    @JvmStatic
    fun openLink(link: String) {
        (GlobalContext as? Activity)?.let { activity ->
            activity.runOnUiThread {
                var prefix = "file:"
                if (link.startsWith(prefix)) {
                    if (link.startsWith("file://")) prefix += "//"
                    val newLink = link.removePrefix(prefix)
                    lInfo("open link: $newLink")

                    val file = File(newLink)
                    shareFile(activity, file)
                    lInfo("In-game Share File/Folder: ${file.absolutePath}")
                } else {
                    activity.openLink(link, "*/*")
                }
            }
        }
    }

    @JvmStatic
    fun querySystemClipboard() {
        (GlobalContext as? Activity)?.let { activity ->
            activity.runOnUiThread {
                val clipData = (activity.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip ?: run {
                    ZLBridge.clipboardReceived(null, null)
                    return@runOnUiThread
                }
                val clipItemText = clipData.getItemAt(0).text ?: run {
                    ZLBridge.clipboardReceived(null, null)
                    return@runOnUiThread
                }
                ZLBridge.clipboardReceived(clipItemText.toString(), "plain")
            }
        }
    }

    @JvmStatic
    fun putClipboardData(data: String, mimeType: String) {
        (GlobalContext as? Activity)?.let { activity ->
            activity.runOnUiThread {
                val clipData = when (mimeType) {
                    "text/plain" -> ClipData.newPlainText(InfoDistributor.LAUNCHER_IDENTIFIER, data)
                    "text/html" -> ClipData.newHtmlText(InfoDistributor.LAUNCHER_IDENTIFIER, data, data)
                    else -> null
                }
                clipData?.let {
                    (activity.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(it)
                }
            }
        }
    }

    @JvmStatic
    fun putFpsValue(fps: Int) {
        ZLBridgeStates.currentFPS = fps
    }

    @JvmStatic
    fun jvmExit(exitCode: Int, isSignal: Boolean) {
        staticLauncher?.exit()
        staticLauncher?.onExit?.invoke(exitCode, isSignal)
        staticLauncher = null
        killProgress()
    }
}