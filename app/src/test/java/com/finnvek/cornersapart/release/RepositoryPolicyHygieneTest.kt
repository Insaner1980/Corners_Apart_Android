package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.extractKotlinBlock
import com.finnvek.cornersapart.filesUnder
import com.finnvek.cornersapart.isGradleScriptFileName
import com.finnvek.cornersapart.projectRoot
import com.finnvek.cornersapart.withoutLineComments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class RepositoryPolicyHygieneTest {
    @Test
    fun pluginResolutionRepositoriesPinKnownPluginGroups() {
        val root = projectRoot()
        val settingsFile = root.resolve("settings.gradle.kts")
        val settings = settingsFile.toFile().readText()
        val activeSettings = settings.withoutLineComments()
        val repositoriesBlock =
            activeSettings
                .extractKotlinBlock("pluginManagement")
                .extractKotlinBlock("repositories")

        assertContainsAll(
            sourceName = "google plugin repository filter",
            sourceText = repositoriesBlock.extractKotlinBlock("google"),
            expected =
                listOf(
                    """includeGroupByRegex("com\\.android.*")""",
                    """includeGroupByRegex("androidx.*")""",
                ),
        )
        assertContainsAll(
            sourceName = "Gradle Plugin Portal repository filter",
            sourceText = repositoriesBlock.extractKotlinBlock("gradlePluginPortal"),
            expected =
                listOf(
                    """includeGroup("org.jlleitschuh.gradle.ktlint")""",
                    """includeGroup("org.jlleitschuh.gradle")""",
                    """includeGroup("dev.detekt")""",
                    """includeGroup("org.gradle.toolchains")""",
                    """includeGroup("org.gradle.toolchains.foojay-resolver-convention")""",
                    """includeGroup("org.owasp.dependencycheck")""",
                    """includeModule("org.owasp", "dependency-check-gradle")""",
                    """includeGroup("org.sonarqube")""",
                    """includeModule("org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin")""",
                ),
        )
        assertContainsAll(
            sourceName = "mavenCentral plugin repository filter",
            sourceText = repositoriesBlock.extractKotlinBlock("mavenCentral"),
            expected =
                listOf(
                    """excludeGroupByRegex("com\\.android.*")""",
                    """excludeGroupByRegex("androidx.*")""",
                    """excludeGroup("org.jlleitschuh.gradle.ktlint")""",
                    """excludeGroup("org.jlleitschuh.gradle")""",
                    """excludeGroup("dev.detekt")""",
                    """excludeGroup("org.gradle.toolchains")""",
                    """excludeGroup("org.gradle.toolchains.foojay-resolver-convention")""",
                    """excludeGroup("org.owasp.dependencycheck")""",
                    """excludeModule("org.owasp", "dependency-check-gradle")""",
                    """excludeGroup("org.sonarqube")""",
                    """excludeModule("org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin")""",
                ),
        )
    }

    @Test
    fun dependencyResolutionRepositoriesStayInRootSettings() {
        val root = projectRoot()
        val settingsFile = root.resolve("settings.gradle.kts")
        val settings = settingsFile.toFile().readText()
        val activeSettings = settings.withoutLineComments()

        assertTrue(
            "settings.gradle.kts must keep FAIL_ON_PROJECT_REPOS enabled.",
            activeSettings.contains("repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)") ||
                activeSettings.contains("repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS"),
        )

        val dependencyResolutionBlock = activeSettings.extractKotlinBlock("dependencyResolutionManagement")
        val repositoriesBlock = dependencyResolutionBlock.extractKotlinBlock("repositories")
        val repositoryCalls =
            Regex("""\b(google|mavenCentral|gradlePluginPortal)\s*\(\s*\)""")
                .findAll(repositoriesBlock)
                .map { match -> "${match.groupValues[1]}()" }
                .toList()

        assertEquals(
            "Dependency resolution must use only the approved root repositories.",
            listOf("google()", "mavenCentral()"),
            repositoryCalls,
        )
        assertTrue(
            "Dependency resolution must not add ad hoc Maven/Ivy/flatDir repositories.",
            Regex("""\b(maven|ivy|flatDir)\s*(\(|\{)""").find(repositoriesBlock) == null,
        )
    }

    @Test
    fun projectAndConventionBuildScriptsDoNotDeclareRepositories() {
        val root = projectRoot()
        val rootSettings = root.resolve("settings.gradle.kts").toAbsolutePath().normalize()
        val offenders =
            gradleScriptsUnder(root)
                .filterNot { path -> path.toAbsolutePath().normalize() == rootSettings }
                .flatMap { path -> path.matchingActiveLines(Regex("""\brepositories\s*\{"""), root) }
        val formattedOffenders = offenders.joinToString("\n")

        assertTrue(
            "Project, module, buildSrc, and convention-plugin repository blocks must stay out of " +
                "build scripts because they bypass the root repository policy:\n$formattedOffenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun includedBuildsRequireAnExplicitRepositoryPolicyTest() {
        val root = projectRoot()
        val includeBuildUsages =
            gradleScriptsUnder(root)
                .flatMap { path -> path.matchingActiveLines(Regex("""\bincludeBuild\s*\("""), root) }
        val formattedIncludeBuildUsages = includeBuildUsages.joinToString("\n")

        assertTrue(
            "Included builds have their own settings and can define their own repositories. Add explicit " +
                "repository-policy coverage before committing includeBuild usage:\n$formattedIncludeBuildUsages",
            includeBuildUsages.isEmpty(),
        )
    }

    @Test
    fun repositoryInitScriptsAreNotCommitted() {
        val root = projectRoot()
        val initScripts =
            gradleScriptsUnder(root)
                .filter { path -> path.fileName.toString().isInitScriptName() }
                .map { path -> root.relativize(path).toString() }

        assertTrue(
            "Repository-committed Gradle init scripts can rewrite repository policy before project evaluation:\n" +
                initScripts.joinToString("\n"),
            initScripts.isEmpty(),
        )
    }

    private fun gradleScriptsUnder(root: Path): List<Path> =
        filesUnder(
            root = root,
            ignoredPathSegments = ignoredPathSegments,
        ) { fileName -> fileName.isGradleScriptFileName() }

    private fun Path.matchingActiveLines(
        pattern: Regex,
        root: Path,
    ): List<String> =
        toFile()
            .readLines()
            .mapIndexedNotNull { index, line ->
                val activeLine = line.substringBefore("//")
                if (pattern.containsMatchIn(activeLine)) {
                    "${root.relativize(this)}:${index + 1}: ${activeLine.trim()}"
                } else {
                    null
                }
            }

    private fun String.isInitScriptName(): Boolean =
        this == "init.gradle" ||
            this == "init.gradle.kts" ||
            endsWith(".init.gradle") ||
            endsWith(".init.gradle.kts")

    private fun assertContainsAll(
        sourceName: String,
        sourceText: String,
        expected: List<String>,
    ) {
        val missing = expected.filterNot { value -> sourceText.contains(value) }

        assertTrue(
            "$sourceName is missing required repository policy filters:\n" +
                missing.joinToString("\n"),
            missing.isEmpty(),
        )
    }

    private companion object {
        private val ignoredPathSegments =
            setOf(
                ".git",
                ".gradle",
                ".idea",
                ".sonar",
                ".tmp",
                "android",
                "app/build",
                "build",
                "caches",
                "daemon",
                "kotlin-profile",
                "native",
                "notifications",
                "reports",
                "workers",
            )
    }
}
