package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.filesUnder
import com.finnvek.cornersapart.isGradleToolchainFileName
import com.finnvek.cornersapart.projectFiles
import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BuildToolchainCompatibilityTest {
    @Test
    fun gradleAndPluginPinsStayOnTheVerifiedCompatibilityLine() {
        val root = projectRoot()
        val settingsFile = root.resolve("settings.gradle.kts").toFile().readText()
        val gradleProperties = root.resolve("gradle.properties").toFile().readText()
        val wrapperProperties = root.resolve("gradle/wrapper/gradle-wrapper.properties").toFile().readText()
        val versionsCatalog = root.resolve("gradle/libs.versions.toml").toFile().readText()

        assertTrue(settingsFile.contains("org.gradle.toolchains.foojay-resolver-convention"))
        assertTrue(gradleProperties.contains("foojayResolverConventionVersion=1.0.0"))
        assertTrue(settingsFile.contains("version foojayResolverConventionVersion"))
        assertFalse(
            settingsFile.contains(
                """id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"""",
            ),
        )
        assertTrue(
            wrapperProperties
                .contains("distributionUrl=https\\://services.gradle.org/distributions/gradle-9.6.1-bin.zip"),
        )
        assertTrue(wrapperProperties.contains("distributionSha256Sum="))
        assertTrue(versionsCatalog.contains("agp = \"9.3.1\""))
        assertTrue(versionsCatalog.contains("detekt = \"2.0.0-alpha.5\""))
        assertTrue(versionsCatalog.contains("detektComposeRules = \"0.6.3\""))
        assertTrue(versionsCatalog.contains("detekt = { id = \"dev.detekt\", version.ref = \"detekt\" }"))
    }

    @Test
    fun detektConfigStaysOnDetektTwoSchema() {
        val detektConfig = projectRoot().resolve("config/detekt/detekt.yml").toFile().readText()
        val obsoleteLines =
            detektConfig
                .lines()
                .mapIndexedNotNull { index, line ->
                    val activeLine = line.substringBefore("#").trim()
                    if (
                        activeLine.isNotBlank() &&
                        obsoleteDetektTwoConfigPatterns.any { pattern -> pattern.containsMatchIn(activeLine) }
                    ) {
                        "${index + 1}: $activeLine"
                    } else {
                        null
                    }
                }

        assertTrue(
            "Detekt 2 config must use current schema keys instead of validation-excluded 1.x names:\n" +
                obsoleteLines.joinToString("\n"),
            obsoleteLines.isEmpty(),
        )
    }

    @Test
    fun ktlintOwnsFormattingLineLength() {
        val root = projectRoot()
        val editorConfig = root.resolve(".editorconfig").toFile().readText()
        val detektConfig = root.resolve("config/detekt/detekt.yml").toFile().readText()
        val maxLineLengthBlock =
            requireNotNull(detektRuleBlock("MaxLineLength", detektConfig)) {
                "Detekt MaxLineLength block is missing."
            }

        assertTrue(editorConfig.contains("max_line_length = 120"))
        assertTrue(
            "Line length must have one formatter owner: ktlint reads .editorconfig, detekt must not duplicate it.",
            maxLineLengthBlock.contains("active: false"),
        )
    }

    @Test
    fun appBuildPinsJavaAndKotlinToolchainsToJdk17() {
        val appBuildFile = projectRoot().resolve("app/build.gradle.kts").toFile().readText()

        assertTrue(appBuildFile.contains("sourceCompatibility = JavaVersion.VERSION_17"))
        assertTrue(appBuildFile.contains("targetCompatibility = JavaVersion.VERSION_17"))
        assertTrue(appBuildFile.contains("jvmToolchain(17)"))
        assertTrue(appBuildFile.contains("jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17"))
    }

    @Test
    fun gradlePropertiesDoNotForceKotlinCompilerInProcess() {
        val gradleProperties =
            Properties().apply {
                projectRoot()
                    .resolve("gradle.properties")
                    .toFile()
                    .inputStream()
                    .use(::load)
            }

        assertFalse(
            "Kotlin compiler must not be forced into the Gradle daemon; use the default daemon strategy instead.",
            gradleProperties.getProperty("kotlin.compiler.execution.strategy") == "in-process",
        )
    }

    @Test
    fun dependencyCheckAnalyzeAvoidsOwaspTaskWhenConfigurationCacheIsRequested() {
        val appBuildFile = projectRoot().resolve("app/build.gradle.kts").toFile().readText()
        val pluginsBlock =
            Regex("""plugins\s*\{[\s\S]*?^}""", RegexOption.MULTILINE)
                .find(appBuildFile)
                ?.value
                .orEmpty()

        assertFalse(
            "OWASP Dependency-Check must not be applied unconditionally because its Analyze task " +
                "captures Project state and poisons direct configuration-cache runs.",
            unconditionalOwaspDependencyCheckPluginPattern.containsMatchIn(pluginsBlock),
        )
        assertTrue(
            "Configuration-cache requests must get a lightweight dependencyCheckAnalyze fallback.",
            configurationCacheDependencyCheckFallbackPattern.containsMatchIn(appBuildFile),
        )
        assertTrue(
            "Non-configuration-cache runs must still apply OWASP Dependency-Check.",
            owaspDependencyCheckConditionalApplyPattern.containsMatchIn(appBuildFile),
        )
    }

    @Test
    fun releaseSigningGuardRejectsRealReleaseArtifactTaskGraphWithoutSigningEnvironment() {
        listOf(
            "assembleRelease",
            ":app:makeApkFromBundleForRelease",
            ":app:zipApksForRelease",
        ).forEach { taskName ->
            val result =
                runGradleWithoutReleaseSigning(
                    taskName.toCacheKey(),
                    taskName,
                    "--dry-run",
                    "--no-configuration-cache",
                )

            assertFalse(
                "Release artifact task must fail without signing environment: $taskName\n${result.output}",
                result.succeeded,
            )
            assertTrue(
                "Release artifact task must fail at the signing gate: $taskName\n${result.output}",
                result.output.contains(RELEASE_SIGNING_FAILURE_MESSAGE),
            )
        }
    }

    @Test
    fun releaseSigningGuardDoesNotBlockRealReleaseVerificationTasksWithoutSigningEnvironment() {
        val result =
            runGradleWithoutReleaseSigning(
                "release-verification-tasks",
                ":app:compileReleaseKotlin",
                ":app:detektRelease",
                ":app:lintRelease",
                "--dry-run",
                "--no-configuration-cache",
            )

        assertTrue(
            "Release verification tasks must not require signing environment.\n${result.output}",
            result.succeeded,
        )
        assertFalse(
            "Release verification tasks must not hit the signing gate.\n${result.output}",
            result.output.contains(RELEASE_SIGNING_FAILURE_MESSAGE),
        )
    }

    @Test
    fun releaseSigningGuardRejectsReleaseArtifactsAfterConfigurationCacheSigningEnvironmentChange() {
        val cacheKey = "configuration-cache-signing-env"
        val signedResult =
            runGradleWithReleaseSigning(
                cacheKey,
                ":app:assembleRelease",
                "--dry-run",
                "--configuration-cache",
            )
        assertTrue(
            "Release artifact dry-run must pass when signing environment is present.\n${signedResult.output}",
            signedResult.succeeded,
        )

        val unsignedResult =
            runGradleWithoutReleaseSigning(
                cacheKey,
                ":app:assembleRelease",
                "--dry-run",
                "--configuration-cache",
            )
        assertFalse(
            "Release artifact dry-run must fail after signing environment is removed.\n${unsignedResult.output}",
            unsignedResult.succeeded,
        )
        assertTrue(
            "Configuration-cache run must still fail at the signing gate after env removal.\n${unsignedResult.output}",
            unsignedResult.output.contains(RELEASE_SIGNING_FAILURE_MESSAGE),
        )
    }

    @Test
    fun appDoesNotApplyLegacyAndroidKotlinOrKaptConfiguration() {
        val root = projectRoot()
        val offenders =
            gradleToolchainFilesUnder(root)
                .flatMap { path ->
                    path
                        .toFile()
                        .readLines()
                        .mapIndexedNotNull { index, line ->
                            val activeLine = line.substringBefore("//").substringBefore("#")
                            if (
                                legacyAndroidKotlinPluginPatterns.any { pattern ->
                                    pattern.containsMatchIn(activeLine)
                                }
                            ) {
                                "${root.relativize(path)}:${index + 1}: ${activeLine.trim()}"
                            } else {
                                null
                            }
                        }
                }

        assertTrue(
            "AGP 9 built-in Kotlin must stay as the only Android Kotlin compiler path:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun hiltTestComponentGenerationStaysWiredForJvmAndInstrumentedTests() {
        val files = projectFiles()

        assertTrue(
            files.versionsCatalog.contains(
                "hilt-android-testing = { group = \"com.google.dagger\", name = \"hilt-android-testing\", version.ref = \"hilt\" }",
            ),
        )
        assertTrue(files.appBuildFile.contains("testImplementation(libs.hilt.android.testing)"))
        assertTrue(files.appBuildFile.contains("kspTest(libs.hilt.compiler)"))
        assertTrue(files.appBuildFile.contains("androidTestImplementation(libs.hilt.android.testing)"))
        assertTrue(files.appBuildFile.contains("kspAndroidTest(libs.hilt.compiler)"))
        assertFalse(
            "Hilt unit test component generation must not be disabled.",
            disabledHiltUnitTestCompilePattern.containsMatchIn(files.appBuildFile),
        )
    }

    @Test
    fun jacocoDebugReportUsesCurrentAgpClassOutputs() {
        val appBuildFile = projectRoot().resolve("app/build.gradle.kts").toFile().readText()

        assertTrue(appBuildFile.contains("intermediates/javac/debug/compileDebugJavaWithJavac/classes"))
        assertFalse(
            "JaCoCo must not point at the stale AGP javac output directory.",
            appBuildFile.contains("intermediates/javac/debug/classes"),
        )
        assertTrue(appBuildFile.contains("tmp/kotlin-classes/debug"))
        assertTrue(appBuildFile.contains("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"))
    }

    @Test
    fun jacocoPreviewExclusionDoesNotDropProductionPreviewModels() {
        val root = projectRoot()
        val appBuildFile = root.resolve("app/build.gradle.kts").toFile().readText()
        val moveResultFile =
            root.resolve("app/src/main/java/com/finnvek/cornersapart/engine/MoveResult.kt").toFile().readText()

        assertFalse(
            "A broad *Preview* exclusion drops production models such as PlacementPreview.",
            appBuildFile.contains(""""**/*Preview*.*""""),
        )
        assertTrue(appBuildFile.contains(""""**/*PreviewKt*.*""""))
        assertTrue(moveResultFile.contains("data class PlacementPreview"))
    }

    @Test
    fun legacyAndroidKotlinPluginPatternsRecognizeForbiddenDeclarations() {
        val forbiddenDeclarations =
            listOf(
                """id("org.jetbrains.kotlin.android")""",
                """id("kotlin-android")""",
                """alias(libs.plugins.kotlin.android)""",
                """kotlin("android")""",
                """id("org.jetbrains.kotlin.kapt")""",
                """kotlin("kapt")""",
                """android.builtInKotlin=false""",
            )

        forbiddenDeclarations.forEach { declaration ->
            assertTrue(
                "Expected legacy Kotlin guard to reject: $declaration",
                legacyAndroidKotlinPluginPatterns.any { pattern -> pattern.containsMatchIn(declaration) },
            )
        }

        listOf(
            """id("org.jetbrains.kotlin.plugin.compose")""",
            """id("org.jetbrains.kotlin.plugin.serialization")""",
            """alias(libs.plugins.kotlin.compose)""",
            """alias(libs.plugins.ksp)""",
            """ksp(libs.hilt.compiler)""",
            """kotlin.code.style=official""",
        ).forEach { declaration ->
            assertFalse(
                "Expected AGP 9-compatible Kotlin configuration to stay allowed: $declaration",
                legacyAndroidKotlinPluginPatterns.any { pattern -> pattern.containsMatchIn(declaration) },
            )
        }
    }

    private fun gradleToolchainFilesUnder(root: Path): List<Path> =
        filesUnder(
            root = root,
            ignoredPathSegments = ignoredPathSegments,
        ) { fileName -> fileName.isGradleToolchainFileName() }

    private fun runGradleWithoutReleaseSigning(
        cacheKey: String,
        vararg arguments: String,
    ): GradleRunResult =
        runGradle(
            cacheKey = cacheKey,
            processEnvironment = releaseSigningEnvironment(isAvailable = false),
            arguments = arguments,
        )

    private fun runGradleWithReleaseSigning(
        cacheKey: String,
        vararg arguments: String,
    ): GradleRunResult =
        runGradle(
            cacheKey = cacheKey,
            processEnvironment = releaseSigningEnvironment(isAvailable = true),
            arguments = arguments,
        )

    private fun runGradle(
        cacheKey: String,
        processEnvironment: Map<String, String>,
        arguments: Array<out String>,
    ): GradleRunResult {
        val root = projectRoot()
        val projectCacheDir = root.resolve("build/test-gradle-cache/release-signing/$cacheKey")
        val command =
            gradleWrapperCommand(root) +
                arguments.toList() +
                listOf(
                    "--project-cache-dir",
                    projectCacheDir.toString(),
                    "--console=plain",
                    "--no-daemon",
                )
        val process =
            ProcessBuilder(command)
                .directory(root.toFile())
                .redirectErrorStream(true)
                .apply {
                    environment().clear()
                    environment().putAll(processEnvironment)
                }.start()

        val outputReader = Executors.newSingleThreadExecutor()
        val outputFuture =
            outputReader.submit<String> {
                process.inputStream.bufferedReader().use { reader -> reader.readText() }
            }

        try {
            check(process.waitFor(GRADLE_INVOCATION_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly()
                "Gradle invocation timed out: ${command.joinToString(" ")}"
            }
            return GradleRunResult(
                exitCode = process.exitValue(),
                output = outputFuture.get(10, TimeUnit.SECONDS),
            )
        } finally {
            outputReader.shutdownNow()
        }
    }

    private fun releaseSigningEnvironment(isAvailable: Boolean): Map<String, String> =
        System
            .getenv()
            .filterKeys { key -> key !in RELEASE_SIGNING_ENV_NAMES }
            .toMutableMap()
            .apply {
                if (isAvailable) {
                    this["CORNERS_APART_KEYSTORE_PATH"] = "build/test-release-signing/missing-release-key.jks"
                    this["CORNERS_APART_KEYSTORE_PASSWORD"] = "password"
                    this["CORNERS_APART_KEY_ALIAS"] = "corners-apart"
                    this["CORNERS_APART_KEY_PASSWORD"] = "password"
                }
            }

    private fun gradleWrapperCommand(root: Path): List<String> {
        val isWindows = (System.getProperty("os.name") ?: "").contains("Windows", ignoreCase = true)
        return if (isWindows) {
            listOf("cmd", "/c", root.resolve("gradlew.bat").toString())
        } else {
            listOf(root.resolve("gradlew").toString())
        }
    }

    private fun String.toCacheKey(): String =
        lowercase()
            .replace(Regex("""[^a-z0-9]+"""), "-")
            .trim('-')

    private data class GradleRunResult(
        val exitCode: Int,
        val output: String,
    ) {
        val succeeded: Boolean = exitCode == 0
    }

    private fun detektRuleBlock(
        ruleName: String,
        detektConfig: String,
    ): String? =
        Regex(
            pattern = """(?ms)^  $ruleName:\R.*?(?=^  [A-Za-z][A-Za-z0-9]*:|\z)""",
        ).find(detektConfig)?.value

    private companion object {
        private val ignoredPathSegments =
            setOf(
                ".git",
                ".gradle",
                ".idea",
                ".sonar",
                ".tmp",
                "app/build",
                "build",
                "reports",
            )

        private val legacyAndroidKotlinPluginPatterns =
            listOf(
                Regex("""\borg\.jetbrains\.kotlin\.android\b"""),
                Regex("""\bkotlin-android\b"""),
                Regex("""\bkotlin\.android\b"""),
                Regex("""\bkotlin\s*\(\s*["']android["']\s*\)"""),
                Regex("""\borg\.jetbrains\.kotlin\.kapt\b"""),
                Regex("""\bkotlin-kapt\b"""),
                Regex("""\bkotlin\s*\(\s*["']kapt["']\s*\)"""),
                Regex("""\bkapt\s*\("""),
                Regex("""\bandroid\.builtInKotlin\s*=\s*false\b"""),
            )
        private val disabledHiltUnitTestCompilePattern =
            Regex("""hiltJavaCompile[\s\S]*UnitTest[\s\S]*enabled\s*=\s*false""")
        private const val GRADLE_INVOCATION_TIMEOUT_MINUTES = 4L
        private val RELEASE_SIGNING_ENV_NAMES =
            setOf(
                "CORNERS_APART_KEYSTORE_PATH",
                "CORNERS_APART_KEYSTORE_PASSWORD",
                "CORNERS_APART_KEY_ALIAS",
                "CORNERS_APART_KEY_PASSWORD",
            )
        private const val RELEASE_SIGNING_FAILURE_MESSAGE =
            "Release signing requires these environment variables"
        private val obsoleteDetektTwoConfigPatterns =
            listOf(
                Regex("""^maxIssues:"""),
                Regex("""^excludeCorrectable:"""),
                Regex("""^weights:"""),
                Regex("""^output-reports:"""),
                Regex("""^threshold:"""),
                Regex("""^functionThreshold:"""),
                Regex("""^constructorThreshold:"""),
                Regex("""^-\s*'build'"""),
                Regex("""^-\s*'output-reports'"""),
                Regex(""">threshold'"""),
                Regex(""">functionThreshold'"""),
                Regex(""">constructorThreshold'"""),
                Regex("""^CommentOverPrivate(Function|Property):"""),
                Regex("""^Function(Max|Min)Length:"""),
                Regex("""^MayBeConst:"""),
                Regex("""^PreferToOverPairSyntax:"""),
                Regex("""^RedundantVisibilityModifierRule:"""),
                Regex("""^SpacingBetweenPackageAndImports:"""),
                Regex("""^UnnecessaryAnnotationUseSiteTarget:"""),
                Regex("""^UntilInsteadOfRangeTo:"""),
                Regex("""^UnusedImports:"""),
                Regex("""^UnusedPrivateMember:"""),
            )
        private val configurationCacheDependencyCheckFallbackPattern =
            Regex(
                """if\s*\(\s*configurationCacheActive\s*\)\s*\{[\s\S]*?""" +
                    """tasks\.register\(\s*["']dependencyCheckAnalyze["']\s*\)""",
            )
        private val owaspDependencyCheckConditionalApplyPattern =
            Regex(
                """else\s*\{[\s\S]*?""" +
                    """apply\(\s*plugin\s*=\s*["']org\.owasp\.dependencycheck["']\s*\)""",
            )
        private val unconditionalOwaspDependencyCheckPluginPattern =
            Regex("""alias\(libs\.plugins\.owasp\.dependency\.check\)(?!\s+apply\s+false)""")
    }
}
