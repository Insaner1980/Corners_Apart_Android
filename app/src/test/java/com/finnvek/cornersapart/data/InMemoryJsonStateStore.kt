package com.finnvek.cornersapart.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryJsonStateStore<T>(
    initialValue: T,
) : JsonStateStore<T> {
    private val state = MutableStateFlow(initialValue)
    private val updateMutex = Mutex()

    override val data: Flow<T> = state

    override suspend fun update(transform: suspend (T) -> T): T =
        updateMutex.withLock {
            val next = transform(state.value)
            state.value = next
            next
        }
}
