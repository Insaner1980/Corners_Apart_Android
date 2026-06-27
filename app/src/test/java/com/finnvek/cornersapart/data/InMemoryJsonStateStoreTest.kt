package com.finnvek.cornersapart.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryJsonStateStoreTest {
    @Test
    fun concurrentUpdatesApplyEveryTransformAtomically() =
        runTest {
            val store = InMemoryJsonStateStore(0)

            List(10) {
                async {
                    store.update { current ->
                        yield()
                        current + 1
                    }
                }
            }.awaitAll()

            assertEquals(10, store.data.first())
        }
}
