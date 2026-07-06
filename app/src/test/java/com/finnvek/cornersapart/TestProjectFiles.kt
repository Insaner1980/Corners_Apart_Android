package com.finnvek.cornersapart

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

internal class ProjectFiles(
    private val root: Path = projectRoot(),
) {
    val appBuildFile: String by lazy { read("app/build.gradle.kts") }
    val versionsCatalog: String by lazy { read("gradle/libs.versions.toml") }
    val manifest: String by lazy { read("app/src/main/AndroidManifest.xml") }
    val strings: String by lazy { read("app/src/main/res/values/strings.xml") }

    fun read(relativePath: String): String = root.resolve(relativePath).toFile().readText()
}

internal fun projectFiles(root: Path = projectRoot()): ProjectFiles = ProjectFiles(root)

internal fun String.withoutLineComments(): String =
    lineSequence()
        .joinToString("\n") { line -> line.substringBefore("//") }

internal fun String.extractKotlinBlock(name: String): String {
    val start =
        Regex("""\b${Regex.escape(name)}\s*\{""")
            .find(this)
            ?.range
            ?.first
            ?: error("$name block was not found.")
    val openBrace = indexOf('{', start)
    var depth = 0
    for (index in openBrace until length) {
        when (this[index]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) {
                    return substring(openBrace + 1, index)
                }
            }
        }
    }
    error("$name block was not closed.")
}

internal fun filesUnder(
    root: Path,
    ignoredPathSegments: Set<String>,
    isIncludedFileName: (String) -> Boolean,
): List<Path> {
    val stream = Files.walk(root)
    return try {
        val visibleFiles =
            stream
                .filter { path -> Files.isRegularFile(path) }
                .filter { path ->
                    root.relativize(path).none { segment ->
                        segment.toString() in ignoredPathSegments
                    }
                }

        visibleFiles
            .filter { path -> isIncludedFileName(path.fileName.toString()) }
            .collect(Collectors.toList())
    } finally {
        stream.close()
    }
}

internal fun String.isGradleScriptFileName(): Boolean = endsWith(".gradle") || endsWith(".gradle.kts")

internal fun String.isGradleToolchainFileName(): Boolean =
    isGradleScriptFileName() ||
        this == "gradle.properties" ||
        this == "libs.versions.toml"
