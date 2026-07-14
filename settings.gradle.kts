pluginManagement {
    val foojayResolverConventionVersion: String by settings

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version foojayResolverConventionVersion
    }

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral {
            content {
                excludeGroupByRegex("com\\.android.*")
                excludeGroupByRegex("androidx.*")
                excludeGroup("org.jlleitschuh.gradle.ktlint")
                excludeGroup("org.jlleitschuh.gradle")
                excludeGroup("dev.detekt")
                excludeGroup("org.gradle.toolchains")
                excludeGroup("org.gradle.toolchains.foojay-resolver-convention")
                excludeGroup("org.owasp.dependencycheck")
                excludeModule("org.owasp", "dependency-check-gradle")
                excludeGroup("org.sonarqube")
                excludeModule("org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin")
            }
        }
        gradlePluginPortal {
            content {
                includeGroup("org.jlleitschuh.gradle.ktlint")
                includeGroup("org.jlleitschuh.gradle")
                includeGroup("dev.detekt")
                includeGroup("org.gradle.toolchains")
                includeGroup("org.gradle.toolchains.foojay-resolver-convention")
                includeGroup("org.owasp.dependencycheck")
                includeModule("org.owasp", "dependency-check-gradle")
                includeGroup("org.sonarqube")
                includeModule("org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CornersApart"
include(":app")
