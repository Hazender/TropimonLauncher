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

package com.hazender.tropimonlauncher.coroutine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class Task private constructor(
    val id: String,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val task: suspend CoroutineScope.(Task) -> Unit,
    val onError: suspend (Throwable) -> Unit = {},
    val onFinally: () -> Unit = {},
    val onCancel: () -> Unit = {}
) {
    var taskState by mutableStateOf(TaskState.PREPARING)

    var currentProgress by mutableFloatStateOf(-1f)
        private set
    var currentMessageRes by mutableStateOf<Int?>(null)
        private set
    var currentMessageArgs by mutableStateOf<Array<out Any>?>(null)
        private set

    suspend fun await() {
        while (taskState != TaskState.COMPLETED && taskState != TaskState.CANCELLED) {
            delay(50)
        }
        if (taskState == TaskState.CANCELLED) {
            throw CancellationException("Task was cancelled")
        }
    }

    fun updateProgress(percentage: Float) {
        this.currentProgress = (percentage.takeIf { it.isFinite() } ?: 0f).coerceIn(-1f, 1f)
    }

    fun updateProgress(percentage: Float, message: Int?) {
        this.updateProgress(percentage = percentage)
        this.updateMessage(message = message)
    }

    fun updateProgress(percentage: Float, message: Int?, vararg args: Any) {
        this.updateProgress(percentage = percentage)
        this.updateMessage(message = message, args = args)
    }

    fun updateMessage(message: Int?) {
        this.currentMessageRes = message
        this.currentMessageArgs = null
    }

    fun updateMessage(message: Int?, vararg args: Any) {
        this.currentMessageRes = message
        this.currentMessageArgs = args
    }

    override fun equals(other: Any?): Boolean = other is Task && other.id == this.id

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun runTask(
            id: String? = null,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            task: suspend CoroutineScope.(Task) -> Unit,
            onError: suspend (Throwable) -> Unit = {},
            onFinally: () -> Unit = {},
            onCancel: () -> Unit = {}
        ): Task =
            Task(
                id = id ?: getRandomID(),
                dispatcher = dispatcher,
                task = task,
                onError = onError,
                onFinally = onFinally,
                onCancel = onCancel
            )

        private fun getRandomID(): String = UUID.randomUUID().toString()
    }
}