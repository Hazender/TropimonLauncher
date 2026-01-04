package com.hazender.tropimonlauncher.game.server

import com.hazender.tropimonlauncher.game.download.game.tropimon.CloudflareR2Service
import com.hazender.tropimonlauncher.utils.logging.Logger.lDebug
import com.hazender.tropimonlauncher.utils.logging.Logger.lError
import com.hazender.tropimonlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

data class ServerStatus(
    val online: Boolean,
    val playerCount: Int = 0
)

/**
 * Vérifie le statut d'un serveur Minecraft
 * Récupère automatiquement les infos depuis androidConfig.json via CloudflareR2Service
 */
object MinecraftServerStatus {
    private const val TIMEOUT_MS = 5000L
    private const val PROTOCOL_VERSION = 47 // Version 1.8+

    // Cache pour la résolution DNS
    private data class ResolvedServer(val domain: String, val ip: String, val port: Int)
    private var cachedResolved: ResolvedServer? = null

    /**
     * Ping le serveur en utilisant les infos depuis androidConfig.json
     */
    suspend fun pingServer(): ServerStatus? = withContext(Dispatchers.IO) {
        try {
            val (domain, ip, port) = getServerInfoFromConfig()
            lInfo("Pinging server: $domain -> $ip:$port")
            pingServerDirect(ip, port, domain)
        } catch (e: Exception) {
            lError("Failed to ping server", e)
            null
        }
    }

    /**
     * Récupère les infos serveur depuis androidConfig.json
     */
    private suspend fun getServerInfoFromConfig(): Triple<String, String, Int> = withContext(Dispatchers.IO) {
        // Vérifier le cache
        cachedResolved?.let {
            lDebug("Using cached server: ${it.domain} -> ${it.ip}:${it.port}")
            return@withContext Triple(it.domain, it.ip, it.port)
        }

        try {
            val config = CloudflareR2Service.fetchConfig()
            val serverInfo = config.server

            val domain = serverInfo.ip
            val port = serverInfo.port.toIntOrNull() ?: 25565

            // Vérifier si c'est une IP directe ou un domaine
            val ip = if (domain.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                lInfo("Using direct IP: $domain:$port")
                domain
            } else {
                lInfo("Resolving domain: $domain")
                val resolved = CloudflareDnsResolver.resolveMinecraftServer(domain)

                if (resolved != null) {
                    lInfo("Resolved: $domain -> ${resolved.first}:${resolved.second}")
                    resolved.first
                } else {
                    lError("Failed to resolve $domain, using as-is")
                    domain
                }
            }

            // Mettre en cache
            cachedResolved = ResolvedServer(domain, ip, port)
            Triple(domain, ip, port)
        } catch (e: Exception) {
            lError("Failed to fetch server config", e)
            throw IllegalStateException("Impossible de récupérer la configuration du serveur", e)
        }
    }

    /**
     * Vide le cache de résolution DNS
     */
    fun clearCache() {
        cachedResolved = null
        lDebug("Cache cleared")
    }

    /**
     * Ping un serveur Minecraft directement
     */
    private suspend fun pingServerDirect(
        ip: String,
        port: Int,
        handshakeHost: String
    ): ServerStatus? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), TIMEOUT_MS.toInt())

                    val out = DataOutputStream(socket.getOutputStream())
                    val input = DataInputStream(socket.getInputStream())

                    // Envoyer handshake
                    sendHandshake(out, handshakeHost, port)

                    // Envoyer status request
                    sendStatusRequest(out)

                    // Lire la réponse
                    val response = readResponse(input)

                    // Parser uniquement playerCount
                    val playerCount = extractPlayerCount(response)

                    ServerStatus(online = true, playerCount = playerCount)
                }
            } catch (e: Exception) {
                lError("Ping failed for $ip:$port", e)
                ServerStatus(online = false, playerCount = 0)
            }
        } ?: ServerStatus(online = false, playerCount = 0)
    }

    private fun sendHandshake(out: DataOutputStream, host: String, port: Int) {
        val handshakeData = buildPacket {
            writeVarInt(0x00)
            writeVarInt(PROTOCOL_VERSION)
            writeString(host)
            writeShort(port)
            writeVarInt(1) // Status state
        }
        out.write(handshakeData)
        out.flush()
    }

    private fun sendStatusRequest(out: DataOutputStream) {
        val statusRequest = buildPacket {
            writeVarInt(0x00)
        }
        out.write(statusRequest)
        out.flush()
    }

    private fun readResponse(input: DataInputStream): String {
        readVarInt(input) // Packet length
        readVarInt(input) // Packet ID
        val jsonLength = readVarInt(input)
        val jsonBytes = ByteArray(jsonLength)
        input.readFully(jsonBytes)
        return String(jsonBytes, StandardCharsets.UTF_8)
    }

    /**
     * Extrait uniquement le nombre de joueurs depuis la réponse JSON
     */
    private fun extractPlayerCount(json: String): Int {
        val regex = """"online"\s*:\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    // --- Packet Builder ---

    private fun buildPacket(builder: PacketBuilder.() -> Unit): ByteArray {
        val packetBuilder = PacketBuilder()
        packetBuilder.builder()
        return packetBuilder.toByteArray()
    }

    private class PacketBuilder {
        private val buffer = mutableListOf<Byte>()

        fun writeVarInt(value: Int) {
            var v = value
            while (v and 0x7F.inv() != 0) {
                buffer.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
            buffer.add(v.toByte())
        }

        fun writeString(string: String) {
            val bytes = string.toByteArray(StandardCharsets.UTF_8)
            writeVarInt(bytes.size)
            buffer.addAll(bytes.toList())
        }

        fun writeShort(value: Int) {
            buffer.add((value shr 8).toByte())
            buffer.add(value.toByte())
        }

        fun toByteArray(): ByteArray {
            val data = buffer.toByteArray()
            val lengthBytes = mutableListOf<Byte>()
            var length = data.size

            while (length and 0x7F.inv() != 0) {
                lengthBytes.add(((length and 0x7F) or 0x80).toByte())
                length = length ushr 7
            }
            lengthBytes.add(length.toByte())

            return lengthBytes.toByteArray() + data
        }
    }

    private fun readVarInt(input: DataInputStream): Int {
        var result = 0
        var shift = 0
        var b: Byte

        do {
            b = input.readByte()
            result = result or ((b.toInt() and 0x7F) shl shift)
            shift += 7
        } while (b.toInt() and 0x80 != 0)

        return result
    }
}