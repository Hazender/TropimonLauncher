package com.hazender.tropimonlauncher.game.download.game.tropimon

import kotlinx.serialization.Serializable

@Serializable
data class TropimonConfig(
    val server: ServerInfo,
    val version: VersionInfo,
    val links: Links,
    val resources: Resources,
    val ignoredDirectories: List<String>
)

@Serializable
data class ServerInfo(
    val ip: String,
    val port: String
)

@Serializable
data class VersionInfo(
    val minecraft: String,
    val fabric: String,
    val tropimon: String
)

@Serializable
data class Links(
    val discord: String,
    val website: String,
    val shop: String,
    val wiki: String,
    val purchaseMinecraft: String
)

@Serializable
data class Resources(
    val bucket: String,
    val assetsURL: String,
    val prodResources: String,
    val keys: ResourceKeys
)

@Serializable
data class ResourceKeys(
    val requiredMods: String,
    val config: String,
    val resourcepacks: String,
    val shaders: String,
    val scripts: String,
    val options: String
)