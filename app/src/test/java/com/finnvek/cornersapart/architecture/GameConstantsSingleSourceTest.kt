package com.finnvek.cornersapart.architecture

import com.finnvek.cornersapart.projectFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameConstantsSingleSourceTest {
    @Test
    fun helpRuleCopyUsesCanonicalRuleValues() {
        val files = projectFiles()
        val strings = files.strings
        val dialogs = files.read("app/src/main/java/com/finnvek/cornersapart/ui/screens/GamePolishDialogs.kt")
        val helpScoringValues = strings.pluralValues("help_scoring_body")

        assertFalse(strings.stringValue("help_goal_body").contains("one point"))
        assertTrue(helpScoringValues.none { value -> value.contains("one point") })
        assertTrue(helpScoringValues.none { value -> value.contains("21 pieces") })
        assertFalse(strings.stringValue("help_bonus_body").contains("three points"))
        assertTrue(dialogs.contains("GameConstants.PLACED_CELL_POINTS"))
        assertTrue(dialogs.contains("GameConstants.BONUS_TILE_POINTS"))
        assertTrue(dialogs.contains("PieceCatalog.all.size"))
    }

    @Test
    fun playerPaletteUsesGameConstantsForColorCount() {
        val palette = projectFiles().read("app/src/main/java/com/finnvek/cornersapart/ui/theme/PlayerPalette.kt")

        assertFalse(palette.contains("PLAYER_COLOR_COUNT"))
        assertTrue(palette.contains("GameConstants.PLAYER_COLORS.size"))
    }

    private fun String.stringValue(name: String): String =
        Regex("""<string name="${Regex.escape(name)}">(.+)</string>""")
            .find(this)
            ?.groupValues
            ?.get(1)
            ?: error("String resource $name was not found.")

    private fun String.pluralValues(name: String): List<String> =
        Regex(
            """<plurals name="${Regex.escape(name)}">(.*?)</plurals>""",
            RegexOption.DOT_MATCHES_ALL,
        ).find(this)
            ?.groupValues
            ?.get(1)
            ?.let { body ->
                Regex("""<item quantity="[^"]+">(.+)</item>""")
                    .findAll(body)
                    .map { match -> match.groupValues[1] }
                    .toList()
            }.orEmpty()
            .ifEmpty { error("Plural resource $name was not found.") }
}
