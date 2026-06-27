package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class GuardrailHygieneTest {
    @Test
    fun productionUiSourcesDoNotHardcodeThemeEquivalentColorsOutsideThemeTokens() {
        val uiRoot = projectRoot().resolve("app/src/main/java/com/finnvek/cornersapart/ui")
        val hardcodedColorMatches =
            Files
                .walk(uiRoot)
                .use { paths ->
                    paths
                        .filter { path -> Files.isRegularFile(path) && path.toString().endsWith(".kt") }
                        .filter { path -> !path.isInPackage("theme") }
                        .flatMap { path -> hardcodedColorMatches(path).stream() }
                        .toList()
                }

        assertTrue(
            hardcodedColorMatches.joinToString(
                separator = System.lineSeparator(),
                prefix = "Theme-equivalent colors must be declared in ui/theme tokens:${System.lineSeparator()}",
            ),
            hardcodedColorMatches.isEmpty(),
        )
    }

    @Test
    fun generatedAnalyzerAndReportOutputsAreIgnored() {
        val gitignore = projectRoot().resolve(".gitignore").toFile().readText()

        assertTrue(gitignore.contains("/reports/"))
        assertTrue(gitignore.contains("/app/stability/"))
    }

    private fun hardcodedColorMatches(path: Path): List<String> =
        path
            .toFile()
            .readLines()
            .mapIndexedNotNull { index, line ->
                if (hardcodedThemeColor.containsMatchIn(line)) {
                    "${path.toString().replace('\\', '/')}:${index + 1}: ${line.trim()}"
                } else {
                    null
                }
            }

    private fun Path.isInPackage(packageName: String): Boolean =
        toString().contains("${File.separator}$packageName${File.separator}")

    private companion object {
        val hardcodedThemeColor =
            Regex(
                """\bColor\.(Black|White|Red|Green|Blue|Gray|LightGray|DarkGray)\b|Color\(0x[0-9A-Fa-f_]+""",
            )
    }
}
