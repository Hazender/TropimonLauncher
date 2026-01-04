package com.hazender.tropimonlauncher.game.download.game.tropimon

import com.amazonaws.services.s3.model.S3ObjectSummary
import com.hazender.tropimonlauncher.R
import com.hazender.tropimonlauncher.coroutine.Task
import com.hazender.tropimonlauncher.coroutine.TitledTask
import com.hazender.tropimonlauncher.game.version.installed.Version
import com.hazender.tropimonlauncher.game.version.installed.VersionFolders
import com.hazender.tropimonlauncher.utils.file.formatFileSize
import com.hazender.tropimonlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class UpdateCheckResult(
    val needsUpdate: Boolean,
    val updateSize: Long
)

data class UpdateTasksData(
    val filesToDownload: List<Pair<S3ObjectSummary, File>>,
    val filesToDelete: List<File>,
    val totalSize: Long
)

object TropimonUpdater {

    /**
     * Vérifie MD5 via ETag
     */
    private fun verifyMd5(localFile: File, s3File: S3ObjectSummary): Boolean {
        if (!localFile.exists()) return false
        if (localFile.length() != s3File.size) return false

        val remoteETag = s3File.eTag?.replace("\"", "") ?: return false

        return try {
            val digest = java.security.MessageDigest.getInstance("MD5")
            localFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val localMd5 = digest.digest().joinToString("") { "%02x".format(it) }
            val matches = remoteETag.equals(localMd5, ignoreCase = true)

            if (!matches) {
                lInfo("MD5 mismatch pour ${localFile.name}: local=$localMd5, remote=$remoteETag")
            }

            matches
        } catch (e: Exception) {
            lInfo("Erreur MD5 pour ${localFile.name}: ${e.message}")
            false
        }
    }

    /**
     * Extrait le chemin relatif depuis une clé S3
     */
    private fun extractRelativePath(key: String, folderName: String): String {
        val keyParts = key.split("/")
        val folderIndex = keyParts.indexOf(folderName)
        return if (folderIndex >= 0 && folderIndex < keyParts.size - 1) {
            keyParts.subList(folderIndex + 1, keyParts.size).joinToString("/")
        } else {
            keyParts.last()
        }
    }

    /**
     * Vérifie si un fichier est dans un répertoire ignoré
     * relativePath est le chemin après extraction (ex: "fancymenu/file.txt")
     * ignoredDir vient du JSON (ex: "config/fancymenu")
     * On extrait juste la partie après "config/" pour la comparaison
     */
    private fun isInIgnoredDirectory(relativePath: String, ignoredDirectories: List<String>): Boolean {
        return ignoredDirectories.any { ignoredDir ->
            // Extraire la partie après "config/" (ex: "config/fancymenu" -> "fancymenu")
            val normalizedIgnoredDir = ignoredDir.removePrefix("config/")
            relativePath.startsWith(normalizedIgnoredDir + "/") || relativePath == normalizedIgnoredDir
        }
    }

    /**
     * Prépare les données de mise à jour (analyse ce qui doit être fait)
     */
    private suspend fun prepareUpdateData(version: Version): UpdateTasksData = withContext(Dispatchers.IO) {
        lInfo("Préparation des données de mise à jour...")

        // Récupérer la config pour accéder à ignoredDirectories
        val tropimonConfig = CloudflareR2Service.fetchConfig()
        val ignoredDirectories = tropimonConfig.ignoredDirectories

        lInfo("Répertoires ignorés (nécessitant vérification MD5): $ignoredDirectories")

        val remoteMods = CloudflareR2Service.listRequiredMods()
        val remoteConfigs = CloudflareR2Service.listConfigFiles().toMutableList()
        val remoteResourcepacks = CloudflareR2Service.listResourcepacks()
        CloudflareR2Service.getOptionsFile()?.let { remoteConfigs.add(it) }

        lInfo("Fichiers distants: ${remoteMods.size} mods, ${remoteConfigs.size} configs, ${remoteResourcepacks.size} resourcepacks")

        val versionDir = version.getVersionPath()
        val modsDir = File(versionDir, VersionFolders.MOD.folderName)
        val configDir = File(versionDir, "config")
        val resourcepacksDir = File(versionDir, "resourcepacks")
        val optionsFile = File(versionDir, "options.txt")

        modsDir.mkdirs()
        configDir.mkdirs()
        resourcepacksDir.mkdirs()

        val remoteModNames = remoteMods.map { CloudflareR2Service.getFileName(it.key) }.toSet()
        val filesToDelete = mutableListOf<File>()
        val filesToDownload = mutableListOf<Pair<S3ObjectSummary, File>>()

        // Collecter les fichiers à supprimer
        modsDir.listFiles()?.forEach { localMod ->
            if (localMod.isFile && !localMod.name.contains("LOCAL", ignoreCase = true)) {
                if (localMod.name !in remoteModNames) {
                    lInfo("À supprimer: ${localMod.name}")
                    filesToDelete.add(localMod)
                }
            }
        }

        // Collecter les mods à télécharger
        remoteMods.forEach { s3File ->
            val relativePath = extractRelativePath(s3File.key, "required-mods")
            val localFile = File(modsDir, relativePath)

            val needsDownload = if (!localFile.exists()) {
                lInfo("Mod manquant: ${localFile.name}")
                true
            } else if (!verifyMd5(localFile, s3File)) {
                lInfo("Mod à mettre à jour (MD5): ${localFile.name}")
                true
            } else {
                false
            }

            if (needsDownload) {
                if (localFile.exists()) {
                    localFile.delete()
                }
                localFile.parentFile?.mkdirs()
                filesToDownload.add(s3File to localFile)
            }
        }

        // Collecter les configs à télécharger avec gestion des ignoredDirectories
        remoteConfigs.forEach { s3File ->
            val relativePath = extractRelativePath(s3File.key, "config")
            val localFile = if (relativePath == "options.txt") {
                optionsFile
            } else {
                File(configDir, relativePath)
            }

            // Déterminer si ce fichier nécessite une vérification MD5
            val requiresMd5Check = isInIgnoredDirectory(relativePath, ignoredDirectories)

            val needsDownload = if (!localFile.exists()) {
                lInfo("Config manquant: ${localFile.name} (path: $relativePath)")
                true
            } else if (requiresMd5Check) {
                // Pour les répertoires ignorés, vérifier le MD5 comme pour les mods
                if (!verifyMd5(localFile, s3File)) {
                    lInfo("Config à mettre à jour (MD5 - ignoredDir): ${localFile.name} (path: $relativePath)")
                    true
                } else {
                    lInfo("Config à jour (ignoredDir): ${localFile.name}")
                    false
                }
            } else {
                // Pour les autres configs, pas de vérification MD5
                false
            }

            if (needsDownload) {
                if (localFile.exists()) {
                    lInfo("Suppression de l'ancien fichier: ${localFile.name}")
                    localFile.delete()
                }
                localFile.parentFile?.mkdirs()
                filesToDownload.add(s3File to localFile)
            }
        }

        // Supprimer les fichiers locaux obsolètes dans les ignoredDirectories
        ignoredDirectories.forEach { ignoredDir ->
            // Normaliser le chemin (ex: "config/fancymenu" -> "fancymenu")
            val normalizedIgnoredDir = ignoredDir.removePrefix("config/")
            val localDir = File(configDir, normalizedIgnoredDir)

            if (localDir.exists() && localDir.isDirectory) {
                val remoteFilesInDir = remoteConfigs
                    .map { extractRelativePath(it.key, "config") }
                    .filter { it.startsWith(normalizedIgnoredDir + "/") }
                    .toSet()

                localDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { localFile ->
                        val relativePath = localFile.relativeTo(configDir).path.replace("\\", "/")
                        if (relativePath !in remoteFilesInDir) {
                            lInfo("Fichier obsolète dans ignoredDir à supprimer: $relativePath")
                            filesToDelete.add(localFile)
                        }
                    }
            }
        }

        // Collecter les resourcepacks à télécharger
        remoteResourcepacks.forEach { s3File ->
            val relativePath = extractRelativePath(s3File.key, "resourcepacks")
            val localFile = File(resourcepacksDir, relativePath)
            if (!localFile.exists()) {
                lInfo("Resourcepack manquant: ${localFile.name}")
                localFile.parentFile?.mkdirs()
                filesToDownload.add(s3File to localFile)
            }
        }

        val totalSize = filesToDownload.sumOf { it.first.size }
        lInfo("Résumé: ${filesToDelete.size} à supprimer, ${filesToDownload.size} à télécharger (${formatFileSize(totalSize)})")

        UpdateTasksData(filesToDownload, filesToDelete, totalSize)
    }

    /**
     * Vérifie si des mises à jour sont disponibles
     */
    suspend fun checkForUpdates(version: Version): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val updateData = prepareUpdateData(version)
            val needsUpdate = updateData.filesToDelete.isNotEmpty() || updateData.filesToDownload.isNotEmpty()
            UpdateCheckResult(needsUpdate, updateData.totalSize)
        } catch (e: Exception) {
            lInfo("Erreur lors de la vérification des mises à jour: ${e.message}")
            e.printStackTrace()
            UpdateCheckResult(false, 0L)
        }
    }

    /**
     * Crée les tâches de mise à jour séparées
     */
    suspend fun createUpdateTasks(version: Version): List<TitledTask> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<TitledTask>()
        val updateData = prepareUpdateData(version)

        // Tâche 1: Suppression des fichiers obsolètes
        if (updateData.filesToDelete.isNotEmpty()) {
            tasks.add(
                TitledTask(
                    title = "Suppression des fichiers obsolètes",
                    task = Task.runTask(
                        id = "Update.Tropimon.Delete",
                        dispatcher = Dispatchers.IO,
                        task = { task ->
                            task.updateProgress(-1f, R.string.download_tropimon_deleting_obsolete_mods)
                            updateData.filesToDelete.forEach { file ->
                                if (file.delete()) {
                                    lInfo("Fichier obsolète supprimé : ${file.name}")
                                }
                            }
                            task.updateProgress(1f)
                        }
                    )
                )
            )
        }

        // Séparer les fichiers par type
        val modFiles = updateData.filesToDownload.filter {
            it.first.key.contains("required-mods") || it.first.key.contains("/mods/")
        }
        val configFiles = updateData.filesToDownload.filter {
            it.first.key.contains("config") || it.first.key.contains("options.txt")
        }
        val resourcepackFiles = updateData.filesToDownload.filter {
            it.first.key.contains("resourcepacks")
        }

        lInfo("Séparation: ${modFiles.size} mods, ${configFiles.size} configs, ${resourcepackFiles.size} resourcepacks")

        // Tâche 2: Téléchargement des mods
        if (modFiles.isNotEmpty()) {
            tasks.add(
                TitledTask(
                    title = "Téléchargement des mods",
                    task = createDownloadTask(
                        id = "Update.Tropimon.Mods",
                        files = modFiles,
                        messageResId = R.string.download_tropimon_mods_progress
                    )
                )
            )
        }

        // Tâche 3: Téléchargement des configs
        if (configFiles.isNotEmpty()) {
            tasks.add(
                TitledTask(
                    title = "Téléchargement des configurations",
                    task = createDownloadTask(
                        id = "Update.Tropimon.Config",
                        files = configFiles,
                        messageResId = R.string.download_tropimon_config_progress
                    )
                )
            )
        }

        // Tâche 4: Téléchargement des resourcepacks
        if (resourcepackFiles.isNotEmpty()) {
            tasks.add(
                TitledTask(
                    title = "Téléchargement des resourcepacks",
                    task = createDownloadTask(
                        id = "Update.Tropimon.Resourcepacks",
                        files = resourcepackFiles,
                        messageResId = R.string.download_tropimon_resourcepacks_progress
                    )
                )
            )
        }

        if (tasks.isEmpty()) {
            lInfo("Aucune tâche créée - Tropimon est déjà à jour")
        } else {
            lInfo("${tasks.size} tâche(s) de mise à jour créée(s)")
        }

        tasks
    }

    /**
     * Crée une tâche de téléchargement pour un groupe de fichiers
     */
    private fun createDownloadTask(
        id: String,
        files: List<Pair<S3ObjectSummary, File>>,
        messageResId: Int
    ): Task = Task.runTask(
        id = id,
        dispatcher = Dispatchers.IO,
        task = { task ->
            lInfo("Début du téléchargement: $id avec ${files.size} fichier(s)")

            val totalFileSize = AtomicLong(files.sumOf { it.first.size })
            val downloadedFileSize = AtomicLong(0)
            val downloadedFileCount = AtomicInteger(0)
            val totalFileCount = files.size

            kotlinx.coroutines.coroutineScope {
                val progressJob = launch(Dispatchers.Main) {
                    while (isActive) {
                        ensureActive()

                        val currentFileSize = downloadedFileSize.get()
                        val totalSize = totalFileSize.get().run {
                            if (this < currentFileSize) currentFileSize else this
                        }

                        task.updateProgress(
                            percentage = (currentFileSize.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f),
                            message = messageResId,
                            args = arrayOf<Any>(
                                downloadedFileCount.get(),
                                totalFileCount,
                                formatFileSize(currentFileSize),
                                formatFileSize(totalSize)
                            )
                        )
                        delay(100)
                    }
                }

                try {
                    val downloadTasks = files.map { (s3File, targetFile) ->
                        TropimonDownloadTask(
                            url = CloudflareR2Service.getPublicUrl(s3File.key),
                            targetFile = targetFile,
                            sha1 = null,
                            fileSize = s3File.size,
                            onFileDownloadStarted = {
                                lInfo("Téléchargement démarré: ${targetFile.name}")
                                downloadedFileCount.incrementAndGet()
                            },
                            onFileDownloadedSize = { downloadedBytes ->
                                downloadedFileSize.addAndGet(downloadedBytes)
                            },
                            onFileDownloaded = {
                                lInfo("Téléchargement terminé: ${targetFile.name}")
                            }
                        )
                    }

                    downloadTasks.forEach { it.download() }

                    task.updateProgress(
                        percentage = 1f,
                        message = messageResId,
                        args = arrayOf<Any>(
                            totalFileCount,
                            totalFileCount,
                            formatFileSize(totalFileSize.get()),
                            formatFileSize(totalFileSize.get())
                        )
                    )

                    lInfo("Téléchargement $id terminé : $totalFileCount fichier(s)")

                } finally {
                    progressJob.cancel()
                }
            }
        }
    )
}