package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

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
        assertTrue(gitignore.contains("/app/build/"))
    }

    @Test
    fun composeStabilityValidationKeepsTrackedStrictBaselines() {
        val root = projectRoot()
        val gitignore = root.resolve(".gitignore").toFile().readText()
        val appBuildFile = root.resolve("app/build.gradle.kts").toFile().readText()
        val stabilityFiles =
            root
                .resolve("app/stability")
                .toFile()
                .walkTopDown()
                .filter { file -> file.isFile && file.extension == "stability" }
                .toList()
        val trackedStabilityFiles = gitTrackedFiles("app/stability/*.stability")

        assertFalse(
            "Compose stability baselines must be tracked so clean clones do not skip stabilityCheck.",
            gitignore.contains("/app/stability/"),
        )
        assertTrue(
            "Compose stability validation needs at least one tracked baseline file.",
            stabilityFiles.isNotEmpty(),
        )
        assertTrue(
            "Compose stability baselines must be in Git, not only present as local generated files.",
            trackedStabilityFiles.containsAll(stabilityFiles.map { file -> "app/stability/${file.name}" }),
        )
        assertTrue(appBuildFile.contains("composeStabilityAnalyzer"))
        assertTrue(appBuildFile.contains("stabilityValidation"))
        assertTrue(appBuildFile.contains("enabled.set(true)"))
        assertTrue(appBuildFile.contains("failOnStabilityChange.set(true)"))
        assertTrue(appBuildFile.contains("allowMissingBaseline.set(false)"))
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

    private fun gitTrackedFiles(pathspec: String): Set<String> {
        val process =
            ProcessBuilder("git", "ls-files", pathspec)
                .directory(projectRoot().toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(10, TimeUnit.SECONDS)) { "git ls-files timed out for $pathspec" }
        check(process.exitValue() == 0) { output }
        return output
            .lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }
            .toSet()
    }

    private companion object {
        val hardcodedThemeColor =
            Regex(
                """\bColor\.(Black|White|Red|Green|Blue|Gray|LightGray|DarkGray)\b|Color\(0x[0-9A-Fa-f_]+""",
            )
    }
}
