package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectFiles
import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BuildDependencyHygieneTest {
    @Test
    fun unusedDependencyGuardRecognizesForbiddenCoordinatesAndImports() {
        val buildInputs =
            mapOf(
                "app/build.gradle.kts" to
                    """
                    dependencies {
                        implementation("androidx.navigation:navigation-compose:2.9.8")
                        implementation("androidx.hilt:hilt-navigation-compose")
                        implementation("androidx.datastore:datastore-preferences:1.2.1")
                        implementation("androidx.compose.animation:animation")
                        implementation("androidx.compose.material:material-icons-extended:1.7.8")
                    }
                    """.trimIndent(),
                "gradle/libs.versions.toml" to
                    """
                    navigation-ui = { group = "androidx.navigation", name = "navigation-compose", version = "2.9.8" }
                    icons-pack = { module = "androidx.compose.material:material-icons-extended", version = "1.7.8" }
                    """.trimIndent(),
            )
        val sourceInputs =
            mapOf(
                "app/src/main/java/Example.kt" to
                    """
                    import androidx.navigation.compose.NavHost
                    import androidx.hilt.navigation.compose.hiltViewModel
                    import androidx.datastore.preferences.core.Preferences
                    import androidx.compose.animation.AnimatedVisibility
                    import androidx.compose.material.icons.Icons
                    """.trimIndent(),
            )

        val violations =
            unusedRuntimeDependencySeamViolations(
                buildInputs = buildInputs,
                sourceInputs = sourceInputs,
                seams = allForbiddenUnusedDependencySeams,
            )

        listOf(
            "Navigation Compose",
            "Hilt Navigation Compose",
            "DataStore Preferences",
            "Compose Animation",
            "Material Icons Extended",
        ).forEach { dependencyName ->
            assertTrue(
                "Expected guard to reject $dependencyName",
                violations.any { dependencyName in it },
            )
        }
    }

    @Test
    fun appDoesNotDeclareUnusedRuntimeDependencySeams() {
        val root = projectRoot()
        val appBuildFile = root.resolve("app/build.gradle.kts").toFile().readText()
        val violations =
            unusedRuntimeDependencySeamViolations(
                buildInputs = buildPolicyInputs(root.toFile()),
                sourceInputs = sourcePolicyInputs(root.toFile()),
                seams = unusedRuntimeDependencySeams,
            )

        assertTrue(
            "Unused runtime dependency seams must stay undeclared and unimported:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
        assertTrue(appBuildFile.contains("implementation(libs.hilt.lifecycle.viewmodel.compose)"))
    }

    @Test
    fun appDoesNotPackageMaterialIconsExtendedForAHandfulOfIcons() {
        val root = projectRoot()
        val violations =
            unusedRuntimeDependencySeamViolations(
                buildInputs = buildPolicyInputs(root.toFile()),
                sourceInputs = sourcePolicyInputs(root.toFile()),
                seams = listOf(materialIconsExtendedSeam),
            )

        assertTrue(
            "Material Icons Extended must stay out of build declarations and source imports:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun composeDependenciesStayUnderBomAndMaterial3Policy() {
        val root = projectRoot()
        val files = projectFiles(root)

        assertTrue(
            files.versionsCatalog.contains(
                "compose-bom = { group = \"androidx.compose\", name = \"compose-bom\", version.ref = \"composeBom\" }",
            ),
        )
        assertTrue(files.appBuildFile.contains("val composeBom = platform(libs.compose.bom)"))
        assertTrue(files.appBuildFile.contains("implementation(composeBom)"))
        assertTrue(files.appBuildFile.contains("androidTestImplementation(composeBom)"))

        val violations =
            composeBomPolicyViolations(
                versionsCatalog = files.versionsCatalog,
                appBuildFile = files.appBuildFile,
            )

        assertTrue(
            "Compose dependencies must stay BOM-controlled and Material3-only:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun composeBomPolicyRecognizesForbiddenDeclarations() {
        val forbiddenDeclarations =
            listOf(
                """compose-ui = { group = "androidx.compose.ui", name = "ui", version.ref = "composeUi" }""",
                "compose-material3 = { group = \"androidx.compose.material3\", " +
                    "name = \"material3\", version = \"1.4.0\" }",
                """implementation("androidx.compose.ui:ui:1.11.2")""",
                """implementation("androidx.compose.material:material")""",
                """implementation("androidx.compose.material:material:1.7.8")""",
                """compose-material = { group = "androidx.compose.material", name = "material" }""",
                """composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }""",
                """kotlinCompilerExtensionVersion = "1.5.15"""",
            )

        val violations =
            composeBomPolicyViolations(
                versionsCatalog = forbiddenDeclarations.joinToString("\n"),
                appBuildFile = forbiddenDeclarations.joinToString("\n"),
            )

        forbiddenDeclarations.forEach { declaration ->
            assertTrue(
                "Expected Compose BOM guard to reject: $declaration",
                violations.any { declaration in it },
            )
        }
    }

    private fun composeBomPolicyViolations(
        versionsCatalog: String,
        appBuildFile: String,
    ): List<String> =
        composeCatalogViolations(versionsCatalog) +
            gradleComposePolicyViolations("app/build.gradle.kts", appBuildFile) +
            gradleComposePolicyViolations("gradle/libs.versions.toml", versionsCatalog)

    private fun composeCatalogViolations(versionsCatalog: String): List<String> =
        versionsCatalog
            .lines()
            .mapIndexedNotNull { index, line ->
                val activeLine = line.activeBuildDeclaration()
                val isComposeLibraryAlias =
                    activeLine.startsWith("compose-") &&
                        activeLine.contains("""group = "androidx.compose""") &&
                        !activeLine.startsWith("compose-bom")
                val hasExplicitVersion = explicitVersionPattern.containsMatchIn(activeLine)
                val isObsoleteMaterialAlias =
                    activeLine.contains("""group = "androidx.compose.material"""") &&
                        activeLine.contains("""name = "material"""")

                when {
                    isComposeLibraryAlias && hasExplicitVersion -> {
                        "gradle/libs.versions.toml:${index + 1}: $activeLine"
                    }

                    isObsoleteMaterialAlias -> {
                        "gradle/libs.versions.toml:${index + 1}: $activeLine"
                    }

                    else -> {
                        null
                    }
                }
            }

    private fun gradleComposePolicyViolations(
        sourceName: String,
        sourceText: String,
    ): List<String> =
        sourceText
            .lines()
            .flatMapIndexed { index, line ->
                val activeLine = line.activeBuildDeclaration()
                listOfNotNull(
                    activeLine.takeIf { composeCoordinateWithVersionPattern.containsMatchIn(it) },
                    activeLine.takeIf { obsoleteMaterialCoordinatePattern.containsMatchIn(it) },
                    activeLine.takeIf { it.contains("kotlinCompilerExtensionVersion") },
                    activeLine.takeIf { it.contains("composeOptions") },
                ).map { "$sourceName:${index + 1}: $it" }
            }

    private fun String.activeBuildDeclaration(): String =
        substringBefore("//")
            .substringBefore("#")
            .trim()

    private fun unusedRuntimeDependencySeamViolations(
        buildInputs: Map<String, String>,
        sourceInputs: Map<String, String>,
        seams: List<ForbiddenUnusedDependencySeam>,
    ): List<String> =
        buildInputs.flatMap { (sourceName, sourceText) ->
            buildDependencySeamViolations(
                sourceName = sourceName,
                sourceText = sourceText,
                seams = seams,
            )
        } +
            sourceInputs.flatMap { (sourceName, sourceText) ->
                sourceImportSeamViolations(
                    sourceName = sourceName,
                    sourceText = sourceText,
                    seams = seams,
                )
            }

    private fun buildDependencySeamViolations(
        sourceName: String,
        sourceText: String,
        seams: List<ForbiddenUnusedDependencySeam>,
    ): List<String> =
        sourceText
            .lines()
            .flatMapIndexed { index, line ->
                val activeLine = line.activeBuildDeclaration()
                seams
                    .filter { seam -> seam.matchesBuildDeclaration(activeLine) }
                    .map { seam -> "$sourceName:${index + 1}: ${seam.name}: $activeLine" }
            }

    private fun sourceImportSeamViolations(
        sourceName: String,
        sourceText: String,
        seams: List<ForbiddenUnusedDependencySeam>,
    ): List<String> =
        sourceText
            .lines()
            .flatMapIndexed { index, line ->
                val activeLine = line.trim()
                seams
                    .filter { seam -> seam.matchesSourceImport(activeLine) }
                    .map { seam -> "$sourceName:${index + 1}: ${seam.name}: $activeLine" }
            }

    private fun buildPolicyInputs(root: File): Map<String, String> =
        root
            .walkTopDown()
            .onEnter { directory -> directory.name !in skippedPolicyDirectories }
            .filter { file -> file.isFile && file.isBuildPolicyInput(root) }
            .associate { file -> file.relativePathFrom(root) to file.readText() }

    private fun sourcePolicyInputs(root: File): Map<String, String> =
        listOf(
            root.resolve("app/src/main/java"),
            root.resolve("app/src/main/kotlin"),
        ).filter { sourceRoot -> sourceRoot.exists() }
            .flatMap { sourceRoot ->
                sourceRoot
                    .walkTopDown()
                    .filter { file -> file.isFile && file.extension in sourcePolicyExtensions }
            }.associate { file -> file.relativePathFrom(root) to file.readText() }

    private fun File.isBuildPolicyInput(root: File): Boolean {
        val relativePath = relativePathFrom(root)
        return name.endsWith(".gradle") ||
            name.endsWith(".gradle.kts") ||
            relativePath == "gradle/libs.versions.toml"
    }

    private fun File.relativePathFrom(root: File): String = relativeTo(root).invariantSeparatorsPath

    private data class ForbiddenUnusedDependencySeam(
        val name: String,
        val catalogAlias: String,
        val accessor: String,
        val group: String,
        val module: String,
        val importPrefixes: List<String>,
    ) {
        private val aliasPattern = Regex("""^${Regex.escape(catalogAlias)}\s*=""")
        private val coordinatePattern =
            Regex("""["']${Regex.escape("$group:$module")}(?::[^"']+)?["']""")

        fun matchesBuildDeclaration(activeLine: String): Boolean =
            activeLine.isNotBlank() &&
                (
                    activeLine.contains("libs.$accessor") ||
                        aliasPattern.containsMatchIn(activeLine) ||
                        coordinatePattern.containsMatchIn(activeLine) ||
                        (
                            activeLine.contains("""group = "$group"""") &&
                                activeLine.contains("""name = "$module"""")
                        )
                )

        fun matchesSourceImport(activeLine: String): Boolean {
            if (!activeLine.startsWith("import ")) return false
            val importTarget =
                activeLine
                    .removePrefix("import ")
                    .removeSuffix(";")
                    .substringBefore(" as ")
            return importPrefixes.any { prefix ->
                importTarget == prefix || importTarget.startsWith("$prefix.")
            }
        }
    }

    private companion object {
        private val skippedPolicyDirectories =
            setOf(
                ".git",
                ".gradle",
                "build",
                "reports",
            )
        private val sourcePolicyExtensions = setOf("java", "kt")

        private val navigationComposeSeam =
            ForbiddenUnusedDependencySeam(
                name = "Navigation Compose",
                catalogAlias = "navigation-compose",
                accessor = "navigation.compose",
                group = "androidx.navigation",
                module = "navigation-compose",
                importPrefixes = listOf("androidx.navigation.compose"),
            )
        private val hiltNavigationComposeSeam =
            ForbiddenUnusedDependencySeam(
                name = "Hilt Navigation Compose",
                catalogAlias = "hilt-navigation-compose",
                accessor = "hilt.navigation.compose",
                group = "androidx.hilt",
                module = "hilt-navigation-compose",
                importPrefixes = listOf("androidx.hilt.navigation.compose"),
            )
        private val datastorePreferencesSeam =
            ForbiddenUnusedDependencySeam(
                name = "DataStore Preferences",
                catalogAlias = "datastore-preferences",
                accessor = "datastore.preferences",
                group = "androidx.datastore",
                module = "datastore-preferences",
                importPrefixes = listOf("androidx.datastore.preferences"),
            )
        private val composeAnimationSeam =
            ForbiddenUnusedDependencySeam(
                name = "Compose Animation",
                catalogAlias = "compose-animation",
                accessor = "compose.animation",
                group = "androidx.compose.animation",
                module = "animation",
                importPrefixes = listOf("androidx.compose.animation"),
            )
        private val materialIconsExtendedSeam =
            ForbiddenUnusedDependencySeam(
                name = "Material Icons Extended",
                catalogAlias = "compose-material-icons-extended",
                accessor = "compose.material.icons.extended",
                group = "androidx.compose.material",
                module = "material-icons-extended",
                importPrefixes = listOf("androidx.compose.material.icons"),
            )

        // Compose Animation on nykyään käytössä (candy-animaatiot), joten sen
        // seam ei enää kuulu live-valvontaan — vain tunnistintestiin alla.
        private val unusedRuntimeDependencySeams =
            listOf(
                navigationComposeSeam,
                hiltNavigationComposeSeam,
                datastorePreferencesSeam,
            )
        private val allForbiddenUnusedDependencySeams =
            unusedRuntimeDependencySeams + materialIconsExtendedSeam + composeAnimationSeam

        private val explicitVersionPattern = Regex("""\bversion(?:\.ref)?\s*=""")
        private val composeCoordinateWithVersionPattern =
            Regex("""androidx\.compose(?:\.[A-Za-z0-9_.-]+)?:[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+""")
        private val obsoleteMaterialCoordinatePattern =
            Regex("""androidx\.compose\.material:material(?::[A-Za-z0-9_.-]+)?""")
    }
}
