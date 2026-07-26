import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.stability.analyzer)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.owasp.dependency.check) apply false
    jacoco
}

abstract class GradleBuildFeatureAccess
    @Inject
    constructor(
        val buildFeatures: BuildFeatures,
    )

val configurationCacheActive =
    objects
        .newInstance<GradleBuildFeatureAccess>()
        .buildFeatures
        .configurationCache
        .active
        .get()

val releaseSigningEnvPrefix = "CORNERS_APART"

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

val releaseSigningEnvNames =
    listOf(
        "${releaseSigningEnvPrefix}_KEYSTORE_PATH",
        "${releaseSigningEnvPrefix}_KEYSTORE_PASSWORD",
        "${releaseSigningEnvPrefix}_KEY_ALIAS",
        "${releaseSigningEnvPrefix}_KEY_PASSWORD",
    )

val releaseSigningAvailable =
    releaseSigningEnvNames.all { envName ->
        providers.environmentVariable(envName).orNull?.isNotBlank() == true
    }

fun requiredReleaseEnv(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: error("Release signing requires the $name environment variable.")

android {
    namespace = "com.finnvek.cornersapart"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.finnvek.cornersapart"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningAvailable) {
                storeFile = file(requiredReleaseEnv("${releaseSigningEnvPrefix}_KEYSTORE_PATH"))
                storePassword = requiredReleaseEnv("${releaseSigningEnvPrefix}_KEYSTORE_PASSWORD")
                keyAlias = requiredReleaseEnv("${releaseSigningEnvPrefix}_KEY_ALIAS")
                keyPassword = requiredReleaseEnv("${releaseSigningEnvPrefix}_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Add debug-only build config fields here
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true

        enable +=
            setOf(
                "NewApi",
                "InlinedApi",
                "ObsoleteSdkInt",
                "UnusedResources",
                "MissingPermission",
                "HardcodedText",
                "MissingTranslation",
                "Recycle",
                "StaticFieldLeak",
                "SetTextI18n",
                "RtlHardcoded",
                "ContentDescription",
                "PrivateResource",
                "InvalidPackage",
                "WrongThread",
            )

        disable +=
            setOf(
                "GradleDependency",
                "AndroidGradlePluginVersion",
            )

        fatal += setOf("OldTargetApi")

        checkGeneratedSources = false
        htmlReport = true
        xmlReport = true
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

gradle.taskGraph.whenReady {
    val releaseArtifactTaskNamesRequiringSigning =
        setOf(
            "assembleRelease",
            "bundleRelease",
            "packageRelease",
            "packageReleaseBundle",
            "signReleaseBundle",
            "packageReleaseUniversalApk",
        )

    fun isReleaseArtifactTaskRequiringSigning(name: String): Boolean =
        name in releaseArtifactTaskNamesRequiringSigning ||
            (
                name.endsWith("Release") &&
                    (
                        name.startsWith("assemble") ||
                            name.startsWith("bundle") ||
                            name.startsWith("package") ||
                            name.startsWith("publish")
                    )
            )

    val releaseArtifactsRequested =
        allTasks.any { task ->
            isReleaseArtifactTaskRequiringSigning(task.name)
        }

    if (releaseArtifactsRequested && !releaseSigningAvailable) {
        error(
            "Release signing requires these environment variables: " +
                releaseSigningEnvNames.joinToString(),
        )
    }
}

hilt {
    enableAggregatingTask = true
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("detekt-baseline.xml")
    parallel = true
}

composeStabilityAnalyzer {
    stabilityValidation {
        enabled.set(true)
        outputDir.set(layout.projectDirectory.dir("stability"))
        failOnStabilityChange.set(true)
        allowMissingBaseline.set(false)
    }
}

if (configurationCacheActive) {
    tasks.register("dependencyCheckAnalyze") {
        group = "verification"
        description =
            "OWASP Dependency-Check vaatii ajon ilman Gradlen configuration cachea."

        doLast {
            throw GradleException(
                "OWASP Dependency-Check Analyze ei ole yhteensopiva Gradlen configuration cachen " +
                    "kanssa. Aja tarkistus komennolla: .\\gradlew.bat :app:dependencyCheckAnalyze " +
                    "--no-configuration-cache --console=plain",
            )
        }
    }
} else {
    apply(plugin = "org.owasp.dependencycheck")

    extensions.configure<DependencyCheckExtension>("dependencyCheck") {
        formats = listOf("HTML", "JSON")
        outputDirectory = rootProject.layout.projectDirectory.dir("reports")
        suppressionFile =
            rootProject.layout.projectDirectory
                .file("config/dependency-check/suppressions.xml")
                .asFile.absolutePath
        data {
            val defaultDataDirectory =
                rootProject.layout.projectDirectory
                    .dir(".gradle/dependency-check-data")
                    .asFile.absolutePath

            directory =
                providers
                    .environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY")
                    .orElse(defaultDataDirectory)
                    .get()
        }
        autoUpdate =
            providers
                .environmentVariable("DEPENDENCY_CHECK_AUTO_UPDATE")
                .map {
                    it.equals("true", ignoreCase = true) ||
                        it == "1" ||
                        it.equals("yes", ignoreCase = true)
                }.getOrElse(true)
        failBuildOnCVSS =
            providers
                .environmentVariable("DEPENDENCY_CHECK_FAIL_BUILD_ON_CVSS")
                .map { it.toFloatOrNull() ?: 7f }
                .getOrElse(7f)
        scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
        skipTestGroups = true
        analyzers {
            ossIndex {
                enabled = false
            }
        }
        nvd {
            providers.environmentVariable("NVD_API_KEY").orNull?.let { apiKey = it }
            delay =
                providers
                    .environmentVariable("NVD_API_DELAY_MS")
                    .map { it.toIntOrNull() ?: 6_000 }
                    .getOrElse(6_000)
            maxRetryCount =
                providers
                    .environmentVariable("NVD_API_MAX_RETRY_COUNT")
                    .map { it.toIntOrNull() ?: 20 }
                    .getOrElse(20)
            validForHours =
                providers
                    .environmentVariable("NVD_VALID_FOR_HOURS")
                    .map { it.toIntOrNull() ?: 24 }
                    .getOrElse(24)
        }
    }

    tasks.named("dependencyCheckAnalyze") {
        notCompatibleWithConfigurationCache(
            "OWASP Dependency-Check Analyze sailyttaa Project-viitteita taskin tilassa.",
        )
    }
}

val releasePackagedManifest =
    layout.buildDirectory.file(
        "intermediates/packaged_manifests/release/processReleaseManifestForPackage/AndroidManifest.xml",
    )

tasks.withType<Test>().configureEach {
    dependsOn("processReleaseManifestForPackage")
    inputs
        .file(releasePackagedManifest)
        .withPathSensitivity(PathSensitivity.RELATIVE)

    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val jacocoDebugUnitTestReportExclusions =
    listOf(
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/R.class",
        "**/R$*.class",
        "**/*Test*.*",
        "**/*PreviewKt*.*",
        "**/*ComposableSingletons*.*",
        "**/di/**",
    )

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Luo JaCoCo XML -raportin SonarCloudin debug unit test -coveragea varten."

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml"),
        )
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoDebugUnitTestReport/html"))
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
                exclude(jacocoDebugUnitTestReportExclusions)
            },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                exclude(jacocoDebugUnitTestReportExclusions)
            },
            fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
                exclude(jacocoDebugUnitTestReportExclusions)
            },
        ),
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            )
        },
    )
}

dependencies {
    val composeBom = platform(libs.compose.bom)

    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.datastore)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.activity.compose)
    implementation(libs.play.services.nearby)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)

    detektPlugins(libs.detekt.compose.rules)

    lintChecks(libs.android.security.lints)

    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)

    kspTest(libs.hilt.compiler)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)

    kspAndroidTest(libs.hilt.compiler)
}
