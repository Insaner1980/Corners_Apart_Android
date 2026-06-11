package com.finnvek.cornersapart.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryJsonStateStore<T>(
    initialValue: T,
) : JsonStateStore<T> {
    private val state = MutableStateFlow(initialValue)

    override val data: Flow<T> = state

    override suspend fun update(transform: suspend (T) -> T): T {
        val next = transform(state.value)
        state.value = next
        return next
    }
}
