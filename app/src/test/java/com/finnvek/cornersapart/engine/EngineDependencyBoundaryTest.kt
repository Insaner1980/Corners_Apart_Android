package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayDeque

class EngineDependencyBoundaryTest {
    @Test
    fun engineReachableProductionSourcesStayInsidePureKotlinBoundary() {
        val sourceRoot = projectRoot().resolve("app/src/main/java")
        val sourceGraph = SourceGraph.load(sourceRoot)
        val reachableSources = sourceGraph.reachableFromPackage(ENGINE_PACKAGE)
        val forbiddenMatches = reachableSources.flatMap(::forbiddenMatches)

        assertTrue(
            forbiddenMatches.joinToString(
                separator = System.lineSeparator(),
                prefix = "Engine source graph has forbidden production dependencies:${System.lineSeparator()}",
            ) { match ->
                "${sourceRoot.relativize(match.path)}:${match.lineNumber}: ${match.ruleName}: ${match.line.trim()}"
            },
            forbiddenMatches.isEmpty(),
        )
    }

    @Test
    fun forbiddenDependencyScannerCatchesKnownBoundaryLeaks() {
        val source =
            SourceFile(
                path = Paths.get("FakeLeak.kt"),
                packageName = ENGINE_PACKAGE,
                declarations = setOf("FakeLeak"),
                imports =
                    listOf(
                        "android.content.Context",
                        "androidx.compose.runtime.Composable",
                        "androidx.datastore.core.DataStore",
                        "dagger.hilt.InstallIn",
                        "com.google.android.gms.nearby.connection.Payload",
                        "java.time.Instant",
                        "java.util.Locale",
                        "com.finnvek.cornersapart.multiplayer.GameMessage",
                    ),
                text =
                    """
                    package $ENGINE_PACKAGE

                    import android.content.Context
                    import androidx.compose.runtime.Composable
                    import androidx.datastore.core.DataStore
                    import dagger.hilt.InstallIn
                    import com.google.android.gms.nearby.connection.Payload
                    import java.time.Instant
                    import java.util.Locale
                    import com.finnvek.cornersapart.multiplayer.GameMessage

                    class FakeLeak {
                        fun now(): Long = System.currentTimeMillis()
                    }
                    """.trimIndent(),
            )

        val matchedRules = forbiddenMatches(source).map { match -> match.ruleName }.toSet()

        assertTrue(
            "Scanner did not catch all expected boundary leak categories: $matchedRules",
            matchedRules.containsAll(
                setOf(
                    "Android framework",
                    "Compose",
                    "DataStore",
                    "Hilt or DI framework",
                    "Play Services",
                    "Wall-clock or time-zone API",
                    "Locale API",
                    "Transport or app-layer package",
                ),
            ),
        )
    }

    private data class SourceFile(
        val path: Path,
        val packageName: String,
        val declarations: Set<String>,
        val imports: List<String>,
        val text: String,
    )

    private data class SourceGraph(
        private val sources: List<SourceFile>,
    ) {
        private val sourcesByPackage: Map<String, List<SourceFile>> = sources.groupBy { source -> source.packageName }
        private val sourcesByFqName: Map<String, SourceFile> =
            sources
                .flatMap { source ->
                    source.declarations.map { declaration -> "${source.packageName}.$declaration" to source }
                }.toMap()

        fun reachableFromPackage(packageName: String): Set<SourceFile> {
            val queue = ArrayDeque<SourceFile>()
            sources
                .filter { source -> source.packageName == packageName }
                .forEach(queue::add)

            val visited = linkedSetOf<SourceFile>()
            while (queue.isNotEmpty()) {
                val source = queue.removeFirst()
                if (!visited.add(source)) continue

                dependenciesOf(source)
                    .filterNot(visited::contains)
                    .forEach(queue::add)
            }
            return visited
        }

        private fun dependenciesOf(source: SourceFile): Set<SourceFile> =
            buildSet {
                source.imports.forEach { imported ->
                    if (imported.endsWith(".*")) {
                        sourcesByPackage[imported.removeSuffix(".*")]?.let(::addAll)
                    } else {
                        sourcesByFqName[imported]?.let(::add)
                    }
                }

                localReferences
                    .findAll(source.text)
                    .mapNotNull { match -> sourceForFqNameReference(match.value) }
                    .forEach(::add)

                sourcesByPackage[source.packageName]
                    .orEmpty()
                    .filterNot { candidate -> candidate.path == source.path }
                    .filter { candidate ->
                        candidate.declarations.any { declaration ->
                            Regex("""\b${Regex.escape(declaration)}\b""").containsMatchIn(source.text)
                        }
                    }.forEach(::add)
            }

        private fun sourceForFqNameReference(reference: String): SourceFile? {
            var candidate = reference
            while (candidate.contains('.')) {
                sourcesByFqName[candidate]?.let { source -> return source }
                candidate = candidate.substringBeforeLast('.')
            }
            return null
        }

        companion object {
            fun load(sourceRoot: Path): SourceGraph {
                Files.walk(sourceRoot).use { paths ->
                    return SourceGraph(
                        paths
                            .filter { path -> Files.isRegularFile(path) && path.toString().endsWith(".kt") }
                            .map(::parseSourceFile)
                            .toList(),
                    )
                }
            }

            private fun parseSourceFile(path: Path): SourceFile {
                val text = Files.readAllBytes(path).toString(Charsets.UTF_8)
                val packageName =
                    packageRegex.find(text)?.groupValues?.get(1)
                        ?: error("Missing package declaration in $path")
                val declarations =
                    declarationRegex
                        .findAll(text)
                        .map { match -> match.groupValues[1] }
                        .toSet()
                val imports =
                    importRegex
                        .findAll(text)
                        .map { match -> match.groupValues[1] }
                        .toList()

                return SourceFile(
                    path = path,
                    packageName = packageName,
                    declarations = declarations,
                    imports = imports,
                    text = text,
                )
            }
        }
    }

    private data class ForbiddenRule(
        val name: String,
        val pattern: Regex,
    )

    private data class ForbiddenMatch(
        val path: Path,
        val lineNumber: Int,
        val line: String,
        val ruleName: String,
    )

    private fun forbiddenMatches(source: SourceFile): List<ForbiddenMatch> =
        source.text
            .lineSequence()
            .flatMapIndexed { index, line ->
                forbiddenRules
                    .asSequence()
                    .filter { rule -> rule.pattern.containsMatchIn(line) }
                    .map { rule ->
                        ForbiddenMatch(
                            path = source.path,
                            lineNumber = index + 1,
                            line = line,
                            ruleName = rule.name,
                        )
                    }
            }.toList()

    private companion object {
        private const val ENGINE_PACKAGE = "com.finnvek.cornersapart.engine"

        private val packageRegex = Regex("""(?m)^package\s+([A-Za-z0-9_.]+)""")
        private val importRegex = Regex("""(?m)^import\s+([A-Za-z0-9_.]+(?:\.\*)?)""")
        private val declarationRegex =
            Regex(
                """(?m)^\s*(?:data\s+)?(?:sealed\s+)?(?:class|interface|object|enum\s+class)\s+([A-Za-z_][A-Za-z0-9_]*)""",
            )
        private val localReferences = Regex("""com\.finnvek\.cornersapart(?:\.[A-Za-z_][A-Za-z0-9_]*)+""")
        private val wallClockOrTimeZoneRegex =
            Regex(
                listOf(
                    """\bSystem\.(?:currentTimeMillis|nanoTime)\s*\(""",
                    """\bjava\.time\.""",
                    """\bkotlin\.time\.""",
                    """\b(?:Clock|Instant|LocalDate|ZoneId|Calendar)\b""",
                    """\bDate\s*\(""",
                ).joinToString(separator = "|"),
            )
        private val transportOrAppLayerRegex =
            Regex(
                listOf(
                    """\bcom\.finnvek\.cornersapart\.(?:data|ui|viewmodel|multiplayer|opponents)\.""",
                    """\b(?:GameProtocol|GameMessage|Connections|Payload|Endpoint|Nearby|Bluetooth|Wifi|WiFi|Transport)\b""",
                ).joinToString(separator = "|"),
            )

        private val forbiddenRules =
            listOf(
                ForbiddenRule("Android framework", Regex("""\bandroid\.""")),
                ForbiddenRule("Compose", Regex("""\bandroidx\.compose\.|\b@Composable\b|\bComposable\b""")),
                ForbiddenRule("DataStore", Regex("""\bandroidx\.datastore\.|\bDataStore\b""")),
                ForbiddenRule("Hilt or DI framework", Regex("""\bdagger\.|\bjavax\.inject\.|\b@Hilt|\b@Inject\b""")),
                ForbiddenRule("Play Services", Regex("""\bcom\.google\.android\.gms\.""")),
                ForbiddenRule(
                    "Wall-clock or time-zone API",
                    wallClockOrTimeZoneRegex,
                ),
                ForbiddenRule("Locale API", Regex("""\bjava\.util\.Locale\b|\bLocale\b""")),
                ForbiddenRule(
                    "Transport or app-layer package",
                    transportOrAppLayerRegex,
                ),
                ForbiddenRule(
                    "Test-only helper",
                    Regex("""\b(?:TestFixtures|ScoreFixtures|MainDispatcherRule|InMemoryJsonStateStore)\b"""),
                ),
            )
    }
}
