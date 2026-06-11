package com.finnvek.cornersapart.release

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ReleaseIdentityTest {
    @Test
    fun userFacingStringsDoNotMentionExternalGameNamesOrAi() {
        val strings = File("src/main/res/values/strings.xml").readText()

        assertFalse(EXTERNAL_GAME_NAME.containsMatchIn(strings))
        assertFalse(USER_FACING_AI.containsMatchIn(strings))
    }

    @Test
    fun buildFilesDoNotDependOnRemoteAvatarOrNetworkImageServices() {
        val checkedFiles =
            listOf(
                File("build.gradle.kts"),
                File("../gradle/libs.versions.toml"),
            )

        checkedFiles.forEach { file ->
            val content = file.readText()
            assertFalse(REMOTE_AVATAR_OR_IMAGE_DEPENDENCY.containsMatchIn(content))
        }
    }

    private companion object {
        val EXTERNAL_GAME_NAME = Regex("""\bBlokus\b""")
        val USER_FACING_AI = Regex(""">[^<]*\bAI\b[^<]*<""")
        val REMOTE_AVATAR_OR_IMAGE_DEPENDENCY = Regex("""\b(coil|dicebear)\b""", RegexOption.IGNORE_CASE)
    }
}
