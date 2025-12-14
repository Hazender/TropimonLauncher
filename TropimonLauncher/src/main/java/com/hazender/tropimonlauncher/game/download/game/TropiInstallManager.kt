package com.hazender.tropimonlauncher.game.download.game

import com.hazender.tropimonlauncher.coroutine.TropiTaskSystem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object TropiInstallManager {
    private val installationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _isInstalling = MutableStateFlow(false)
    val isInstalling = _isInstalling.asStateFlow()

    private var currentInstallationJob: Job? = null

    fun startInstallation(
        installer: TropimonInstaller,
        onInstalled: suspend () -> Unit,
        onError: suspend (Throwable) -> Unit,
        onGameAlreadyInstalled: suspend () -> Unit,
        onCancelled: suspend () -> Unit
    ) {
        if (_isInstalling.value) return

        _isInstalling.value = true

        installationScope.launch {
            try {
                installer.installGameSuspend()
                onInstalled()
            } catch (e: TropimonAlreadyInstalledException) {
                onGameAlreadyInstalled()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    onCancelled()
                } else {
                    onError(e)
                }
            } finally {
                _isInstalling.value = false
                currentInstallationJob = null
            }
        }
    }

    fun cancelInstallation() {
        currentInstallationJob?.cancel()
        TropiTaskSystem.cancelAll()
        _isInstalling.value = false
    }
}