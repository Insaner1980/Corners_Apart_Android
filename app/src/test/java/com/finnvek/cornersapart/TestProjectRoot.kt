package com.finnvek.cornersapart

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal fun projectRoot(): Path =
    generateSequence(Paths.get("").toAbsolutePath().normalize()) { path -> path.parent }
        .firstOrNull { path -> Files.exists(path.resolve("settings.gradle.kts")) }
        ?: error("Project root was not found from ${System.getProperty("user.dir")}")
