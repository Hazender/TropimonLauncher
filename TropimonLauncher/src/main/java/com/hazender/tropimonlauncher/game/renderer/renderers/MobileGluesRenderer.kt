// Dans C:/Users/Hazender/StudioProjects/TropimonLauncher/TropimonLauncher/src/main/java/com/hazender/tropimonlauncher/game/renderer/renderers/MobileGluesRenderer.kt

package com.hazender.tropimonlauncher.game.renderer.renderers

import com.hazender.tropimonlauncher.game.renderer.RendererInterface

object MobileGluesRenderer : RendererInterface {


    override fun getRendererId(): String = "opengles3"


    override fun getUniqueIdentifier(): String = "f5a8c9b0-6d3e-4f8a-9c7b-8d4a3e2f1b0a"

    override fun getRendererName(): String = "MobileGlues"

    override fun getMaxMCVersion(): String? = null

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "LIBGL_ES" to "3",
            "POJAVEXEC_EGL" to "libmobileglues.so",
            "LIBGL_EGL" to "libmobileglues.so"
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy {
        listOf("libspirv-cross-c-shared.so")
    }

    override fun getRendererLibrary(): String = "libmobileglues.so"
}
