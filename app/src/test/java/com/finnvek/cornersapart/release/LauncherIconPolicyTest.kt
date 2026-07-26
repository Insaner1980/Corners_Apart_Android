package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconPolicyTest {
    @Test
    fun adaptiveLauncherIconUsesDrawableArtworkForColorForeground() {
        val root = projectRoot()
        val iconXml = root.resolve(LAUNCHER_ICON_PATH).toFile().readText()

        assertEquals("@drawable/ic_launcher_foreground", launcherLayerDrawable(iconXml, "foreground"))

        val foreground = root.resolve(LAUNCHER_FOREGROUND_PATH).toFile()

        assertTrue("Missing launcher foreground drawable.", foreground.isFile)
        assertTrue(
            "Launcher foreground drawable must use the foreground color token.",
            foreground.readText().contains("""android:fillColor="@color/ic_launcher_foreground""""),
        )
    }

    @Test
    fun adaptiveLauncherIconKeepsThemedMonochromeLayer() {
        val iconXml = projectRoot().resolve(LAUNCHER_ICON_PATH).toFile().readText()

        assertEquals("@drawable/ic_launcher_monochrome", launcherLayerDrawable(iconXml, "monochrome"))
    }

    private fun launcherLayerDrawable(
        iconXml: String,
        layer: String,
    ): String =
        Regex("""<${Regex.escape(layer)}\s+android:drawable="([^"]+)"""")
            .find(iconXml)
            ?.groupValues
            ?.get(1)
            ?: error("Missing launcher icon layer: $layer")

    private companion object {
        const val LAUNCHER_ICON_PATH = "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"
        const val LAUNCHER_FOREGROUND_PATH = "app/src/main/res/drawable/ic_launcher_foreground.xml"
    }
}
