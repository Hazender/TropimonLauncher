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