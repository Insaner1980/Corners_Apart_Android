package com.finnvek.cornersapart.architecture

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class PackageDependencyBoundaryTest {
    @Test
    fun productionPackageGraphHasNoCyclesOrConfirmedUpwardEdges() {
        val graph = PackageGraph.load(projectRoot().resolve("app/src/main/java"))
        val cycles = graph.cycles()
        val forbiddenEdges =
            graph.edges.filter { edge ->
                edge in forbiddenPackageEdges
            }

        assertTrue(
            buildString {
                if (cycles.isNotEmpty()) {
                    appendLine("Package dependency cycles:")
                    cycles.forEach { cycle -> appendLine(cycle.joinToString(" -> ")) }
                }
                if (forbiddenEdges.isNotEmpty()) {
                    appendLine("Forbidden package edges:")
                    forbiddenEdges.forEach { edge -> appendLine("${edge.from} -> ${edge.to}") }
                }
            },
            cycles.isEmpty() && forbiddenEdges.isEmpty(),
        )
    }

    private data class PackageEdge(
        val from: String,
        val to: String,
    )

    private data class PackageGraph(
        val edges: Set<PackageEdge>,
    ) {
        fun cycles(): List<List<String>> =
            edges
                .map { edge -> edge.from }
                .toSet()
                .flatMap { start -> cyclesFrom(start) }
                .distinctBy { cycle -> cycle.canonicalKey() }

        private fun cyclesFrom(start: String): List<List<String>> {
            val found = mutableListOf<List<String>>()

            fun visit(
                current: String,
                path: List<String>,
            ) {
                edges
                    .filter { edge -> edge.from == current }
                    .forEach { edge ->
                        when {
                            edge.to == start -> found += path + start
                            edge.to !in path -> visit(edge.to, path + edge.to)
                        }
                    }
            }

            visit(start, listOf(start))
            return found
        }

        private fun List<String>.canonicalKey(): String {
            val cycle = dropLast(1)
            val rotations = cycle.indices.map { index -> cycle.drop(index) + cycle.take(index) }
            return rotations.minOf { rotation -> rotation.joinToString("->") }
        }

        companion object {
            fun load(sourceRoot: Path): PackageGraph {
                Files.walk(sourceRoot).use { paths ->
                    return PackageGraph(
                        paths
                            .filter { path -> Files.isRegularFile(path) && path.toString().endsWith(".kt") }
                            .flatMap { path -> sourceEdges(path).stream() }
                            .toList()
                            .toSet(),
                    )
                }
            }

            private fun sourceEdges(path: Path): List<PackageEdge> {
                val text = path.toFile().readText()
                val packageName = packageRegex.find(text)?.groupValues?.get(1) ?: return emptyList()
                val from = packageName.topLevelAppPackage() ?: return emptyList()
                return importRegex
                    .findAll(text)
                    .mapNotNull { match ->
                        val to = match.groupValues[1].topLevelAppPackage()
                        to
                            ?.takeUnless { target -> target == from }
                            ?.let { target -> PackageEdge(from = from, to = target) }
                    }.toList()
            }

            private fun String.topLevelAppPackage(): String? =
                removePrefix("$APP_PACKAGE.")
                    .takeIf { packageName -> packageName != this }
                    ?.substringBefore('.')
                    ?.takeIf(String::isNotBlank)
        }
    }

    private companion object {
        private const val APP_PACKAGE = "com.finnvek.cornersapart"
        private val packageRegex = Regex("""(?m)^package\s+([A-Za-z0-9_.]+)""")
        private val importRegex = Regex("""(?m)^import\s+(com\.finnvek\.cornersapart\.[A-Za-z0-9_.]+)""")
        private val forbiddenPackageEdges =
            setOf(
                PackageEdge(from = "model", to = "engine"),
                PackageEdge(from = "data", to = "viewmodel"),
                PackageEdge(from = "multiplayer", to = "ui"),
            )
    }
}
