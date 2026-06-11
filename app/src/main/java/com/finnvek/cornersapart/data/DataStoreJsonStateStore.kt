package com.finnvek.cornersapart.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

class DataStoreJsonStateStore<T>(
    private val dataStore: DataStore<T>,
) : JsonStateStore<T> {
    override val data: Flow<T> = dataStore.data

    override suspend fun update(transform: suspend (T) -> T): T = dataStore.updateData(transform)
}
