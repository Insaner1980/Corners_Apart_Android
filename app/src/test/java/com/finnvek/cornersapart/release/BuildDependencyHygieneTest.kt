package com.finnvek.cornersapart.release

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildDependencyHygieneTest {
    @Test
    fun appDoesNotDeclareUnusedRuntimeDependencySeams() {
        val root = projectRoot()
        val appBuildFile = root.resolve("app/build.gradle.kts").toFile().readText()
        val versionsCatalog = root.resolve("gradle/libs.versions.toml").toFile().readText()

        assertFalse(appBuildFile.contains("implementation(libs.navigation.compose)"))
        assertFalse(versionsCatalog.contains("navigation-compose ="))

        assertFalse(appBuildFile.contains("implementation(libs.hilt.navigation.compose)"))
        assertFalse(versionsCatalog.contains("hilt-navigation-compose ="))
        assertTrue(appBuildFile.contains("implementation(libs.hilt.lifecycle.viewmodel.compose)"))

        assertFalse(appBuildFile.contains("implementation(libs.datastore.preferences)"))
        assertFalse(versionsCatalog.contains("datastore-preferences ="))

        assertFalse(appBuildFile.contains("implementation(libs.compose.animation)"))
        assertFalse(versionsCatalog.contains("compose-animation ="))
    }
}
