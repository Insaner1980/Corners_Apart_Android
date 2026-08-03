package com.finnvek.cornersapart.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRuntimeModuleTest {
    @Test
    fun providersCreateJvmRuntimeCollaborators() {
        val engine = GameRuntimeModule.provideGameEngine()
        val opponentEngine = GameRuntimeModule.provideComputerOpponentEngine(engine)
        val replayer = GameRuntimeModule.provideGameReplayer(engine)
        val analyzer = GameRuntimeModule.provideMatchReviewAnalyzer(engine, replayer)
        val timeProvider = GameRuntimeModule.provideTimeProvider()

        assertNotNull(engine)
        assertNotNull(opponentEngine)
        assertNotNull(replayer)
        assertNotNull(analyzer)
        assertTrue(timeProvider.nowEpochMillis() > 0L)
    }
}
