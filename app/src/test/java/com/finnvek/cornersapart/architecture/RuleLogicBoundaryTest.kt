package com.finnvek.cornersapart.architecture

import com.finnvek.cornersapart.projectRoot
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

class RuleLogicBoundaryTest {
    @Test
    fun opponentsDoNotDeriveBonusClaimsFromScoreMath() {
        val root = projectRoot()
        val offenders =
            kotlinSources(root.resolve("app/src/main/java/com/finnvek/cornersapart/opponents"))
                .filter { path -> path.toFile().readText().contains("bonusTilePoints /") }
                .map { path -> root.relativize(path).toString() }

        assertTrue(
            "Opponent code must consume engine-provided bonus claim counts instead of reversing Scoring.scoreMove: " +
                offenders.joinToString(),
            offenders.isEmpty(),
        )
    }

    private fun kotlinSources(root: Path): List<Path> {
        val stream = Files.walk(root)
        return try {
            stream
                .filter { path -> Files.isRegularFile(path) && path.toString().endsWith(".kt") }
                .collect(Collectors.toList())
        } finally {
            stream.close()
        }
    }
}
