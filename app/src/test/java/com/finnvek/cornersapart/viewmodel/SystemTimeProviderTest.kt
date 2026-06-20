package com.finnvek.cornersapart.viewmodel

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemTimeProviderTest {
    @Test
    fun returnsEpochMillisAndIsoDate() {
        val provider = SystemTimeProvider()

        assertTrue(provider.nowEpochMillis() > 0L)
        assertTrue(LocalDate.parse(provider.todayIsoDate()) <= LocalDate.now())
    }
}
