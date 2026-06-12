import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.owasp.dependency.check)
    jacoco
}

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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.finnvek.cornersapart"
        minSdk = 26
        targetSdk = 36
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

        checkGeneratedSources = false
        htmlReport = true
        xmlReport = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

gradle.taskGraph.whenReady {
    val releaseArtifactsRequested =
        allTasks.any { task ->
            val name = task.name
            name.endsWith("Release") &&
                (
                    name.startsWith("assemble") ||
                        name.startsWith("bundle") ||
                        name.startsWith("package") ||
                        name.startsWith("publish")
                )
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

dependencyCheck {
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
            }
            .getOrElse(true)
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

tasks.withType<Test>().configureEach {
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
        "**/*Preview*.*",
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
            fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
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

tasks.configureEach {
    if (name.startsWith("hiltJavaCompile") && name.endsWith("UnitTest")) {
        enabled = false
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore)
    implementation(libs.datastore.preferences)

    // Serialization
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Nearby
    implementation(libs.play.services.nearby)

    // Detekt plugins
    detektPlugins(libs.detekt.compose.rules)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
