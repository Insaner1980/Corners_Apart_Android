package com.finnvek.cornersapart.data

import kotlinx.coroutines.flow.Flow

interface JsonStateStore<T> {
    val data: Flow<T>

    suspend fun update(transform: suspend (T) -> T): T
}
