package com.finnvek.cornersapart.viewmodel

import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

interface TimeProvider {
    fun nowEpochMillis(): Long

    fun todayIsoDate(): String
}

class SystemTimeProvider
    @Inject
    constructor() : TimeProvider {
        override fun nowEpochMillis(): Long = System.currentTimeMillis()

        override fun todayIsoDate(): String =
            Instant
                .ofEpochMilli(nowEpochMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
    }
