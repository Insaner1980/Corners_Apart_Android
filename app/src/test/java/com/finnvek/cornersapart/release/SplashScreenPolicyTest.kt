package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashScreenPolicyTest {
    @Test
    fun launchActivityUsesTheCompatSplashTheme() {
        val root = projectRoot()
        val manifest = root.resolve(MANIFEST_PATH).toFile().readText()
        val themes = root.resolve(THEMES_PATH).toFile().readText()
        val mainActivity = root.resolve(MAIN_ACTIVITY_PATH).toFile().readText()

        assertTrue(manifest.contains("""android:theme="@style/Theme.CornersApart.Starting"""))
        assertTrue(themes.contains("""parent="Theme.SplashScreen"""))
        assertTrue(themes.contains("""<item name="postSplashScreenTheme">@style/Theme.CornersApart</item>"""))

        val installIndex = mainActivity.indexOf("installSplashScreen()")
        val superIndex = mainActivity.indexOf("super.onCreate(savedInstanceState)")
        assertTrue("installSplashScreen must run before super.onCreate.", installIndex >= 0)
        assertTrue("installSplashScreen must run before super.onCreate.", installIndex < superIndex)
    }

    @Test
    fun animatedLogoUsesFourPackagedWebpLayers() {
        val drawableDirectory = projectRoot().resolve(DRAWABLE_DIRECTORY).toFile()

        SPLASH_PIECES.forEach { fileName ->
            val image = drawableDirectory.resolve(fileName)
            assertTrue("Missing splash piece: $fileName", image.isFile)

            val header = image.inputStream().use { it.readNBytes(WEBP_HEADER_SIZE) }
            assertEquals("RIFF", header.copyOfRange(0, 4).decodeToString())
            assertEquals("WEBP", header.copyOfRange(8, 12).decodeToString())
        }
    }

    private companion object {
        const val MANIFEST_PATH = "app/src/main/AndroidManifest.xml"
        const val THEMES_PATH = "app/src/main/res/values/themes.xml"
        const val MAIN_ACTIVITY_PATH = "app/src/main/java/com/finnvek/cornersapart/MainActivity.kt"
        const val DRAWABLE_DIRECTORY = "app/src/main/res/drawable-nodpi"
        const val WEBP_HEADER_SIZE = 12

        val SPLASH_PIECES =
            listOf(
                "splash_piece_cyan.webp",
                "splash_piece_orange.webp",
                "splash_piece_pink.webp",
                "splash_piece_green.webp",
            )
    }
}
