package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectFiles
import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReleaseIdentityTest {
    @Test
    fun releaseIdentityMatchesPinnedV1Values() {
        val files = projectFiles()

        assertEquals(EXPECTED_APPLICATION_ID, gradleStringAssignment(files.appBuildFile, "namespace"))
        assertEquals(EXPECTED_APPLICATION_ID, gradleStringAssignment(files.appBuildFile, "applicationId"))
        assertEquals(EXPECTED_VERSION_CODE, gradleIntAssignment(files.appBuildFile, "versionCode"))
        assertEquals(EXPECTED_VERSION_NAME, gradleStringAssignment(files.appBuildFile, "versionName"))
        assertEquals(APP_NAME_RESOURCE_REFERENCE, androidApplicationLabel(files.manifest))
        assertEquals(EXPECTED_APP_LABEL, stringResourceValue(files.strings, "app_name"))
    }

    @Test
    fun userFacingStringsDoNotMentionExternalGameNamesOrAi() {
        val strings = projectFiles().strings

        assertFalse(EXTERNAL_GAME_NAME.containsMatchIn(strings))
        assertFalse(USER_FACING_AI.containsMatchIn(strings))
    }

    @Test
    fun buildFilesDoNotDependOnRemoteAvatarOrNetworkImageServices() {
        val root = projectRoot()
        val checkedFiles =
            listOf(
                root.resolve("app/build.gradle.kts").toFile(),
                root.resolve("gradle/libs.versions.toml").toFile(),
            )

        checkedFiles.forEach { file ->
            val content = file.readText()
            assertFalse(REMOTE_AVATAR_OR_IMAGE_DEPENDENCY.containsMatchIn(content))
        }
    }

    private fun gradleStringAssignment(
        content: String,
        name: String,
    ): String =
        Regex("""(?m)^\s*${Regex.escape(name)}\s*=\s*"([^"]+)"\s*$""")
            .find(content)
            ?.groupValues
            ?.get(1)
            ?: error("Missing Gradle string assignment: $name")

    private fun gradleIntAssignment(
        content: String,
        name: String,
    ): Int =
        Regex("""(?m)^\s*${Regex.escape(name)}\s*=\s*(\d+)\s*$""")
            .find(content)
            ?.groupValues
            ?.get(1)
            ?.toInt()
            ?: error("Missing Gradle integer assignment: $name")

    private fun androidApplicationLabel(manifest: String): String =
        Regex("(?s)<application\\b[^>]*\\bandroid:label=\"([^\"]+)\"")
            .find(manifest)
            ?.groupValues
            ?.get(1)
            ?: error("Missing Android application label")

    private fun stringResourceValue(
        strings: String,
        name: String,
    ): String =
        Regex("<string\\s+name=\"${Regex.escape(name)}\">([^<]+)</string>")
            .find(strings)
            ?.groupValues
            ?.get(1)
            ?: error("Missing string resource: $name")

    private companion object {
        const val EXPECTED_APPLICATION_ID = "com.finnvek.cornersapart"
        const val EXPECTED_VERSION_CODE = 1
        const val EXPECTED_VERSION_NAME = "1.0.0"
        const val APP_NAME_RESOURCE_REFERENCE = "@string/app_name"
        const val EXPECTED_APP_LABEL = "Corners Apart"
        val EXTERNAL_GAME_NAME = Regex("""\bBlokus\b""")
        val USER_FACING_AI = Regex(""">[^<]*\bAI\b[^<]*<""")
        val REMOTE_AVATAR_OR_IMAGE_DEPENDENCY = Regex("""\b(coil|dicebear)\b""", RegexOption.IGNORE_CASE)
    }
}
