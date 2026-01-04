package com.hazender.tropimonlauncher.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object TropiTaskSystem {

    private val _tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    val tasksFlow = _tasksFlow.asStateFlow()

    private val taskQueue = Channel<Task>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var processorJob: Job? = null
    private var currentTaskJob: Job? = null

    init {
        startProcessor()
    }

    private fun startProcessor() {
        processorJob?.cancel()
        processorJob = scope.launch {
            try {
                while (isActive) {
                    val task = taskQueue.receive()

                    _tasksFlow.update { it + task }
                    task.taskState = TaskState.RUNNING

                    try {
                        currentTaskJob = coroutineContext[Job]

                        withContext(task.dispatcher) {
                            task.task(this, task)
                        }
                        task.taskState = TaskState.COMPLETED
                    } catch (e: CancellationException) {
                        task.taskState = TaskState.CANCELLED
                        task.onCancel()
                        throw e
                    } catch (e: Exception) {
                        task.taskState = TaskState.COMPLETED
                        task.onError(e)
                    } finally {
                        currentTaskJob = null
                        task.onFinally()
                        _tasksFlow.update { current -> current.filterNot { it.id == task.id } }
                    }
                }
            } catch (e: CancellationException) {
            }
        }
    }

    fun submitTask(task: Task) {
        scope.launch { taskQueue.send(task) }
    }

    fun cancelAll() {
        // Annuler la tâche en cours d'exécution
        currentTaskJob?.cancel()

        // Annuler le processor
        processorJob?.cancel()

        // Vider la file d'attente
        while (taskQueue.tryReceive().getOrNull() != null) { /* vide */ }

        // Marquer toutes les tâches comme annulées
        _tasksFlow.value.forEach { task ->
            if (task.taskState != TaskState.COMPLETED) {
                task.taskState = TaskState.CANCELLED
                task.onCancel()
            }
        }

        _tasksFlow.value = emptyList()

        // Relancer le processor
        startProcessor()
    }

    fun containsTask(taskId: String?): Boolean {
        if (taskId == null) return false
        return _tasksFlow.value.any { it.id == taskId }
    }
}