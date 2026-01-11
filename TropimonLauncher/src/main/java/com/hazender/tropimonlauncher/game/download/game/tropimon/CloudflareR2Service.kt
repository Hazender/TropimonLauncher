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

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.ListObjectsV2Request
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.hazender.tropimonlauncher.info.InfoDistributor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL

object CloudflareR2Service {

    private val R2_ACCOUNT_ID = InfoDistributor.R2_ACCOUNT_ID
    private val R2_ACCESS_KEY = InfoDistributor.R2_ACCESS_KEY
    private val R2_SECRET_KEY = InfoDistributor.R2_SECRET_KEY

    private const val CONFIG_URL = "https://files.tropimon.fr/androidConfig.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedConfig: TropimonConfig? = null

    private val s3Client = AmazonS3Client(BasicAWSCredentials(R2_ACCESS_KEY, R2_SECRET_KEY)).apply {
        setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())
        endpoint = "https://$R2_ACCOUNT_ID.r2.cloudflarestorage.com"
    }

    suspend fun fetchConfig(): TropimonConfig = withContext(Dispatchers.IO) {
        if (cachedConfig == null) {
            val configJson = URL(CONFIG_URL).readText()
            cachedConfig = json.decodeFromString<TropimonConfig>(configJson)
        }
        cachedConfig!!
    }

    private suspend fun listFiles(fullPath: String, bucket: String): List<S3ObjectSummary> =
        withContext(Dispatchers.IO) {
            val files = mutableListOf<S3ObjectSummary>()
            var request = ListObjectsV2Request()
                .withBucketName(bucket)
                .withPrefix(fullPath)

            do {
                val result = s3Client.listObjectsV2(request)
                result.objectSummaries
                    .filter { !it.key.endsWith("/") }
                    .forEach { files.add(it) }

                request.continuationToken = result.nextContinuationToken
            } while (result.isTruncated)

            files
        }

    suspend fun listRequiredMods(): List<S3ObjectSummary> {
        val config = fetchConfig()
        val fullPath = "${config.resources.prodResources}/${config.resources.keys.requiredMods}"
        return listFiles(fullPath, config.resources.bucket)
    }

    suspend fun listConfigFiles(): List<S3ObjectSummary> {
        val config = fetchConfig()
        val fullPath = "${config.resources.prodResources}/${config.resources.keys.config}"
        return listFiles(fullPath, config.resources.bucket)
    }

    suspend fun listResourcepacks(): List<S3ObjectSummary> {
        val config = fetchConfig()
        val fullPath = "${config.resources.prodResources}/${config.resources.keys.resourcepacks}"
        return listFiles(fullPath, config.resources.bucket)
    }

    suspend fun getOptionsFile(): S3ObjectSummary? = withContext(Dispatchers.IO) {
        val config = fetchConfig()
        val optionsKey = "${config.resources.prodResources}/${config.resources.keys.options}"

        try {
            val metadata = s3Client.getObjectMetadata(config.resources.bucket, optionsKey)
            S3ObjectSummary().apply {
                key = optionsKey
                size = metadata.contentLength
                eTag = metadata.eTag
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPublicUrl(key: String): String {
        val config = fetchConfig()
        return "${config.resources.assetsURL}$key"
    }

    fun getFileName(key: String) = key.substringAfterLast("/")

    suspend fun getTropimonConfig(): TropimonConfig? {
        return try {
            fetchConfig()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getServerVersionInfo(): VersionInfo? {
        return getTropimonConfig()?.version
    }

}