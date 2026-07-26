buildscript {
    configurations.classpath {
        resolutionStrategy.force(
            *providers
                .gradleProperty("buildToolSecurityOverrides")
                .get()
                .split(",")
                .toTypedArray(),
        )
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.stability.analyzer) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
    alias(libs.plugins.sonarqube)
}

val buildToolSecurityOverrides =
    providers
        .gradleProperty("buildToolSecurityOverrides")
        .get()
        .split(",")
        .toTypedArray()

allprojects {
    configurations.configureEach {
        resolutionStrategy.force(*buildToolSecurityOverrides)
    }
}

val sonarProjectProperties =
    java.util.Properties().apply {
        val file = rootProject.file("sonar-project.properties")
        if (file.isFile) {
            file.inputStream().use(::load)
        }
    }

val gradleManagedSonarProperties =
    setOf(
        "sonar.sources",
        "sonar.tests",
        "sonar.java.binaries",
        "sonar.java.test.binaries",
        "sonar.java.libraries",
        "sonar.java.test.libraries",
        "sonar.kotlin.binaries",
        "sonar.coverage.jacoco.xmlReportPaths",
    )

sonar {
    properties {
        property("sonar.host.url", sonarProjectProperties.getProperty("sonar.host.url", "https://sonarcloud.io"))
        sonarProjectProperties.forEach { key, value ->
            val propertyName = key.toString()
            if (propertyName !in gradleManagedSonarProperties) {
                property(propertyName, value.toString())
            }
        }
    }
}

project(":app") {
    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory
                    .file("reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml")
                    .get()
                    .asFile
                    .absolutePath,
            )
        }
    }
}

tasks.named("sonar") {
    dependsOn(":app:assembleDebug", ":app:jacocoDebugUnitTestReport")
}

ktlint {
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
