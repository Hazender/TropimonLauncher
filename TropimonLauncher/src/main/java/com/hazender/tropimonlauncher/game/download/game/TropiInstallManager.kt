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
package com.hazender.tropimonlauncher.game.download.game

import android.content.Context
import com.hazender.tropimonlauncher.coroutine.TropiTaskSystem
import com.hazender.tropimonlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.hazender.tropimonlauncher.game.download.game.tropimon.CloudflareR2Service
import com.hazender.tropimonlauncher.game.download.game.tropimon.TropimonUpdater
import com.hazender.tropimonlauncher.game.download.game.tropimon.VersionInfo
import com.hazender.tropimonlauncher.game.version.installed.Version
import com.hazender.tropimonlauncher.game.version.installed.VersionFolders
import com.hazender.tropimonlauncher.game.version.installed.VersionsManager
import com.hazender.tropimonlauncher.utils.logging.Logger.lError
import com.hazender.tropimonlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object TropiInstallManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _isInstalling = MutableStateFlow(false)
    val isInstalling = _isInstalling.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking = _isChecking.asStateFlow()

    private var currentJob: Job? = null
    private var cachedVersionInfo: VersionInfo? = null

    fun checkAndUpdateAutomatic(
        context: Context,
        version: Version?,
        onComplete: suspend () -> Unit,
        onError: suspend (Throwable) -> Unit
    ) {
        if (_isUpdating.value || _isInstalling.value || _isChecking.value) return

        _isChecking.value = true
        currentJob = scope.launch {
            try {
                val serverInfo = getServerVersionInfo()
                    ?: throw Exception("Impossible de récupérer les informations du serveur")

                when {
                    version == null -> installFromScratch(context, serverInfo, onComplete)
                    needsMajorUpdate(version, serverInfo) -> performMajorUpdate(context, version, serverInfo, onComplete)
                    else -> performMinorUpdate(version, serverInfo, onComplete)
                }
            } catch (e: Exception) {
                handleError(e, onError)
            } finally {
                resetStates()
            }
        }
    }

    fun cancelInstallation() {
        currentJob?.cancel()
        TropiTaskSystem.cancelAll()
        resetStates()
    }

    private suspend fun getServerVersionInfo() = withContext(Dispatchers.IO) {
        cachedVersionInfo ?: CloudflareR2Service.getServerVersionInfo()?.also {
            cachedVersionInfo = it
        }
    }

    private fun needsMajorUpdate(version: Version, serverInfo: VersionInfo): Boolean {
        val versionInfo = version.getVersionInfo() ?: return false
        return versionInfo.minecraftVersion != serverInfo.minecraft ||
                versionInfo.loaderInfo?.version != serverInfo.fabric
    }

    private suspend fun installFromScratch(
        context: Context,
        serverInfo: VersionInfo,
        onComplete: suspend () -> Unit
    ) {
        lInfo("Installation complète depuis zéro...")
        _isChecking.value = false
        _isInstalling.value = true

        try {
            val versionName = "Tropimon-${serverInfo.tropimon}"

            // Installation base
            installBaseVersion(context, serverInfo, versionName)

            // Refresh et sélection
            withContext(Dispatchers.Main) {
                VersionsManager.refresh()
            }
            waitForRefresh()

            withContext(Dispatchers.Main) {
                VersionsManager.saveCurrentVersion(versionName)
            }
            delay(500)

            val installedVersion = VersionsManager.versions.value
                .find { it.getVersionName() == versionName }

            if (installedVersion != null) {
                lInfo("✅ Version trouvée : ${installedVersion.getVersionName()}")
                lInfo("Téléchargement des assets Tropimon...")

                val updateTasks = withContext(Dispatchers.IO) {
                    TropimonUpdater.createUpdateTasks(installedVersion)
                }

                if (updateTasks.isNotEmpty()) {
                    lInfo("${updateTasks.size} tâche(s) de téléchargement")
                    updateTasks.forEach { titledTask ->
                        TropiTaskSystem.submitTask(titledTask.task)
                        titledTask.task.await()
                    }
                    lInfo("✅ Assets téléchargés")
                } else {
                    lInfo("Aucun asset à télécharger")
                }
            } else {
                lError("Version installée non trouvée après refresh : $versionName", null)
                lInfo("Versions disponibles : ${VersionsManager.versions.value.map { it.getVersionName() }}")
            }

            lInfo("✅ Installation complète terminée")
            _isInstalling.value = false
            onComplete()
        } catch (e: Exception) {
            _isInstalling.value = false
            throw e
        }
    }

    private suspend fun performMajorUpdate(
        context: Context,
        currentVersion: Version,
        serverInfo: VersionInfo,
        onComplete: suspend () -> Unit
    ) = withContext(Dispatchers.IO) {
        lInfo("Mise à jour majeure détectée...")
        _isChecking.value = false
        _isInstalling.value = true

        try {
            val newVersionName = "Tropimon-${serverInfo.tropimon}"
            val oldVersionDir = File(currentVersion.getVersionPath().absolutePath)

            // Installation nouvelle version
            installBaseVersion(context, serverInfo, newVersionName)

            // Migration fichiers utilisateur
            migrateUserFiles(oldVersionDir, VersionsManager.getVersionPath(newVersionName))

            // Suppression ancienne version
            deleteOldVersion(oldVersionDir, newVersionName)

            // Refresh et sélection
            withContext(Dispatchers.Main) {
                VersionsManager.refresh()
            }
            waitForRefresh()

            withContext(Dispatchers.Main) {
                VersionsManager.saveCurrentVersion(newVersionName)
            }
            delay(500)

            val newVersion = VersionsManager.versions.value
                .find { it.getVersionName() == newVersionName }

            if (newVersion != null) {
                lInfo("Nouvelle version trouvée : ${newVersion.getVersionName()}")
                lInfo("Téléchargement des assets Tropimon...")

                val updateTasks = TropimonUpdater.createUpdateTasks(newVersion)

                if (updateTasks.isNotEmpty()) {
                    lInfo("${updateTasks.size} tâche(s) de téléchargement")
                    updateTasks.forEach { titledTask ->
                        TropiTaskSystem.submitTask(titledTask.task)
                        titledTask.task.await()
                    }
                    lInfo("Assets téléchargés")
                } else {
                    lInfo("Aucun asset à télécharger")
                }
            } else {
                lError("Nouvelle version non trouvée après refresh : $newVersionName", null)
                lInfo("Versions disponibles : ${VersionsManager.versions.value.map { it.getVersionName() }}")
            }

            lInfo("Mise à jour majeure terminée")
            _isInstalling.value = false
            onComplete()
        } catch (e: Exception) {
            _isInstalling.value = false
            throw e
        }
    }

    private suspend fun performMinorUpdate(
        version: Version,
        serverInfo: VersionInfo,
        onComplete: suspend () -> Unit
    ) {
        val expectedName = "Tropimon-${serverInfo.tropimon}"
        val currentName = version.getVersionName()

        // Renommer si nécessaire
        val versionToUpdate = if (currentName != expectedName) {
            lInfo("Renommage nécessaire: $currentName → $expectedName")
            renameVersion(version, expectedName) ?: run {
                lInfo("Échec du renommage, utilisation de la version actuelle")
                version
            }
        } else {
            version
        }

        // Vérifier les updates
        val updateResult = withContext(Dispatchers.IO) {
            TropimonUpdater.checkForUpdates(versionToUpdate)
        }

        if (!updateResult.needsUpdate) {
            _isChecking.value = false
            onComplete()
            return
        }

        _isChecking.value = false
        _isUpdating.value = true

        val updateTasks = withContext(Dispatchers.IO) {
            TropimonUpdater.createUpdateTasks(versionToUpdate)
        }

        if (updateTasks.isEmpty()) {
            _isUpdating.value = false
            onComplete()
            return
        }

        updateTasks.forEach { titledTask ->
            TropiTaskSystem.submitTask(titledTask.task)
            titledTask.task.await()
        }

        _isUpdating.value = false
        onComplete()
    }

    private suspend fun installBaseVersion(
        context: Context,
        serverInfo: VersionInfo,
        versionName: String
    ) {
        lInfo("Installation de $versionName...")

        val installInfo = GameDownloadInfo(
            customVersionName = versionName,
            gameVersion = serverInfo.minecraft,
            fabric = FabricVersion(
                inherit = serverInfo.minecraft,
                version = serverInfo.fabric,
                stable = true
            )
        )

        val versionDir = VersionsManager.getVersionPath(versionName)
        if (versionDir.exists()) {
            forceDeleteDirectory(versionDir)
        }

        TropimonInstaller(context, installInfo).installGameSuspend()

        if (!File(versionDir, "$versionName.json").exists()) {
            throw Exception("Installation échouée - JSON manquant")
        }

        lInfo("Version $versionName installée")
    }

    private suspend fun renameVersion(version: Version, newName: String): Version? {
        lInfo("Renommage: ${version.getVersionName()} → $newName")

        return try {
            VersionsManager.renameVersion(version, newName)

            withContext(Dispatchers.Main) {
                VersionsManager.refresh()
            }
            waitForRefresh()
            delay(300)

            val renamedVersion = VersionsManager.versions.value
                .find { it.getVersionName() == newName }

            if (renamedVersion != null) {
                withContext(Dispatchers.Main) {
                    VersionsManager.saveCurrentVersion(newName)
                }
                lInfo("Renommage réussi")
                renamedVersion
            } else {
                lError("Version renommée non trouvée: $newName", null)
                null
            }
        } catch (e: Exception) {
            lError("Échec du renommage", e)
            null
        }
    }

    private suspend fun migrateUserFiles(oldDir: File, newDir: File) {
        lInfo("Migration des fichiers utilisateur...")

        listOf(
            VersionFolders.MOD,
            VersionFolders.CONFIG,
            VersionFolders.RESOURCE_PACK,
            VersionFolders.SHADERS,
            VersionFolders.SAVES,
            VersionFolders.XAERO
        ).forEach { folder ->
            val oldFolder = File(oldDir, folder.folderName)
            val newFolder = File(newDir, folder.folderName)

            if (oldFolder.exists()) {
                lInfo("Déplacement de ${folder.folderName}...")
                moveDirectoryContents(oldFolder, newFolder)
            }
        }

        val oldOptions = File(oldDir, "options.txt")
        val newOptions = File(newDir, "options.txt")
        if (oldOptions.exists()) {
            lInfo("Déplacement de options.txt...")
            oldOptions.renameTo(newOptions) || run {
                oldOptions.copyTo(newOptions, overwrite = true)
                oldOptions.delete()
            }
        }

        lInfo("Migration terminée")
    }

    private suspend fun deleteOldVersion(oldDir: File, newVersionName: String) {
        val newVersionPath = VersionsManager.getVersionPath(newVersionName).absolutePath

        if (!oldDir.exists() || oldDir.absolutePath == newVersionPath) {
            return
        }

        lInfo("Suppression de l'ancienne version...")
        delay(300)

        if (forceDeleteDirectory(oldDir)) {
            lInfo("Ancienne version supprimée")
        } else {
            lInfo("Suppression partielle de l'ancienne version")
            try {
                Runtime.getRuntime().exec("rm -rf \"${oldDir.absolutePath}\"").waitFor()
                delay(300)
            } catch (e: Exception) {
                lInfo("Échec rm -rf: ${e.message}")
            }
        }
    }

    private fun moveDirectoryContents(from: File, to: File) {
        if (!from.exists() || !from.isDirectory) return
        to.mkdirs()

        from.listFiles()?.forEach { file ->
            val target = File(to, file.name)
            try {
                if (file.isDirectory) {
                    moveDirectoryContents(file, target)
                    file.delete()
                } else {
                    if (!file.renameTo(target)) {
                        file.copyTo(target, overwrite = true)
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                lInfo("Erreur déplacement ${file.name}: ${e.message}")
                try {
                    if (!target.exists()) {
                        file.copyTo(target, overwrite = true)
                        file.delete()
                    }
                } catch (e2: Exception) {
                    lInfo("Échec fallback: ${e2.message}")
                }
            }
        }
    }

    private fun forceDeleteDirectory(directory: File): Boolean {
        if (!directory.exists()) return true
        if (!directory.isDirectory) return directory.delete()

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                forceDeleteDirectory(file)
            } else {
                if (!file.delete()) {
                    file.setWritable(true)
                    file.setReadable(true)
                    file.delete()
                }
            }
        }

        val deleted = directory.delete()
        if (!deleted) {
            directory.setWritable(true)
            directory.setReadable(true)
            return directory.delete()
        }
        return deleted
    }

    private suspend fun waitForRefresh() {
        var attempts = 0
        while (VersionsManager.isRefreshing && attempts < 50) {
            delay(100)
            attempts++
        }
    }

    private fun resetStates() {
        _isInstalling.value = false
        _isUpdating.value = false
        _isChecking.value = false
        currentJob = null
    }

    private suspend fun handleError(e: Exception, onError: suspend (Throwable) -> Unit) {
        e.printStackTrace()
        if (e !is CancellationException) {
            lInfo("Erreur: ${e.message}")
            onError(e)
        }
    }
}