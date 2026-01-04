package com.hazender.tropimonlauncher.game.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazender.tropimonlauncher.game.download.game.tropimon.CloudflareR2Service
import com.hazender.tropimonlauncher.game.download.game.tropimon.TropimonConfig
import com.hazender.tropimonlauncher.utils.logging.Logger.lDebug
import com.hazender.tropimonlauncher.utils.logging.Logger.lError
import com.hazender.tropimonlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel centralisé pour la configuration du serveur Tropimon
 * Cache la config pour toute la durée de vie de l'application
 * Utilisé par : liens Discord/Shop, statut serveur, infos de connexion, etc.
 */
class ServerConfigViewModel : ViewModel() {

    private val _config = MutableStateFlow<TropimonConfig?>(null)
    val config: StateFlow<TropimonConfig?> = _config.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchConfig()
    }

    /**
     * Récupère la configuration depuis CloudflareR2
     * Utilise le cache interne de CloudflareR2Service
     */
    private fun fetchConfig() {
        viewModelScope.launch {
            if (_config.value != null) {
                lDebug("ServerConfigViewModel: Config already loaded from cache")
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            try {
                lInfo("ServerConfigViewModel: Fetching config from CloudflareR2...")
                val fetchedConfig = CloudflareR2Service.fetchConfig()
                _config.value = fetchedConfig
                lInfo("ServerConfigViewModel: Config loaded successfully")
                lDebug("ServerConfigViewModel: Discord=${fetchedConfig.links.discord}, Shop=${fetchedConfig.links.shop}")
                lDebug("ServerConfigViewModel: Server=${fetchedConfig.server.ip}:${fetchedConfig.server.port}")
            } catch (e: Exception) {
                lError("ServerConfigViewModel: Failed to fetch config", e)
                _error.value = e.message

                // Pas de valeurs par défaut ici - laisse les composants gérer leurs propres fallbacks
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Force le rechargement de la config (utile pour refresh manuel)
     */
    fun refresh() {
        _config.value = null
        fetchConfig()
    }

    // Helpers pour accès direct aux valeurs

    fun getDiscordUrl(): String? = _config.value?.links?.discord

    fun getShopUrl(): String? = _config.value?.links?.shop

    fun getServerIp(): String? = _config.value?.server?.ip

    fun getServerPort(): Int? = _config.value?.server?.port?.toIntOrNull()

    fun getMinecraftVersion(): String? = _config.value?.version?.minecraft

    fun getFabricVersion(): String? = _config.value?.version?.fabric

    fun getTropimonVersion(): String? = _config.value?.version?.tropimon
}