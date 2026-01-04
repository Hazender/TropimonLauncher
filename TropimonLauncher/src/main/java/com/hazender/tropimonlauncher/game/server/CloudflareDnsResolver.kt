package com.hazender.tropimonlauncher.game.server

import com.hazender.tropimonlauncher.utils.logging.Logger.lDebug
import com.hazender.tropimonlauncher.utils.logging.Logger.lError
import com.hazender.tropimonlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Résout les DNS en utilisant l'API Cloudflare DNS over HTTPS
 */
object CloudflareDnsResolver {
    private const val CLOUDFLARE_DNS_API = "https://cloudflare-dns.com/dns-query"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class DnsResponse(
        val Status: Int,
        val Answer: List<DnsAnswer>? = null
    )

    @Serializable
    data class DnsAnswer(
        val name: String,
        val type: Int,
        val TTL: Int? = null,
        val data: String
    )

    /**
     * Résout un enregistrement SRV en utilisant l'API Cloudflare
     * @param domain Le domaine à résoudre (ex: play.tropimon.fr)
     * @return Pair(host, port) ou null si échec
     */
    suspend fun resolveSrv(domain: String): Pair<String, Int>? = withContext(Dispatchers.IO) {
        try {
            val srvDomain = "_minecraft._tcp.$domain"
            lDebug("Resolving SRV via Cloudflare DNS API: $srvDomain")

            // Faire la requête DNS SRV (type 33)
            val url = URL("$CLOUDFLARE_DNS_API?name=$srvDomain&type=SRV")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/dns-json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            val response = connection.inputStream.bufferedReader().readText()
            lDebug("Cloudflare DNS response: $response")

            val dnsResponse = json.decodeFromString<DnsResponse>(response)

            if (dnsResponse.Status == 0 && dnsResponse.Answer != null) {
                // Trouver l'enregistrement SRV
                for (answer in dnsResponse.Answer) {
                    if (answer.type == 33) { // Type SRV
                        // Format SRV: "priority weight port target"
                        // Ex: "0 5 25565 mc.tropimon.fr."
                        val parts = answer.data.split(" ")
                        if (parts.size >= 4) {
                            val port = parts[2].toIntOrNull() ?: 25565
                            var target = parts[3]

                            // Retirer le point final
                            if (target.endsWith(".")) {
                                target = target.dropLast(1)
                            }

                            lInfo("Resolved SRV via Cloudflare: $domain -> $target:$port")

                            // Maintenant résoudre le target en IP (enregistrement A)
                            val ip = resolveA(target)
                            if (ip != null) {
                                lInfo("Resolved A record: $target -> $ip")
                                return@withContext ip to port
                            }

                            // Si on ne peut pas résoudre l'IP, retourner le hostname
                            return@withContext target to port
                        }
                    }
                }
            }

            lDebug("No valid SRV record found via Cloudflare API")
            null
        } catch (e: Exception) {
            lError("Failed to resolve SRV via Cloudflare API for $domain", e)
            null
        }
    }

    /**
     * Résout un enregistrement A (IPv4) en utilisant l'API Cloudflare
     * @param hostname Le hostname à résoudre
     * @return L'adresse IP ou null si échec
     */
    suspend fun resolveA(hostname: String): String? = withContext(Dispatchers.IO) {
        try {
            lDebug("Resolving A record via Cloudflare DNS API: $hostname")

            // Faire la requête DNS A (type 1)
            val url = URL("$CLOUDFLARE_DNS_API?name=$hostname&type=A")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/dns-json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            val response = connection.inputStream.bufferedReader().readText()
            lDebug("Cloudflare DNS A response: $response")

            val dnsResponse = json.decodeFromString<DnsResponse>(response)

            if (dnsResponse.Status == 0 && dnsResponse.Answer != null) {
                // Prendre la première IP
                for (answer in dnsResponse.Answer) {
                    if (answer.type == 1) { // Type A (IPv4)
                        lInfo("Resolved A record: $hostname -> ${answer.data}")
                        return@withContext answer.data
                    }
                }
            }

            lDebug("No A record found for $hostname")
            null
        } catch (e: Exception) {
            lError("Failed to resolve A record for $hostname", e)
            null
        }
    }

    /**
     * Résout complètement un domaine Minecraft (SRV + A)
     * @param domain Le domaine (ex: play.tropimon.fr)
     * @return Triple(ip, port, hostname) ou null si échec
     */
    suspend fun resolveMinecraftServer(domain: String): Triple<String, Int, String>? = withContext(Dispatchers.IO) {
        // Essayer SRV d'abord
        val srvResult = resolveSrv(domain)
        if (srvResult != null) {
            val (host, port) = srvResult

            // Si host est déjà une IP, retourner directement
            if (host.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                return@withContext Triple(host, port, domain)
            }

            // Sinon résoudre l'IP
            val ip = resolveA(host)
            if (ip != null) {
                return@withContext Triple(ip, port, domain)
            }
        }

        // Fallback : résoudre directement le domaine en A
        val ip = resolveA(domain)
        if (ip != null) {
            return@withContext Triple(ip, 25565, domain)
        }

        null
    }
}